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

-- 创建会话表
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT NULL,
    user_id VARCHAR(100),
    title VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_character (character_id)
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT,
    group_id BIGINT,
    sender_id BIGINT,
    sender_name VARCHAR(255),
    content TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_group (group_id)
);

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