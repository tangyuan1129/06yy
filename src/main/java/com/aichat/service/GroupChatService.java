package com.aichat.service;

import com.aichat.entity.Character;
import com.aichat.entity.GroupChat;
import com.aichat.entity.GroupMember;
import com.aichat.mapper.GroupChatMapper;
import com.aichat.mapper.GroupMemberMapper;
import com.aichat.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final GroupChatMapper groupChatMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final MessageMapper messageMapper;
    private final CharacterService characterService;

    public List<GroupChat> getAllGroups() {
        return groupChatMapper.selectList(
            new LambdaQueryWrapper<GroupChat>()
                .orderByDesc(GroupChat::getCreatedAt)
        );
    }

    public GroupChat getGroupById(Long id) {
        return groupChatMapper.selectById(id);
    }

    public List<Character> getGroupMembers(Long groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
        );
        
        List<Character> characters = new ArrayList<>();
        for (GroupMember member : members) {
            Character character = characterService.getCharacterById(member.getCharacterId());
            if (character != null) {
                characters.add(character);
            }
        }
        return characters;
    }

    @Transactional
    public GroupChat createGroup(String name, String description, List<Long> characterIds) {
        GroupChat group = new GroupChat();
        group.setName(name);
        group.setDescription(description);
        groupChatMapper.insert(group);
        
        for (Long characterId : characterIds) {
            GroupMember member = new GroupMember();
            member.setGroupId(group.getId());
            member.setCharacterId(characterId);
            groupMemberMapper.insert(member);
        }
        
        return group;
    }

    @Transactional
    public void addMemberToGroup(Long groupId, Long characterId) {
        GroupMember existing = groupMemberMapper.selectOne(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getCharacterId, characterId)
        );
        
        if (existing == null) {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setCharacterId(characterId);
            groupMemberMapper.insert(member);
        }
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long characterId) {
        groupMemberMapper.delete(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getCharacterId, characterId)
        );
    }

    @Transactional
    public void clearGroupMessages(Long groupId) {
        log.info("清空群聊消息: groupId={}", groupId);
        int count = messageMapper.delete(
            new LambdaQueryWrapper<com.aichat.entity.Message>()
                .eq(com.aichat.entity.Message::getGroupId, groupId)
        );
        log.info("已清空 {} 条群聊消息", count);
    }

    @Transactional
    public void deleteGroup(Long id) {
        log.info("开始解散群聊: groupId={}", id);
        
        // 1. 先删除该群聊的所有消息
        int messageCount = messageMapper.delete(
            new LambdaQueryWrapper<com.aichat.entity.Message>()
                .eq(com.aichat.entity.Message::getGroupId, id)
        );
        log.info("已删除 {} 条群聊消息", messageCount);
        
        // 2. 删除该群聊的所有成员
        int memberCount = groupMemberMapper.delete(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, id)
        );
        log.info("已删除 {} 个群聊成员", memberCount);
        
        // 3. 最后删除群聊本身
        groupChatMapper.deleteById(id);
        log.info("群聊 {} 已成功解散", id);
    }
}