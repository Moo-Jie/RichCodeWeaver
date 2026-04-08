<template>
  <a-modal
    v-model:open="chatStore.chatDrawerVisible"
    title="消息"
    centered
    :footer="null"
    :width="468"
    :bodyStyle="{ padding: 0, height: '78vh' }"
    wrapClassName="chat-dialog-modal"
  >
    <div class="chat-modal-body">
      <!-- Conversation List View -->
      <div v-if="!activeChatUser && !pendingChatMode" class="conversation-view">
        <div v-if="chatStore.conversations.length === 0" class="empty-state">
          <MessageOutlined class="empty-icon" />
          <p>暂无会话</p>
          <p class="empty-hint">去好友列表发起聊天吧</p>
        </div>
        <div v-if="chatStore.conversations.length > 0" class="conversation-list">
          <div
            v-for="conv in chatStore.conversations"
            :key="conv.id"
            :class="['conversation-item', { 'conversation-item-active': chatStore.activeConversationId === conv.id }]"
            @click="openChat(conv)"
          >
            <div class="conv-info">
              <div class="conv-header">
                <span class="conv-name">{{ conv.targetUserName }}</span>
                <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <div class="conv-preview">
                <span class="conv-message">{{ conv.lastMessageContent || '暂无消息' }}</span>
                <a-badge
                  v-if="conv.unreadCount && conv.unreadCount > 0"
                  :count="conv.unreadCount"
                  :numberStyle="{ fontSize: '11px', minWidth: '18px', height: '18px', lineHeight: '18px' }"
                />
              </div>
            </div>
            <a-avatar :size="48" :src="conv.targetUserAvatar" style="margin-left: 12px;">
              {{ conv.targetUserName?.charAt(0) || 'U' }}
            </a-avatar>
          </div>
        </div>
      </div>

      <!-- Pending Chat Mode: new conversation with a friend -->
      <div v-if="pendingChatMode" class="chat-view">
        <div class="chat-header">
          <a-button type="text" size="small" @click="cancelPendingChat">
            <ArrowLeftOutlined />
          </a-button>
          <span class="chat-title">{{ pendingFriendInfo?.friendName || '新对话' }}</span>
          <div class="chat-header-spacer"></div>
          <a-avatar :size="36" :src="pendingFriendInfo?.friendAvatar" style="margin-left: 12px;">
            {{ pendingFriendInfo?.friendName?.charAt(0) || 'U' }}
          </a-avatar>
        </div>
        <div class="chat-messages">
          <div class="empty-state" style="padding: 32px 16px">
            <MessageOutlined class="empty-icon" style="font-size: 28px" />
            <p style="font-size: 13px">发送第一条消息开始聊天</p>
          </div>
        </div>
        <div class="chat-input-area">
          <a-textarea
            v-model:value="inputMessage"
            placeholder="输入消息..."
            :auto-size="{ minRows: 1, maxRows: 4 }"
            @pressEnter="handlePendingSend"
          />
          <a-button
            type="primary"
            :disabled="!inputMessage.trim()"
            @click="handlePendingSend"
          >
            <SendOutlined />
          </a-button>
        </div>
      </div>

      <!-- Chat Window View -->
      <div v-else-if="activeChatUser" class="chat-view">
        <!-- Chat Header -->
        <div class="chat-header">
          <a-button type="text" size="small" @click="closeChat">
            <ArrowLeftOutlined />
          </a-button>
          <span class="chat-title">{{ activeChatUser.targetUserName }}</span>
          <div class="chat-header-spacer"></div>
          <a-avatar :size="36" :src="activeChatUser.targetUserAvatar" style="margin-left: 12px;">
            {{ activeChatUser.targetUserName?.charAt(0) || 'U' }}
          </a-avatar>
        </div>

        <!-- Messages Area -->
        <div ref="messagesContainer" class="chat-messages">
          <div v-if="loadingMessages" class="loading-state">
            <a-spin size="small" />
            <span>加载消息中...</span>
          </div>
          <div v-if="hasMoreMessages && !loadingMessages" class="load-more">
            <a-button type="link" size="small" @click="loadMoreMessages">加载更多</a-button>
          </div>
          <div
            v-for="msg in chatStore.currentMessages"
            :key="msg.id"
            :class="['message-item', { 'message-self': msg.senderId === currentUserId }]"
          >
            <a-avatar :size="30" :src="msg.senderAvatar" v-if="msg.senderId !== currentUserId" style="margin-left: 12px;">
              {{ msg.senderName?.charAt(0) || 'U' }}
            </a-avatar>
            <div class="message-bubble">
              <div v-if="msg.messageType === 'collab_invite'" class="collab-message-card">
                <div class="collab-message-title">
                  {{ msg.senderId === currentUserId ? '你发起了协作邀请' : `${msg.senderName || '对方'} 邀请你协作` }}
                </div>
                <div class="collab-message-app">
                  <img v-if="msg.appCover" :src="msg.appCover" alt="" class="collab-app-cover" />
                  <div class="collab-message-app-info">
                    <div class="collab-message-app-name">{{ msg.appName || '未知产物' }}</div>
                    <div class="collab-message-app-meta">
                      <FileOutlined class="invite-app-icon" />
                      <span>{{ getCollabRoleLabel(msg.collabRole) }}</span>
                    </div>
                  </div>
                  <a-tag :color="getCollabStatusColor(msg.collabStatus)">
                    {{ getCollabStatusLabel(msg.collabStatus) }}
                  </a-tag>
                </div>
                <div v-if="canHandleCollabInvite(msg)" class="invite-actions invite-actions-message">
                  <a-button type="primary" size="small" @click="handleCollabAction(msg, 1)">
                    <CheckOutlined />
                    接受
                  </a-button>
                  <a-button size="small" danger @click="handleCollabAction(msg, 2)">
                    <CloseOutlined />
                    拒绝
                  </a-button>
                </div>
              </div>
              <div v-else class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <a-avatar :size="30" :src="msg.senderAvatar" v-if="msg.senderId === currentUserId" style="margin-left: 12px;">
              {{ msg.senderName?.charAt(0) || 'U' }}
            </a-avatar>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input-area">
          <a-textarea
            v-model:value="inputMessage"
            placeholder="输入消息..."
            :auto-size="{ minRows: 1, maxRows: 4 }"
            @pressEnter="handleSend"
          />
          <a-button
            type="primary"
            :disabled="!inputMessage.trim()"
            @click="handleSend"
          >
            <SendOutlined />
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, watch, nextTick, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useChatStore } from '@/stores/chatStore'
import { useLoginUserStore } from '@/stores/loginUser'
import { listMessages, markAsRead } from '@/api/chatController'
import { handleCollabInvitation } from '@/api/collaboratorController'
import {
  ArrowLeftOutlined,
  CheckOutlined,
  CloseOutlined,
  FileOutlined,
  MessageOutlined,
  SendOutlined
} from '@ant-design/icons-vue'

const chatStore = useChatStore()
const loginUserStore = useLoginUserStore()

/** 当前登录用户id */
const currentUserId = computed(() => loginUserStore.loginUser?.id)
/** 当前聊天对象（会话信息） */
const activeChatUser = ref<API.ChatConversationVO | null>(null)
/** 输入框内容 */
const inputMessage = ref('')
/** 消息加载状态 */
const loadingMessages = ref(false)
/** 是否有更多历史消息 */
const hasMoreMessages = ref(false)
/** 当前页码 */
const currentPage = ref(1)
/** 消息容器引用 */
const messagesContainer = ref<HTMLElement | null>(null)

/** 是否处于待发起聊天模式（从好友列表发起新对话） */
const pendingChatMode = computed(() => {
  return chatStore.pendingChatFriendId !== null && !activeChatUser.value
})

/** 待聊天好友的信息 */
const pendingFriendInfo = computed(() => {
  if (!chatStore.pendingChatFriendId) return null
  return chatStore.friends.find((f) => f.friendId === chatStore.pendingChatFriendId)
})

/**
 * 打开聊天窗口
 * @param conv 会话信息
 */
async function openChat(conv: API.ChatConversationVO) {
  activeChatUser.value = conv
  chatStore.selectConversation(conv.id!)
  currentPage.value = 1
  await loadMessages()
  // 标记已读
  if (conv.id) {
    await markAsRead({ conversationId: conv.id })
    chatStore.sendWsRead(conv.id)
    // 刷新会话列表以更新未读数
    chatStore.fetchConversations()
    chatStore.fetchUnreadCount()
  }
}

/**
 * 关闭聊天窗口回到会话列表
 */
function closeChat() {
  activeChatUser.value = null
  chatStore.clearCurrentMessages()
  chatStore.fetchConversations()
}

/**
 * 加载消息
 */
async function loadMessages() {
  if (!chatStore.activeConversationId) return
  loadingMessages.value = true
  try {
    const res = await listMessages({
      conversationId: chatStore.activeConversationId,
      pageNum: currentPage.value,
      pageSize: 50
    })
    if (res.data.code === 0 && res.data.data) {
      const page = res.data.data
      if (currentPage.value === 1) {
        chatStore.currentMessages = page.records || []
      } else {
        // 加载更多时，将旧消息插入到前面
        chatStore.currentMessages = [...(page.records || []), ...chatStore.currentMessages]
      }
      hasMoreMessages.value = (page.pageNumber || 1) < (page.totalPage || 1)
    }
  } catch (e) {
    message.error('加载消息失败')
  } finally {
    loadingMessages.value = false
    // 首次加载滚动到底部
    if (currentPage.value === 1) {
      await nextTick()
      scrollToBottom()
    }
  }
}

/**
 * 加载更多历史消息
 */
async function loadMoreMessages() {
  currentPage.value++
  await loadMessages()
}

/**
 * 发送消息
 */
async function handleSend(e?: KeyboardEvent) {
  // Shift+Enter 换行，Enter 发送
  if (e && e.shiftKey) return
  if (e) e.preventDefault()

  const content = inputMessage.value.trim()
  if (!content || !activeChatUser.value?.targetUserId) return

  try {
    await chatStore.sendWsMessage(activeChatUser.value.targetUserId, content)
    inputMessage.value = ''
  } catch (error) {
    message.error('消息发送失败，请稍后重试')
  }
}

/**
 * 滚动消息到底部
 */
function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/**
 * 格式化时间显示
 */
function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 监听新消息自动滚动
watch(
  () => chatStore.currentMessages.length,
  () => {
    nextTick(() => scrollToBottom())
  }
)

/**
 * 待聊天模式下发送消息：通过WebSocket发送，创建新会话
 * 发送后保持聊天视图，乐观展示已发消息，轮询等待后端创建会话后自动切换
 */
async function handlePendingSend(e?: KeyboardEvent) {
  if (e && e.shiftKey) return
  if (e) e.preventDefault()
  const content = inputMessage.value.trim()
  if (!content || !chatStore.pendingChatFriendId) return

  const friendId = chatStore.pendingChatFriendId
  try {
    await chatStore.sendWsMessage(friendId, content)
    inputMessage.value = ''
  } catch (error) {
    message.error('消息发送失败，请稍后重试')
    return
  }

  if (!chatStore.activeConversationId && !chatStore.currentMessages.some((item) => !item.id)) {
    chatStore.currentMessages.push({
      senderId: currentUserId.value,
      content: content,
      messageType: 'text',
      createTime: new Date().toISOString()
    } as API.ChatMessageVO)
  }

  // 轮询等待后端创建会话，最多尝试5次
  const pollForConversation = async (attempts: number) => {
    if (attempts <= 0) return
    await chatStore.fetchConversations()
    const conv = chatStore.conversations.find((c) => c.targetUserId === friendId)
    if (conv) {
      chatStore.pendingChatFriendId = null
      openChat(conv)
    } else {
      setTimeout(() => pollForConversation(attempts - 1), 800)
    }
  }
  setTimeout(() => pollForConversation(5), 600)
}

/**
 * 取消待聊天模式，回到会话列表
 */
function cancelPendingChat() {
  chatStore.pendingChatFriendId = null
}

/**
 * 处理协作邀请（接受/拒绝）
 */
async function handleCollabAction(msg: API.ChatMessageVO, action: number) {
  if (!msg.collabId) return
  try {
    const res = await handleCollabInvitation({ id: msg.collabId, action })
    if (res.data.code === 0) {
      message.success(action === 1 ? '已接受协作邀请' : '已拒绝协作邀请')
      chatStore.currentMessages = chatStore.currentMessages.map((item) => {
        if (item.id === msg.id) {
          return {
            ...item,
            collabStatus: action === 1 ? 1 : 2
          }
        }
        return item
      })
      chatStore.fetchConversations()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('处理协作邀请失败')
  }
}

function canHandleCollabInvite(msg: API.ChatMessageVO) {
  return msg.messageType === 'collab_invite' && msg.receiverId === currentUserId.value && msg.collabStatus === 0
}

function getCollabRoleLabel(role?: string) {
  return role === 'viewer' ? '查看者' : '编辑者'
}

function getCollabStatusLabel(status?: number) {
  if (status === 1) return '已接受'
  if (status === 2) return '已拒绝'
  if (status === 3) return '已移除'
  return '待处理'
}

function getCollabStatusColor(status?: number) {
  if (status === 1) return 'green'
  if (status === 2) return 'red'
  if (status === 3) return 'default'
  return 'gold'
}

/**
 * 监听pendingChatFriendId和抽屉可见性
 * 当从好友列表发起聊天时：查找已有会话并打开，否则停留在待聊天模式
 */
watch(
  () => [chatStore.pendingChatFriendId, chatStore.chatDrawerVisible] as const,
  async ([friendId, visible]) => {
    if (friendId && visible) {
      await chatStore.fetchConversations()
      const existing = chatStore.conversations.find(
        (c) => c.targetUserId === friendId
      )
      if (existing) {
        // 找到已有会话，直接打开并加载消息
        chatStore.pendingChatFriendId = null
        openChat(existing)
      }
      // 否则停留在pendingChatMode，用户可发送第一条消息
    }
  },
  { immediate: true }
)

// 抽屉关闭时清理状态
watch(
  () => chatStore.chatDrawerVisible,
  (visible) => {
    if (!visible) {
      activeChatUser.value = null
      chatStore.clearCurrentMessages()
    }
  }
)
</script>

<style scoped>
.chat-modal-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Conversation List */
.conversation-view {
  flex: 1;
  overflow-y: auto;
}

.conversation-list {
  padding: 10px 12px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 16px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.15s ease, box-shadow 0.15s ease;
}

.conversation-item:hover {
  background: #f5f5f5;
}

.conversation-item-active {
  background: #f3f7ff;
  box-shadow: inset 0 0 0 1px #d6e4ff;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-name {
  font-size: 17px;
  font-weight: 500;
  color: #1a1a1a;
}

.conv-time {
  font-size: 13px;
  color: #bbb;
  flex-shrink: 0;
}

.conv-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.conv-message {
  font-size: 15px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 8px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72px 20px;
  color: #999;
}

.empty-icon {
  font-size: 44px;
  margin-bottom: 16px;
  color: #d9d9d9;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.empty-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #bbb;
}

/* Chat View */
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fff;
}

.chat-header-spacer {
  flex: 1;
}

.chat-title {
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
}

/* Messages Area */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #fafbfc;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  color: #999;
  font-size: 13px;
}

.load-more {
  text-align: center;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
}

.message-self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(78%, 340px);
  display: flex;
  flex-direction: column;
}

.message-self .message-bubble {
  align-items: flex-end;
}

.message-content {
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 16px;
  line-height: 1.5;
  word-break: break-word;
  background: #f0f0f0;
  color: #1a1a1a;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.message-self .message-content {
  background: #1677ff;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #bbb;
  margin-top: 6px;
  padding: 0 4px;
}

.message-self .message-time {
  text-align: right;
}

.invite-app-icon {
  color: #999;
}

.collab-message-card {
  padding: 16px;
  border-radius: 18px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.message-self .collab-message-card {
  background: rgba(22, 119, 255, 0.12);
  border-color: rgba(22, 119, 255, 0.22);
}

.collab-message-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.message-self .collab-message-title {
  color: #fff;
}

.collab-message-app {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collab-app-cover {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

.collab-message-app-info {
  flex: 1;
  min-width: 0;
}

.collab-message-app-name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.message-self .collab-message-app-name {
  color: #fff;
}

.collab-message-app-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.message-self .collab-message-app-meta {
  color: rgba(255, 255, 255, 0.85);
}

.invite-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.invite-actions-message {
  margin-top: 12px;
}

/* Input Area */
.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fff;
}

.chat-input-area :deep(.ant-input) {
  border-radius: 16px;
  resize: none;
  font-size: 16px;
  padding: 11px 14px;
}

.chat-input-area .ant-btn {
  border-radius: 16px;
  height: 44px;
  min-width: 44px;
  padding: 0 16px;
}
</style>
