package com.aichat.service;

import com.aichat.mapper.ChatLogMapper;
import com.aichat.entity.ChatLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatLogService {

    private final ChatLogMapper chatLogMapper;

    public void logChat(Long userId, String ipAddress, String userMessage, String aiResponse, Long characterId, Long groupId) {
        ChatLog log = new ChatLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setUserMessage(userMessage);
        log.setAiResponse(aiResponse);
        log.setCharacterId(characterId);
        log.setGroupId(groupId);
        chatLogMapper.insert(log);
    }
}