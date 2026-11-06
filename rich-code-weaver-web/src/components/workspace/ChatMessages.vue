<template>
  <div class="chat-messages">
    <!-- Load More -->
    <div v-if="hasMore || loadingHistory" class="load-more">
      <button v-if="hasMore && !loadingHistory" class="load-more-btn" @click="$emit('loadMore')">
        加载更多历史消息
      </button>
      <div v-if="loadingHistory && messages.length === 0" class="history-loading-state">
        <span class="history-loading-spinner" aria-hidden="true"></span>
        <span>正在加载对话记录...</span>
        <span class="history-loading-track" aria-hidden="true"><span></span></span>
      </div>
      <a-spin v-else-if="loadingHistory" size="small" />
    </div>

    <!-- Messages -->
    <div ref="containerRef" class="messages-scroll" @scroll.passive="handleScroll">
      <div
        v-for="msg in messages"
        :key="msg.id"
        v-memo="[
          msg.id,
          msg.content,
          msg.loading,
          msg.type,
          msg.senderName,
          msg.senderAvatar,
          msg.senderRoleLabel,
          msg.isSelf,
          loginUser.userName,
          loginUser.userAvatar,
          collapseLongCode,
        ]"
        class="message-row"
      >
        <div v-if="msg.type === 'user' && msg.isSelf" class="msg-user msg-user--self">
          <div class="msg-content-wrap msg-content-wrap--self">
            <div class="msg-meta msg-meta--self">
              <span class="msg-sender">{{ msg.senderName || loginUser.userName || '我' }}</span>
              <span v-if="msg.senderRoleLabel" class="msg-role-pill">{{
                msg.senderRoleLabel
              }}</span>
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
              <span v-if="msg.senderRoleLabel" class="msg-role-pill">{{
                msg.senderRoleLabel
              }}</span>
            </div>
            <div class="msg-bubble msg-bubble--other-user">{{ msg.content }}</div>
          </div>
        </div>
        <div v-else class="msg-ai">
          <a-avatar :size="32" :src="aiAvatarSrc" class="msg-avatar msg-avatar--ai" />
          <div class="msg-bubble msg-bubble--ai">
            <MarkdownRenderer
              v-if="msg.content"
              :content="msg.content"
              :streaming="msg.loading"
              :collapse-long-code="collapseLongCode"
              :auto-collapse-sections="true"
            />
            <div v-if="msg.loading && !msg.content" class="msg-loading-state">
              <span class="msg-loading-spinner" aria-hidden="true"></span>
              <div class="msg-loading-copy">
                <strong>正在生成内容</strong>
                <span>正在连接模型并准备上下文...</span>
              </div>
            </div>
            <div v-else-if="msg.loading" class="msg-stream-status" role="status" aria-live="polite">
              <span class="stream-status-dot" aria-hidden="true"></span>
              <span>正在输出</span>
              <span class="stream-status-dots" aria-hidden="true">
                <span></span><span></span><span></span>
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="messages.length === 0 && !loadingHistory" class="empty-messages">
        <p class="empty-text">开始对话，创造无限可能</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onUnmounted, ref, watch } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import aiAvatarSrc from '@/assets/aiAvatar.png'

interface Message {
  id: string
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
  collapseLongCode?: boolean
}

const props = defineProps<Props>()
defineEmits(['loadMore'])

const containerRef = ref<HTMLElement>()
const userScrolledUp = ref(false)
let lastScrollHeight = 0
let scrollTimeout: number | null = null
let scrollFrameId: number | null = null
let streamFollowFrameId: number | null = null
let lastTaskScrollAt = 0

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
  const container = containerRef.value
  if (!container) return
  const { scrollTop, scrollHeight, clientHeight } = container
  const isNearBottom = scrollHeight - scrollTop - clientHeight < 80
  if (!force && (userScrolledUp.value || !isNearBottom)) return
  if (scrollFrameId !== null) return

  scrollFrameId = requestAnimationFrame(() => {
    scrollFrameId = null
    const currentContainer = containerRef.value
    if (!currentContainer) return
    currentContainer.scrollTop = currentContainer.scrollHeight
    lastScrollHeight = currentContainer.scrollHeight
  })
}

// 新消息仅在用户位于底部时跟随；加载历史或阅读旧消息时不抢滚动位置。
watch(
  () => props.messages.length,
  (length, previousLength) => {
    if (length < previousLength) {
      lastScrollHeight = 0
      userScrolledUp.value = false
      return
    }
    if (length <= previousLength || props.loadingHistory || userScrolledUp.value) return
    nextTick(() => scrollToBottom(true))
  },
  { flush: 'post' },
)

// 流式内容高频更新时合并到单帧，避免每个 token 都触发布局查询与滚动。
const scheduleStreamFollow = () => {
  if (streamFollowFrameId !== null) return
  streamFollowFrameId = requestAnimationFrame(() => {
    streamFollowFrameId = null
    const container = containerRef.value
    if (!container || userScrolledUp.value) return

    const currentScrollHeight = container.scrollHeight
    if (currentScrollHeight > lastScrollHeight) {
      container.scrollTop = currentScrollHeight
    }
    lastScrollHeight = currentScrollHeight

    if (performance.now() - lastTaskScrollAt < 160) return
    const taskScrollTarget = container.querySelector('.task-scroll-target')
    if (taskScrollTarget) {
      lastTaskScrollAt = performance.now()
      taskScrollTarget.scrollIntoView({ behavior: 'auto', block: 'nearest' })
    }
  })
}

watch(() => props.messages[props.messages.length - 1]?.content || '', scheduleStreamFollow, {
  flush: 'post',
})

onUnmounted(() => {
  if (scrollFrameId !== null) {
    cancelAnimationFrame(scrollFrameId)
    scrollFrameId = null
  }
  if (streamFollowFrameId !== null) {
    cancelAnimationFrame(streamFollowFrameId)
    streamFollowFrameId = null
  }
  if (scrollTimeout !== null) {
    clearTimeout(scrollTimeout)
    scrollTimeout = null
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
  overflow-anchor: none;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: #e0e0e0 transparent;
  will-change: scroll-position;
  contain: layout style;
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
  contain: layout style;
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
  width: 100%;
  min-width: 0;
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
  flex: 1;
  min-width: 0;
  max-width: 900px;
  padding: 14px 16px;
  border: 1px solid #ececec;
  border-radius: 12px;
  background: #fff;
  color: #1a1a1a;
  box-shadow: none;
}

@media (max-width: 720px) {
  .messages-scroll {
    padding: 14px 12px;
  }

  .msg-content-wrap {
    max-width: calc(100% - 42px);
  }

  .msg-bubble {
    padding: 10px 12px;
    font-size: 13px;
  }

  .msg-bubble--ai {
    max-width: 100%;
  }
}

.history-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 0 22px;
  color: #666;
  font-size: 13px;
}

.history-loading-spinner,
.msg-loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #e5e5e5;
  border-top-color: #1a1a1a;
  border-radius: 50%;
  animation: cw-loading-spin 760ms linear infinite;
}

.history-loading-track {
  width: 140px;
  height: 2px;
  overflow: hidden;
  border-radius: 2px;
  background: #ececec;
}

.history-loading-track > span {
  display: block;
  width: 42%;
  height: 100%;
  border-radius: inherit;
  background: #1a1a1a;
  animation: cw-loading-sweep 1.15s ease-in-out infinite;
}

.msg-loading-state {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 6px 2px;
}

.msg-loading-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.msg-loading-copy strong {
  color: #333;
  font-size: 13px;
  font-weight: 600;
}

.msg-loading-copy span {
  color: #999;
  font-size: 11.5px;
}

.msg-stream-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: max-content;
  margin-top: 12px;
  padding: 6px 10px;
  border: 1px solid #ececec;
  border-radius: 999px;
  background: #fafafa;
  color: #666;
  font-size: 11.5px;
}

.stream-status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #1a1a1a;
  animation: cw-stream-dot-pulse 1.15s ease-in-out infinite;
}

.stream-status-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.stream-status-dots > span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #777;
  animation: cw-stream-dots 1.15s ease-in-out infinite;
}

.stream-status-dots > span:nth-child(2) {
  animation-delay: 0.14s;
}

.stream-status-dots > span:nth-child(3) {
  animation-delay: 0.28s;
}

@keyframes cw-loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes cw-loading-sweep {
  0% {
    transform: translateX(-120%);
  }
  100% {
    transform: translateX(340%);
  }
}

@keyframes cw-stream-dot-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes cw-stream-dots {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-2px);
  }
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
  0%,
  60%,
  100% {
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
