package com.aichat.controller;

import com.aichat.entity.Conversation;
import com.aichat.mapper.CharacterMapper;
import com.aichat.mapper.ConversationMapper;
import com.aichat.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cleanup")
@RequiredArgsConstructor
public class DatabaseCleanupController {

    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final CharacterMapper characterMapper;

    @DeleteMapping("/character/{characterId}")
    @Transactional
    public Map<String, Object> cleanupCharacterData(@PathVariable Long characterId) {
        try {
            log.info("开始清空角色 {} 的所有数据...", characterId);

            // 1. 查询该角色的所有会话
            List<Conversation> conversations = conversationMapper.selectList(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getCharacterId, characterId)
            );
            
            int messageCount = 0;
            // 2. 删除每个会话的所有消息
            for (Conversation conv : conversations) {
                int count = messageMapper.delete(
                        new LambdaQueryWrapper<com.aichat.entity.Message>()
                                .eq(com.aichat.entity.Message::getConversationId, conv.getId())
                );
                messageCount += count;
            }
            
            // 3. 删除该角色的所有会话
            int conversationCount = conversationMapper.delete(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getCharacterId, characterId)
            );

            log.info("清空角色 {} 数据完成：删除了 {} 条消息，{} 个会话", characterId, messageCount, conversationCount);

            return Map.of(
                "success", true,
                "message", "角色数据已清空",
                "deletedMessages", messageCount,
                "deletedConversations", conversationCount
            );
        } catch (Exception e) {
            log.error("清空角色数据失败", e);
            return Map.of(
                "success", false,
                "message", "清空失败: " + e.getMessage()
            );
        }
    }

    @DeleteMapping("/all")
    @Transactional
    public Map<String, Object> cleanupAllData() {
        try {
            log.info("开始清空所有数据...");

            // 1. 删除所有消息
            int messageCount = messageMapper.delete(null);
            log.info("删除了 {} 条消息", messageCount);

            // 2. 删除所有会话
            int conversationCount = conversationMapper.delete(null);
            log.info("删除了 {} 个会话", conversationCount);

            // 3. 删除所有角色
            int characterCount = characterMapper.delete(null);
            log.info("删除了 {} 个角色", characterCount);

            log.info("所有数据已清空");

            return Map.of(
                "success", true,
                "message", "所有数据已清空",
                "deletedMessages", messageCount,
                "deletedConversations", conversationCount,
                "deletedCharacters", characterCount
            );
        } catch (Exception e) {
            log.error("清空数据失败", e);
            return Map.of(
                "success", false,
                "message", "清空失败: " + e.getMessage()
            );
        }
    }
}