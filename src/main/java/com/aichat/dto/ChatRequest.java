package com.aichat.dto;

import lombok.Data;

@Data
public class ChatRequest {
	private String message;
	private Long conversationId;   // 可为空，为空时新建会话
}