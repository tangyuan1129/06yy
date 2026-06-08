package com.aichat.controller;

import com.aichat.entity.User;
import com.aichat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、密码管理等接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账号")
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
    @Operation(summary = "用户登录", description = "用户登录并获取会话信息")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            return Map.of(
                "success", true,
                "message", "登录成功",
                "userId", user.getId(),
                "username", user.getUsername(),
                "remainingQuota", user.getDailyQuota() - user.getUsedToday(),
                "dailyQuota", user.getDailyQuota(),
                "hasApiKey", user.getApiKey() != null && !user.getApiKey().isEmpty()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @GetMapping("/quota/{userId}")
    @Operation(summary = "获取剩余配额", description = "获取用户今日剩余聊天次数")
    public Map<String, Object> getQuota(@Parameter(description = "用户ID") @PathVariable Long userId) {
        int remaining = userService.getRemainingQuota(userId);
        return Map.of(
            "success", true,
            "remaining", remaining
        );
    }

    @GetMapping("/user-info/{userId}")
    @Operation(summary = "获取用户信息", description = "获取用户基本信息和API Key状态")
    public Map<String, Object> getUserInfo(@Parameter(description = "用户ID") @PathVariable Long userId) {
        try {
            com.aichat.entity.User user = userService.getUserById(userId);
            if (user == null) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }
            return Map.of(
                "success", true,
                "userId", user.getId(),
                "username", user.getUsername(),
                "hasApiKey", user.getApiKey() != null && !user.getApiKey().isEmpty()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @PostMapping("/update-api-key")
    @Operation(summary = "更新API Key", description = "保存或删除用户的API Key")
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

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Map<String, Object> changePassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return Map.of(
                "success", true,
                "message", "密码修改成功"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "忘记密码时通过用户名重置密码")
    public Map<String, Object> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPasswordByUsername(request.getUsername(), request.getNewPassword());
            return Map.of(
                "success", true,
                "message", "密码重置成功"
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

    @Data
    static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    static class ResetPasswordRequest {
        private String username;
        private String newPassword;
    }
}