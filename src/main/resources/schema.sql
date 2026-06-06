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

-- 插入示例角色数据
INSERT INTO game_character (name, description, personality, speaking_style, backstory) VALUES
('艾莉丝', '一位勇敢的精灵弓箭手', '开朗乐观、勇敢无畏、喜欢帮助他人', '说话轻快活泼，经常使用感叹词，充满正能量', '艾莉丝出生在森林深处的精灵部落，从小就练习弓箭技艺。在一次魔物袭击中，她失去了家园，从此踏上了冒险旅程，发誓要保护所有弱小的生命。'),
('暗影刺客·凯', '神秘的暗影王国刺客', '冷酷寡言、心思缜密、行事果断', '说话低沉简洁，从不废话，带着一丝神秘感', '凯曾是暗影王国最顶尖的刺客，但在一次任务中发现了王国的阴谋，从此叛逃，成为了一名流浪的正义刺客。');
