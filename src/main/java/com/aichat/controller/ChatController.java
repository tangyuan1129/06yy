package com.aichat.controller;

import com.aichat.dto.ChatRequest;
import com.aichat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@PostMapping("/chat/stream")
	public SseEmitter chatStream(@RequestBody ChatRequest request) {
		return chatService.handleChat(request);
	}
}