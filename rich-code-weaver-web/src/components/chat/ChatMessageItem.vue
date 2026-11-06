<template>
  <article :class="['message-row', { 'is-self': isSelf }]">
    <UserAvatar
      v-if="!isSelf"
      :src="resolvedPeerAvatar"
      :name="resolvedPeerName"
      :size="30"
      class="message-avatar peer-avatar"
    />
    <span v-else class="message-avatar-spacer" />

    <div class="message-stack">
      <div v-if="msg.messageType === 'collab_invite'" class="event-card collab-card">
        <div class="event-kicker">
          <AppstoreOutlined />
          <span>协作邀请</span>
          <span :class="['event-status', `status-${msg.collabStatus ?? 0}`]">
            {{ collabStatusLabel }}
          </span>
        </div>
        <div class="event-main">
          <div class="event-cover">
            <img
              v-if="msg.appCover && !coverFailed"
              :src="msg.appCover"
              alt=""
              @error="coverFailed = true"
            />
            <AppstoreOutlined v-else />
          </div>
          <div class="event-copy">
            <strong>{{ msg.appName || '数字产物' }}</strong>
            <span>{{ isSelf ? '你发起邀请' : `${msg.senderName || peerName} 邀请你协作` }}</span>
            <span>{{ collabRoleLabel }}</span>
          </div>
        </div>
        <div v-if="canHandleInvite" class="event-actions">
          <a-button size="small" @click="$emit('collab-action', msg, 2)">拒绝</a-button>
          <a-button size="small" type="primary" @click="$emit('collab-action', msg, 1)"
            >接受</a-button
          >
        </div>
      </div>

      <div v-else-if="msg.messageType === 'app_forward'" class="event-card forward-card">
        <div class="event-kicker">
          <ExportOutlined />
          <span>产物分享</span>
        </div>
        <div class="event-main">
          <div class="event-cover">
            <img
              v-if="(msg.appCover || forwardPayload?.appCover) && !coverFailed"
              :src="msg.appCover || forwardPayload?.appCover"
              alt=""
              @error="coverFailed = true"
            />
            <AppstoreOutlined v-else />
          </div>
          <div class="event-copy">
            <strong>{{ msg.appName || forwardPayload?.appName || '数字产物' }}</strong>
            <span>{{ forwardTypeLabel }}</span>
          </div>
        </div>
        <div class="event-actions">
          <a-button size="small" @click="$emit('open-app', msg)">
            查看产物
            <ArrowRightOutlined />
          </a-button>
        </div>
      </div>

      <div v-else class="message-bubble">
        <template v-for="(part, index) in textParts" :key="index">
          <a
            v-if="part.type === 'link'"
            :href="part.text"
            target="_blank"
            rel="noopener noreferrer"
            >{{ part.text }}</a
          >
          <span v-else>{{ part.text }}</span>
        </template>
      </div>

      <div class="message-meta">
        <span>{{ formatFullTime(msg.createTime) }}</span>
        <template v-if="isSelf">
          <span v-if="msg.deliveryState === 'sending'" class="delivery sending">
            <LoadingOutlined spin />
            发送中
          </span>
          <span v-else-if="msg.deliveryState === 'failed'" class="delivery failed">
            <ExclamationCircleOutlined />
            {{ msg.deliveryError || '发送失败' }}
            <button type="button" class="retry-button" @click="$emit('retry', msg)">重试</button>
          </span>
          <span v-else class="delivery sent">
            <CheckOutlined />
            已发送
          </span>
        </template>
      </div>
    </div>

    <UserAvatar
      v-if="isSelf"
      :src="resolvedCurrentUserAvatar"
      :name="resolvedCurrentUserName"
      :size="30"
      class="message-avatar self-avatar"
    />
    <span v-else class="message-avatar-spacer" />
  </article>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
  ExportOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import UserAvatar from '@/components/chat/UserAvatar.vue'
import type { LocalChatMessage } from '@/stores/communicationStore'

interface ForwardPayload {
  appId?: number
  appName?: string
  appCover?: string
  codeGenType?: string
}

const props = defineProps<{
  msg: LocalChatMessage
  currentUserId?: number | string
  currentUserName?: string
  currentUserAvatar?: string
  peerName?: string
  peerAvatar?: string
}>()

defineEmits<{
  retry: [message: LocalChatMessage]
  'collab-action': [message: LocalChatMessage, action: number]
  'open-app': [message: LocalChatMessage]
}>()

const coverFailed = ref(false)
const isSelf = computed(() => String(props.msg.senderId) === String(props.currentUserId))
const resolvedPeerName = computed(() => props.msg.senderName || props.peerName || '会话用户')
const resolvedPeerAvatar = computed(() => props.msg.senderAvatar || props.peerAvatar)
const resolvedCurrentUserName = computed(
  () => props.msg.senderName || props.currentUserName || '我',
)
const resolvedCurrentUserAvatar = computed(() => props.msg.senderAvatar || props.currentUserAvatar)
const canHandleInvite = computed(
  () =>
    !isSelf.value &&
    String(props.msg.receiverId) === String(props.currentUserId) &&
    (props.msg.collabStatus ?? 0) === 0,
)
const collabStatusLabel = computed(() => {
  const status = props.msg.collabStatus ?? 0
  if (status === 1) return '已接受'
  if (status === 2) return '已拒绝'
  if (status === 3) return '已移除'
  return '待处理'
})
const collabRoleLabel = computed(() =>
  props.msg.collabRole === 'viewer' ? '查看者权限' : '编辑者权限',
)
const forwardPayload = computed<ForwardPayload | null>(() => {
  if (!props.msg.content) return null
  try {
    return JSON.parse(props.msg.content) as ForwardPayload
  } catch {
    return null
  }
})
const forwardTypeLabel = computed(() => {
  const codeGenType = forwardPayload.value?.codeGenType
  if (codeGenType === 'single_html') return '单文件产物'
  if (codeGenType === 'multi_file') return '多文件产物'
  if (codeGenType === 'vue_project') return 'Vue 工程产物'
  return '数字产物'
})
const textParts = computed(() => {
  const content = props.msg.content || ''
  const chunks = content.split(/(https?:\/\/[^\s]+)/g)
  return chunks
    .filter((chunk) => chunk !== '')
    .map((chunk) => ({
      type: /^https?:\/\//.test(chunk) ? ('link' as const) : ('text' as const),
      text: chunk,
    }))
})

function formatFullTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return ''
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(
  () => props.msg.appCover,
  () => {
    coverFailed.value = false
  },
)
</script>

<style scoped>
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  width: 100%;
  padding: 2px 0;
}

.message-row.is-self {
  justify-content: flex-end;
}

.message-avatar {
  margin-top: 1px;
}

.message-avatar-spacer {
  width: 30px;
  min-width: 30px;
}

.message-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  max-width: min(72%, 680px);
}

.is-self .message-stack {
  align-items: flex-end;
}

.message-bubble {
  max-width: 100%;
  padding: 9px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: #fff;
  color: #232323;
  font-size: 13.5px;
  line-height: 1.62;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.02);
}

.is-self .message-bubble {
  border-color: #1a1a1a;
  border-radius: 10px;
  background: #1a1a1a;
  color: #fff;
  box-shadow: none;
}

.message-bubble a {
  color: #1677ff;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.is-self .message-bubble a {
  color: #a8cfff;
}

.message-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 4px;
  padding: 0 2px;
  color: #aaa;
  font-size: 10.5px;
  line-height: 1.4;
}

.is-self .message-meta {
  justify-content: flex-end;
}

.delivery {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.delivery.failed {
  color: #c65f5f;
}

.retry-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #1a1a1a;
  font: inherit;
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;
}

.event-card {
  width: min(100%, 430px);
  padding: 12px;
  border: 1px solid #e6e6e6;
  border-radius: 12px;
  background: #fff;
}

.is-self .event-card {
  background: #fafafa;
}

.event-kicker {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #777;
  font-size: 11px;
}

.event-status {
  margin-left: auto;
  padding: 2px 6px;
  border-radius: 999px;
  background: #f2f2f2;
  color: #777;
  font-size: 10px;
}

.status-1 {
  background: #eef6ee;
  color: #527352;
}

.status-2,
.status-3 {
  background: #f5f5f5;
  color: #888;
}

.event-main {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.event-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  min-width: 42px;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #f7f7f7;
  color: #999;
  font-size: 18px;
}

.event-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.event-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.event-copy strong,
.event-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-copy strong {
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 600;
}

.event-copy span {
  color: #888;
  font-size: 11px;
}

:deep(.ant-btn-primary:not(:disabled)) {
  border-color: #1a1a1a !important;
  background: #1a1a1a !important;
}

:deep(.ant-btn-primary:not(:disabled):hover) {
  border-color: #333 !important;
  background: #333 !important;
}

.event-actions {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  margin-top: 10px;
}

@media (max-width: 720px) {
  .message-stack {
    max-width: calc(100% - 44px);
  }

  .message-bubble {
    font-size: 13px;
  }
}
</style>
