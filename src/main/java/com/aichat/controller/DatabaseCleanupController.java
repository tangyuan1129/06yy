package com.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ⚠️ 危险操作已禁用 - 生产环境不应提供数据删除接口
 * 如需清理数据，请直接操作数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/cleanup")
@RequiredArgsConstructor
public class DatabaseCleanupController {

    // 所有危险接口已禁用
    // 如需清理数据，请使用数据库命令：
    // DELETE FROM message;
    // DELETE FROM conversation;
    
    @RequestMapping("/disabled")
    public Map<String, Object> disabled() {
        return Map.of(
            "success", false,
            "message", "此接口已禁用，请勿通过API删除数据"
        );
    }
}