package com.aichat.controller;

import com.aichat.dto.ChatRequest;
import com.aichat.dto.GroupChatRequest;
import com.aichat.entity.Message;
import com.aichat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")	
@RequiredArgsConstructor
@Tag(name = "聊天管理", description = "单聊、群聊、消息历史等接口")
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/test")
	@Operation(summary = "测试接口", description = "检查后端服务状态")
	public Map<String, Object> test() {
		Map<String, Object> result = new HashMap<>();
		result.put("status", "ok");
		result.put("message", "后端服务正常运行");
		return result;
	}

	@PostMapping("/chat/stream")
	@Operation(summary = "单聊（SSE流式）", description = "与单个角色进行流式聊天")
	public SseEmitter chatStream(@RequestBody ChatRequest request) {
		return chatService.handleChat(request);
	}

	@PostMapping("/chat/group")
	@Operation(summary = "群聊", description = "与多个角色进行群聊")
	public Map<String, Object> groupChat(@RequestBody GroupChatRequest request) {
		return chatService.handleGroupChat(request);
	}

	@GetMapping("/groups/{groupId}/messages")
	public List<Message> getGroupMessages(@PathVariable Long groupId) {
		return chatService.getGroupMessages(groupId);
	}

	@GetMapping("/conversations/{characterId}")
	public List<com.aichat.entity.Conversation> getConversationsByCharacter(@PathVariable Long characterId) {
		return chatService.getConversationsByCharacter(characterId);
	}

	@GetMapping("/messages/{conversationId}")
	public List<Message> getMessagesByConversation(@PathVariable Long conversationId) {
		return chatService.getMessagesByConversationId(conversationId);
	}

	@GetMapping("/messages/character/{characterId}")
	public List<Message> getMessagesByCharacter(
			@PathVariable Long characterId,
			@RequestParam(required = false) Long userId) {
		return chatService.getMessagesByCharacter(characterId, userId);
	}
}