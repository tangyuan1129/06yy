-- 数据库迁移脚本：将现有明文密码标记为需要重置
-- 注意：此脚本仅用于开发环境，生产环境需要用户重新注册或使用密码重置功能

-- 方案1：清空所有用户密码，要求用户重新注册（安全但影响用户体验）
-- UPDATE user SET password = NULL WHERE password IS NOT NULL;

-- 方案2：保留现有用户，但标记为需要重置密码（推荐）
-- ALTER TABLE user ADD COLUMN password_reset_required BOOLEAN DEFAULT FALSE;
-- UPDATE user SET password_reset_required = TRUE WHERE password IS NOT NULL;

-- 方案3：为测试用户设置默认 BCrypt 密码（仅开发环境）
-- BCrypt hash for "123456": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE password != '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';