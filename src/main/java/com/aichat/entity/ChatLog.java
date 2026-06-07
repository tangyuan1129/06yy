package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_log")
public class ChatLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String ipAddress;
    private String userMessage;
    private String aiResponse;
    private Long characterId;
    private Long groupId;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
}