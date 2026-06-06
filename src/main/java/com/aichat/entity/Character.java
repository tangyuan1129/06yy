package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_character")
public class Character {
	@TableId(type = IdType.AUTO)
	private Long id;
	private String name;
	private String description;
	private String personality;
	private String speakingStyle;
	private String backstory;
	private String avatarUrl;
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}