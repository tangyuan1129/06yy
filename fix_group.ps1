# -*- coding: utf-8 -*-
import sys

with open('src/main/resources/static/bai-e.html', 'r', encoding='utf-8') as f:
    html = f.read()

if 'remove-member-btn' in html:
    print('Changes already applied')
    sys.exit(0)

# Replace group member section
old_start = html.find("<div v-if=\"chatType === 'group'\">")
old_start = html.find("<div v-if=\"chatType === 'group'\">", old_start + 100)

if old_start > 0:
    # Find the end - look for mobile character detail comment
    end_marker = "<!-- 移动端角色详情页面 -->"
    old_end = html.find(end_marker)
    
    if old_end > 0:
        new_section = '''<div v-if="chatType === 'group'">
                    <div class="group-members-list">
                        <div v-for="member in currentGroupMembers" :key="member.id" class="group-member-item">
                            <div class="group-member-avatar" @click="showCharacterProfile(member)">
                                <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="member.name">
                                <span v-else>{{ member.name.charAt(0) }}</span>
                            </div>
                            <div class="member-name">{{ member.name }}</div>
                            <button class="remove-member-btn" @click="removeMemberFromGroup(member)" title="移除成员">x</button>
                        </div>
                    </div>
                    
                    <button class="add-member-trigger-btn" @click="showAddMemberModal = true">
                        + 添加成员
                    </button>
                </div>

            '''
        
        html = html[:old_start] + new_section + html[old_end:]
        print('Step 1: OK')

# Add modal
modal = '''
        <!-- 添加成员弹窗 -->
        <div class="modal-overlay" v-if="showAddMemberModal" @click.self="showAddMemberModal = false">
            <div class="modal">
                <div class="modal-title">添加成员</div>
                <input type="text" class="modal-input" v-model="searchAddMembers" placeholder="搜索角色..." style="margin-bottom: 15px;">
                <div class="character-select-grid" style="max-height: 300px; overflow-y: auto;">
                    <div v-if="filteredAddMembers.length === 0" style="padding: 10px; text-align: center; color: #888; grid-column: 1/-1;">
                        没有找到匹配的角色
                    </div>
                    <div class="character-option" v-for="char in filteredAddMembers" :key="char.id" :class="{ selected: selectedMemberToAdd === char.id }" @click="selectedMemberToAdd = char.id">
                        <div class="character-option-name">{{ char.name }}</div>
                    </div>
                </div>
                <div style="display: flex; gap: 10px; margin-top: 15px;">
                    <button class="modal-btn" @click="showAddMemberModal = false" style="flex: 1;">取消</button>
                    <button class="modal-btn modal-btn-primary" @click="addMemberToGroup" :disabled="!selectedMemberToAdd" style="flex: 1;">添加</button>
                </div>
            </div>
        </div>

        <!-- 创建群聊弹窗 -->'''

html = html.replace('<!-- 创建群聊弹窗 -->', modal)
print('Step 2: OK')

# Add CSS
css = '''
            .group-member-item { position: relative; display: flex; flex-direction: column; align-items: center; }
            .remove-member-btn { position: absolute; top: -5px; right: -5px; background: #e74c3c; color: white; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 14px; cursor: pointer; display: none; align-items: center; justify-content: center; transition: all 0.2s; z-index: 10; }
            .group-member-item:hover .remove-member-btn { display: flex; }
            .remove-member-btn:hover { background: #c0392b; transform: scale(1.1); }
            .add-member-trigger-btn { width: 100%; padding: 10px; margin-top: 15px; background: #07c160; color: white; border: none; border-radius: 5px; cursor: pointer; font-size: 14px; transition: background 0.2s; }
            .add-member-trigger-btn:hover { background: #06ad56; }
            .modal-btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-size: 14px; background: #e0e0e0; color: #333; }
            .modal-btn:hover { background: #d0d0d0; }
            .modal-btn:disabled { background: #ccc; cursor: not-allowed; }
            .modal-btn-primary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
            .modal-btn-primary:hover:not(:disabled) { background: linear-gradient(135deg, #5568d3 0%, #6a3f91 100%); }
'''

style_end = html.find('</style>')
html = html[:style_end] + css + html[style_end:]
print('Step 3: OK')

with open('src/main/resources/static/bai-e.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Done!')