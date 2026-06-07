-- 更新角色头像URL（根据ChatAvatar文件夹中的图片）
-- 注意：文件夹名称是 images 而不是 iamges
UPDATE game_character SET avatar_url = '/images/ChatAvatar/白厄聊天头像.jpg' WHERE name = '白厄';
UPDATE game_character SET avatar_url = '/images/ChatAvatar/赛飞儿聊天头像.jpg' WHERE name = '赛飞儿';

-- 查询验证
SELECT id, name, avatar_url FROM game_character WHERE avatar_url IS NOT NULL;