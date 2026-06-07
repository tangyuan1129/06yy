-- 创建游戏角色表
CREATE TABLE IF NOT EXISTS game_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    personality TEXT,
    speaking_style TEXT,
    backstory TEXT,
    avatar_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 修改会话表，添加角色ID字段
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS character_id BIGINT;

-- 修改消息表，添加群聊ID和发送者信息字段
ALTER TABLE message ADD COLUMN IF NOT EXISTS group_id BIGINT;
ALTER TABLE message ADD COLUMN IF NOT EXISTS sender_id BIGINT;
ALTER TABLE message ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255);
ALTER TABLE message ADD COLUMN IF NOT EXISTS conversation_id BIGINT;

-- AI内心独白表
CREATE TABLE IF NOT EXISTS ai_inner_monologue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT NOT NULL,
    group_id BIGINT,
    content TEXT NOT NULL,
    trigger_message_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_character_time (character_id, created_at DESC)
);

-- AI主动消息计数表
CREATE TABLE IF NOT EXISTS ai_active_message_count (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT NOT NULL,
    date VARCHAR(10) NOT NULL,
    count INT DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_character_date (character_id, date)
);