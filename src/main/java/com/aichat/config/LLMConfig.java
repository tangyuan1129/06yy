package com.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置类
 * 使用 @ConfigurationProperties 替代硬编码
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    private String provider;
    private Api api;
    private String model;
    private Double temperature;
    private Integer maxTokens;

    @Data
    public static class Api {
        private String key;
        private String url;
    }
}