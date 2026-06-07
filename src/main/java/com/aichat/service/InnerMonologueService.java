package com.aichat.service;

import com.aichat.config.GroupChatConfig;
import com.aichat.entity.Character;
import com.aichat.entity.InnerMonologue;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内心独白服务 - 为每个AI生成私有内心独白
 * 内心独白不发送到群里，但会影响AI的后续行为和回复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InnerMonologueService {

    private final JdbcTemplate jdbcTemplate;
    private final GroupChatConfig config;

    /**
     * 生成内心独白
     * @param character 角色信息
     * @param recentMessages 最近的群聊消息
     * @param currentMessage 当前消息
     * @return 内心独白内容
     */
    public String generateInnerMonologue(Character character, List<com.aichat.entity.Message> recentMessages, 
                                         String currentMessage) {
        try {
            // 构建生成prompt
            String prompt = buildMonologuePrompt(character, recentMessages, currentMessage);
            
            // 调用模型生成独白
            String monologue = callModelForMonologue(character, prompt);
            
            return monologue;
        } catch (Exception e) {
            log.error("[内心独白] 生成失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建生成内心独白的prompt
     */
    private String buildMonologuePrompt(Character character, List<com.aichat.entity.Message> recentMessages, 
                                        String currentMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("【内心独白生成指令】\n\n");
        prompt.append("你是").append(character.getName()).append("。请根据当前群聊情况，生成一段你的内心独白。\n\n");
        
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
        
        // 最近群聊消息
        prompt.append("【最近群聊消息】\n");
        List<com.aichat.entity.Message> last10 = recentMessages.stream()
            .skip(Math.max(0, recentMessages.size() - 10))
            .collect(java.util.stream.Collectors.toList());
        for (com.aichat.entity.Message msg : last10) {
            prompt.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }
        prompt.append("\n");
        
        // 当前消息
        prompt.append("【当前消息】\n");
        prompt.append(currentMessage).append("\n\n");
        
        // 独白要求
        prompt.append("【独白要求】\n");
        prompt.append("1. 写出你对当前对话的真实想法\n");
        prompt.append("2. 对其他发言者的看法（可以有不认同、顾虑等）\n");
        prompt.append("3. 你没说出口的顾虑或思考\n");
        prompt.append("4. 必须符合你的性格和角色设定\n");
        prompt.append("5. 独白要简洁自然（100字以内）\n");
        prompt.append("6. 不要使用AI、模型等词汇\n\n");
        
        prompt.append("请直接写出你的内心独白：");
        
        return prompt.toString();
    }

    /**
     * 调用模型生成独白
     */
    private String callModelForMonologue(Character character, String prompt) {
        // 这里复用现有的LLM服务调用逻辑
        // 实际实现时应该调用LLM服务
        return "[内心独白：我在思考刚才的对话...]"; // 临时占位
    }

    /**
     * 保存内心独白到数据库
     */
    public void saveMonologue(Long characterId, Long groupId, String content, Long triggerMessageId) {
        try {
            jdbcTemplate.update(
                "INSERT INTO ai_inner_monologue (character_id, group_id, content, trigger_message_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                characterId, groupId, content, triggerMessageId, LocalDateTime.now()
            );
            log.debug("[内心独白] 已保存，角色ID: {}", characterId);
        } catch (Exception e) {
            log.error("[内心独白] 保存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取角色最近的内心独白
     */
    public List<InnerMonologue> getRecentMonologues(Long characterId, int limit) {
        try {
            return jdbcTemplate.query(
                "SELECT * FROM ai_inner_monologue WHERE character_id = ? ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> {
                    InnerMonologue monologue = new InnerMonologue();
                    monologue.setId(rs.getLong("id"));
                    monologue.setCharacterId(rs.getLong("character_id"));
                    monologue.setGroupId(rs.getLong("group_id"));
                    monologue.setContent(rs.getString("content"));
                    monologue.setTriggerMessageId(rs.getLong("trigger_message_id"));
                    monologue.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return monologue;
                },
                characterId, limit
            );
        } catch (Exception e) {
            log.error("[内心独白] 获取失败: {}", e.getMessage(), e);
            return List.of();
        }
    }
}