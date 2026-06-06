package com.aichat.controller;

import com.aichat.entity.Character;
import com.aichat.service.CharacterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cleanup")
@RequiredArgsConstructor
public class CharacterCleanupController {

    private final CharacterService characterService;

    @GetMapping("/duplicates")
    public Map<String, Object> cleanupDuplicates() {
        try {
            log.info("开始清理重复角色...");

            // 获取所有角色
            List<Character> allCharacters = characterService.getAllCharacters();
            
            // 按名称分组
            Map<String, List<Character>> groups = allCharacters.stream()
                    .collect(Collectors.groupingBy(Character::getName));

            int deletedCount = 0;

            // 对每个分组，只保留一个（创建时间最早的）
            for (Map.Entry<String, List<Character>> entry : groups.entrySet()) {
                List<Character> characters = entry.getValue();
                
                if (characters.size() > 1) {
                    log.info("发现重复角色: {}, 数量: {}", entry.getKey(), characters.size());
                    
                    // 按创建时间排序，保留第一个，删除其余
                    characters.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                    
                    for (int i = 1; i < characters.size(); i++) {
                        Character toDelete = characters.get(i);
                        log.info("删除重复角色: {}", toDelete.getId());
                        characterService.deleteCharacter(toDelete.getId());
                        deletedCount++;
                    }
                }
            }

            log.info("清理完成，删除了 {} 个重复角色", deletedCount);
            
            return Map.of(
                "success", true,
                "deletedCount", deletedCount,
                "message", "清理了 " + deletedCount + " 个重复角色"
            );
        } catch (Exception e) {
            log.error("清理重复角色失败", e);
            return Map.of(
                "success", false,
                "message", "清理失败: " + e.getMessage()
            );
        }
    }
}