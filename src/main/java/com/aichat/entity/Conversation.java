package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long userId;
	private Long characterId;
	private String title;
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}