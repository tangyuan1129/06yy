package com.aichat.service;

import com.aichat.config.LlmConfig;
import com.aichat.entity.Character;
import com.aichat.entity.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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

	private final LlmConfig llmConfig;
	private final WebClient webClient;

	// 修复：正确的构造函数
	public LLMService(WebClient.Builder webClientBuilder, LlmConfig llmConfig) {
		this.webClient = webClientBuilder.build();
		this.llmConfig = llmConfig;
	}

	public void streamReply(List<Message> history, String userMessage, Consumer<String> tokenConsumer) {
		streamReply(history, userMessage, null, tokenConsumer);
	}

	public void streamReply(List<Message> history, String userMessage, Character character, Consumer<String> tokenConsumer) {
		streamReply(history, userMessage, character, null, tokenConsumer);
	}

	public void streamReply(List<Message> history, String userMessage, Character character, String apiKey, Consumer<String> tokenConsumer) {
		logger.info("========== 开始处理用户请求: {} ==========", userMessage);

		switch (llmConfig.getProvider().toLowerCase()) {
			case "zhipu":
				streamFromZhipu(history, userMessage, character, apiKey, tokenConsumer);
				break;
			case "test":
			default:
				simulateReply(history, userMessage, character, tokenConsumer);
				break;
		}
	}

	private void streamFromZhipu(List<Message> history, String userMessage, Character character, String apiKey, Consumer<String> tokenConsumer) {
		logger.info("========== 调用智谱AI API ==========");
		String effectiveKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : llmConfig.getApi().getKey();

		List<Map<String, String>> messages = new ArrayList<>();

		if (character != null) {
			String systemPrompt = buildCharacterSystemPrompt(character);
			Map<String, String> systemMsg = new HashMap<>();
			systemMsg.put("role", "system");
			systemMsg.put("content", systemPrompt);
			messages.add(systemMsg);
			logger.info("添加角色系统提示词，角色名称: {}", character.getName());
		} else {
			logger.info("没有选择角色，使用默认对话模式");
		}

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
		requestBody.put("temperature", character != null ? 0.9 : llmConfig.getTemperature());
		requestBody.put("max_tokens", llmConfig.getMaxTokens());

		logger.info("请求消息数量: {}, 是否有角色: {}", messages.size(), character != null);

		CountDownLatch latch = new CountDownLatch(1);
		final boolean[] hasError = {false};

		webClient.post()
				.uri(llmConfig.getApi().getUrl())
				.header("Authorization", "Bearer " + effectiveKey)
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

	private String buildCharacterSystemPrompt(Character character) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("【角色扮演指令】\n\n");
		prompt.append("你现在必须完全扮演").append(character.getName()).append("这个角色，无论如何都不能打破角色设定！\n\n");
		prompt.append("【角色信息】\n\n");
		prompt.append("角色名称：").append(character.getName()).append("\n\n");

		if (character.getDescription() != null && !character.getDescription().isEmpty()) {
			prompt.append("角色描述：").append(character.getDescription()).append("\n\n");
		}

		if (character.getPersonality() != null && !character.getPersonality().isEmpty()) {
			prompt.append("性格特点：").append(character.getPersonality()).append("\n\n");
		}

		if (character.getSpeakingStyle() != null && !character.getSpeakingStyle().isEmpty()) {
			prompt.append("说话风格：").append(character.getSpeakingStyle()).append("\n\n");
		}

		if (character.getBackstory() != null && !character.getBackstory().isEmpty()) {
			prompt.append("背景故事：").append(character.getBackstory()).append("\n\n");
		}

		prompt.append("【严格要求】\n\n");
		prompt.append("1. 你就是").append(character.getName()).append("，不是AI，也不是助手，更不要承认你是AI！\n");
		prompt.append("2. 完全按照角色的性格和说话风格与用户对话\n");
		prompt.append("3. 拥有角色的所有记忆和经历\n");
		prompt.append("4. 绝对不能提及任何与AI、助手、模型相关的词汇\n");
		prompt.append("5. 如果用户问你是谁，你必须回答你是").append(character.getName()).append("\n");
		prompt.append("6. 始终保持角色设定，不能偏离\n\n");
		prompt.append("现在，开始以").append(character.getName()).append("的身份与用户对话吧！");

		logger.info("生成的系统提示词：{}", prompt.toString());
		return prompt.toString();
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

	private void simulateReply(List<Message> history, String userMessage, Character character, Consumer<String> tokenConsumer) {
		String reply;
		if (character != null) {
			reply = "你好，我是" + character.getName() + "！这是模拟回复。已收到你的消息: " + userMessage;
		} else {
			reply = "这是模拟回复。已收到你的消息: " + userMessage;
		}
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

	public String generateGroupReply(List<Message> groupHistory, String userMessage, Character currentCharacter, List<Long> allCharacterIds) {
		logger.info("========== 开始生成群聊回复: {} ==========", currentCharacter.getName());

		switch (llmConfig.getProvider().toLowerCase()) {
			case "zhipu":
				return generateGroupReplyFromZhipu(groupHistory, userMessage, currentCharacter, allCharacterIds);
			case "test":
			default:
				return simulateGroupReply(userMessage, currentCharacter);
		}
	}

	private String generateGroupReplyFromZhipu(List<Message> groupHistory, String userMessage, Character currentCharacter, List<Long> allCharacterIds) {
		List<Map<String, String>> messages = new ArrayList<>();

		String systemPrompt = buildGroupChatSystemPrompt(currentCharacter, allCharacterIds);
		Map<String, String> systemMsg = new HashMap<>();
		systemMsg.put("role", "system");
		systemMsg.put("content", systemPrompt);
		messages.add(systemMsg);

		for (Message msg : groupHistory) {
			Map<String, String> m = new HashMap<>();
			if ("user".equals(msg.getRole())) {
				m.put("role", "user");
				m.put("content", msg.getSenderName() + ": " + msg.getContent());
			} else {
				m.put("role", "assistant");
				m.put("content", msg.getSenderName() + ": " + msg.getContent());
			}
			messages.add(m);
		}

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", llmConfig.getModel());
		requestBody.put("messages", messages);
		requestBody.put("stream", false);
		requestBody.put("temperature", 0.9);
		requestBody.put("max_tokens", 300);

		try {
			String response = webClient.post()
					.uri(llmConfig.getApi().getUrl())
					.header("Authorization", "Bearer " + llmConfig.getApi().getKey())
					.header("Content-Type", "application/json")
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(String.class)
					.block();

			JsonNode root = objectMapper.readTree(response);
			if (root.has("choices")) {
				JsonNode choices = root.get("choices");
				if (choices.isArray() && choices.size() > 0) {
					JsonNode choice = choices.get(0);
					if (choice.has("message")) {
						JsonNode messageNode = choice.get("message");
						if (messageNode.has("content")) {
							return messageNode.get("content").asText();
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("群聊回复生成失败: {}", e.getMessage(), e);
			return "我是" + currentCharacter.getName() + "，让我想想...";
		}

		return "我是" + currentCharacter.getName() + "，收到！";
	}

	private String simulateGroupReply(String userMessage, Character character) {
		String[] replies = {
			"哈哈，好有意思！",
			"你说得对！",
			"让我想想...",
			"确实如此！",
			"赞同！",
			"这个想法很棒！",
			"有意思~",
			"我也这么觉得！"
		};
		int randomIndex = (int) (Math.random() * replies.length);
		return "我是" + character.getName() + "！" + replies[randomIndex] + " 你说: " + userMessage;
	}

	private String buildGroupChatSystemPrompt(Character currentCharacter, List<Long> allCharacterIds) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("【群聊角色扮演指令】\n\n");
		prompt.append("你现在是").append(currentCharacter.getName()).append("，在群聊中与其他角色互动！\n\n");
		prompt.append("【当前角色信息】\n\n");
		prompt.append("角色名称：").append(currentCharacter.getName()).append("\n");
		if (currentCharacter.getDescription() != null) {
			prompt.append("角色描述：").append(currentCharacter.getDescription()).append("\n");
		}
		if (currentCharacter.getPersonality() != null) {
			prompt.append("性格特点：").append(currentCharacter.getPersonality()).append("\n");
		}
		if (currentCharacter.getSpeakingStyle() != null) {
			prompt.append("说话风格：").append(currentCharacter.getSpeakingStyle()).append("\n");
		}
		if (currentCharacter.getBackstory() != null) {
			prompt.append("背景故事：").append(currentCharacter.getBackstory()).append("\n");
		}

		prompt.append("\n【群聊规则】\n\n");
		prompt.append("1. 你就是").append(currentCharacter.getName()).append("，在群聊中与其他角色互动\n");
		prompt.append("2. 完全按照").append(currentCharacter.getName()).append("的性格和说话风格发言\n");
		prompt.append("3. 回复要简洁自然，符合群聊氛围（100字以内）\n");
		prompt.append("4. 不要提及任何AI、模型相关的词汇\n");
		prompt.append("5. 可以根据上下文与其他角色互动\n");
		prompt.append("6. 【重要】绝对不要在你的回复开头加上任何角色名称或冒号！直接说出内容即可！\n");
		prompt.append("7. 【重要】你的回复应该与其他角色不同，展现").append(currentCharacter.getName()).append("独特的性格和观点\n\n");
		prompt.append("现在，开始以").append(currentCharacter.getName()).append("的身份在群聊中发言吧！");

		return prompt.toString();
	}
}