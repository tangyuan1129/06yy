package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
	private Long id;
	private String title;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}