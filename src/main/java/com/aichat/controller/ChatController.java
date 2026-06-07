package com.aichat.controller;

import com.aichat.dto.ChatRequest;
import com.aichat.dto.GroupChatRequest;
import com.aichat.entity.Message;
import com.aichat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")	
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/test")
	public Map<String, Object> test() {
		Map<String, Object> result = new HashMap<>();
		result.put("status", "ok");
		result.put("message", "后端服务正常运行");
		return result;
	}

	@PostMapping("/chat/stream")
	public SseEmitter chatStream(@RequestBody ChatRequest request) {
		return chatService.handleChat(request);
	}

	@PostMapping("/chat/group")
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
}