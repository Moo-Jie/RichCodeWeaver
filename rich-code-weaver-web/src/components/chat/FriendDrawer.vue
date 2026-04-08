<template>
  <a-drawer
    v-model:open="chatStore.friendDrawerVisible"
    title="好友"
    placement="left"
    :width="360"
    :bodyStyle="{ padding: 0 }"
    :headerStyle="{ borderBottom: '1px solid #f0f0f0' }"
  >
    <!-- Tab Navigation -->
    <div class="friend-tabs">
      <div
        :class="['friend-tab', { active: activeTab === 'list' }]"
        @click="activeTab = 'list'"
      >
        <TeamOutlined />
        <span>好友列表</span>
      </div>
      <div
        :class="['friend-tab', { active: activeTab === 'pending' }]"
        @click="activeTab = 'pending'"
      >
        <a-badge :count="chatStore.pendingCount" :offset="[6, -2]" size="small">
          <BellOutlined />
        </a-badge>
        <span>好友申请</span>
      </div>
      <div
        :class="['friend-tab', { active: activeTab === 'search' }]"
        @click="activeTab = 'search'"
      >
        <SearchOutlined />
        <span>添加好友</span>
      </div>
    </div>

    <!-- Friend List Tab -->
    <div v-show="activeTab === 'list'" class="friend-content">
      <div v-if="chatStore.friends.length === 0" class="empty-state">
        <UserOutlined class="empty-icon" />
        <p>暂无好友</p>
        <a-button type="link" size="small" @click="activeTab = 'search'">去添加好友</a-button>
      </div>
      <div v-else class="friend-list">
        <div
          v-for="friend in chatStore.friends"
          :key="friend.id"
          class="friend-item"
        >
          <a-avatar :size="38" :src="friend.friendAvatar">
            {{ friend.friendName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="friend-info">
            <div class="friend-name">{{ friend.friendName }}</div>
          </div>
          <div class="friend-actions">
            <a-tooltip title="发消息">
              <a-button type="text" size="small" @click="startChat(friend)">
                <MessageOutlined />
              </a-button>
            </a-tooltip>
            <a-popconfirm title="确定要删除该好友吗？" @confirm="doRemoveFriend(friend)">
              <a-tooltip title="删除好友">
                <a-button type="text" size="small" danger>
                  <DeleteOutlined />
                </a-button>
              </a-tooltip>
            </a-popconfirm>
          </div>
        </div>
      </div>
    </div>

    <!-- Pending Requests Tab -->
    <div v-show="activeTab === 'pending'" class="friend-content">
      <div v-if="chatStore.pendingRequests.length === 0" class="empty-state">
        <BellOutlined class="empty-icon" />
        <p>暂无好友申请</p>
      </div>
      <div v-else class="friend-list">
        <div
          v-for="req in chatStore.pendingRequests"
          :key="req.id"
          class="friend-item pending-item"
        >
          <a-avatar :size="38" :src="req.friendAvatar">
            {{ req.friendName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="friend-info">
            <div class="friend-name">{{ req.friendName }}</div>
            <div class="friend-remark" v-if="req.remark">{{ req.remark }}</div>
          </div>
          <div class="friend-actions">
            <a-button type="primary" size="small" @click="doHandleRequest(req.id!, 1)">
              <CheckOutlined />
              同意
            </a-button>
            <a-button size="small" danger @click="doHandleRequest(req.id!, 2)">
              <CloseOutlined />
              拒绝
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Search / Add Friend Tab -->
    <div v-show="activeTab === 'search'" class="friend-content">
      <div class="search-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="输入用户名搜索"
          @search="doSearch"
          :loading="searching"
          allow-clear
        />
      </div>
      <div v-if="searchResults.length === 0 && searchDone" class="empty-state">
        <SearchOutlined class="empty-icon" />
        <p>未找到匹配的用户</p>
      </div>
      <div v-else class="friend-list">
        <div
          v-for="user in searchResults"
          :key="user.id"
          class="friend-item"
        >
          <a-avatar :size="38" :src="user.userAvatar">
            {{ user.userName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="friend-info">
            <div class="friend-name">{{ user.userName }}</div>
            <div class="friend-remark" v-if="user.userProfile">{{ user.userProfile }}</div>
          </div>
          <div class="friend-actions">
            <a-button
              type="primary"
              size="small"
              :disabled="isFriend(user.id!)"
              @click="doAddFriend(user.id!)"
            >
              <PlusOutlined />
              {{ isFriend(user.id!) ? '已是好友' : '添加' }}
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useChatStore } from '@/stores/chatStore'
import { searchUsers, sendFriendRequest, handleFriendRequest, removeFriend } from '@/api/friendController'
import {
  BellOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  MessageOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const chatStore = useChatStore()

/** 当前激活的tab: list / pending / search */
const activeTab = ref('list')
/** 搜索关键词 */
const searchKeyword = ref('')
/** 搜索结果列表 */
const searchResults = ref<API.UserVO[]>([])
/** 搜索加载状态 */
const searching = ref(false)
/** 是否已执行过搜索 */
const searchDone = ref(false)

/**
 * 搜索用户
 */
async function doSearch() {
  if (!searchKeyword.value.trim()) {
    message.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  searchDone.value = false
  try {
    const res = await searchUsers({ keyword: searchKeyword.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      searchResults.value = res.data.data
    } else {
      searchResults.value = []
    }
    searchDone.value = true
  } catch (e) {
    message.error('搜索用户失败')
  } finally {
    searching.value = false
  }
}

/**
 * 发送好友申请
 */
async function doAddFriend(friendId: number) {
  try {
    const res = await sendFriendRequest({ friendId, remark: '' })
    if (res.data.code === 0) {
      message.success('好友申请已发送')
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch (e) {
    message.error('发送好友申请失败')
  }
}

/**
 * 处理好友申请（同意/拒绝）
 * @param id 好友关系id
 * @param action 1=同意, 2=拒绝
 */
async function doHandleRequest(id: number, action: number) {
  try {
    const res = await handleFriendRequest({ id, action })
    if (res.data.code === 0) {
      message.success(action === 1 ? '已同意好友申请' : '已拒绝好友申请')
      // 刷新列表
      chatStore.fetchPendingRequests()
      if (action === 1) {
        chatStore.fetchFriends()
      }
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('处理好友申请失败')
  }
}

/**
 * 删除好友
 */
async function doRemoveFriend(friend: API.UserFriendshipVO) {
  try {
    const res = await removeFriend({ friendId: friend.friendId! })
    if (res.data.code === 0) {
      message.success('已删除好友')
      chatStore.fetchFriends()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    message.error('删除好友失败')
  }
}

/**
 * 发起聊天：关闭好友面板，打开聊天面板并选中与该好友的会话
 */
function startChat(friend: API.UserFriendshipVO) {
  if (friend.friendId) {
    chatStore.startChatWithFriend(friend.friendId)
  }
}

/**
 * 检查是否已是好友
 */
function isFriend(userId: number): boolean {
  return chatStore.friends.some(
    (f) => f.friendId === userId
  )
}
</script>

<style scoped>
.friend-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.friend-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.friend-tab:hover {
  color: #333;
  background: #f0f0f0;
}

.friend-tab.active {
  color: #1a1a1a;
  font-weight: 500;
  border-bottom-color: #1a1a1a;
  background: #fff;
}

.friend-content {
  padding: 8px 0;
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #999;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 12px;
  color: #d9d9d9;
}

.empty-state p {
  margin: 0 0 8px;
  font-size: 14px;
}

.friend-list {
  padding: 0 8px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.friend-item:hover {
  background: #f5f5f5;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-remark {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.pending-item .friend-actions {
  gap: 6px;
}

.search-bar {
  padding: 12px 16px;
}
</style>
