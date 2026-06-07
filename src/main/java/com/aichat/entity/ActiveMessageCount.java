package com.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI主动消息计数实体
 */
@Data
@TableName("ai_active_message_count")
public class ActiveMessageCount {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 角色ID
    private Long characterId;
    
    // 日期
    private String date;
    
    // 当日已发送的主动消息数量
    private int count;
    
    // 最后更新时间
    private LocalDateTime updatedAt;
}