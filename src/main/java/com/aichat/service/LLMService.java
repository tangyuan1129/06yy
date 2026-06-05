package com.aichat.service;

import com.aichat.config.LLMConfig;
import com.aichat.entity.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final LLMConfig llmConfig;
	private final WebClient webClient;

	// 修复：正确的构造函数
	public LLMService(WebClient.Builder webClientBuilder, LLMConfig llmConfig) {
		this.webClient = webClientBuilder.build();
		this.llmConfig = llmConfig;
	}

	public void streamReply(List<Message> history, String userMessage, Consumer<String> tokenConsumer) {
		logger.info("========== 开始处理用户请求: {} ==========", userMessage);

		switch (llmConfig.getProvider().toLowerCase()) {
			case "zhipu":
				streamFromZhipu(history, userMessage, tokenConsumer);
				break;
			case "test":
			default:
				simulateReply(history, userMessage, tokenConsumer);
				break;
		}
	}

	private void streamFromZhipu(List<Message> history, String userMessage, Consumer<String> tokenConsumer) {
		logger.info("========== 调用智谱AI API ==========");

		List<Map<String, String>> messages = new ArrayList<>();
		for (Message msg : history) {
			Map<String, String> m = new HashMap<>();
			m.put("role", msg.getRole());
			m.put("content", msg.getContent());
			messages.add(m);
		}

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", llmConfig.getModel());
		requestBody.put("messages", messages);
		requestBody.put("stream", true);
		requestBody.put("temperature", llmConfig.getTemperature());
		requestBody.put("max_tokens", llmConfig.getMaxTokens());

		logger.info("请求消息数量: {}", messages.size());

		CountDownLatch latch = new CountDownLatch(1);
		final boolean[] hasError = {false};

		webClient.post()
				.uri(llmConfig.getApi().getUrl())
				.header("Authorization", "Bearer " + llmConfig.getApi().getKey())
				.header("Content-Type", "application/json")
				.bodyValue(requestBody)
				.retrieve()
				.bodyToFlux(String.class)
				.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
						.maxBackoff(Duration.ofSeconds(10)))
				.doOnError(error -> {
					logger.error("智谱AI请求失败: {}", error.getMessage());
					tokenConsumer.accept("\n[连接智谱AI失败，请检查网络或API Key]\n");
					hasError[0] = true;
					latch.countDown();
				})
				.subscribe(
						data -> {
							logger.debug("收到智谱响应: {}", data);
							try {
								String jsonStr = data.trim();

								if (jsonStr.startsWith("data: ")) {
									jsonStr = jsonStr.substring(6).trim();
								}

								if (jsonStr.equals("[DONE]")) {
									logger.debug("收到 DONE 信号");
									return;
								}

								if (jsonStr.startsWith("{")) {
									parseZhipuResponse(jsonStr, tokenConsumer);
								}
							} catch (Exception e) {
								logger.warn("解析响应失败: {}", e.getMessage(), e);
							}
						},
						error -> {
							logger.error("智谱AI流式响应错误: ", error);
							if (!hasError[0]) {
								tokenConsumer.accept("\n[AI服务异常]\n");
							}
							latch.countDown();
						},
						() -> {
							logger.info("========== 智谱AI响应完成 ==========");
							latch.countDown();
						}
				);

		try {
			logger.info("等待智谱AI响应...");
			latch.await();
			logger.info("智谱AI响应处理完毕");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("等待被中断");
		}
	}

	private void parseZhipuResponse(String jsonStr, Consumer<String> tokenConsumer) {
		try {
			logger.debug("正在解析JSON: {}", jsonStr);
			JsonNode root = objectMapper.readTree(jsonStr);

			if (root.has("choices")) {
				JsonNode choices = root.get("choices");
				if (choices.isArray() && choices.size() > 0) {
					JsonNode choice = choices.get(0);
					if (choice.has("delta")) {
						JsonNode delta = choice.get("delta");
						if (delta.has("content")) {
							String content = delta.get("content").asText();
							if (!content.isEmpty()) {
								logger.info("提取到token: [{}]", content);
								tokenConsumer.accept(content);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("解析token失败: {}", e.getMessage(), e);
		}
	}

	private void simulateReply(List<Message> history, String userMessage, Consumer<String> tokenConsumer) {
		String reply = "这是模拟回复。已收到你的消息: " + userMessage;
		for (char c : reply.toCharArray()) {
			try {
				tokenConsumer.accept(String.valueOf(c));
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}