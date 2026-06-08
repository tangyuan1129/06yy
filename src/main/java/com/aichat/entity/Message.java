package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
	private Long id;
	private Long conversationId;
	private Long groupId;
	private Long userId;
	private Long characterId;
	private String role;        // "user" 或 "assistant"
	private Long senderId;
	private String senderName;
	private String content;
	private LocalDateTime createdAt;
}