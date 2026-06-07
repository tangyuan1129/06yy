﻿const { createApp, ref, nextTick, onMounted, computed } = Vue;

const API_BASE = '/api';

createApp({
    setup() {
        const characters = ref([]);
        const groups = ref([]);
        const currentTab = ref('single');
        const chatType = ref(null);
        const currentCharacterId = ref(null);
        const currentGroupId = ref(null);
        const messages = ref([]);
        const userInput = ref('');
        const isGenerating = ref(false);
        const status = ref('');
        const messagesContainer = ref(null);
        const showCreateGroupModal = ref(false);
        const newGroupName = ref('');
        const selectedCharactersForGroup = ref([]);
        const selectedMemberToAdd = ref(null);
        const currentGroupMembers = ref([]);
        const groupMemberCache = ref({});
        const showProfileModal = ref(false);
        const selectedProfileCharacter = ref(null);
        const showChatMenu = ref(false);
        const searchCharacter = ref('');
        const searchGroup = ref('');
        const searchGroupMembers = ref('');
        const searchAddMembers = ref('');

        // 获取角色头像URL的辅助函数
        const getCharacterAvatar = (characterId) => {
            const char = characters.value.find(c => c.id === characterId);
            return char && char.avatarUrl ? char.avatarUrl : null;
        };

        // 当前角色信息
        const currentCharacter = computed(() => {
            return characters.value.find(c => c.id === currentCharacterId.value);
        });

        const currentCharacterName = computed(() => {
            return currentCharacter.value ? currentCharacter.value.name : '';
        });

        const currentGroupName = computed(() => {
            const group = groups.value.find(g => g.id === currentGroupId.value);
            return group ? group.name : '';
        });

        const filteredCharacters = computed(() => {
            if (!searchCharacter.value.trim()) {
                return characters.value;
            }
            const keyword = searchCharacter.value.toLowerCase();
            return characters.value.filter(char => 
                char.name.toLowerCase().includes(keyword)
            );
        });

        const filteredGroups = computed(() => {
            if (!searchGroup.value.trim()) {
                return groups.value;
            }
            const keyword = searchGroup.value.toLowerCase();
            return groups.value.filter(group => 
                group.name.toLowerCase().includes(keyword)
            );
        });

        const filteredGroupMembers = computed(() => {
            if (!searchGroupMembers.value.trim()) {
                return characters.value;
            }
            const keyword = searchGroupMembers.value.toLowerCase();
            return characters.value.filter(char => 
                char.name.toLowerCase().includes(keyword)
            );
        });

        const filteredAddMembers = computed(() => {
            const available = availableCharactersToAdd.value;
            if (!searchAddMembers.value.trim()) {
                return available;
            }
            const keyword = searchAddMembers.value.toLowerCase();
            return available.filter(char => 
                char.name.toLowerCase().includes(keyword)
            );
        });

        // 初始化
        const init = async () => {
            try {
                // 先初始化白厄角色
                await initBaiE();
                
                // 更新所有角色头像
                await updateAvatars();
                
                await Promise.all([
                    loadCharacters(),
                    loadGroups()
                ]);
            } catch (error) {
                console.error('初始化失败:', error);
            }
        };

        const initBaiE = async () => {
            try {
                const response = await fetch(API_BASE + '/init/bai-e', {
                    method: 'POST'
                });
                if (response.ok) {
                    console.log('白厄角色初始化成功');
                }
            } catch (error) {
                console.error('白厄角色初始化失败:', error);
            }
        };

        const updateAvatars = async () => {
            try {
                const response = await fetch(API_BASE + '/init/update-avatars', {
                    method: 'POST'
                });
                if (response.ok) {
                    const data = await response.json();
                    console.log('头像更新结果:', data);
                    
                    // 重新加载角色
                    await loadCharacters();
                }
            } catch (error) {
                console.error('头像更新失败:', error);
            }
        };

        const initDatabase = async () => {
            try {
                await fetch(API_BASE + '/db/init', {
                    method: 'GET'
                });
                await loadCharacters();
                await loadGroups();
            } catch (error) {
                console.error('数据库初始化失败:', error);
            }
        };

        const loadCharacters = async () => {
            try {
                const response = await fetch(API_BASE + '/characters');
                if (!response.ok) {
                    console.error('加载角色失败，状态码:', response.status);
                    return;
                }
                const data = await response.json();
                if (Array.isArray(data)) {
                    characters.value = data;
                    console.log('加载角色成功，共', data.length, '个角色');
                }
            } catch (error) {
                console.error('加载角色失败:', error);
            }
        };

        const loadGroups = async () => {
            try {
                console.log('正在加载群聊列表...');
                const response = await fetch(API_BASE + '/groups');
                console.log('群聊响应状态:', response.status);
                
                const data = await response.json();
                console.log('群聊数据:', data);
                
                if (Array.isArray(data)) {
                    groups.value = data;
                    for (const group of data) {
                        await loadGroupMembers(group.id);
                    }
                    console.log('成功加载', groups.value.length, '个群聊');
                }
            } catch (error) {
                console.error('加载群聊失败:', error);
            }
        };

        const loadGroupMembers = async (groupId) => {
            try {
                const response = await fetch(API_BASE + '/groups/' + groupId + '/members');
                const data = await response.json();
                if (Array.isArray(data)) {
                    // 去重：根据成员ID去重
                    const uniqueMembers = [];
                    const seenIds = new Set();
                    for (const member of data) {
                        if (!seenIds.has(member.id)) {
                            seenIds.add(member.id);
                            uniqueMembers.push(member);
                        }
                    }
                    groupMemberCache.value[groupId] = uniqueMembers;
                }
            } catch (error) {
                console.error('加载群成员失败:', error);
            }
        };

        const getGroupMemberCount = (groupId) => {
            return groupMemberCache.value[groupId] ? groupMemberCache.value[groupId].length : 0;
        };

        // 选择角色
        const selectCharacter = async (character) => {
            currentTab.value = 'single';
            chatType.value = 'single';
            currentCharacterId.value = character.id;
            currentGroupId.value = null;
            messages.value = [];
            status.value = '';
            await loadCharacterConversation(character.id);
        };

        // 选择群聊
        const selectGroup = async (group) => {
            currentTab.value = 'group';
            chatType.value = 'group';
            currentGroupId.value = group.id;
            currentCharacterId.value = null;
            messages.value = [];
            status.value = '';
            await loadGroupMembers(group.id);
            currentGroupMembers.value = groupMemberCache.value[group.id] || [];
            await loadGroupConversation(group.id);
        };

        // 加载群聊会话
        const loadGroupConversation = async (groupId) => {
            try {
                console.log('加载群聊', groupId, '的历史消息');
                const msgResponse = await fetch(API_BASE + '/groups/' + groupId + '/messages');
                console.log('群聊消息响应状态:', msgResponse.status);
                
                if (!msgResponse.ok) {
                    console.error('响应失败:', msgResponse.statusText);
                    return;
                }
                
                const historyMessages = await msgResponse.json();
                console.log('群聊消息数据:', historyMessages);
                
                if (historyMessages && historyMessages.length > 0) {
                    messages.value = historyMessages.map(msg => ({
                        role: msg.role,
                        content: msg.content,
                        senderId: msg.senderId,
                        senderName: msg.senderName,
                        senderAvatar: getCharacterAvatar(msg.senderId)
                    }));
                    console.log('设置好的 messages.value:', messages.value);
                } else {
                    console.log('没有群聊消息');
                    messages.value = [];
                }
                await scrollToBottom();
            } catch (error) {
                console.error('加载群聊会话失败:', error);
            }
        };

        // 加载角色会话
        const loadCharacterConversation = async (charId) => {
            try {
                const convResponse = await fetch(API_BASE + '/conversations/' + charId);
                const conversations = await convResponse.json();
                
                if (conversations && conversations.length > 0) {
                    const latestConv = conversations[0];
                    const msgResponse = await fetch(API_BASE + '/messages/' + latestConv.id);
                    const historyMessages = await msgResponse.json();
                    
                    if (historyMessages && historyMessages.length > 0) {
                        messages.value = historyMessages.map(msg => ({
                            role: msg.role,
                            content: msg.content,
                            senderId: msg.senderId,
                            senderName: msg.senderName,
                            senderAvatar: getCharacterAvatar(msg.senderId)
                        }));
                    }
                }
                await scrollToBottom();
            } catch (error) {
                console.error('加载会话失败:', error);
            }
        };

        // 发送消息
        const sendMessage = async () => {
            if (!userInput.value.trim() || !chatType.value || isGenerating.value) {
                return;
            }

            const message = userInput.value.trim();
            userInput.value = '';
            isGenerating.value = true;
            status.value = chatType.value === 'single' ? '正在生成回复...' : '正在生成群聊回复...';

            messages.value.push({
                role: 'user',
                content: message,
                senderName: '我'
            });
            await scrollToBottom();

            try {
                let requestBody;
                let endpoint;

                if (chatType.value === 'single') {
                    requestBody = {
                        message: message,
                        characterId: currentCharacterId.value
                    };
                    endpoint = API_BASE + '/chat/stream';
                } else {
                    requestBody = {
                        message: message,
                        groupId: currentGroupId.value,
                        characterIds: currentGroupMembers.value.map(m => m.id)
                    };
                    endpoint = API_BASE + '/chat/group';
                }
                
                console.log('发送请求:', endpoint, requestBody);

                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestBody)
                });

                if (!response.ok) {
                    throw new Error('请求失败: ' + response.status);
                }

                if (chatType.value === 'single') {
                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();
                    messages.value.push({
                        role: 'assistant',
                        content: '',
                        senderId: currentCharacterId.value,
                        senderName: currentCharacterName.value,
                        senderAvatar: getCharacterAvatar(currentCharacterId.value)
                    });
                    
                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;

                        const chunk = decoder.decode(value);
                        const lines = chunk.split('\n');

                        for (let line of lines) {
                            line = line.trim();
                            if (line === '') continue;
                            if (line === '[DONE]') break;
                            
                            let content = line;
                            if (line.startsWith('data: ')) {
                                content = line.substring(6);
                            } else if (line.startsWith('data:')) {
                                content = line.substring(5);
                            }
                            
                            content = content.trim();
                            if (content.startsWith('[CONVERSATION_ID:')) continue;
                            if (content === '[DONE]') break;
                            
                            if (content) {
                                messages.value[messages.value.length - 1].content += content;
                                await scrollToBottom();
                            }
                        }
                    }
                } else {
                    const data = await response.json();
                    console.log('群聊响应:', data);
                    if (data && data.messages) {
                        for (const msg of data.messages) {
                            messages.value.push({
                                role: 'assistant',
                                content: msg.content,
                                senderId: msg.characterId,
                                senderName: msg.characterName,
                                senderAvatar: getCharacterAvatar(msg.characterId)
                            });
                            await scrollToBottom();
                            await new Promise(resolve => setTimeout(resolve, 500));
                        }
                    }
                }

                status.value = '发送成功';
                setTimeout(() => {
                    status.value = '';
                }, 2000);
            } catch (error) {
                console.error('发送消息失败:', error);
                messages.value.push({
                    role: 'assistant',
                    content: '发送失败: ' + error.message,
                    senderName: '系统'
                });
                status.value = '发送失败';
            } finally {
                isGenerating.value = false;
            }
        };

        // 清空聊天
        const clearChat = async () => {
            if (!confirm('确定要清空聊天记录吗？')) {
                return;
            }
            messages.value = [];
            showChatMenu.value = false;
            status.value = '聊天记录已清空';
            setTimeout(() => {
                status.value = '';
            }, 2000);
        };

        const toggleChatMenu = () => {
            showChatMenu.value = !showChatMenu.value;
        };

        const closeChatMenu = () => {
            showChatMenu.value = false;
        };

        // API Key相关
        const saveApiKey = () => {
            if (apiKeyInput.value.trim()) {
                localStorage.setItem('zhipu_api_key', apiKeyInput.value.trim());
                showApiKeyModal.value = false;
                status.value = 'API Key已保存';
                setTimeout(() => { status.value = ''; }, 2000);
            }
        };

        const loadApiKey = () => {
            const savedKey = localStorage.getItem('zhipu_api_key');
            if (savedKey) {
                apiKeyInput.value = savedKey;
            }
        };

        // 群聊相关
        const isCharacterSelectedForGroup = (charId) => {
            return selectedCharactersForGroup.value.includes(charId);
        };

        const toggleCharacterSelection = (charId) => {
            const index = selectedCharactersForGroup.value.indexOf(charId);
            if (index > -1) {
                selectedCharactersForGroup.value.splice(index, 1);
            } else {
                selectedCharactersForGroup.value.push(charId);
            }
        };

        const createGroup = async () => {
            if (!newGroupName.value.trim()) {
                alert('请输入群聊名称');
                return;
            }
            if (selectedCharactersForGroup.value.length < 2) {
                alert('请至少选择2个成员');
                return;
            }

            try {
                console.log('正在创建群聊:', {
                    name: newGroupName.value,
                    characterIds: selectedCharactersForGroup.value
                });
                
                const response = await fetch(API_BASE + '/groups', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: newGroupName.value,
                        description: '',
                        characterIds: selectedCharactersForGroup.value
                    })
                });

                console.log('创建群聊响应状态:', response.status);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error('HTTP错误: ' + response.status + ' - ' + errorText);
                }
                
                const group = await response.json();
                console.log('创建群聊成功:', group);
                
                showCreateGroupModal.value = false;
                newGroupName.value = '';
                selectedCharactersForGroup.value = [];
                
                await loadGroups();
                await selectGroup(group);
                
                status.value = '群聊创建成功！';
                setTimeout(() => { status.value = ''; }, 2000);
            } catch (error) {
                console.error('创建群聊失败:', error);
                alert('创建群聊失败: ' + error.message);
            }
        };

        const addMemberToGroup = async () => {
            if (!selectedMemberToAdd.value) {
                return;
            }

            try {
                await fetch(API_BASE + '/groups/' + currentGroupId.value + '/members', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        characterId: parseInt(selectedMemberToAdd.value)
                    })
                });

                selectedMemberToAdd.value = null;
                await loadGroupMembers(currentGroupId.value);
                currentGroupMembers.value = groupMemberCache.value[currentGroupId.value] || [];
                status.value = '成员添加成功';
                setTimeout(() => {
                    status.value = '';
                }, 2000);
            } catch (error) {
                console.error('添加成员失败:', error);
                status.value = '添加成员失败';
            }
        };

        const deleteGroup = async (groupId, event) => {
            event.stopPropagation();
            
            if (!confirm('确定要解散这个群聊吗？这将删除所有聊天记录！')) {
                return;
            }

            try {
                console.log('正在解散群聊:', groupId);
                const response = await fetch(API_BASE + '/groups/' + groupId, {
                    method: 'DELETE'
                });

                if (!response.ok) {
                    throw new Error('HTTP错误: ' + response.status);
                }

                console.log('群聊解散成功');
                
                if (currentGroupId.value === groupId) {
                    currentGroupId.value = null;
                    chatType.value = null;
                    messages.value = [];
                }

                await loadGroups();
                status.value = '群聊已解散';
                setTimeout(() => {
                    status.value = '';
                }, 2000);
            } catch (error) {
                console.error('解散群聊失败:', error);
                alert('解散群聊失败: ' + error.message);
            }
        };

        const showCharacterProfile = (character) => {
            selectedProfileCharacter.value = character;
            showProfileModal.value = true;
        };

        const startPrivateChat = (character) => {
            showProfileModal.value = false;
            selectCharacter(character);
        };

        const removeMemberFromGroup = async (member) => {
            if (!confirm(`确定要将 ${member.name} 移出群聊吗？`)) {
                return;
            }

            try {
                const response = await fetch(API_BASE + '/groups/' + currentGroupId.value + '/members/' + member.id, {
                    method: 'DELETE'
                });

                if (!response.ok) {
                    throw new Error('HTTP错误: ' + response.status);
                }

                await loadGroupMembers(currentGroupId.value);
                currentGroupMembers.value = groupMemberCache.value[currentGroupId.value] || [];
                status.value = `已将 ${member.name} 移出群聊`;
                showProfileModal.value = false;
                setTimeout(() => {
                    status.value = '';
                }, 2000);
            } catch (error) {
                console.error('移除成员失败:', error);
                alert('移除成员失败: ' + error.message);
            }
        };

        const scrollToBottom = async () => {
            await nextTick();
            if (messagesContainer.value) {
                messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
            }
        };

        // 计算不在群聊中的角色（用于添加成员）
        const availableCharactersToAdd = computed(() => {
            if (!currentGroupId.value || !currentGroupMembers.value.length) {
                return characters.value;
            }
            // 获取当前群聊成员ID集合
            const groupMemberIds = new Set(currentGroupMembers.value.map(m => m.id));
            // 过滤掉已经在群聊中的角色
            return characters.value.filter(char => !groupMemberIds.has(char.id));
        });

        onMounted(async () => {
            await init();
            loadApiKey();
        });

        return {
            characters,
            groups,
            currentTab,
            chatType,
            currentCharacterId,
            currentGroupId,
            messages,
            userInput,
            isGenerating,
            status,
            messagesContainer,
            showCreateGroupModal,
            newGroupName,
            selectedCharactersForGroup,
            selectedMemberToAdd,
            currentGroupMembers,
            availableCharactersToAdd,
            currentCharacter,
            currentCharacterName,
            currentGroupName,
            showProfileModal,
            selectedProfileCharacter,
            showChatMenu,
            showApiKeyModal,
            apiKeyInput,
            searchCharacter,
            searchGroup,
            searchGroupMembers,
            searchAddMembers,
            filteredCharacters,
            filteredGroups,
            filteredGroupMembers,
            filteredAddMembers,
            selectCharacter,
            selectGroup,
            sendMessage,
            clearChat,
            isCharacterSelectedForGroup,
            toggleCharacterSelection,
            createGroup,
            addMemberToGroup,
            getGroupMemberCount,
            deleteGroup,
            showCharacterProfile,
            startPrivateChat,
            removeMemberFromGroup,
            toggleChatMenu,
            closeChatMenu,
            saveApiKey,
            loadApiKey
        };
    }
}).mount('#app');