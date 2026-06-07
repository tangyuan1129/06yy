package com.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "groupchat")
public class GroupChatConfig {

    // 发言门控配置
    private GateConfig gate = new GateConfig();
    
    // Life Tick 配置
    private LifeTickConfig lifeTick = new LifeTickConfig();
    
    // 主动消息配置
    private ActiveMessageConfig activeMessage = new ActiveMessageConfig();
    
    // 模型路由配置
    private ModelRouteConfig modelRoute = new ModelRouteConfig();

    @Data
    public static class GateConfig {
        // 最近N条群聊历史用于判断
        private int historyCount = 15;
        // 5分钟内最大回复次数
        private int maxRepliesInWindow = 3;
        // 时间窗口（分钟）
        private int timeWindowMinutes = 3;
        // 使用的模型
        private String model = "glm-4-flash";
    }

    @Data
    public static class LifeTickConfig {
        // 最小间隔（分钟）
        private int minIntervalMinutes = 20;
        // 最大间隔（分钟）
        private int maxIntervalMinutes = 120;
        // 使用的模型
        private String model = "glm-4";
        // 最近N条群聊摘要
        private int recentMessageCount = 20;
    }

    @Data
    public static class ActiveMessageConfig {
        // 每日主动消息上限
        private int dailyMaxCount = 3;
        // 禁止发送时间开始（小时）
        private int quietStartHour = 1;
        // 禁止发送时间结束（小时）
        private int quietEndHour = 7;
    }

    @Data
    public static class ModelRouteConfig {
        // 便宜模型（用于发言门控）
        private String cheapModel = "glm-4-flash";
        // 中等模型（用于记忆摘要）
        private String mediumModel = "glm-4-plus";
        // 贵模型（用于最终回复和Life Tick）
        private String expensiveModel = "glm-4";
    }
}