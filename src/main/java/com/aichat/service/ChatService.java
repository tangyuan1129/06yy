package com.aichat.service;

import com.aichat.dto.ChatRequest;
import com.aichat.entity.Conversation;
import com.aichat.entity.Message;
import com.aichat.mapper.ConversationMapper;
import com.aichat.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

	private final ConversationMapper conversationMapper;
	private final MessageMapper messageMapper;
	private final LLMService llmService;

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

		CompletableFuture.runAsync(() -> {
			logger.info("========== 异步任务开始 ==========");
			try {
				logger.info("开始调用 LLMService.streamReply");
				llmService.streamReply(history, request.getMessage(), token -> {
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
}