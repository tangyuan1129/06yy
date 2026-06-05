package com.aichat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	public SseEmitter handleException(Exception e) {
		logger.error("处理请求时发生错误: ", e);

		SseEmitter emitter = new SseEmitter(300_000L);
		try {
			emitter.send(SseEmitter.event()
					.name("error")
					.data(Map.of("message", "服务器内部错误: " + e.getMessage())));
		} catch (Exception ex) {
			logger.error("发送错误事件失败: ", ex);
		}

		return emitter;
	}
}