package com.aichat.dto;

import lombok.Data;

@Data
public class ChatRequest {
	private String message;
	private Long conversationId;   // 可为空，为空时新建会话
	private Long characterId;      // 角色ID，可为空
	private Long userId;           // 用户ID，用于配额控制
	private String apiKey;         // 用户API Key，优先使用
}