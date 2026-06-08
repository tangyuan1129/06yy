package com.aichat.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理异常，避免暴露敏感信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        return Map.of(
            "success", false,
            "message", e.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        // 生产环境不暴露详细错误信息
        return Map.of(
            "success", false,
            "message", "服务器内部错误"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Map<String, Object> handleNoResourceFoundException(NoResourceFoundException e) {
        return Map.of(
            "success", false,
            "message", "资源不存在"
        );
    }
}