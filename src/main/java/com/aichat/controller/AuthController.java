package com.aichat.controller;

import com.aichat.entity.User;
import com.aichat.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody AuthRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword());
            return Map.of(
                "success", true,
                "message", "注册成功",
                "userId", user.getId()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            return Map.of(
                "success", true,
                "message", "登录成功",
                "userId", user.getId(),
                "username", user.getUsername(),
                "remainingQuota", user.getDailyQuota() - user.getUsedToday(),
                "dailyQuota", user.getDailyQuota()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @GetMapping("/quota/{userId}")
    public Map<String, Object> getQuota(@PathVariable Long userId) {
        int remaining = userService.getRemainingQuota(userId);
        return Map.of(
            "success", true,
            "remaining", remaining
        );
    }

    @PostMapping("/update-api-key")
    public Map<String, Object> updateApiKey(@RequestBody UpdateApiKeyRequest request) {
        try {
            userService.updateApiKey(request.getUserId(), request.getApiKey());
            return Map.of(
                "success", true,
                "message", "API Key已更新"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @Data
    static class AuthRequest {
        private String username;
        private String password;
    }

    @Data
    static class UpdateApiKeyRequest {
        private Long userId;
        private String apiKey;
    }
}