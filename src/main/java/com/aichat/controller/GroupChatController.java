package com.aichat.controller;

import com.aichat.entity.Character;
import com.aichat.entity.GroupChat;
import com.aichat.service.GroupChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupChatController {

    private final GroupChatService groupChatService;

    @GetMapping
    public List<GroupChat> getAllGroups() {
        log.info("获取所有群聊列表");
        List<GroupChat> groups = groupChatService.getAllGroups();
        log.info("返回 {} 个群聊", groups.size());
        return groups;
    }

    @GetMapping("/{id}")
    public GroupChat getGroupById(@PathVariable Long id) {
        log.info("获取群聊: id={}", id);
        return groupChatService.getGroupById(id);
    }

    @GetMapping("/{id}/members")
    public List<Character> getGroupMembers(@PathVariable Long id) {
        log.info("获取群聊成员: groupId={}", id);
        return groupChatService.getGroupMembers(id);
    }

    @PostMapping
    public GroupChat createGroup(@RequestBody Map<String, Object> request) {
        log.info("创建群聊请求: {}", request);
        String name = (String) request.get("name");
        String description = (String) request.getOrDefault("description", "");
        
        @SuppressWarnings("unchecked")
        List<Object> characterIdsRaw = (List<Object>) request.get("characterIds");
        List<Long> characterIds = characterIdsRaw.stream()
            .map(obj -> {
                if (obj instanceof Integer) {
                    return ((Integer) obj).longValue();
                } else if (obj instanceof Long) {
                    return (Long) obj;
                } else {
                    return Long.parseLong(obj.toString());
                }
            })
            .toList();
            
        log.info("创建群聊: name={}, characterIds={}", name, characterIds);
        GroupChat group = groupChatService.createGroup(name, description, characterIds);
        log.info("群聊创建成功: id={}, name={}", group.getId(), group.getName());
        return group;
    }

    @PostMapping("/{id}/members")
    public void addMember(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Object characterIdObj = request.get("characterId");
        Long characterId;
        if (characterIdObj instanceof Integer) {
            characterId = ((Integer) characterIdObj).longValue();
        } else if (characterIdObj instanceof Long) {
            characterId = (Long) characterIdObj;
        } else {
            characterId = Long.parseLong(characterIdObj.toString());
        }
        groupChatService.addMemberToGroup(id, characterId);
    }

    @DeleteMapping("/{id}/members/{characterId}")
    public void removeMember(@PathVariable Long id, @PathVariable Long characterId) {
        groupChatService.removeMemberFromGroup(id, characterId);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable Long id) {
        groupChatService.deleteGroup(id);
    }
}