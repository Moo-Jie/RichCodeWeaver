<template>
  <div class="workspace">
    <!-- Default view: no app selected -->
    <template v-if="!appStore.hasSelectedApp && !appId">
      <div class="workspace-default">
        <div class="greeting">
          <img alt="Logo" class="greeting-logo" src="@/assets/logo.png" />
          <h1 class="greeting-title">织码睿奇</h1>
          <p class="greeting-sub">只需一句话，让创意触手可及</p>
        </div>
        <!-- Prompt suggestions -->
        <div class="prompt-grid">
          <div
            v-for="item in promptOptions"
            :key="item.label"
            class="prompt-card"
            @click="userPrompt = item.value"
          >
            <span class="prompt-label">{{ item.label }}</span>
            <span class="prompt-desc">{{ item.desc }}</span>
          </div>
        </div>
      </div>
      <ChatInput
        v-model="userPrompt"
        :generator-mode="useAgentMode"
        :sending="creating"
        :show-mode-selector="true"
        placeholder="描述您想要创建的应用..."
        @send="handleCreate"
        @update:generator-mode="useAgentMode = $event"
      />
    </template>

    <!-- App selected view -->
    <template v-else>
      <!-- Edit mode top bar -->
      <div v-if="isEditMode" class="edit-mode-bar">
        <span class="edit-bar-text">
          <EditOutlined style="margin-right: 6px;" />
          可视化编辑模式已启用 — 点击预览页面中的元素并输入修改需求
        </span>
        <button class="edit-bar-exit" @click="toggleEditMode">退出编辑</button>
      </div>

      <!-- Mode switcher -->
      <ModeSwitch :mode="appStore.currentMode" @update:mode="appStore.setMode" />

      <!-- Body: content + right panel side by side -->
      <div class="workspace-body">
        <!-- Chat section (visible only in chat mode) -->
        <div v-show="appStore.currentMode === 'chat'" class="split-chat">
          <ChatMessages
            ref="chatMessagesRef"
            :has-more="hasMore"
            :loading-history="loadingHistory"
            :login-user="loginUserStore.loginUser"
            :messages="messages"
            @load-more="loadMoreHistory"
          />
          <ChatInput
            v-model="userInput"
            :is-app-mode="true"
            :is-owner="isOwner"
            :selected-element="selectedElement"
            :sending="isGenerating"
            placeholder="✨ 描述越详尽，创作越符合您的预期 ✨"
            @clear-selection="clearSelection"
            @send="sendMessage"
          />
        </div>

        <!-- Preview section (always rendered, expands full width in app mode) -->
        <div :class="['preview-area', { 'preview-full': appStore.currentMode === 'app' }]">
          <AppPreview
            ref="appPreviewRef"
            :generating-time="generatingTime"
            :is-generating="isGenerating"
            :preview-url="previewUrl"
            @iframe-load="onIframeLoad"
          />
        </div>

        <!-- Right panel -->
        <RightPanel
          :app="appStore.selectedApp"
          :deploying="deploying"
          :downloading="downloading"
          :is-admin="isAdmin"
          :is-deployed="isDeployed"
          :is-edit-mode="isEditMode"
          :is-generating="isGenerating"
          :is-owner="isOwner"
          :mode="appStore.currentMode"
          :preview-url="previewUrl"
          :visible="appStore.rightPanelVisible"
          @deploy="deployApp"
          @download="downloadCode"
          @preview-fullscreen="openInNewTab"
          @re-deploy="confirmReDeploy"
          @show-detail="showAppDetail"
          @toggle="appStore.toggleRightPanel"
          @toggle-edit="toggleEditMode"
          @visit-site="visitDeployedSite"
        />
      </div>
    </template>

    <!-- Modals -->
    <AppInfo
      v-model:open="appDetailVisible"
      :app="appStore.selectedApp ?? undefined"
      :show-actions="isOwner || isAdmin"
      @delete="handleDeleteApp"
      @edit="handleEditApp"
    />
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="visitDeployedSite"
    />
  </div>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {message, Modal} from 'ant-design-vue'
import {EditOutlined} from '@ant-design/icons-vue'
import {useAppStore} from '@/stores/appStore'
import {useLoginUserStore} from '@/stores/loginUser'
import {addApp, deleteApp as deleteAppApi, deployApp as deployAppApi, getAppVoById} from '@/api/appController'
import {listAppChatHistoryByPage} from '@/api/chatHistoryController'
import {CodeGenTypeEnum} from '@/enums/codeGenTypes'
import {API_BASE_URL, DEPLOY_DOMAIN, getStaticPreviewUrl, getWebProjectStaticPreviewUrl} from '@/config/env'
import {type ElementInfo, visualEditorUtil} from '@/utils/visualEditorUtil'
import {promptOptions} from '@/constants/prompts'
import request from '@/request'
import ChatInput from '@/components/workspace/ChatInput.vue'
import ChatMessages from '@/components/workspace/ChatMessages.vue'
import ModeSwitch from '@/components/workspace/ModeSwitch.vue'
import AppPreview from '@/components/workspace/AppPreview.vue'
import RightPanel from '@/components/workspace/RightPanel.vue'
import AppInfo from '@/components/AppInfo.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const loginUserStore = useLoginUserStore()

// === Create Mode State ===
const userPrompt = ref('')
const useAgentMode = ref(true)
const creating = ref(false)

// === App Chat State ===
const appId = ref<string>(route.params.id as string || '')

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const loadingHistory = ref(false)
const lastCreateTime = ref<string | null>(null)
const hasMore = ref(true)

// === Preview State ===
const previewUrl = ref('')
const previewReady = ref(false)

// === Deploy State ===
const deploying = ref(false)
const downloading = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// === Visual Editor State ===
const visualEditor = ref<visualEditorUtil | null>(null)
const isEditMode = ref(false)
const selectedElement = ref<ElementInfo | null>(null)

// === UI Refs ===
const chatMessagesRef = ref<InstanceType<typeof ChatMessages>>()
const appPreviewRef = ref<InstanceType<typeof AppPreview>>()
const appDetailVisible = ref(false)

// === Timer ===
const generatingTime = ref(0)
const timer = ref<ReturnType<typeof setInterval> | null>(null)

// === SSE Reconnect ===
const lastEventId = ref<string | null>(null)
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 5

// === Computed ===
const isOwner = computed(() => {
  return appStore.selectedApp?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const isDeployed = computed(() => {
  return !!appStore.selectedApp?.deployKey
})

const deployedSiteUrl = computed(() => {
  if (appStore.selectedApp?.deployKey) {
    return `${DEPLOY_DOMAIN}/${appStore.selectedApp.deployKey}`
  }
  return ''
})

// === localStorage generating tracker ===
const getGeneratingKey = () => `rcw_generating_${appId.value}`
const markGeneratingStart = (userMessage: string) => {
  localStorage.setItem(getGeneratingKey(), JSON.stringify({
    message: userMessage,
    timestamp: Date.now()
  }))
}
const markGeneratingEnd = () => {
  localStorage.removeItem(getGeneratingKey())
}
const getGeneratingInfo = (): { message: string; timestamp: number } | null => {
  const raw = localStorage.getItem(getGeneratingKey())
  if (!raw) return null
  try {
    const info = JSON.parse(raw)
    if (Date.now() - info.timestamp > 30 * 60 * 1000) {
      localStorage.removeItem(getGeneratingKey())
      return null
    }
    return info
  } catch {
    localStorage.removeItem(getGeneratingKey())
    return null
  }
}

// === Create App ===
const handleCreate = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
      generatorType: 'HTML'
    })
    if (res.data.code === 0 && res.data.data) {
      const newAppId = res.data.data
      message.success('应用创建成功')
      userPrompt.value = ''
      await router.push(`/app/chat/${newAppId}?useAgent=${useAgentMode.value}`)
      await appStore.loadMyApps()
    } else {
      message.error('创建失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (e: any) {
    message.error('创建失败：' + (e.message || '请稍后重试'))
  } finally {
    creating.value = false
  }
}

// === Fetch App Info ===
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) return

  appId.value = id

  try {
    const res = await getAppVoById({id: id as unknown as number})
    if (res.data.code === 0 && res.data.data) {
      appStore.refreshSelectedApp(res.data.data)
      appStore.rightPanelVisible = true

      await fetchChatHistory()
      updatePreview()

      // Auto-send initial prompt if new app with init prompt
      if (appStore.selectedApp?.initPrompt && isOwner.value && messages.value.length === 0) {
        await sendInitialMessage(appStore.selectedApp.initPrompt)
      } else if (messages.value.length >= 2) {
        updatePreview()
      }
    } else {
      message.error('获取应用信息失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
  }
}

// === Chat History ===
const fetchChatHistory = async (loadMore = false) => {
  if (!appId.value || loadingHistory.value) return

  loadingHistory.value = true
  try {
    const params: any = {
      appId: appId.value,
      pageSize: 10,
      lastCreateTime: lastCreateTime.value || undefined
    }

    const res = await listAppChatHistoryByPage(params)
    if (res.data.code === 0 && res.data.data) {
      const historyData = (res.data.data.records || []).slice().reverse()
      const newMessages: Message[] = historyData.map((item: any) => ({
        type: item.messageType === 'user' ? 'user' : 'ai',
        content: item.message,
        createTime: item.createTime
      }))

      if (loadMore) {
        messages.value = [...newMessages, ...messages.value]
      } else {
        messages.value = newMessages
      }

      if (newMessages.length > 0) {
        lastCreateTime.value = newMessages[0].createTime || null
        hasMore.value = newMessages.length === 10
      } else {
        hasMore.value = false
      }

      await nextTick()
      chatMessagesRef.value?.scrollToBottom()
      updatePreview()

      if (!loadMore) {
        checkAndResumeGeneration()
      }
    }
  } finally {
    loadingHistory.value = false
  }
}

const checkAndResumeGeneration = () => {
  if (!isOwner.value) return
  const generatingInfo = getGeneratingInfo()
  if (!generatingInfo) return

  message.info('检测到未完成的任务，正在恢复生成...')
  const lastMessage = messages.value[messages.value.length - 1]
  let aiMessageIndex: number

  if (lastMessage?.type === 'ai') {
    aiMessageIndex = messages.value.length - 1
    messages.value[aiMessageIndex].loading = true
  } else {
    aiMessageIndex = messages.value.length
    messages.value.push({type: 'ai', content: '', loading: true})
  }

  isGenerating.value = true
  generateCode(generatingInfo.message, aiMessageIndex, true)
}

const loadMoreHistory = async () => {
  if (hasMore.value) {
    await fetchChatHistory(true)
  }
}

// === Send Messages ===
const sendInitialMessage = async (prompt: string) => {
  messages.value.push({type: 'user', content: prompt})
  const aiMessageIndex = messages.value.length
  messages.value.push({type: 'ai', content: '', loading: true})
  await nextTick()
  chatMessagesRef.value?.scrollToBottom()
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) return

  const msg = userInput.value.trim()
  userInput.value = ''

  let prompt = msg
  if (selectedElement.value?.selector) {
    prompt = `我选中了页面元素\n（selector: \`${selectedElement.value.selector}\`）\n 请帮我修改选中模块。\n我的具体需求是：${msg}`
  }

  messages.value.push({type: 'user', content: prompt})
  const aiMessageIndex = messages.value.length
  messages.value.push({type: 'ai', content: '', loading: true})
  await nextTick()
  chatMessagesRef.value?.scrollToBottom()

  if (selectedElement.value) clearSelection()
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
  }

  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// === SSE Code Generation ===
const generateCode = async (userMessage: string, aiMessageIndex: number, isReconnect = false) => {
  generatingTime.value = 0
  timer.value = setInterval(() => generatingTime.value++, 1000)
  reconnectAttempts.value = 0
  lastEventId.value = null

  let eventSource: EventSource | null = null
  let streamCompleted = false
  let fullContent = ''
  const existingContentLength = isReconnect ? (messages.value[aiMessageIndex]?.content?.length || 0) : 0

  if (!isReconnect) markGeneratingStart(userMessage)

  const onStreamEnd = () => {
    if (streamCompleted) return
    streamCompleted = true
    eventSource?.close()
    markGeneratingEnd()
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
    isGenerating.value = false
    if (fullContent) {
      messages.value[aiMessageIndex].content = fullContent
    }
    messages.value[aiMessageIndex].loading = false
    setTimeout(async () => {
      await fetchAppInfo()
      updatePreview()
      appPreviewRef.value?.refresh()
    }, 5000)
  }

  // Agent mode from route query
  const useAgent = route.query.useAgent !== 'false'
  let reconnectMode = isReconnect

  const connectSSE = () => {
    try {
      const baseURL = request.defaults.baseURL || API_BASE_URL
      const params = new URLSearchParams({
        appId: appId.value || '',
        message: userMessage,
        isAgent: String(useAgent),
        reconnect: String(reconnectMode)
      })
      if (lastEventId.value) {
        params.set('lastEventId', lastEventId.value)
      }
      const url = `${baseURL}/app/gen/code/stream?${params}`
      eventSource = new EventSource(url, {withCredentials: true})

      eventSource.onmessage = function (event) {
        if (streamCompleted) return
        if (event.lastEventId) lastEventId.value = event.lastEventId
        try {
          const parsed = JSON.parse(event.data)
          const content = parsed.b
          if (content !== undefined && content !== null) {
            fullContent += content
            if (fullContent.length >= existingContentLength) {
              messages.value[aiMessageIndex].content = fullContent
              messages.value[aiMessageIndex].loading = false
            }
            chatMessagesRef.value?.scrollToBottom()
          }
        } catch (error) {
          console.error('解析消息失败:', error)
        }
      }

      eventSource.addEventListener('end', () => onStreamEnd())

      eventSource.addEventListener('error', () => {
        if (streamCompleted || !isGenerating.value) return
        eventSource?.close()
        if (reconnectAttempts.value < maxReconnectAttempts) {
          reconnectAttempts.value++
          reconnectMode = true
          message.info(`连接中断，正在重连... (${reconnectAttempts.value}/${maxReconnectAttempts})`)
          setTimeout(connectSSE, 1000 * reconnectAttempts.value)
        } else {
          markGeneratingEnd()
          streamCompleted = true
          isGenerating.value = false
          if (timer.value) {
            clearInterval(timer.value)
            timer.value = null
          }
          message.error('连接失败次数过多，请刷新页面重试')
          messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
          messages.value[aiMessageIndex].loading = false
        }
      })
    } catch (error) {
      console.error('创建 EventSource 失败：', error)
      messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
      if (timer.value) {
        clearInterval(timer.value)
        timer.value = null
      }
    }
  }

  connectSSE()
}

// === Preview ===
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appStore.selectedApp?.codeGenType || CodeGenTypeEnum.HTML
    if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
      previewUrl.value = getWebProjectStaticPreviewUrl(codeGenType, appId.value)
    } else {
      previewUrl.value = getStaticPreviewUrl(codeGenType, appId.value)
    }
    previewReady.value = true
  }
}

// === Visual Editor ===
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = appPreviewRef.value?.getIframe()
  if (iframe) {
    visualEditor.value?.init(iframe)
    visualEditor.value?.onIframeLoad()
  }
}

const toggleEditMode = () => {
  if (visualEditor.value) {
    isEditMode.value = visualEditor.value.toggleEditMode()
    if (!isEditMode.value) clearSelection()
  }
}

const clearSelection = () => {
  selectedElement.value = null
  visualEditor.value?.clearSelection()
}

// === Deploy ===
const deployApp = async () => {
  if (!appId.value) return
  if (isDeployed.value) return

  deploying.value = true
  try {
    const res = await deployAppApi({appId: appId.value as unknown as number})
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      await fetchAppInfo()
    } else {
      message.error('部署失败：' + (res.data.message || ''))
    }
  } catch (error) {
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

const confirmReDeploy = async () => {
  Modal.confirm({
    title: '重复部署警告',
    content: '请勿频繁部署，若违反则系统自动封号处理！确定要继续部署吗？',
    okText: '确定部署',
    cancelText: '取消',
    onOk: async () => {
      deploying.value = true
      try {
        const res = await deployAppApi({appId: appId.value as unknown as number})
        if (res.data.code === 0 && res.data.data) {
          deployUrl.value = res.data.data
          deployModalVisible.value = true
          message.success('重新部署成功')
          await fetchAppInfo()
        } else {
          message.error('重新部署失败：' + (res.data.message || ''))
        }
      } catch (error) {
        message.error('重新部署失败，请重试')
      } finally {
        deploying.value = false
      }
    }
  })
}

// === Download ===
const downloadCode = async () => {
  if (!appId.value) return
  downloading.value = true
  try {
    const res = await request.get(`/download/code/zip/${appId.value}`, {responseType: 'blob'})
    const blob = new Blob([res.data], {type: 'application/zip'})
    const contentDisposition = res.headers['content-disposition']
    let filename = `${appStore.selectedApp?.appName ?? 'code'}.zip`
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="([^"]+)"/)
      if (match?.[1]) filename = decodeURIComponent(match[1])
    }
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  } catch (e: any) {
    message.error('下载失败，' + e.message)
  } finally {
    downloading.value = false
  }
}

// === Actions ===
const openInNewTab = () => {
  if (previewUrl.value) window.open(previewUrl.value, '_blank')
}

const visitDeployedSite = () => {
  if (deployedSiteUrl.value) window.open(deployedSiteUrl.value, '_blank')
  else if (deployUrl.value) window.open(deployUrl.value, '_blank')
}

const showAppDetail = () => {
  appDetailVisible.value = true
}

const handleEditApp = () => {
  if (appStore.selectedApp?.id) {
    router.push(`/app/edit/${appStore.selectedApp.id}`)
  }
}

const handleDeleteApp = async () => {
  if (!appStore.selectedApp?.id) return
  try {
    const res = await deleteAppApi({id: appStore.selectedApp.id})
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      appStore.clearSelectedApp()
      await appStore.loadMyApps()
      router.push('/')
    } else {
      message.error('删除失败：' + (res.data.message || ''))
    }
  } catch (error) {
    message.error('删除失败，请重试')
  }
}

// === Route Watcher ===
watch(() => route.params.id, (newId) => {
  if (newId) {
    // Set appId first to prevent flash of default view
    appId.value = newId as string
    resetChatState()
    appId.value = newId as string // Re-set after reset clears it
    fetchAppInfo()
  } else {
    appStore.clearSelectedApp()
    resetChatState()
  }
}, {immediate: false})

const resetChatState = () => {
  appId.value = ''
  messages.value = []
  userInput.value = ''
  isGenerating.value = false
  previewUrl.value = ''
  previewReady.value = false
  loadingHistory.value = false
  lastCreateTime.value = null
  hasMore.value = true
  generatingTime.value = 0
  isEditMode.value = false
  selectedElement.value = null
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

// === Lifecycle ===
onMounted(() => {
  visualEditor.value = new visualEditorUtil({
    onElementSelected: (elementInfo: ElementInfo) => {
      selectedElement.value = elementInfo
    }
  })
  window.addEventListener('message', handleIframeMessage)

  // If route has app id, load it
  if (route.params.id) {
    fetchAppInfo()
  }

  // Load user's apps for sidebar
  if (loginUserStore.loginUser.id) {
    appStore.loadMyApps()
  }
})

onUnmounted(() => {
  window.removeEventListener('message', handleIframeMessage)
  visualEditor.value?.disableEditMode()
  if (timer.value) clearInterval(timer.value)
})

const handleIframeMessage = (event: MessageEvent) => {
  visualEditor.value?.handleIframeMessage(event)
}
</script>

<style scoped>
.workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #fff;
  position: relative;
}

/* Default view (no app) */
.workspace-default {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  gap: 32px;
}

.greeting {
  text-align: center;
}

.greeting-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  margin-bottom: 16px;
}

.greeting-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.greeting-sub {
  font-size: 15px;
  color: #999;
  margin: 0;
}

/* Prompt grid */
.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
  max-width: 760px;
  width: 100%;
}

.prompt-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt-card:hover {
  background: #f5f5f5;
  border-color: #e5e5e5;
  transform: translateY(-1px);
}

.prompt-label {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.prompt-desc {
  font-size: 12px;
  color: #bbb;
}

/* App selected view */
.workspace-body {
  flex: 1;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  min-height: 0;
}

/* Chat mode: split view — chat left, preview right */
.split-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  min-width: 0;
  max-width: 420px;
  border-right: 1px solid #f0f0f0;
}

/* Preview area — flexible, always present */
.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  min-width: 0;
  padding: 8px;
  transition: flex 0.3s ease;
}

/* Full-width when in app mode (chat hidden via v-show) */
.preview-full {
  flex: 1;
}

/* Edit mode bar */
.edit-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { transform: translateY(-100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.edit-bar-text {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.edit-bar-exit {
  padding: 5px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.edit-bar-exit:hover {
  background: #fafafa;
  border-color: #d0d0d0;
}
</style>
