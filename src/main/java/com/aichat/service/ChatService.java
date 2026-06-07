package com.aichat.service;

import com.aichat.config.GroupChatConfig;
import com.aichat.dto.ChatRequest;
import com.aichat.dto.GroupChatRequest;
import com.aichat.entity.Character;
import com.aichat.entity.Conversation;
import com.aichat.entity.Message;
import com.aichat.mapper.ConversationMapper;
import com.aichat.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

	private final ConversationMapper conversationMapper;
	private final MessageMapper messageMapper;
	private final LLMService llmService;
	private final CharacterService characterService;
	private final JdbcTemplate jdbcTemplate;
	private final GroupChatConfig groupChatConfig;
	private final GateControlService gateControlService;
	private final InnerMonologueService innerMonologueService;

	@Transactional
	public SseEmitter handleChat(ChatRequest request) {
		logger.info("========== 开始处理聊天请求 ==========");
		logger.info("用户消息: {}", request.getMessage());

		Long conversationId = request.getConversationId();
		if (conversationId == null) {
			logger.info("创建新会话");
			Conversation conv = new Conversation();
			conv.setTitle(request.getMessage().substring(0, Math.min(20, request.getMessage().length())));
			conversationMapper.insert(conv);
			conversationId = conv.getId();
			logger.info("新会话ID: {}", conversationId);
		}

		logger.info("保存用户消息");
		Message userMsg = new Message();
		userMsg.setConversationId(conversationId);
		userMsg.setRole("user");
		userMsg.setContent(request.getMessage());
		messageMapper.insert(userMsg);

		List<Message> history = messageMapper.selectList(
				new LambdaQueryWrapper<Message>()
						.eq(Message::getConversationId, conversationId)
						.orderByAsc(Message::getCreatedAt)
		);
		logger.info("历史消息数量: {}", history.size());

		SseEmitter emitter = new SseEmitter(300000L);
		logger.info("SSE emitter 创建成功");

		StringBuilder fullReply = new StringBuilder();
		final Long finalConversationId = conversationId;

		// 获取角色信息
		Character character = request.getCharacterId() != null ? characterService.getCharacterById(request.getCharacterId()) : null;
		if (character != null) {
			logger.info("已获取角色信息，角色: {}", character.getName());
		} else if (request.getCharacterId() != null) {
			logger.warn("未找到角色ID: {}", request.getCharacterId());
		}

		CompletableFuture.runAsync(() -> {
			logger.info("========== 异步任务开始 ==========");
			try {
				logger.info("开始调用 LLMService.streamReply");
				llmService.streamReply(history, request.getMessage(), character, token -> {
					logger.debug("准备发送 token: {}", token);
					try {
						emitter.send(SseEmitter.event().data(token));
						fullReply.append(token);
						logger.debug("token 发送成功");
					} catch (IOException e) {
						logger.error("发送 SSE 事件失败: {}", e.getMessage(), e);
						try {
							emitter.completeWithError(e);
						} catch (Exception ex) {
							logger.error("完成 emitter 失败: {}", ex.getMessage(), ex);
						}
						throw new RuntimeException(e);
					}
				});

				logger.info("LLM 调用完成，准备发送 [DONE]");
				emitter.send(SseEmitter.event().data("[DONE]"));
				logger.info("[DONE] 发送成功");

				logger.info("保存 AI 回复，内容长度: {}", fullReply.length());
				Message assistantMsg = new Message();
				assistantMsg.setConversationId(finalConversationId);
				assistantMsg.setRole("assistant");
				assistantMsg.setContent(fullReply.toString());
				messageMapper.insert(assistantMsg);

				logger.info("完成 SSE emitter");
				emitter.complete();
			} catch (Exception e) {
				logger.error("处理聊天时发生错误: ", e);
				try {
					emitter.send(SseEmitter.event().data("\n[错误: " + e.getMessage() + "]\n"));
					emitter.completeWithError(e);
				} catch (Exception ex) {
					logger.error("发送错误消息失败: ", ex);
				}
			}
			logger.info("========== 异步任务结束 ==========");
		});

		logger.info("返回 SSE emitter");
		return emitter;
	}

	@Transactional
	public Map<String, Object> handleGroupChat(GroupChatRequest request) {
		logger.info("========== [群聊升级] 开始处理 ==========");
		logger.info("[群聊] 用户消息: {}", request.getMessage());
		logger.info("[群聊] 群聊ID: {}", request.getGroupId());
		logger.info("[群聊] 角色列表: {}", request.getCharacterIds());

		Long groupId = request.getGroupId();
		String userMessage = request.getMessage();
		List<Long> characterIds = request.getCharacterIds();

		List<Map<String, Object>> responses = new ArrayList<>();

		// 1. 保存用户消息
		try {
			jdbcTemplate.update(
				"INSERT INTO message (group_id, role, sender_name, content, created_at) VALUES (?, ?, ?, ?, ?)",
				groupId, "user", "我", userMessage, java.time.LocalDateTime.now()
			);
			logger.info("[群聊] 用户消息已保存");
		} catch (Exception e) {
			logger.error("[群聊] 保存用户消息失败: ", e);
		}

		// 2. 获取完整群聊历史
		List<Message> fullGroupHistory = new ArrayList<>();
		try {
			fullGroupHistory = messageMapper.selectList(
				new LambdaQueryWrapper<Message>()
					.eq(Message::getGroupId, groupId)
					.orderByAsc(Message::getCreatedAt)
			);
			logger.info("[群聊] 获取历史消息 {} 条", fullGroupHistory.size());
		} catch (Exception e) {
			logger.error("[群聊] 获取历史消息失败: ", e);
		}

		// 3. 获取最近N条消息用于发言门控判断
		int historyCount = groupChatConfig.getGate().getHistoryCount();
		List<Message> recentMessages = fullGroupHistory.stream()
			.skip(Math.max(0, fullGroupHistory.size() - historyCount))
			.collect(java.util.stream.Collectors.toList());

		// 4. 发言门控：判断哪些角色应该回复
		List<Long> shouldReplyCharacterIds = new ArrayList<>();
		for (Long characterId : characterIds) {
			try {
				Character character = characterService.getCharacterById(characterId);
				if (character == null) {
					logger.warn("[群聊] 角色不存在: {}", characterId);
					continue;
				}

				// 获取该角色最近的回复
				List<Message> recentReplies = gateControlService.getRecentReplies(
					characterId, 
					fullGroupHistory, 
					groupChatConfig.getGate().getTimeWindowMinutes(),
					groupChatConfig.getGate().getMaxRepliesInWindow()
				);

				// 发言门控判断
				boolean shouldReply = gateControlService.shouldReply(
					character, 
					recentMessages, 
					userMessage, 
					recentReplies
				);

				if (shouldReply) {
					shouldReplyCharacterIds.add(characterId);
					logger.info("[发言门控] 角色 {} 决定回复", character.getName());
				} else {
					logger.info("[发言门控] 角色 {} 决定沉默", character.getName());
				}
			} catch (Exception e) {
				logger.error("[发言门控] 判断失败，默认允许回复: {}", e.getMessage(), e);
				shouldReplyCharacterIds.add(characterId); // 失败时默认允许
			}
		}

		logger.info("[群聊] 通过门控的角色数量: {}", shouldReplyCharacterIds.size());

		// 5. 为通过门控的角色生成回复，添加时序控制
		// 按随机顺序处理，模拟人类打字速度
		Collections.shuffle(shouldReplyCharacterIds);
		
		// 第一个角色立即回复，后续角色添加延迟
		for (int i = 0; i < shouldReplyCharacterIds.size(); i++) {
			Long characterId = shouldReplyCharacterIds.get(i);
			
			try {
				Character character = characterService.getCharacterById(characterId);
				if (character == null) {
					continue;
				}

				logger.info("[群聊] 为角色 {} 生成回复 (顺序: {})", character.getName(), i + 1);
				
				// 第一个角色不延迟，后续角色延迟1-4秒（减少等待时间）
				if (i > 0) {
					int delaySeconds = 1 + (int)(Math.random() * 4);
					logger.info("[时序控制] 角色 {} 延迟 {} 秒后回复", character.getName(), delaySeconds);
					try {
						Thread.sleep(delaySeconds * 1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.warn("[时序控制] 延迟被中断");
					}
				}

				// 重新获取最新历史（包含前面角色的回复）
				List<Message> currentHistory = messageMapper.selectList(
					new LambdaQueryWrapper<Message>()
						.eq(Message::getGroupId, groupId)
						.orderByAsc(Message::getCreatedAt)
				);

				// 生成回复
				String reply;
				try {
					reply = llmService.generateGroupReply(currentHistory, userMessage, character, characterIds);
				} catch (Exception e) {
					logger.error("[群聊] LLM 调用失败，使用模拟回复: ", e);
					reply = "收到你的消息了！";
				}
				
				// 清理回复中的所有角色名称前缀（循环清理直到没有前缀为止）
				String cleanReply = reply.trim();
				
				// 获取所有角色名称用于清理
				List<Character> allCharacters = characterService.getAllCharacters();
				List<String> allNamePrefixes = new ArrayList<>();
				for (Character c : allCharacters) {
					allNamePrefixes.add(c.getName() + ":");
					allNamePrefixes.add(c.getName() + "：");
				}
				
				// 循环清理所有角色名称前缀
				boolean changed = true;
				while (changed) {
					changed = false;
					for (String prefix : allNamePrefixes) {
						if (cleanReply.startsWith(prefix)) {
							cleanReply = cleanReply.substring(prefix.length()).trim();
							changed = true;
							break; // 重新开始检查
						}
					}
				}
				
				// 保存回复到数据库
				try {
					jdbcTemplate.update(
						"INSERT INTO message (group_id, role, sender_id, sender_name, content, created_at) VALUES (?, ?, ?, ?, ?, ?)",
						groupId, "assistant", characterId, character.getName(), cleanReply, java.time.LocalDateTime.now()
					);
					logger.info("[群聊] 角色 {} 回复已保存", character.getName());
				} catch (Exception e) {
					logger.error("[群聊] 保存角色回复失败: ", e);
				}

				// 生成内心独白（不发送到群里）
				try {
					String monologue = innerMonologueService.generateInnerMonologue(
						character, currentHistory, userMessage
					);
					if (monologue != null && !monologue.isEmpty()) {
						innerMonologueService.saveMonologue(characterId, groupId, monologue, null);
						logger.debug("[内心独白] 角色 {} 独白已生成", character.getName());
					}
				} catch (Exception e) {
					logger.error("[内心独白] 生成失败: {}", e.getMessage(), e);
				}

				// 构建响应
				Map<String, Object> response = new HashMap<>();
				response.put("characterId", characterId);
				response.put("characterName", character.getName());
				response.put("content", cleanReply);
				responses.add(response);

			} catch (Exception e) {
				logger.error("[群聊] 处理角色 {} 异常: ", characterId, e);
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("messages", responses);

		logger.info("[群聊] ========== 处理完成 ==========");
		return result;
	}

	public List<Message> getGroupMessages(Long groupId) {
		logger.info("[群聊] 获取群聊 {} 的历史消息", groupId);
		List<Message> messages = messageMapper.selectList(
			new LambdaQueryWrapper<Message>()
				.eq(Message::getGroupId, groupId)
				.orderByAsc(Message::getCreatedAt)
		);
		logger.info("[群聊] 找到 {} 条群聊消息", messages.size());
		return messages;
	}

	public List<Message> getMessagesByConversationId(Long conversationId) {
		logger.info("获取会话 {} 的消息", conversationId);
		return messageMapper.selectList(
			new LambdaQueryWrapper<Message>()
				.eq(Message::getConversationId, conversationId)
				.orderByAsc(Message::getCreatedAt)
		);
	}

	public List<Conversation> getConversationsByCharacter(Long characterId) {
		logger.info("获取角色 {} 的会话列表", characterId);
		return conversationMapper.selectList(
			new LambdaQueryWrapper<Conversation>()
				.eq(Conversation::getCharacterId, characterId)
				.orderByDesc(Conversation::getUpdatedAt)
		);
	}
}