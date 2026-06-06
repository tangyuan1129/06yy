package com.aichat.controller;

import com.aichat.dto.ChatRequest;
import com.aichat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")	
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
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
}