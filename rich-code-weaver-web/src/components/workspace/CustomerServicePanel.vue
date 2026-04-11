<template>
  <div
    ref="panelRef"
    :class="[
      'customer-service-panel',
      panelModeClass,
      {
        'is-collapsed': isCollapsed,
        'is-dragging': isDragging,
        'is-snapped-right': isSnappedRight
      }
    ]"
    :style="floatingStyle"
  >
    <transition mode="out-in" name="panel-fade">
      <div v-if="isCollapsed" key="collapsed" class="customer-service-collapsed" @click="expandPanel">
        <div class="collapsed-core">
          <div class="assistant-avatar assistant-avatar--image collapsed-avatar">
            <img :src="customerLogo" alt="RUBY 智能助手" class="assistant-avatar-image" />
          </div>
          <div class="collapsed-text">
            <span class="collapsed-title">RUBY</span>
            <span class="collapsed-sub">智能助手</span>
          </div>
        </div>
        <button class="collapsed-expand-btn" title="展开客服" @click.stop="expandPanel">
          <MenuUnfoldOutlined />
        </button>
      </div>

      <div v-else key="expanded" class="customer-service-shell">
        <div class="customer-service-header" :class="{ 'is-draggable': isFloating }">
          <div class="header-main">
            <div class="assistant-avatar assistant-avatar--image">
              <img :src="customerLogo" alt="RUBY 智能助手" class="assistant-avatar-image" />
            </div>
            <div class="assistant-meta">
              <div class="assistant-name">RUBY 智能助手</div>
              <div class="assistant-status">
                <span :class="['status-dot', { 'is-streaming': streaming, 'is-offline': !userModuleOnline && !streaming }]" />
                <span>{{ streaming ? '响应生成中' : (userModuleOnline ? '在线' : '离线') }}</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <button v-if="isFloating" class="icon-btn drag-btn" title="拖动面板" @mousedown.stop="startDrag">
              <DragOutlined />
            </button>
            <button class="icon-btn" title="收缩到右侧" @click="collapsePanel">
              <MenuFoldOutlined />
            </button>
            <button class="icon-btn" :title="isFloating ? '吸附到右侧' : '切换悬浮窗'" @click="toggleMode">
              <PushpinOutlined v-if="isFloating" />
              <BorderOuterOutlined v-else />
            </button>
          </div>
        </div>

        <div class="customer-service-body">
          <transition name="history-panel">
            <div v-if="showConversationHistory" class="history-overlay" @click.self="closeConversationHistory">
              <div class="history-panel">
                <div class="history-panel-header">
                  <div class="sidebar-title">
                    <HistoryOutlined />
                    <span>会话历史</span>
                  </div>
                  <button class="history-close-btn" title="收起历史" @click="closeConversationHistory">
                    收起
                  </button>
                </div>
                <div class="conversation-toolbar">
                  <button class="new-chat-btn" title="新建对话" @click="handleCreateConversation">
                    <PlusOutlined />
                    <span>新建</span>
                  </button>
                </div>
                <div class="conversation-list">
                  <button
                    v-for="conversation in conversations"
                    :key="conversation.id"
                    :class="['conversation-item', { active: conversation.id === activeConversationId }]"
                    @click="selectConversation(conversation.id)"
                  >
                    <div class="conversation-item-line">
                      <span class="conversation-name">{{ conversation.title || '新对话' }}</span>
                      <span class="conversation-time">{{ formatConversationTime(conversation.lastMessageTime) }}</span>
                    </div>
                    <span class="conversation-preview">{{ conversation.lastMessagePreview || '开始新的咨询' }}</span>
                  </button>
                </div>
              </div>
            </div>
          </transition>

          <section class="chat-section">
            <div class="chat-banner">
              <div class="chat-banner-content">
                <span class="banner-chip">平台引导</span>
                <span class="banner-text">快速解答功能入口、部署、后台配置与常见异常</span>
              </div>
              <button class="banner-history-btn" @click="toggleConversationHistory">
                <HistoryOutlined />
                <span>{{ showConversationHistory ? '收起历史' : '会话历史' }}</span>
              </button>
            </div>

            <div ref="messagesRef" :class="['message-list', { 'is-empty': messages.length === 0 }]">
              <div v-if="messages.length === 0" class="empty-state">
                <div class="empty-top">
                  <div>
                    <div class="empty-title">你好，我是RUBY</div>
                    <div class="empty-desc">我可以帮你快速定位平台功能入口、使用步骤和常见问题。</div>
                  </div>
                </div>
                <div class="suggestion-grid">
                  <button
                    v-for="prompt in suggestionPrompts"
                    :key="prompt"
                    class="suggestion-chip"
                    @click="fillSuggestion(prompt)"
                  >
                    <span class="suggestion-title">{{ prompt }}</span>
                  </button>
                </div>
              </div>

              <div
                v-for="(item, index) in messages"
                :key="`${item.id || index}-${item.createTime || ''}`"
                :class="['message-row', item.senderType === 'user' ? 'is-user' : 'is-ai']"
              >
                <div class="message-card">
                  <div class="message-meta">
                    <span class="role-chip">{{ item.senderType === 'user' ? '我' : 'RUBY' }}</span>
                    <span v-if="item.createTime">{{ formatConversationTime(item.createTime) }}</span>
                  </div>
                  <div class="message-bubble">
                    <template v-if="item.senderType === 'ai'">
                      <MarkdownRenderer :content="item.content || (item.loading ? '思考中...' : '')" />
                    </template>
                    <template v-else>
                      <div class="plain-text">{{ item.content }}</div>
                    </template>
                  </div>
                  <div v-if="item.loading" class="message-loading">
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                  </div>
                </div>
              </div>
            </div>

            <div class="input-section">
              <div class="input-shell">
                <a-textarea
                  :value="userInput"
                  :auto-size="{ minRows: 2, maxRows: 5 }"
                  :maxlength="2000"
                  :disabled="streaming"
                  class="customer-input"
                  placeholder="向 RUBY 提问，比如：为什么我生成的效果不尽人意？"
                  @pressEnter="handlePressEnter"
                  @update:value="handleInputChange"
                />
                <div class="input-actions">
                  <span class="hint-text">{{ streaming ? '生成中...' : '服务加载完成' }}</span>
                  <a-button class="send-btn" :loading="streaming" type="primary" @click="sendMessage">
                    <template #icon>
                      <SendOutlined />
                    </template>
                    发送
                  </a-button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import type { CSSProperties } from 'vue'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  BorderOuterOutlined,
  DragOutlined,
  HistoryOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlusOutlined,
  PushpinOutlined,
  SendOutlined
} from '@ant-design/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {
  createCustomerServiceConversation,
  listCustomerServiceConversations,
  listCustomerServiceMessages
} from '@/api/customerServiceController'
import { API_BASE_URL } from '@/config/env'
import request from '@/request'
import customerLogo from '@/assets/customerLogo.png'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { checkUserHealth } from '@/api/healthController'

interface CustomerServiceMessageItem {
  id?: number
  conversationId?: number
  senderType: 'user' | 'ai'
  content: string
  createTime?: string
  loading?: boolean
}

const router = useRouter()
const loginUserStore = useLoginUserStore()

const PANEL_MODE_KEY = 'rcw_customer_service_panel_mode'
const ACTIVE_CONVERSATION_KEY = 'rcw_customer_service_active_conversation'
const GENERATING_KEY = 'rcw_customer_service_generating'
const PANEL_POSITION_KEY = 'rcw_customer_service_panel_position'
const PANEL_COLLAPSED_KEY = 'rcw_customer_service_panel_collapsed'
const PANEL_SNAP_KEY = 'rcw_customer_service_panel_snap'

const suggestionPrompts = [
  '首页有哪些核心功能可以直接体验？',
  '怎么部署我创建的数字产物？',
  '推荐模板和我的身份信息有什么关系？',
  '为什么生成失败了，我应该先检查什么？'
]

const userModuleOnline = ref(false)
let healthCheckTimer: ReturnType<typeof setInterval> | null = null

const checkRubyOnline = async () => {
  try {
    const res = await checkUserHealth()
    userModuleOnline.value = res.data.code === 0 && res.data.data?.status === 'UP'
  } catch {
    userModuleOnline.value = false
  }
}

const conversations = ref<API.CustomerServiceConversationVO[]>([])
const activeConversationId = ref<number>()
const messages = ref<CustomerServiceMessageItem[]>([])
const userInput = ref('')
const streaming = ref(false)
const showConversationHistory = ref(false)
const isFloating = ref(localStorage.getItem(PANEL_MODE_KEY) === 'floating')
const isCollapsed = ref(localStorage.getItem(PANEL_COLLAPSED_KEY) === '1')
const isSnappedRight = ref(localStorage.getItem(PANEL_SNAP_KEY) !== '0')
const isDragging = ref(false)
const panelRef = ref<HTMLElement | null>(null)
const messagesRef = ref<HTMLElement | null>(null)
const lastEventId = ref<string | null>(null)
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 5
const floatingPosition = ref({ x: 0, y: 88 })
const dragState = {
  startMouseX: 0,
  startMouseY: 0,
  startX: 0,
  startY: 0
}
let currentEventSource: EventSource | null = null

const panelModeClass = computed(() => (isFloating.value ? 'is-floating' : 'is-docked'))

const getExpandedPanelWidth = () => {
  return window.innerWidth <= 1200 ? Math.min(window.innerWidth - 32, 420) : 360
}

const getCollapsedPanelWidth = () => 88

const getExpandedSnapLeft = () => Math.max(12, window.innerWidth - getExpandedPanelWidth() - 16)

const getCollapsedSnapLeft = () => Math.max(12, window.innerWidth - getCollapsedPanelWidth() - 16)

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

const clampFloatingPosition = (position: { x: number; y: number }) => {
  const width = getExpandedPanelWidth()
  const height = panelRef.value?.offsetHeight || Math.min(Math.max(window.innerHeight - 120, 620), 760)
  return {
    x: clamp(position.x, 12, Math.max(12, window.innerWidth - width - 12)),
    y: clamp(position.y, 16, Math.max(16, window.innerHeight - height - 16))
  }
}

const getDefaultFloatingPosition = () => ({
  x: getExpandedSnapLeft(),
  y: clamp(88, 16, Math.max(16, window.innerHeight - 220))
})

const persistMode = () => {
  localStorage.setItem(PANEL_MODE_KEY, isFloating.value ? 'floating' : 'docked')
}

const persistCollapsed = () => {
  localStorage.setItem(PANEL_COLLAPSED_KEY, isCollapsed.value ? '1' : '0')
}

const persistSnap = () => {
  localStorage.setItem(PANEL_SNAP_KEY, isSnappedRight.value ? '1' : '0')
}

const persistFloatingPosition = () => {
  localStorage.setItem(PANEL_POSITION_KEY, JSON.stringify(floatingPosition.value))
}

const restoreFloatingPosition = () => {
  const fallback = getDefaultFloatingPosition()
  const raw = localStorage.getItem(PANEL_POSITION_KEY)
  if (!raw) {
    floatingPosition.value = fallback
    return
  }
  try {
    const parsed = JSON.parse(raw)
    floatingPosition.value = clampFloatingPosition({
      x: Number(parsed?.x) || fallback.x,
      y: Number(parsed?.y) || fallback.y
    })
  } catch {
    floatingPosition.value = fallback
  }
}

const syncFloatingPosition = () => {
  if (!isFloating.value) {
    return
  }
  if (isSnappedRight.value) {
    floatingPosition.value = {
      x: getExpandedSnapLeft(),
      y: clamp(floatingPosition.value.y, 16, Math.max(16, window.innerHeight - 180))
    }
  } else {
    floatingPosition.value = clampFloatingPosition(floatingPosition.value)
  }
  persistFloatingPosition()
}

const floatingStyle = computed<CSSProperties | undefined>(() => {
  if (!isFloating.value) {
    return undefined
  }
  const width = isCollapsed.value ? getCollapsedPanelWidth() : getExpandedPanelWidth()
  const left = isCollapsed.value && isSnappedRight.value
    ? getCollapsedSnapLeft()
    : isSnappedRight.value
      ? getExpandedSnapLeft()
      : floatingPosition.value.x
  return {
    left: `${left}px`,
    top: `${floatingPosition.value.y}px`,
    width: `${width}px`
  }
})

const toggleMode = () => {
  if (isFloating.value) {
    isFloating.value = false
    isDragging.value = false
    isCollapsed.value = false
  } else {
    isFloating.value = true
    isCollapsed.value = false
    isSnappedRight.value = true
    restoreFloatingPosition()
    floatingPosition.value.x = getExpandedSnapLeft()
    persistFloatingPosition()
  }
  persistMode()
  persistCollapsed()
  persistSnap()
}

const collapsePanel = () => {
  if (isFloating.value) {
    isSnappedRight.value = true
    floatingPosition.value.x = getExpandedSnapLeft()
    persistSnap()
    persistFloatingPosition()
  }
  isCollapsed.value = true
  persistCollapsed()
}

const expandPanel = () => {
  isCollapsed.value = false
  if (isFloating.value && isSnappedRight.value) {
    floatingPosition.value.x = getExpandedSnapLeft()
    persistFloatingPosition()
  }
  persistCollapsed()
}

const handleWindowResize = () => {
  syncFloatingPosition()
}

const removeDragListeners = () => {
  window.removeEventListener('mousemove', handleDragMove)
  window.removeEventListener('mouseup', stopDrag)
  document.body.style.userSelect = ''
}

const handleDragMove = (event: MouseEvent) => {
  if (!isDragging.value) {
    return
  }
  isSnappedRight.value = false
  floatingPosition.value = clampFloatingPosition({
    x: dragState.startX + event.clientX - dragState.startMouseX,
    y: dragState.startY + event.clientY - dragState.startMouseY
  })
}

const stopDrag = () => {
  if (!isDragging.value) {
    removeDragListeners()
    return
  }
  isDragging.value = false
  const rightGap = window.innerWidth - (floatingPosition.value.x + getExpandedPanelWidth())
  if (rightGap < 36) {
    isSnappedRight.value = true
    floatingPosition.value.x = getExpandedSnapLeft()
  }
  persistSnap()
  persistFloatingPosition()
  removeDragListeners()
}

const startDrag = (event: MouseEvent) => {
  if (!isFloating.value || isCollapsed.value) {
    return
  }
  isDragging.value = true
  dragState.startMouseX = event.clientX
  dragState.startMouseY = event.clientY
  dragState.startX = isSnappedRight.value ? getExpandedSnapLeft() : floatingPosition.value.x
  dragState.startY = floatingPosition.value.y
  window.addEventListener('mousemove', handleDragMove)
  window.addEventListener('mouseup', stopDrag)
  document.body.style.userSelect = 'none'
}

const markGeneratingStart = (conversationId: number, prompt: string) => {
  localStorage.setItem(GENERATING_KEY, JSON.stringify({ conversationId, prompt, timestamp: Date.now() }))
}

const clearGenerating = () => {
  localStorage.removeItem(GENERATING_KEY)
}

const getGeneratingInfo = (): { conversationId: number; prompt: string; timestamp: number } | null => {
  const raw = localStorage.getItem(GENERATING_KEY)
  if (!raw) return null
  try {
    const data = JSON.parse(raw)
    if (Date.now() - data.timestamp > 30 * 60 * 1000) {
      localStorage.removeItem(GENERATING_KEY)
      return null
    }
    return data
  } catch {
    localStorage.removeItem(GENERATING_KEY)
    return null
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const closeEventSource = () => {
  currentEventSource?.close()
  currentEventSource = null
}

const restoreActiveConversation = () => {
  const raw = localStorage.getItem(ACTIVE_CONVERSATION_KEY)
  if (!raw) return
  const id = Number(raw)
  if (!Number.isNaN(id) && id > 0) {
    activeConversationId.value = id
  }
}

const persistActiveConversation = () => {
  if (activeConversationId.value) {
    localStorage.setItem(ACTIVE_CONVERSATION_KEY, String(activeConversationId.value))
  }
}

const handleInputChange = (value: string) => {
  userInput.value = value
}

const toggleConversationHistory = () => {
  showConversationHistory.value = !showConversationHistory.value
}

const closeConversationHistory = () => {
  showConversationHistory.value = false
}

const fillSuggestion = (prompt: string) => {
  userInput.value = prompt
}

const formatConversationTime = (value?: string) => {
  if (!value) {
    return '刚刚'
  }
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return '刚刚'
  }
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const loadConversations = async () => {
  const res = await listCustomerServiceConversations()
  if (res.data.code === 0) {
    conversations.value = res.data.data || []
    const existingConversation = activeConversationId.value
      ? conversations.value.find((item) => item.id === activeConversationId.value)
      : undefined
    if (!existingConversation && conversations.value.length > 0) {
      activeConversationId.value = conversations.value[0].id
    }
    persistActiveConversation()
  }
}

const loadMessages = async (conversationId?: number) => {
  if (!conversationId) {
    messages.value = []
    return
  }
  const res = await listCustomerServiceMessages({ conversationId, pageSize: 50 })
  if (res.data.code === 0) {
    messages.value = ((res.data.data || []) as API.CustomerServiceMessageVO[]).map((item) => ({
      id: item.id,
      conversationId: item.conversationId,
      senderType: (item.senderType as 'user' | 'ai') || 'ai',
      content: item.content || '',
      createTime: item.createTime
    }))
    await scrollToBottom()
  }
}

const handleCreateConversation = async () => {
  const res = await createCustomerServiceConversation()
  if (res.data.code !== 0 || !res.data.data?.id) {
    message.error(res.data.message || '创建会话失败')
    return
  }
  const conversation = res.data.data
  conversations.value.unshift(conversation)
  activeConversationId.value = conversation.id
  persistActiveConversation()
  messages.value = []
  closeConversationHistory()
}

const selectConversation = async (conversationId?: number) => {
  if (!conversationId) return
  activeConversationId.value = conversationId
  persistActiveConversation()
  await loadMessages(conversationId)
  closeConversationHistory()
}

const connectSse = (conversationId: number, prompt: string, isReconnect = false) => {
  closeEventSource()
  let aiMessageIndex = messages.value.length - 1
  const lastMessage = messages.value[aiMessageIndex]
  if (!lastMessage || lastMessage.senderType !== 'ai' || !lastMessage.loading) {
    aiMessageIndex = messages.value.push({
      conversationId,
      senderType: 'ai',
      content: '',
      loading: true
    }) - 1
  }

  const params = new URLSearchParams({
    conversationId: String(conversationId),
    message: prompt,
    reconnect: String(isReconnect)
  })
  if (lastEventId.value) {
    params.set('lastEventId', lastEventId.value)
  }
  const url = `${request.defaults.baseURL || API_BASE_URL}/generator/customerService/chat/stream?${params.toString()}`
  currentEventSource = new EventSource(url, { withCredentials: true })

  currentEventSource.onmessage = async (event) => {
    if (event.lastEventId) {
      lastEventId.value = event.lastEventId
    }
    try {
      const data = JSON.parse(event.data || '{}')
      const chunk = data.b || ''
      messages.value[aiMessageIndex].content += chunk
      messages.value[aiMessageIndex].loading = false
      await scrollToBottom()
    } catch {
      // ignore malformed chunks
    }
  }

  currentEventSource.addEventListener('end', async () => {
    streaming.value = false
    reconnectAttempts.value = 0
    clearGenerating()
    closeEventSource()
    await loadConversations()
    await loadMessages(conversationId)
  })

  currentEventSource.addEventListener('server_error', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data || '{}')
      message.error(data.error || 'AI 客服响应失败')
    } catch {
      message.error('AI 客服响应失败')
    }
    streaming.value = false
    if (messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].loading = false
    }
    clearGenerating()
    closeEventSource()
  })

  currentEventSource.onerror = () => {
    closeEventSource()
    if (streaming.value && reconnectAttempts.value < maxReconnectAttempts) {
      reconnectAttempts.value += 1
      setTimeout(() => connectSse(conversationId, prompt, true), reconnectAttempts.value * 1000)
    } else if (streaming.value) {
      streaming.value = false
      if (messages.value[aiMessageIndex]) {
        messages.value[aiMessageIndex].loading = false
      }
      clearGenerating()
      message.error('客服连接中断，请稍后重试')
    }
  }
}

const sendMessage = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录后再使用AI客服')
    await router.push('/user/login')
    return
  }
  const prompt = userInput.value.trim()
  if (!prompt || streaming.value) return
  expandPanel()
  closeConversationHistory()
  if (!activeConversationId.value) {
    await handleCreateConversation()
  }
  if (!activeConversationId.value) return

  messages.value.push({
    conversationId: activeConversationId.value,
    senderType: 'user',
    content: prompt
  })
  userInput.value = ''
  streaming.value = true
  reconnectAttempts.value = 0
  lastEventId.value = null
  markGeneratingStart(activeConversationId.value, prompt)
  await scrollToBottom()
  connectSse(activeConversationId.value, prompt)
}

const tryResumeGeneration = async () => {
  const generatingInfo = getGeneratingInfo()
  if (!generatingInfo) return
  activeConversationId.value = generatingInfo.conversationId
  persistActiveConversation()
  await loadMessages(generatingInfo.conversationId)
  streaming.value = true
  connectSse(generatingInfo.conversationId, generatingInfo.prompt, true)
}

const handlePressEnter = (e: KeyboardEvent) => {
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

onMounted(async () => {
  restoreActiveConversation()
  restoreFloatingPosition()
  syncFloatingPosition()
  window.addEventListener('resize', handleWindowResize)
  await checkRubyOnline()
  healthCheckTimer = setInterval(checkRubyOnline, 30000)
  if (!loginUserStore.loginUser?.id) return
  await loadConversations()
  if (!activeConversationId.value && conversations.value.length === 0) {
    await handleCreateConversation()
  }
  if (activeConversationId.value) {
    await loadMessages(activeConversationId.value)
  }
  await tryResumeGeneration()
})

onUnmounted(() => {
  closeEventSource()
  removeDragListeners()
  window.removeEventListener('resize', handleWindowResize)
  if (healthCheckTimer) {
    clearInterval(healthCheckTimer)
    healthCheckTimer = null
  }
})
</script>

<style scoped>
.customer-service-panel {
  width: 360px;
  min-width: 360px;
  height: 100%;
  min-height: 0;
  position: relative;
  align-self: stretch;
  transition: width 0.28s ease, transform 0.28s ease;
}

.customer-service-panel.is-collapsed {
  width: 88px;
  min-width: 88px;
}

.customer-service-panel.is-floating {
  position: fixed;
  z-index: 30;
  min-width: 0;
  height: min(78vh, 720px);
  max-height: calc(100vh - 32px);
}

.customer-service-panel.is-dragging {
  transition: none;
}

.customer-service-shell,
.customer-service-collapsed {
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.98) 100%);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08), 0 2px 8px rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(18px);
}

.customer-service-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.customer-service-panel.is-floating .customer-service-shell {
  height: 100%;
  min-height: 0;
}

.customer-service-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background:
    radial-gradient(circle at top right, rgba(15, 23, 42, 0.06), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.88));
}

.customer-service-header.is-draggable {
  cursor: default;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.assistant-meta {
  min-width: 0;
}

.assistant-avatar {
  width: 45px;
  height: 45px;
  border-radius: 12px;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
  overflow: hidden;
}

.assistant-avatar--image {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.92) 0%, rgba(219, 234, 254, 0.9) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22), 0 8px 18px rgba(59, 130, 246, 0.12);
}

.assistant-avatar-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.assistant-name {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}

.assistant-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.2;
}

.assistant-status {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.14);
}

.status-dot.is-streaming {
  background: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
  animation: statusPulse 1.4s ease-in-out infinite;
}

.status-dot.is-offline {
  background: #d1d5db;
  box-shadow: 0 0 0 4px rgba(209, 213, 219, 0.18);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn,
.new-chat-btn,
.collapsed-expand-btn {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.84);
  color: #475569;
  cursor: pointer;
  transition: all 0.22s ease;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover,
.new-chat-btn:hover,
.collapsed-expand-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 23, 42, 0.16);
  color: #111827;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.drag-btn {
  cursor: grab;
}

.customer-service-panel.is-dragging .drag-btn {
  cursor: grabbing;
}

.customer-service-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
  position: relative;
}

.history-overlay {
  position: absolute;
  inset: 0;
  z-index: 4;
  padding: 12px;
  display: flex;
  align-items: stretch;
  background: rgba(248, 250, 252, 0.68);
  backdrop-filter: blur(8px);
}

.history-panel {
  width: 220px;
  max-width: calc(100% - 24px);
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.history-close-btn {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.84);
  color: #64748b;
  border-radius: 10px;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.22s ease;
}

.history-close-btn:hover {
  color: #0f172a;
  border-color: rgba(15, 23, 42, 0.14);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.conversation-toolbar {
  display: flex;
  gap: 8px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.new-chat-btn {
  width: 100%;
  min-height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.conversation-list::-webkit-scrollbar,
.message-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-thumb,
.message-list::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.32);
  border-radius: 999px;
}

.conversation-item {
  text-align: left;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.86);
  padding: 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all 0.22s ease;
}

.conversation-item:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.96);
}

.conversation-item.active {
  border-color: rgba(59, 130, 246, 0.18);
  background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(239, 246, 255, 0.92));
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.08);
}

.conversation-item-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversation-name,
.conversation-time {
  font-size: 11px;
}

.conversation-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-time {
  color: #94a3b8;
  flex-shrink: 0;
}

.conversation-preview {
  font-size: 11px;
  color: #64748b;
  line-height: 1.45;
  line-clamp: 2;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chat-section {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(249, 250, 251, 0.82), rgba(255, 255, 255, 0.94));
}

.chat-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.7);
}

.chat-banner-content {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.banner-chip {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #0f172a;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.banner-text {
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
  min-width: 0;
}

.banner-history-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.22s ease;
}

.banner-history-btn:hover {
  color: #0f172a;
  border-color: rgba(15, 23, 42, 0.14);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-list.is-empty {
  overflow-y: hidden;
}

.message-row {
  display: flex;
}

.message-row.is-user {
  justify-content: flex-end;
}

.message-row.is-ai {
  justify-content: flex-start;
}

.message-card {
  max-width: 86%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  color: #94a3b8;
}

.message-row.is-user .message-meta {
  justify-content: flex-end;
}

.role-chip {
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #334155;
  font-weight: 600;
}

.message-bubble {
  border-radius: 16px;
  padding: 11px 13px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  font-size: 13px;
  line-height: 1.65;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
}

.message-row.is-user .message-bubble {
  background: linear-gradient(135deg, #111827 0%, #1f2937 100%);
  color: #fff;
  border-color: rgba(17, 24, 39, 0.9);
}

.message-row.is-user .role-chip {
  background: rgba(17, 24, 39, 0.08);
}

.message-loading {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
}

.loading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: dotPulse 1s ease-in-out infinite;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.plain-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-state {
  margin: auto 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #64748b;
}

.empty-top {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.empty-orb {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #111827 0%, #334155 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.empty-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.empty-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.suggestion-chip {
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-radius: 14px;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.22s ease;
}

.suggestion-chip:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(59, 130, 246, 0.16);
}

.suggestion-title {
  font-size: 12px;
  font-weight: 600;
  color: #111827;
  line-height: 1.5;
}

.input-section {
  padding: 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.82);
  flex-shrink: 0;
}

.input-shell {
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
  padding: 10px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.input-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hint-text {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

.send-btn {
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(135deg, #111827 0%, #1f2937 100%);
  border: none;
  box-shadow: 0 10px 22px rgba(17, 24, 39, 0.16);
}

.send-btn:hover,
.send-btn:focus {
  background: linear-gradient(135deg, #0f172a 0%, #111827 100%);
}

.customer-service-collapsed {
  min-height: 160px;
  height: min(42vh, 260px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 10px 12px;
  cursor: pointer;
  overflow: hidden;
}

.collapsed-core {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.collapsed-avatar {
  width: 45px;
  height: 45px;
}

.collapsed-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-align: center;
}

.collapsed-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.collapsed-sub {
  font-size: 11px;
  color: #64748b;
}

.collapsed-expand-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.customer-service-panel.is-floating.is-collapsed {
  overflow: visible;
}

.history-panel-enter-active,
.history-panel-leave-active {
  transition: opacity 0.22s ease;
}

.history-panel-enter-active .history-panel,
.history-panel-leave-active .history-panel {
  transition: transform 0.22s ease;
}

.history-panel-enter-from,
.history-panel-leave-to {
  opacity: 0;
}

.history-panel-enter-from .history-panel,
.history-panel-leave-to .history-panel {
  transform: translateX(-10px);
}

.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateX(8px) scale(0.98);
}

:deep(.customer-input .ant-input) {
  padding: 2px 2px 0;
  font-size: 13px;
  line-height: 1.65;
  color: #0f172a;
  background: transparent;
}

:deep(.customer-input.ant-input-textarea) {
  border: none;
  box-shadow: none;
}

:deep(.customer-input .ant-input::placeholder) {
  color: #94a3b8;
}

:deep(.message-bubble p:first-child) {
  margin-top: 0;
}

:deep(.message-bubble p:last-child) {
  margin-bottom: 0;
}

@keyframes statusPulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.16);
  }
}

@keyframes dotPulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

@media (max-width: 1440px) {
  .customer-service-panel {
    width: 340px;
    min-width: 340px;
  }
}

@media (max-width: 1200px) {
  .customer-service-panel {
    display: none;
  }

  .customer-service-panel.is-floating {
    display: block;
  }

  .customer-service-shell {
    min-height: min(74vh, 680px);
  }

  .suggestion-grid {
    grid-template-columns: 1fr;
  }
}
</style>
