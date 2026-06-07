package com.aichat.service;

import com.aichat.config.GroupChatConfig;
import com.aichat.entity.Character;
import com.aichat.entity.Message;
import com.aichat.mapper.CharacterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Life Tick 服务 - 为每个AI角色实现自主生活节奏
 * 每个AI有自己的tick间隔，根据角色设定决定下一次tick时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifeTickService {

    private final GroupChatConfig config;
    private final CharacterMapper characterMapper;
    private final JdbcTemplate jdbcTemplate;
    private final GateControlService gateControlService;
    private final InnerMonologueService innerMonologueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储每个角色的下一次tick任务
    private final Map<Long, ScheduledFuture<?>> scheduledTicks = new ConcurrentHashMap<>();
    
    // 存储每个角色的tick状态
    private final Map<Long, LifeTickState> tickStates = new ConcurrentHashMap<>();
    
    // 线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * Life Tick状态
     */
    public static class LifeTickState {
        public String currentActivity;
        public String mood;
        public LocalDateTime lastTickTime;
        public int nextTickMinutes;
        public int todayActiveMessageCount = 0;
        public LocalDateTime lastUserActivityTime;
    }

    /**
     * 启动所有角色的Life Tick
     */
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void checkAndScheduleTicks() {
        try {
            List<Character> allCharacters = characterMapper.selectList(null);
            
            for (Character character : allCharacters) {
                if (!scheduledTicks.containsKey(character.getId())) {
                    scheduleInitialTick(character);
                }
            }
        } catch (Exception e) {
            log.error("[Life Tick] 检查调度失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 为角色安排初始tick
     */
    private void scheduleInitialTick(Character character) {
        // 初始间隔随机20-120分钟
        int initialDelay = config.getLifeTick().getMinIntervalMinutes() + 
            (int)(Math.random() * (config.getLifeTick().getMaxIntervalMinutes() - 
            config.getLifeTick().getMinIntervalMinutes()));
        
        scheduleTick(character, initialDelay);
    }

    /**
     * 安排角色的下一次tick
     */
    private void scheduleTick(Character character, int delayMinutes) {
        // 取消之前的任务
        ScheduledFuture<?> oldFuture = scheduledTicks.get(character.getId());
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }
        
        log.info("[Life Tick] 为角色 {} 安排下一次tick，延迟 {} 分钟", character.getName(), delayMinutes);
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                executeTick(character);
            } catch (Exception e) {
                log.error("[Life Tick] 执行失败，角色: {}, 错误: {}", character.getName(), e.getMessage(), e);
                // 自愈心跳：失败后30分钟重试
                scheduleTick(character, 30);
            }
        }, delayMinutes, TimeUnit.MINUTES);
        
        scheduledTicks.put(character.getId(), future);
        
        // 更新状态
        LifeTickState state = tickStates.computeIfAbsent(character.getId(), k -> new LifeTickState());
        state.lastTickTime = LocalDateTime.now();
        state.nextTickMinutes = delayMinutes;
    }

    /**
     * 执行角色的tick
     */
    private void executeTick(Character character) {
        log.info("[Life Tick] 开始执行角色 {} 的tick", character.getName());
        
        try {
            // 获取最近群聊消息
            List<Message> recentMessages = getRecentGroupMessages(20);
            
            // 获取角色状态
            LifeTickState state = tickStates.computeIfAbsent(character.getId(), k -> new LifeTickState());
            
            // 构建Life Tick prompt
            String prompt = buildLifeTickPrompt(character, recentMessages, state);
            
            // 调用大模型生成tick决策
            String decisionJson = callModelForLifeTick(character, prompt);
            
            // 解析决策JSON
            LifeTickDecision decision = parseLifeTickDecision(decisionJson);
            
            // 更新状态
            state.currentActivity = decision.currentActivity;
            state.mood = decision.mood;
            state.nextTickMinutes = decision.nextTickMinutes;
            
            // 如果决定要发送消息
            if (decision.shouldSendMessage) {
                // 检查频率限制和时间限制
                if (canSendActiveMessage(character, state)) {
                    sendActiveMessage(character, decision);
                    state.todayActiveMessageCount++;
                } else {
                    log.info("[Life Tick] 角色 {} 被频率限制或时间限制，不发送消息", character.getName());
                }
            }
            
            // 安排下一次tick
            int nextTick = Math.max(config.getLifeTick().getMinIntervalMinutes(),
                Math.min(config.getLifeTick().getMaxIntervalMinutes(), decision.nextTickMinutes));
            scheduleTick(character, nextTick);
            
        } catch (Exception e) {
            log.error("[Life Tick] 执行tick失败: {}", e.getMessage(), e);
            // 自愈心跳
            scheduleTick(character, 30);
        }
    }

    /**
     * 构建Life Tick prompt
     */
    private String buildLifeTickPrompt(Character character, List<Message> recentMessages, LifeTickState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("【Life Tick 自主决策指令】\n\n");
        prompt.append("你是").append(character.getName()).append("。请根据当前情况做出自主决策。\n\n");
        
        // 角色信息
        prompt.append("【你的角色设定】\n");
        if (character.getDescription() != null) {
            prompt.append("描述：").append(character.getDescription()).append("\n");
        }
        if (character.getPersonality() != null) {
            prompt.append("性格：").append(character.getPersonality()).append("\n");
        }
        if (character.getSpeakingStyle() != null) {
            prompt.append("说话风格：").append(character.getSpeakingStyle()).append("\n");
        }
        prompt.append("\n");
        
        // 当前时间
        prompt.append("【当前时间】\n");
        prompt.append(LocalDateTime.now().toString()).append("\n\n");
        
        // 最近群聊消息
        prompt.append("【最近20条群聊消息】\n");
        for (Message msg : recentMessages) {
            prompt.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }
        prompt.append("\n");
        
        // 上次tick结果
        if (state.currentActivity != null) {
            prompt.append("【上次tick状态】\n");
            prompt.append("当前活动：").append(state.currentActivity).append("\n");
            prompt.append("心情：").append(state.mood).append("\n\n");
        }
        
        // 用户最后活跃时间
        if (state.lastUserActivityTime != null) {
            prompt.append("【用户最后活跃时间】\n");
            prompt.append(state.lastUserActivityTime.toString()).append("\n\n");
        }
        
        // 决策要求
        prompt.append("【决策要求】\n");
        prompt.append("请根据角色设定和当前群聊情况，决定：\n");
        prompt.append("1. 你现在正在做什么（符合角色设定）\n");
        prompt.append("2. 你当前的心情\n");
        prompt.append("3. 是否应该在群里主动发消息\n");
        prompt.append("4. 如果要发消息，消息类型是什么（日常关心/话题分享/续接之前的对话/表达情绪）\n");
        prompt.append("5. 消息内容（严格遵循角色说话风格，100字以内）\n");
        prompt.append("6. 下一次tick的间隔时间（20-120分钟）\n\n");
        
        prompt.append("请以JSON格式返回决策：\n");
        prompt.append("{\n");
        prompt.append("  \"current_activity\": \"...\",\n");
        prompt.append("  \"mood\": \"...\",\n");
        prompt.append("  \"should_send_message\": true/false,\n");
        prompt.append("  \"message_type\": \"...\",\n");
        prompt.append("  \"message_content\": \"...\",\n");
        prompt.append("  \"next_tick_minutes\": 30,\n");
        prompt.append("  \"schedule_update\": \"...\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 调用模型生成Life Tick决策
     */
    private String callModelForLifeTick(Character character, String prompt) {
        // 实际实现时应该调用LLM服务
        // 这里返回一个示例JSON
        return "{\n" +
            "  \"current_activity\": \"在群里和大家聊天\",\n" +
            "  \"mood\": \"愉快\",\n" +
            "  \"should_send_message\": false,\n" +
            "  \"message_type\": \"\",\n" +
            "  \"message_content\": \"\",\n" +
            "  \"next_tick_minutes\": 45,\n" +
            "  \"schedule_update\": \"继续观察群聊动态\"\n" +
            "}";
    }

    /**
     * 解析Life Tick决策JSON
     */
    private LifeTickDecision parseLifeTickDecision(String json) {
        LifeTickDecision decision = new LifeTickDecision();
        
        try {
            // 健壮的JSON提取逻辑
            String cleanJson = extractJsonFromResponse(json);
            JsonNode root = objectMapper.readTree(cleanJson);
            
            decision.currentActivity = root.has("current_activity") ? root.get("current_activity").asText() : "";
            decision.mood = root.has("mood") ? root.get("mood").asText() : "";
            decision.shouldSendMessage = root.has("should_send_message") && root.get("should_send_message").asBoolean();
            decision.messageType = root.has("message_type") ? root.get("message_type").asText() : "";
            decision.messageContent = root.has("message_content") ? root.get("message_content").asText() : "";
            decision.nextTickMinutes = root.has("next_tick_minutes") ? root.get("next_tick_minutes").asInt() : 30;
            
        } catch (Exception e) {
            log.error("[Life Tick] 解析决策JSON失败: {}", e.getMessage(), e);
            // 返回默认决策
            decision.nextTickMinutes = 30;
        }
        
        return decision;
    }

    /**
     * 健壮的JSON提取
     */
    private String extractJsonFromResponse(String response) {
        // 1. 直接尝试parse
        try {
            objectMapper.readTree(response);
            return response;
        } catch (Exception e) {
            // 2. 用正则提取大括号
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}").matcher(response);
            if (matcher.find()) {
                return matcher.group();
            }
            // 3. 从markdown代码块提取
            java.util.regex.Matcher mdMatcher = java.util.regex.Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```").matcher(response);
            if (mdMatcher.find()) {
                return mdMatcher.group(1);
            }
        }
        return response;
    }

    /**
     * 检查是否可以发送主动消息
     */
    private boolean canSendActiveMessage(Character character, LifeTickState state) {
        // 检查时间限制（凌晨1点到早上7点禁止）
        int currentHour = LocalDateTime.now().getHour();
        if (currentHour >= config.getActiveMessage().getQuietStartHour() && 
            currentHour < config.getActiveMessage().getQuietEndHour()) {
            // 检查用户是否刚刚活跃
            if (state.lastUserActivityTime == null || 
                LocalDateTime.now().minusMinutes(10).isAfter(state.lastUserActivityTime)) {
                return false;
            }
        }
        
        // 检查每日频率限制
        if (state.todayActiveMessageCount >= config.getActiveMessage().getDailyMaxCount()) {
            return false;
        }
        
        return true;
    }

    /**
     * 发送主动消息
     */
    private void sendActiveMessage(Character character, LifeTickDecision decision) {
        try {
            jdbcTemplate.update(
                "INSERT INTO message (group_id, role, sender_id, sender_name, content, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                1, "assistant", character.getId(), character.getName(), 
                decision.messageContent, LocalDateTime.now()
            );
            log.info("[Life Tick] 角色 {} 发送主动消息: {}", character.getName(), decision.messageContent);
        } catch (Exception e) {
            log.error("[Life Tick] 发送主动消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取最近的群聊消息
     */
    private List<Message> getRecentGroupMessages(int limit) {
        try {
            return jdbcTemplate.query(
                "SELECT * FROM message WHERE group_id IS NOT NULL ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> {
                    Message msg = new Message();
                    msg.setId(rs.getLong("id"));
                    msg.setGroupId(rs.getLong("group_id"));
                    msg.setRole(rs.getString("role"));
                    msg.setSenderId(rs.getLong("sender_id"));
                    msg.setSenderName(rs.getString("sender_name"));
                    msg.setContent(rs.getString("content"));
                    msg.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return msg;
                },
                limit
            );
        } catch (Exception e) {
            log.error("[Life Tick] 获取群聊消息失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Life Tick决策内部类
     */
    public static class LifeTickDecision {
        public String currentActivity;
        public String mood;
        public boolean shouldSendMessage;
        public String messageType;
        public String messageContent;
        public int nextTickMinutes;
    }
}