package com.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ⚠️ 危险操作已禁用 - 生产环境不应提供数据库修改/删除接口
 * 如需修改数据库或初始化数据，请直接操作数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class DatabaseFixController {

    // 所有危险接口已禁用
    // 如需修改数据库，请使用数据库命令：
    // ALTER TABLE message ...
    // DELETE FROM game_character ...
    
    @RequestMapping("/disabled")
    public Map<String, Object> disabled() {
        return Map.of(
            "success", false,
            "message", "此接口已禁用，请勿通过API修改数据库结构"
        );
    }
}