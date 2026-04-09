<template>
  <div class="chat-messages">
    <!-- Load More -->
    <div v-if="hasMore || loadingHistory" class="load-more">
      <button v-if="hasMore && !loadingHistory" class="load-more-btn" @click="$emit('loadMore')">
        加载更多历史消息
      </button>
      <a-spin v-if="loadingHistory" size="small" />
    </div>

    <!-- Messages -->
    <div ref="containerRef" class="messages-scroll">
      <div v-for="(msg, index) in messages" :key="index" class="message-row">
        <div v-if="msg.type === 'user' && msg.isSelf" class="msg-user msg-user--self">
          <div class="msg-content-wrap msg-content-wrap--self">
            <div class="msg-meta msg-meta--self">
              <span class="msg-sender">{{ msg.senderName || loginUser.userName || '我' }}</span>
              <span v-if="msg.senderRoleLabel" class="msg-role-pill">{{ msg.senderRoleLabel }}</span>
            </div>
            <div class="msg-bubble msg-bubble--user">{{ msg.content }}</div>
          </div>
          <a-avatar :size="32" :src="msg.senderAvatar || loginUser.userAvatar" class="msg-avatar">
            {{ (msg.senderName || loginUser.userName)?.charAt(0) || 'U' }}
          </a-avatar>
        </div>
        <div v-else-if="msg.type === 'user'" class="msg-user msg-user--other">
          <a-avatar :size="32" :src="msg.senderAvatar" class="msg-avatar">
            {{ msg.senderName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="msg-content-wrap">
            <div class="msg-meta">
              <span class="msg-sender">{{ msg.senderName || '协作者' }}</span>
              <span v-if="msg.senderRoleLabel" class="msg-role-pill">{{ msg.senderRoleLabel }}</span>
            </div>
            <div class="msg-bubble msg-bubble--other-user">{{ msg.content }}</div>
          </div>
        </div>
        <div v-else class="msg-ai">
          <a-avatar :size="32" :src="aiAvatarSrc" class="msg-avatar msg-avatar--ai" />
          <div class="msg-bubble msg-bubble--ai">
            <MarkdownRenderer v-if="msg.content" :content="msg.content" />
            <div v-if="msg.loading" class="msg-loading">
              <div class="loading-dots">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
              <span class="loading-text">AI 正在编织灵感...</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="messages.length === 0" class="empty-messages">
        <p class="empty-text">开始对话，创造无限可能</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import aiAvatarSrc from '@/assets/aiAvatar.png'

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  senderName?: string
  senderAvatar?: string
  senderRoleLabel?: string
  isSelf?: boolean
}

interface Props {
  messages: Message[]
  loadingHistory: boolean
  hasMore: boolean
  loginUser: API.LoginUserVO
}

const props = defineProps<Props>()
defineEmits(['loadMore'])

const containerRef = ref<HTMLElement>()
const userScrolledUp = ref(false)
let lastScrollHeight = 0
let scrollTimeout: number | null = null

// 检测用户是否主动向上滚动
const handleScroll = () => {
  if (!containerRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = containerRef.value
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  
  // 如果用户距离底部超过 100px，认为用户主动向上滚动
  userScrolledUp.value = distanceFromBottom > 100
  
  // 清除之前的定时器
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
  
  // 如果用户滚动到底部附近，重置标记
  scrollTimeout = window.setTimeout(() => {
    if (distanceFromBottom < 50) {
      userScrolledUp.value = false
    }
  }, 150)
}

const scrollToBottom = (force = false) => {
  if (!containerRef.value) return
  
  const container = containerRef.value
  const { scrollTop, scrollHeight, clientHeight } = container
  
  // 强制滚动（新消息添加时）
  if (force) {
    // 使用 requestAnimationFrame 确保 DOM 更新后再滚动，防止抽搐
    requestAnimationFrame(() => {
      container.scrollTop = container.scrollHeight
    })
    return
  }
  
  // 智能滚动：仅当用户未主动向上滚动时才自动置底
  if (!userScrolledUp.value) {
    const isNearBottom = scrollHeight - scrollTop - clientHeight < 100
    if (isNearBottom) {
      requestAnimationFrame(() => {
        container.scrollTop = container.scrollHeight
      })
    }
  }
}

// 监听消息变化（新消息）
watch(() => props.messages.length, () => {
  nextTick(() => scrollToBottom(true))
})

// 监听消息内容变化（流式更新）
watch(() => props.messages.map(m => m.content).join(''), () => {
  if (!containerRef.value) return
  
  const currentScrollHeight = containerRef.value.scrollHeight
  
  // 内容增长时的智能滚动
  if (currentScrollHeight > lastScrollHeight) {
    nextTick(() => scrollToBottom(false))
  }
  
  lastScrollHeight = currentScrollHeight
  
  // 滚动到任务列表中的进行中任务
  nextTick(() => {
    const taskScrollTarget = containerRef.value?.querySelector('.task-scroll-target')
    if (taskScrollTarget && !userScrolledUp.value) {
      taskScrollTarget.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  })
})

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('scroll', handleScroll, { passive: true })
    lastScrollHeight = containerRef.value.scrollHeight
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScroll)
  }
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
})

defineExpose({ scrollToBottom })
</script>

<style scoped>
.chat-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  flex-shrink: 0;
}

.load-more-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 16px;
  transition: all 0.2s ease;
}

.load-more-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.messages-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 24px;
  scroll-behavior: smooth;
  /* 防止内容突然变化导致的抽搐 */
  will-change: scroll-position;
  contain: layout style paint;
}

.messages-scroll::-webkit-scrollbar {
  width: 4px;
}

.messages-scroll::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 4px;
}

.message-row {
  margin-bottom: 20px;
  animation: msgFadeIn 0.3s ease;
}

@keyframes msgFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.msg-user {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.msg-user--self {
  justify-content: flex-end;
}

.msg-user--other {
  justify-content: flex-start;
}

.msg-ai {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 10px;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-avatar--ai {
  background: #f5f5f5;
}

.msg-content-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.msg-content-wrap--self {
  align-items: flex-end;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 20px;
}

.msg-meta--self {
  justify-content: flex-end;
}

.msg-sender {
  font-size: 12px;
  font-weight: 600;
  color: #24292f;
}

.msg-role-pill {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
  font-size: 10px;
  font-weight: 600;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 14px;
}

.msg-bubble--user {
  background: #1a1a1a;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-bubble--other-user {
  background: #f6f8fa;
  color: #1f2328;
  border: 1px solid #e6ebf1;
  border-bottom-left-radius: 4px;
}

.msg-bubble--ai {
  background: #f7f7f8;
  color: #1a1a1a;
  border-bottom-left-radius: 4px;
  border: 1px solid #f0f0f0;
}

.msg-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}

.loading-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: dotPulse 1.4s infinite ease-in-out;
}

.loading-dots .dot:nth-child(1) {
  animation-delay: 0s;
}

.loading-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dotPulse {
  0%, 60%, 100% {
    transform: scale(1);
    opacity: 0.6;
  }
  30% {
    transform: scale(1.3);
    opacity: 1;
  }
}

.loading-text {
  color: #999;
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
}

.empty-text {
  color: #ccc;
  font-size: 15px;
}
</style>
