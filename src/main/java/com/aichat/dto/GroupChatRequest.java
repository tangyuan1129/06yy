package com.aichat.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupChatRequest {
	private String message;
	private Long groupId;
	private List<Long> characterIds;
}