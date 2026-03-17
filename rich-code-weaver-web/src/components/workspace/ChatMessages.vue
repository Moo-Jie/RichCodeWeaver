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
        <!-- User Message -->
        <div v-if="msg.type === 'user'" class="msg-user">
          <div class="msg-bubble msg-bubble--user">{{ msg.content }}</div>
          <a-avatar :size="32" :src="loginUser.userAvatar" class="msg-avatar">
            {{ loginUser.userName?.charAt(0) || 'U' }}
          </a-avatar>
        </div>
        <!-- AI Message -->
        <div v-else class="msg-ai">
          <a-avatar :size="32" :src="aiAvatarSrc" class="msg-avatar msg-avatar--ai" />
          <div class="msg-bubble msg-bubble--ai">
            <MarkdownRenderer v-if="msg.content" :content="msg.content" />
            <div v-if="msg.loading" class="msg-loading">
              <a-spin size="small" />
              <span>AI 正在编织灵感...</span>
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
import {ref, watch, nextTick} from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import aiAvatarSrc from '@/assets/aiAvatar.png'

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
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

const scrollToBottom = () => {
  if (containerRef.value) {
    containerRef.value.scrollTop = containerRef.value.scrollHeight
  }
}

watch(() => props.messages.length, () => {
  nextTick(scrollToBottom)
})

defineExpose({scrollToBottom})
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
  padding: 16px 24px;
  scroll-behavior: smooth;
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
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-user {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 10px;
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

.msg-bubble {
  max-width: 70%;
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

.msg-bubble--ai {
  background: #f7f7f8;
  color: #1a1a1a;
  border-bottom-left-radius: 4px;
  border: 1px solid #f0f0f0;
}

.msg-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
  margin-top: 4px;
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
