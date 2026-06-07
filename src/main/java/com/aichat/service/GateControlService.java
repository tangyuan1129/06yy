package com.aichat.service;

import com.aichat.config.GroupChatConfig;
import com.aichat.entity.Character;
import com.aichat.entity.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发言门控服务 - 决定每个AI是否应该回复当前消息
 * 使用最便宜的小模型完成YES/NO二分类判断
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GateControlService {

    private final GroupChatConfig config;
    private final WebClient.Builder webClientBuilder;
    private final CharacterService characterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断指定角色是否应该回复
     * 规则：
     * 1. 如果消息中提到特定角色名字，只有被提到的角色回复
     * 2. 如果消息中没有提到任何角色名字，随机选择1-3个角色回复
     */
    public boolean shouldReply(Character character, List<Message> recentMessages, 
                               String currentMessage, List<Message> characterRecentReplies) {
        try {
            // 规则1: 检查消息中是否提到该角色名字
            boolean mentioned = currentMessage.contains("@" + character.getName()) || 
                                currentMessage.contains(character.getName());
            
            if (mentioned) {
                log.info("[发言门控] 角色 {} 被提到，回复", character.getName());
                return true;
            }
            
            // 规则2: 检查消息中是否提到任何其他角色名字
            boolean anyCharacterMentioned = false;
            List<Character> allCharacters = getAllCharacters();
            for (Character c : allCharacters) {
                if (currentMessage.contains(c.getName())) {
                    anyCharacterMentioned = true;
                    break;
                }
            }
            
            // 如果提到了其他角色，当前角色不回复
            if (anyCharacterMentioned) {
                log.info("[发言门控] 角色 {} 未被提到（其他角色被提到），沉默", character.getName());
                return false;
            }
            
            // 规则3: 没有提到任何角色，随机决定是否回复（40%概率）
            boolean randomReply = Math.random() < 0.4;
            if (randomReply) {
                log.info("[发言门控] 角色 {} 随机决定回复", character.getName());
            } else {
                log.info("[发言门控] 角色 {} 随机决定沉默", character.getName());
            }
            return randomReply;
            
        } catch (Exception e) {
            log.error("[发言门控] 判断失败，默认沉默: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取所有角色列表
     */
    private List<Character> getAllCharacters() {
        return characterService.getAllCharacters();
    }

    /**
     * 构建发言门控的prompt
     */
    private String buildGatePrompt(Character character, List<Message> recentMessages, 
                                   String currentMessage, List<Message> characterRecentReplies) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个群聊发言门控判断器。请根据以下信息判断该角色是否应该回复当前消息。\n\n");
        
        // 角色信息
        prompt.append("【当前角色】\n");
        prompt.append("名称：").append(character.getName()).append("\n");
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
        prompt.append("【最近10条群聊消息】\n");
        List<Message> last10 = recentMessages.stream()
            .skip(Math.max(0, recentMessages.size() - 10))
            .collect(Collectors.toList());
        for (Message msg : last10) {
            prompt.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }
        prompt.append("\n");
        
        // 当前消息
        prompt.append("【当前消息】\n");
        prompt.append(currentMessage).append("\n\n");
        
        // 该角色最近的回复
        prompt.append("【该角色最近5分钟内的回复】\n");
        if (characterRecentReplies.isEmpty()) {
            prompt.append("（无）\n\n");
        } else {
            for (Message msg : characterRecentReplies) {
                prompt.append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }
        
        // 判断规则
        prompt.append("【判断规则】\n");
        prompt.append("1. 有人@该角色 → 必须回复(YES)\n");
        prompt.append("2. 话题与角色的兴趣、专业、经历相关 → 回复(YES)\n");
        prompt.append("3. 已有其他人充分回应，且角色没有新的补充观点 → 不回复(NO)\n");
        prompt.append("4. 角色在过去5分钟内已经回复过2次以上 → 克制不回复(NO)\n");
        prompt.append("5. 其他AI在和该角色对话/反驳/提到名字 → 必须回复(YES)\n\n");
        
        prompt.append("请只回答YES或NO，不要有其他内容。");
        
        return prompt.toString();
    }

    /**
     * 调用便宜模型进行判断
     */
    private String callCheapModel(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelRoute().getCheapModel());
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "你是一个群聊发言门控判断器，只回答YES或NO。"),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.1); // 低温度确保判断稳定
            requestBody.put("max_tokens", 10);

            String response = webClientBuilder.build()
                .post()
                .uri("https://open.bigmodel.cn/api/paas/v4/chat/completions")
                .header("Authorization", "Bearer " + System.getenv("ZHIPU_API_KEY"))
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            if (root.has("choices")) {
                return root.get("choices").get(0).get("message").get("content").asText().trim();
            }
            
            return "YES"; // 默认允许
        } catch (Exception e) {
            log.error("[发言门控] 调用模型失败: {}", e.getMessage(), e);
            return "YES"; // 失败时默认允许
        }
    }

    /**
     * 解析判断结果
     */
    private boolean parseGateResult(String result) {
        if (result == null) return true;
        String trimmed = result.trim().toUpperCase();
        return trimmed.contains("YES") || trimmed.startsWith("Y");
    }

    /**
     * 获取角色最近的回复（指定时间窗口内）
     */
    public List<Message> getRecentReplies(Long characterId, List<Message> allMessages, int windowMinutes, int maxCount) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(windowMinutes);
        
        return allMessages.stream()
            .filter(msg -> "assistant".equals(msg.getRole()))
            .filter(msg -> msg.getSenderId() != null && msg.getSenderId().equals(characterId))
            .filter(msg -> msg.getCreatedAt() != null && msg.getCreatedAt().isAfter(cutoff))
            .limit(maxCount + 1) // 多取一条用于判断
            .collect(Collectors.toList());
    }
}