package com.aichat.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    api_key VARCHAR(255),
                    daily_quota INT DEFAULT 20,
                    used_today INT DEFAULT 0,
                    last_reset_date DATE,
                    total_tokens BIGINT DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
            System.out.println("✅ user 表创建成功");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT,
                    ip_address VARCHAR(50),
                    user_message TEXT,
                    ai_response TEXT,
                    character_id BIGINT,
                    group_id BIGINT,
                    tokens_used INT DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user (user_id),
                    INDEX idx_time (created_at)
                )
                """);
            System.out.println("✅ chat_log 表创建成功");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sensitive_word (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    word VARCHAR(100) NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
            System.out.println("✅ sensitive_word 表创建成功");

        } catch (Exception e) {
            System.err.println("❌ 数据库表创建失败: " + e.getMessage());
        }

        // 添加新字段到 message 表
        try {
            jdbcTemplate.execute("ALTER TABLE message ADD COLUMN user_id BIGINT");
        } catch (Exception e) {
            // 字段已存在，忽略
        }
        try {
            jdbcTemplate.execute("ALTER TABLE message ADD COLUMN character_id BIGINT");
        } catch (Exception e) {
            // 字段已存在，忽略
        }
        System.out.println("✅ message 表新增字段完成");

        // 添加新字段到 conversation 表
        try {
            jdbcTemplate.execute("ALTER TABLE conversation ADD COLUMN user_id BIGINT");
        } catch (Exception e) {
            // 字段已存在，忽略
        }
        System.out.println("✅ conversation 表新增字段完成");

        // 添加 total_tokens 字段到 user 表
        try {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN total_tokens BIGINT DEFAULT 0");
        } catch (Exception e) {
            // 字段已存在，忽略
        }
        System.out.println("✅ user 表新增 total_tokens 字段完成");

        // 修复老用户的 quota 字段
        try {
            jdbcTemplate.execute("UPDATE user SET daily_quota = 20 WHERE daily_quota IS NULL OR daily_quota = 0");
            jdbcTemplate.execute("UPDATE user SET used_today = 0 WHERE used_today IS NULL");
            System.out.println("✅ 用户配额初始化完成");
        } catch (Exception e) {
            System.err.println("⚠️ 用户配额初始化失败: " + e.getMessage());
        }

        // 重置所有用户密码为 123456 (BCrypt 加密)
        try {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String bcryptHash = encoder.encode("123456");
            int count = jdbcTemplate.update(
                "UPDATE user SET password = ?", 
                bcryptHash
            );
            System.out.println("✅ 所有用户密码已重置为 123456，共 " + count + " 个用户");
            System.out.println("✅ 新密码Hash: " + bcryptHash);
        } catch (Exception e) {
            System.err.println("⚠️ 密码重置失败: " + e.getMessage());
        }
    }
}