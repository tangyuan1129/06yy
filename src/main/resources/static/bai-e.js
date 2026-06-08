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
        const showApiKeyModal = ref(false);
        const apiKeyInput = ref('');
        const showApiKey = ref(false);
        const showLoginModal = ref(true);
        const isLoggedIn = ref(false);
        const isLoginMode = ref(true);
        const authUsername = ref('');
        const authPassword = ref('');
        const authPasswordConfirm = ref('');
        const showAuthPassword = ref(false);
        const showAuthPasswordConfirm = ref(false);
        const showForgotPasswordModal = ref(false);
        const forgotUsername = ref('');
        const newResetPassword = ref('');
        const newResetPasswordConfirm = ref('');
        const showNewPassword = ref(false);
        const showNewPasswordConfirm = ref(false);
        const currentUserId = ref(null);
        const currentUsername = ref('');
        const remainingQuota = ref(0);
        const currentReader = ref(null);  // 保存当前的SSE流reader
        const abortController = ref(null);
        const showSettingsModal = ref(false);
        const oldPassword = ref('');
        const newPassword = ref('');
        const newPasswordConfirm = ref('');

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

        // 加载角色会话
        const loadCharacterConversation = async (characterId) => {
            try {
                console.log('加载角色', characterId, '的历史消息');
                const msgResponse = await fetch(API_BASE + '/messages/character/' + characterId + '?userId=' + currentUserId.value);
                console.log('历史消息响应状态:', msgResponse.status);
                
                if (msgResponse.ok) {
                    const messagesData = await msgResponse.json();
                    console.log('获取到历史消息数量:', messagesData.length);
                    
                    messages.value = messagesData.map(msg => ({
                        role: msg.role,
                        content: msg.content,
                        senderName: msg.role === 'user' ? '我' : (msg.senderName || currentCharacterName.value),
                        senderAvatar: msg.role === 'assistant' ? getCharacterAvatar(characterId) : null,
                        createdAt: msg.createdAt
                    }));
                    
                    await scrollToBottom();
                }
            } catch (e) {
                console.error('加载历史消息失败:', e);
            }
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

        // 发送消息
        const sendMessage = async () => {
            if (!userInput.value.trim() || !chatType.value || isGenerating.value) {
                return;
            }

            // 检查剩余次数（有API Key的用户不受限制）
            if (isLoggedIn.value && !hasApiKey.value && remainingQuota.value <= 0) {
                messages.value.push({
                    role: 'assistant',
                    content: '您的免费聊天次数已用完，请明天再来或联系管理员获取更多次数',
                    senderName: currentCharacterName.value || '系统',
                    senderAvatar: getCharacterAvatar(currentCharacterId.value)
                });
                await scrollToBottom();
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
                    // API Key只存储在后端，后端会自动使用
                    requestBody = {
                        message: message,
                        characterId: currentCharacterId.value,
                        userId: currentUserId.value,
                        apiKey: '' // 后端会从数据库获取用户的API Key
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
                    currentReader.value = reader;  // 保存reader引用
                    
                    const decoder = new TextDecoder();
                    messages.value.push({
                        role: 'assistant',
                        content: '',
                        senderId: currentCharacterId.value,
                        senderName: currentCharacterName.value,
                        senderAvatar: getCharacterAvatar(currentCharacterId.value)
                    });
                    
                    try {
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
                    } finally {
                        // 流完成后清除reader引用
                        currentReader.value = null;
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
                currentReader.value = null;  // 清除reader引用
                
                // 更新剩余配额
                if (isLoggedIn.value && currentUserId.value && !hasApiKey.value) {
                    try {
                        const response = await fetch(API_BASE + '/auth/quota/' + currentUserId.value);
                        const data = await response.json();
                        if (data.success) {
                            remainingQuota.value = data.remaining;
                        }
                    } catch (error) {
                        console.error('更新配额失败:', error);
                    }
                }
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
        const hasApiKey = ref(false);

        const saveApiKey = async () => {
            if (apiKeyInput.value.trim()) {
                try {
                    const response = await fetch(API_BASE + '/auth/update-api-key', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            userId: currentUserId.value,
                            apiKey: apiKeyInput.value.trim()
                        })
                    });
                    const data = await response.json();
                    if (data.success) {
                        // 不保存到localStorage，只更新状态
                        hasApiKey.value = true;
                        apiKeyInput.value = ''; // 清空输入框，不在前端保留
                        status.value = 'API Key已保存（仅显示一次）';
                        setTimeout(() => { status.value = ''; }, 3000);
                    } else {
                        alert(data.message);
                    }
                } catch (e) {
                    alert('保存失败: ' + e.message);
                }
            }
        };

        const deleteApiKey = async () => {
            if (!confirm('确定要删除API Key吗？删除后将使用系统默认配置')) return;
            try {
                const response = await fetch(API_BASE + '/auth/update-api-key', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        userId: currentUserId.value,
                        apiKey: ''
                    })
                });
                const data = await response.json();
                if (data.success) {
                    apiKeyInput.value = '';
                    hasApiKey.value = false;
                    status.value = 'API Key已删除';
                    setTimeout(() => { status.value = ''; }, 2000);
                } else {
                    alert(data.message);
                }
            } catch (e) {
                alert('删除失败: ' + e.message);
            }
        };

        const loadApiKey = async () => {
            // 如果已登录，从后端获取API Key状态
            if (isLoggedIn.value && currentUserId.value) {
                try {
                    const response = await fetch(API_BASE + '/auth/user-info/' + currentUserId.value);
                    const data = await response.json();
                    if (data.success) {
                        hasApiKey.value = data.hasApiKey;
                        // 不再从localStorage读取，API Key只存储在后端
                        apiKeyInput.value = '';
                        return;
                    }
                } catch (error) {
                    console.error('获取用户信息失败:', error);
                }
            }
            
            hasApiKey.value = false;
        };

        const login = async () => {
            if (!authUsername.value.trim() || !authPassword.value.trim()) {
                alert('请输入用户名和密码');
                return;
            }
            try {
                const response = await fetch(API_BASE + '/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: authUsername.value,
                        password: authPassword.value
                    })
                });
                const data = await response.json();
                if (data.success) {
                    isLoggedIn.value = true;
                    showLoginModal.value = false;
                    currentUserId.value = data.userId;
                    currentUsername.value = data.username;
                    remainingQuota.value = data.remainingQuota;
                    hasApiKey.value = data.hasApiKey || false;
                    localStorage.setItem('userId', data.userId);
                    localStorage.setItem('username', data.username);
                    
                    // API Key只存储在后端，不在前端保留
                    apiKeyInput.value = '';
                    
                    status.value = '登录成功，欢迎 ' + data.username;
                    setTimeout(() => { status.value = ''; }, 2000);
                    init();
                } else {
                    alert(data.message || '登录失败，请检查用户名和密码');
                }
            } catch (e) {
                alert('登录失败，请检查网络连接');
            }
        };

        const register = async () => {
            if (!authUsername.value.trim() || !authPassword.value.trim()) {
                alert('请输入用户名和密码');
                return;
            }
            if (authPassword.value !== authPasswordConfirm.value) {
                alert('两次输入的密码不一致');
                return;
            }
            if (authPassword.value.length < 6) {
                alert('密码长度不能少于6位');
                return;
            }
            try {
                const response = await fetch(API_BASE + '/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: authUsername.value,
                        password: authPassword.value
                    })
                });
                const data = await response.json();
                if (data.success) {
                    alert('注册成功！请登录');
                    isLoginMode.value = true;
                    authPassword.value = '';
                } else {
                    alert(data.message);
                }
            } catch (e) {
                alert('注册失败: ' + e.message);
            }
        };

        const logout = async () => {
            // 如果有正在进行的SSE流，先中断它
            if (currentReader.value) {
                try {
                    await currentReader.value.cancel();
                } catch (e) {
                    // 忽略取消错误
                }
                currentReader.value = null;
            }
            
            isGenerating.value = false;
            
            // 先清空消息列表，让DOM先更新
            messages.value = [];
            
            // 使用nextTick确保DOM更新后再执行其他操作
            await nextTick();
            
            isLoggedIn.value = false;
            showLoginModal.value = true;
            isLoginMode.value = true;
            currentUserId.value = null;
            currentUsername.value = '';
            authUsername.value = '';
            authPassword.value = '';
            authPasswordConfirm.value = '';
            chatType.value = null;
            currentCharacterId.value = null;
            currentGroupId.value = null;
            localStorage.removeItem('userId');
            localStorage.removeItem('username');
        };

        const resetPassword = async () => {
            if (!forgotUsername.value.trim()) {
                alert('请输入用户名');
                return;
            }
            if (!newResetPassword.value.trim()) {
                alert('请输入新密码');
                return;
            }
            if (newResetPassword.value.length < 6) {
                alert('新密码长度不能少于6位');
                return;
            }
            if (newResetPassword.value !== newResetPasswordConfirm.value) {
                alert('两次输入的密码不一致');
                return;
            }
            try {
                const response = await fetch(API_BASE + '/auth/reset-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: forgotUsername.value,
                        newPassword: newResetPassword.value
                    })
                });
                const data = await response.json();
                if (data.success) {
                    alert('密码重置成功！请使用新密码登录');
                    showForgotPasswordModal.value = false;
                    forgotUsername.value = '';
                    newResetPassword.value = '';
                    newResetPasswordConfirm.value = '';
                } else {
                    alert(data.message || '密码重置失败');
                }
            } catch (e) {
                alert('密码重置失败: ' + e.message);
            }
        };

        const changePassword = async () => {
            if (!oldPassword.value.trim()) {
                alert('请输入当前密码');
                return;
            }
            if (!newPassword.value.trim()) {
                alert('请输入新密码');
                return;
            }
            if (newPassword.value.length < 6) {
                alert('新密码长度不能少于6位');
                return;
            }
            if (newPassword.value !== newPasswordConfirm.value) {
                alert('两次输入的新密码不一致');
                return;
            }
            if (newPassword.value === oldPassword.value) {
                alert('新密码不能与当前密码相同');
                return;
            }
            try {
                const response = await fetch(API_BASE + '/auth/change-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-User-Id': currentUserId.value
                    },
                    body: JSON.stringify({
                        oldPassword: oldPassword.value,
                        newPassword: newPassword.value
                    })
                });
                const data = await response.json();
                if (data.success) {
                    alert('密码修改成功！');
                    showSettingsModal.value = false;
                    oldPassword.value = '';
                    newPassword.value = '';
                    newPasswordConfirm.value = '';
                } else {
                    alert(data.message);
                }
            } catch (e) {
                alert('修改密码失败: ' + e.message);
            }
        };

        const checkAutoLogin = async () => {
            const savedUserId = localStorage.getItem('userId');
            const savedUsername = localStorage.getItem('username');
            if (savedUserId) {
                currentUserId.value = parseInt(savedUserId);
                currentUsername.value = savedUsername || '';
                isLoggedIn.value = true;
                showLoginModal.value = false;
                
                // 获取最新的剩余配额和API Key状态
                try {
                    const response = await fetch(API_BASE + '/auth/user-info/' + savedUserId);
                    const data = await response.json();
                    if (data.success) {
                        // 同步API Key状态（只存储在后端）
                        hasApiKey.value = data.hasApiKey;
                        apiKeyInput.value = ''; // 不在前端显示
                    }
                } catch (error) {
                    console.error('获取用户信息失败:', error);
                }
                
                // 获取配额
                try {
                    const response = await fetch(API_BASE + '/auth/quota/' + savedUserId);
                    const data = await response.json();
                    if (data.success) {
                        remainingQuota.value = data.remaining;
                    }
                } catch (error) {
                    console.error('获取配额失败:', error);
                }
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
            await checkAutoLogin();
            if (isLoggedIn.value) {
                await init();
            } else {
                // 未登录时才从localStorage加载API Key（用于下次登录）
                const savedKey = localStorage.getItem('zhipu_api_key');
                if (savedKey) {
                    apiKeyInput.value = savedKey;
                    hasApiKey.value = true;
                }
            }
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
            showApiKey,
            hasApiKey,
            saveApiKey,
            deleteApiKey,
            showLoginModal,
            isLoggedIn,
            isLoginMode,
            authUsername,
            authPassword,
            authPasswordConfirm,
            showAuthPassword,
            showAuthPasswordConfirm,
            showForgotPasswordModal,
            forgotUsername,
            newResetPassword,
            newResetPasswordConfirm,
            showNewPassword,
            showNewPasswordConfirm,
            currentUserId,
            currentUsername,
            remainingQuota,
            showSettingsModal,
            oldPassword,
            newPassword,
            newPasswordConfirm,
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
            loadApiKey,
            login,
            register,
            logout,
            resetPassword,
            changePassword
        };
    }
}).mount('#app');