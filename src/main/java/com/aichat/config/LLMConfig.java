package com.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
@Validated
public class LLMConfig {
	@NotBlank(message = "模型提供商不能为空")
	private String provider = "openai";  // 默认提供者

	private ApiConfig api = new ApiConfig();

	@NotBlank(message = "模型名称不能为空")
	private String model = "gpt-3.5-turbo";

	@NotNull(message = "temperature不能为null")
	private Double temperature = 0.7;

	@NotNull(message = "maxTokens不能为null")
	private Integer maxTokens = 1000;

	@Data
	public static class ApiConfig {
		@NotBlank(message = "API密钥不能为空")
		private String key = "YOUR_API_KEY_HERE";

		@NotBlank(message = "API URL不能为空")
		private String url = "https://api.openai.com/v1/chat/completions";
	}
}