package com.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatabaseFixController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/fix")
    public Map<String, Object> fixDatabase() {
        Map<String, Object> result = new HashMap<>();
        StringBuilder log = new StringBuilder();

        try {
            // 获取当前表结构
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='aichat' AND TABLE_NAME='message'"
            );
            
            java.util.Set<String> columnNames = new java.util.HashSet<>();
            for (Map<String, Object> col : columns) {
                columnNames.add(col.get("COLUMN_NAME").toString().toLowerCase());
            }
            
            log.append("当前字段: ").append(columnNames).append("\n\n");

            // 1. 修改 conversation_id 为可空
            if (columnNames.contains("conversation_id")) {
                try {
                    jdbcTemplate.execute("ALTER TABLE message MODIFY COLUMN conversation_id BIGINT DEFAULT NULL");
                    log.append("✓ conversation_id 已修改为可空\n");
                } catch (Exception e) {
                    log.append("✗ conversation_id 修改失败: ").append(e.getMessage()).append("\n");
                }
            }

            // 2. 添加 group_id 字段
            if (!columnNames.contains("group_id")) {
                try {
                    jdbcTemplate.execute("ALTER TABLE message ADD COLUMN group_id BIGINT DEFAULT NULL");
                    log.append("✓ group_id 字段已添加\n");
                } catch (Exception e) {
                    log.append("✗ group_id 添加失败: ").append(e.getMessage()).append("\n");
                }
            } else {
                log.append("✓ group_id 字段已存在\n");
            }

            // 3. 添加 sender_id 字段
            if (!columnNames.contains("sender_id")) {
                try {
                    jdbcTemplate.execute("ALTER TABLE message ADD COLUMN sender_id BIGINT DEFAULT NULL");
                    log.append("✓ sender_id 字段已添加\n");
                } catch (Exception e) {
                    log.append("✗ sender_id 添加失败: ").append(e.getMessage()).append("\n");
                }
            } else {
                log.append("✓ sender_id 字段已存在\n");
            }

            // 4. 添加 sender_name 字段
            if (!columnNames.contains("sender_name")) {
                try {
                    jdbcTemplate.execute("ALTER TABLE message ADD COLUMN sender_name VARCHAR(255) DEFAULT NULL");
                    log.append("✓ sender_name 字段已添加\n");
                } catch (Exception e) {
                    log.append("✗ sender_name 添加失败: ").append(e.getMessage()).append("\n");
                }
            } else {
                log.append("✓ sender_name 字段已存在\n");
            }

            // 5. 添加 group_id 索引
            try {
                jdbcTemplate.execute("ALTER TABLE message ADD INDEX idx_group (group_id)");
                log.append("✓ group_id 索引已添加\n");
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate key name") || e.getMessage().contains("1061")) {
                    log.append("✓ group_id 索引已存在\n");
                } else {
                    log.append("✗ group_id 索引添加失败: ").append(e.getMessage()).append("\n");
                }
            }

            result.put("success", true);
            result.put("log", log.toString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("log", log.toString());
        }

        return result;
    }

    @GetMapping("/clean-characters")
    public Map<String, Object> cleanCharacters() {
        Map<String, Object> result = new HashMap<>();
        StringBuilder log = new StringBuilder();

        try {
            // 获取所有角色
            List<Map<String, Object>> characters = jdbcTemplate.queryForList(
                "SELECT id, name FROM game_character ORDER BY id"
            );
            
            log.append("清理前角色列表:\n");
            for (Map<String, Object> chara : characters) {
                log.append("  - ID: ").append(chara.get("id")).append(", 名称: ").append(chara.get("name")).append("\n");
            }
            log.append("\n");

            // 删除所有非崩铁角色（只保留包含"崩坏：星穹铁道"描述的角色）
            int deletedCount = jdbcTemplate.update(
                "DELETE FROM game_character WHERE description NOT LIKE '%崩坏：星穹铁道%'"
            );
            log.append("已删除 ").append(deletedCount).append(" 个非崩铁角色\n\n");

            // 获取剩余角色
            List<Map<String, Object>> remaining = jdbcTemplate.queryForList(
                "SELECT id, name FROM game_character ORDER BY id"
            );
            
            log.append("剩余崩铁角色:\n");
            for (Map<String, Object> chara : remaining) {
                log.append("  - ID: ").append(chara.get("id")).append(", 名称: ").append(chara.get("name")).append("\n");
            }

            result.put("success", true);
            result.put("log", log.toString());
            result.put("deletedCount", deletedCount);
            result.put("remainingCount", remaining.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("log", log.toString());
        }

        return result;
    }
}