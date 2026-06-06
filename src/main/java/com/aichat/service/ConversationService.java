package com.aichat.service;

import com.aichat.entity.Conversation;
import com.aichat.mapper.ConversationMapper;
import com.aichat.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public List<Conversation> getConversationsByCharacterId(Long characterId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getCharacterId, characterId)
                        .orderByDesc(Conversation::getCreatedAt)
        );
    }

    public Conversation getConversationById(Long id) {
        return conversationMapper.selectById(id);
    }

    public Conversation createConversation(Conversation conversation) {
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        // 先删除该会话的所有消息
        messageMapper.delete(
                new LambdaQueryWrapper<com.aichat.entity.Message>()
                        .eq(com.aichat.entity.Message::getConversationId, conversationId)
        );
        // 再删除会话本身
        conversationMapper.deleteById(conversationId);
    }
}