package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI内心独白实体 - 存储AI的私有想法
 */
@Data
@TableName("ai_inner_monologue")
public class InnerMonologue {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 角色ID
    private Long characterId;
    
    // 群聊ID（可为空，表示私聊时的独白）
    private Long groupId;
    
    // 独白内容
    private String content;
    
    // 触发独白的消息ID
    private Long triggerMessageId;
    
    // 创建时间
    private LocalDateTime createdAt;
}