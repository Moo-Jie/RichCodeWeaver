<template>
  <div id="appChatPage" :class="{ 'edit-mode-active': isEditMode }" class="creative-chat">
    <!-- 编辑模式全局覆盖层 -->
    <div v-if="isEditMode" class="edit-mode-overlay"></div>
    <!-- 编辑模式顶部提示条 -->
    <div v-if="isEditMode" class="edit-mode-top-bar">
      <div class="top-bar-content">
        <EditOutlined style="color: #595959; margin-right: 8px;" />
        <span style="color: #595959; font-weight: 800;">可视化编辑模式已启用</span>
        <span style="color: #595959; margin-left: 12px; font-size: 20px;">
         （ 点击想要修改的页面元素并输入你的需求）
        </span>
      </div>
      <div class="top-bar-actions">
        <button class="exit-edit-btn" @click="toggleEditMode">
          退出编辑模式
        </button>
      </div>
    </div>
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-bar-info">
        <template v-if="appInfo">
          <div>
            名称：
            <a-tag :color="getTypeColor(appInfo?.codeGenType)">
              {{
                appInfo?.appName?.substring(0, 10) + '...' || '待命名应用'
              }}
            </a-tag>
          </div>
        </template>
        &nbsp;
        <template v-if="appInfo">
          <div>
            生成模式：
            <a-tag :color="getTypeColor(appInfo?.codeGenType)" class="app-tag2">
              {{
                appInfo.codeGenType === 'single_html' ? '单文件结构' : appInfo.codeGenType === 'multi_file' ? '多文件结构' : appInfo.codeGenType === 'vue_project' ? 'VUE 项目工程' : formatCodeGenType(appInfo.codeGenType)
              }}
            </a-tag>
          </div>
        </template>
        &nbsp;
        <!-- 星选应用  -->
        <template v-if="appInfo">
          推广级别：
          <a-tag v-if="appInfo.priority === 99" class="app-tag2" color="gold">
            <star-filled />
            星选应用
          </a-tag>
          <span v-else class="app-tag">普通应用</span>
        </template>
      </div>
      &nbsp;&nbsp;
      <!-- 功能按钮  -->
      <div class="header-right">
        <!-- 查看/编辑 应用信息  -->
        <a-button class="detail-btn" type="primary" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          查看/编辑 应用信息
        </a-button>
        <!-- 全屏查看预览  -->
        <a-button v-if="previewUrl" :disabled="isGenerating" :loading="deploying" class="detail-btn"
                  type="primary"
                  @click="openInNewTab ">
          <template #icon>
            <ExportOutlined />
          </template>
          全屏查看预览
        </a-button>
        <!-- 下载代码 -->
        <a-button :disabled="isGenerating" :loading="downloading" class="detail-btn" type="primary"
                  @click="downloadCode">
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <!-- 已部署状态下的 重复部署 按钮 -->
        <template v-if="isDeployed">
          <!-- 重复部署 按钮 -->
          <a-button :disabled="isGenerating" :loading="deploying" class="detail-btn" type="primary"
                    @click="confirmReDeploy">
            <template #icon>
              <CloudUploadOutlined />
            </template>
            我的代码已更新，重复部署
          </a-button>
          <!-- 访问已部署网站 按钮 -->
          <a-button :href="deployedSiteUrl" class="detail-btn" target="_blank" type="primary">
            <template #icon>
              <ExportOutlined />
            </template>
            访问已部署网站
          </a-button>
        </template>
        <!-- 未部署状态下的 部署为可访问网站 按钮 -->
        <a-button v-else :disabled="isGenerating" :loading="deploying" class="detail-btn"
                  type="primary"
                  @click="deployApp">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署为可访问网站
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <div class="section-header">
          <div class="decorative-line"></div>
          <h2 class="section-title">AI创作对话区</h2>
          <div class="decorative-line"></div>
        </div>

        <!-- 加载更多按钮 -->
        <div v-if="hasMore.value || loadingHistory.value" class="load-more-container">
          <a-button
            v-if="hasMore.value && !loadingHistory.value"
            class="load-more-btn"
            type="text"
            @click="loadMoreHistory"
          >
            加载更多历史消息
          </a-button>
          <a-spin v-else size="small" />
        </div>


        <!-- 消息区域 -->
        <div ref="messagesContainer" class="messages-container">
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content creative-bubble">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" class="avatar-glow" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" class="avatar-glow ai-avatar" />
              </div>
              <div class="message-content creative-bubble">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在编织灵感...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <a-alert
            v-if="selectedElement"
            :message="`已选择“${selectedElement.tagName}”标签元素`"
            class="selected-element-alert"
            closable
            show-icon
            style="border-radius: 12px; border: 1px solid #d9f7be; background: linear-gradient(135deg, rgba(246, 255, 237, 0.95) 0%, rgba(230, 255, 250, 0.95) 100%);"
            type="success"
            @close="clearSelection"
          >
            <template #description>
              <div class="element-details">
                <p style="margin: 4px 0; font-size: 13px; color: #389e0d;">
                  <strong>选择器类型:</strong> {{ selectedElement.className || '无' }}
                </p>
                <p style="margin: 4px 0; font-size: 13px; color: #389e0d;">
                  <strong>代码块:</strong> {{ selectedElement.selector.substring(0, 100) }}
                </p>
              </div>
            </template>
          </a-alert>
          <div>
            <div v-if="!isOwner" class="creator-tip">
              <a-alert description="如需对话请创建您自己的项目" message="这是别人的创作作品"
                       show-icon type="info" />
            </div>
            <a-textarea
              v-else
              v-model:value="userInput"
              :disabled="isGenerating"
              :maxlength="1000"
              :rows="4"
              class="creative-textarea"
              placeholder="✨ 描述越详尽，创作越符合您的预期 ✨"
              @keydown.enter.prevent="sendMessage"
            />
            <br>
            <div class="input-actions">
              <!-- 新手指引按钮 -->
              <a-tooltip placement="top" title="新手指引">
                <a-button
                  class="tour-button"
                  shape="circle"
                  size="large"
                  style="border-radius: 20px; font-weight: 600; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);"
                  type="primary"
                  @click="startTour"
                >
                  <template #icon>
                    <PlayCircleOutlined />
                  </template>
                </a-button>
              </a-tooltip>
              &nbsp;&nbsp;&nbsp;
              <!-- 编辑按钮 -->
              <a-tooltip :title="isEditMode ? '退出编辑模式' : '进入可视化编辑模式'"
                         placement="top">
                <a-button
                  v-if="!isGenerating"
                  :class="{ 'edit-mode-active': isEditMode }"
                  :type="isEditMode ? 'danger' : 'primary'"
                  class="edit-btn"
                  shape="circle"
                  size="large"
                  style="border-radius: 20px; font-weight: 600; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);"
                  @click="toggleEditMode"
                >
                  <template #icon>
                    <EditOutlined />
                  </template>
                </a-button>
              </a-tooltip>
              &nbsp;&nbsp;&nbsp;
              <!-- 发送按钮 -->
              <a-tooltip v-if="!isOwner" placement="top" title="请创建自己的作品来与AI对话">
                <a-button
                  :loading="isGenerating"
                  class="send-btn"
                  shape="circle"
                  size="large"
                  type="primary"
                  @click="sendMessage"
                >
                  <template #icon>
                    <SendOutlined />
                  </template>
                </a-button>
              </a-tooltip>
              &nbsp;&nbsp;&nbsp;
              <a-tooltip v-else placement="top" title="开始对话">
                <a-button
                  :loading="isGenerating"
                  class="send-btn"
                  shape="circle"
                  size="large"
                  type="primary"
                  @click="sendMessage"
                >
                  <template #icon>
                    <SendOutlined />
                  </template>
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <div class="section-header">
            <div class="decorative-line"></div>
            <h2 class="section-title">网页预览</h2>
            <div class="decorative-line"></div>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating"
               class="preview-placeholder creative-preview-placeholder">
            <div class="placeholder-icon">
              <div class="magic-pattern">
                <div class="pattern-element pattern-1"></div>
                <div class="pattern-element pattern-2"></div>
                <div class="pattern-element pattern-3"></div>
                <div class="pattern-element pattern-4"></div>
              </div>
            </div>
            <p class="placeholder-text">灵感火花即将绽放</p>
            <p class="placeholder-subtext">描述您的创意，AI将为您编织网站</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin :tip="generatingTip" size="large" />
            <!-- 已用时间显示 -->
            <div class="generating-time">
              <p class="wait-tip">请勿刷新或退出本页面，否则将断开链接。</p>
              <p class="wait-tip2">已思考时间: {{ generatingTime }}秒</p>
              <p class="wait-tip2">为了生成美观完善的页面，请耐心等待...</p>
            </div>
          </div>
          <iframe
            v-else
            ref="previewIframe"
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
        <div v-if="previewUrl" class="preview-footer">
          <div class="tips-toggle" @click="showTips = !showTips">
            <span>点击查看重点提示</span>
            <span :class="['toggle-icon', { 'expanded': showTips }]">
        ▼
      </span>
          </div>
          <transition name="slide-down">
            <a-alert
              v-if="showTips"
              class="preview-alert"
              message="重点提示："
              show-icon
              type="info"
            >
              <template #description>
                <div>
                  <p>1.预览为静态页面效果，部署后可体验完整功能;</p>
                  <p>2.应用封面将在部署后自动生成（本应用首页），请在部署后静待 1-5s 即可;</p>
                  <p>3.若未生成预览页面请刷新页面;</p>
                  <p>4.请勿频繁部署，若违反则系统自动封号处理。</p>
                </div>
              </template>
            </a-alert>
          </transition>
        </div>
      </div>
    </div>
  </div>

  <!-- 应用详情弹窗 -->
  <AppInfo
    v-model:open="appDetailVisible"
    :app="appInfo"
    :show-actions="isOwner || isAdmin"
    @delete="deleteApp"
    @edit="editApp"
  />

  <!-- 部署成功弹窗 -->
  <DeploySuccessModal
    v-model:open="deployModalVisible"
    :deploy-url="deployUrl"
    @open-site="openDeployedSite"
  />

  <!-- 新手指引组件 -->
  <Tour
    v-model:current="tourCurrent"
    v-model:open="tourOpen"
    :steps="tourSteps"
    @close="handleTourClose"
  />
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { TourProps } from 'ant-design-vue'
import { message, Modal, Tour } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById
} from '@/api/appController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/enums/codeGenTypes.ts'
import request from '@/request'
import { listAppChatHistoryByPage } from '@/api/chatHistoryController'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppInfo from '@/components/AppInfo.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import {
  API_BASE_URL,
  DEPLOY_DOMAIN,
  getStaticPreviewUrl,
  getWebProjectStaticPreviewUrl
} from '@/config/env'

import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  PlayCircleOutlined,
  SendOutlined,
  StarFilled
} from '@ant-design/icons-vue'
import { type ElementInfo, visualEditorUtil } from '@/utils/visualEditorUtil'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 新手指引相关
const tourOpen = ref(false)
const tourCurrent = ref(0)
const tourSteps = ref<TourProps['steps']>([
  {
    title: '对话输入',
    description: '在这里输入您的需求，AI会根据您的描述生成代码',
    target: () => document.querySelector('.creative-textarea') as HTMLElement,
    placement: 'top'
  },
  {
    title: '发送消息',
    description: '输入需求后点击这里发送给AI，开始生成代码',
    target: () => document.querySelector('.send-btn') as HTMLElement,
    placement: 'top'
  },
  {
    title: '预览区域',
    description: '这里实时显示生成的网页效果，可以随时查看和测试',
    target: () => document.querySelector('.preview-section') as HTMLElement,
    placement: 'left'
  },
  {
    title: '可视化编辑',
    description: '点击这里可以进入可视化编辑模式，直接修改网页元素',
    target: () => document.querySelector('.edit-btn') as HTMLElement,
    placement: 'top'
  },
  {
    title: '部署应用',
    description: '生成完成后点击这里将应用部署到线上，获得可访问的网址',
    target: () => document.querySelector('.detail-btn') as HTMLElement,
    placement: 'top'
  }
])

// 开始新手指引
const startTour = () => {
  tourOpen.value = true
  tourCurrent.value = 0
}

// 关闭新手指引
const handleTourClose = () => {
  tourOpen.value = false
}

// 添加计时器相关变量
const generatingTime = ref(0)
const timer = ref(null)


// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 部署按钮点击处理
const handleDeployClick = () => {
  if (isGenerating.value) {
    Modal.info({
      title: '请等待生成完成',
      content: '代码正在生成中，请等待生成完毕后部署网站',
      okText: '知道了'
    })
    return
  }
  deployApp()
}

// 根据生成类型获取标签颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'react': '#61dafb',
    'vue': '#42b883',
    'angular': '#dd0031',
    'html': '#e34c26',
    'nodejs': '#68a063',
    'flutter': '#04599C',
    'swift': '#ff2d55'
  }
  return colors[type] || 'blue'
}

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
// 历史消息加载相关
const loadingHistory = ref(false)
const lastCreateTime = ref<LocalDateTime | null>(null)
const hasMore = ref(true)
// 标记是否已经进行过初始对话
const hasInitialConversation = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const previewIframe = ref<HTMLIFrameElement>()

// 部署相关
const deploying = ref(false)
const downloading = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')
const showReDeployWarning = ref(false)

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 可视化编辑相关
const visualEditor = ref<visualEditorUtil | null>(null)
const isEditMode = ref(false)
const selectedElement = ref<ElementInfo | null>(null)


// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 判断应用是否已部署
const isDeployed = computed(() => {
  return !!appInfo.value?.deployKey
})

// 获取部署后的访问URL
const deployedSiteUrl = computed(() => {
  if (appInfo.value?.deployKey) {
    return `${DEPLOY_DOMAIN}/${appInfo.value.deployKey}`
  }
  return ''
})

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    await router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      await fetchChatHistory()

      // 更新预览
      updatePreview()

      // 自动发送初始提示词（除非是查看模式或已经进行过初始对话）
      if (appInfo.value.initPrompt && isOwner.value && messages.value.length === 0) {
        hasInitialConversation.value = true
        await sendInitialMessage(appInfo.value.initPrompt)
      } else if (messages.value.length >= 2) {
        // 如果有至少2条对话记录，更新预览
        updatePreview()
      }
    } else {
      message.error('获取应用信息失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
  }
}

// 可视化编辑相关
onMounted(() => {
  // 初始化 visualEditor
  visualEditor.value = new visualEditorUtil({
    onElementSelected: (elementInfo) => {
      selectedElement.value = elementInfo
    }
  })

  // 监听来自 iframe 的消息
  window.addEventListener('message', handleIframeMessage)

  // 如果是新用户且没有对话记录，自动显示新手指引
  if (messages.value.length === 0 && isOwner.value) {
    // 延迟显示，确保DOM已渲染完成
    setTimeout(() => {
      startTour()
    }, 1000)
  }
})

onUnmounted(() => {
  window.removeEventListener('message', handleIframeMessage)
  visualEditor.value?.disableEditMode()
})

// 处理来自 iframe 的消息
const handleIframeMessage = (event: MessageEvent) => {
  visualEditor.value?.handleIframeMessage(event)
}

// 切换编辑模式
const toggleEditMode = () => {
  if (visualEditor.value) {
    isEditMode.value = visualEditor.value.toggleEditMode()
    if (!isEditMode.value) {
      // 退出编辑模式时，清除选中
      clearSelection()
    }
  }
}

// 清除选中
const clearSelection = () => {
  selectedElement.value = null
  visualEditor.value?.clearSelection()
}


// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const res = await request.get(`/download/code/zip/${appId.value}`, {
      responseType: 'blob'
    })

    const blob = new Blob([res.data], { type: 'application/zip' })

    const contentDisposition = res.headers['content-disposition']

    // 处理文件名编码
    let filename = `${appInfo.value?.appName ?? 'code'}.zip`
    if (contentDisposition) {
      const filenameRegex = /filename="([^"]+)"/
      const match = contentDisposition.match(filenameRegex)
      if (match && match[1]) {
        // 解码文件名
        filename = decodeURIComponent(match[1])
      }
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

// 获取对话历史
const fetchChatHistory = async (loadMore = false) => {
  if (!appId.value || loadingHistory.value) return

  loadingHistory.value = true
  try {
    const params = {
      appId: appId.value,
      pageSize: 10,
      lastCreateTime: lastCreateTime.value ? lastCreateTime.value.toISOString() : undefined
    }

    const res = await listAppChatHistoryByPage(params)
    if (res.data.code === 0 && res.data.data) {
      const historyData = res.data.data.records.slice().reverse() || []
      const newMessages = historyData.map(item => ({
        type: item.messageType === 'user' ? 'user' : 'ai',
        content: item.message,
        createTime: item.createTime
      }))

      // 处理加载更多
      if (loadMore) {
        // 加载更多时到前面
        messages.value = [...newMessages, ...messages.value]
      } else {
        // 首次加载直接替换
        messages.value = newMessages
      }

      // 更新游标和是否有更多数据
      if (newMessages.length > 0) {
        lastCreateTime.value = new Date(newMessages[0].createTime!).toISOString()
        hasMore.value = newMessages.length === 10
      } else {
        hasMore.value = false
      }

      await nextTick()
      scrollToBottom()
      // 加载历史消息后更新预览
      updatePreview()
    }
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  if (hasMore.value) {
    await fetchChatHistory(true)
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  const message = userInput.value.trim()
  userInput.value = ''

  // 构建带上下文的提示
  let prompt = message
  if (selectedElement.value?.selector) {
    prompt = `我选中了页面元素\n（selector: \`${selectedElement.value.selector}\`）\n 请帮我修改选中模块。\n我的具体需求是：${message}`
  }


  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true
  })

  await nextTick()
  scrollToBottom()

  // 发送后清除选中并退出编辑模式
  if (selectedElement.value) {
    clearSelection()
  }
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
  }

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  // 启动计时器
  generatingTime.value = 0
  timer.value = setInterval(() => {
    generatingTime.value++
  }, 1000)

  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage
    })

    const url = `${baseURL}/app/gen/code/stream?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function(event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.b

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理 end 事件
    eventSource.addEventListener('end', function() {
      if (streamCompleted) return

      // 清除计时器
      if (timer.value) {
        clearInterval(timer.value)
        timer.value = null
      }

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
        // 强制刷新预览 iframe
        if (previewIframe.value) {
          previewIframe.value.src = previewIframe.value.src
        }
      }, 5000) // 5秒延迟
    })

    // 处理错误
    eventSource.onerror = function() {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
          // 强制刷新预览
          const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
          if (iframe) {
            iframe.src = iframe.src
          }
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  // 清除计时器
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    // 默认使用 HTML 类型
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
      previewUrl.value = getWebProjectStaticPreviewUrl(codeGenType, appId.value)
    } else {
      previewUrl.value = getStaticPreviewUrl(codeGenType, appId.value)
    }

    previewReady.value = true

    // 如果iframe已存在，强制刷新
    setTimeout(() => {
      if (previewIframe.value) {
        previewIframe.value.src = previewUrl.value
      }
    }, 1000) // 短暂延迟确保DOM已更新
  }
}

// 重点提示
const showTips = ref(false)

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  // 如果已经部署，显示警告而不是直接部署
  if (isDeployed.value) {
    showReDeployWarning.value = true
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      // 刷新应用信息以获取最新的部署状态
      await fetchAppInfo()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 确认重复部署
const confirmReDeploy = async () => {
  Modal.confirm({
    title: '重复部署警告',
    content: '请勿频繁部署，若违反则系统自动封号处理！确定要继续部署吗？',
    okText: '确定部署',
    cancelText: '取消',
    onOk: async () => {
      deploying.value = true
      try {
        const res = await deployAppApi({
          appId: appId.value as unknown as number
        })

        if (res.data.code === 0 && res.data.data) {
          deployUrl.value = res.data.data
          deployModalVisible.value = true
          message.success('重新部署成功')
          // 刷新应用信息以获取最新的部署状态
          await fetchAppInfo()
        } else {
          message.error('重新部署失败：' + res.data.message)
        }
      } catch (error) {
        console.error('重新部署失败：', error)
        message.error('重新部署失败，请重试')
      } finally {
        deploying.value = false
        showReDeployWarning.value = false
      }
    }
  })
}

// 取消重复部署
const cancelReDeploy = () => {
  showReDeployWarning.value = false
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  if (previewIframe.value) {
    visualEditor.value?.init(previewIframe.value)
    visualEditor.value?.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})

// 清理资源,EventSource 会在组件卸载时自动清理
onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@300;400;700&family=Nunito:wght@300;400;600;700&display=swap');

#appChatPage.creative-chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px 30px;
  background: #f8f9fa linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #333333;
}

/* 背景纹理 */
#appChatPage.creative-chat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><path fill="none" stroke="rgba(180,170,255,0.1)" stroke-width="1" d="M20,20 Q40,5 60,20 T100,20 M20,40 Q30,30 40,40 T80,40 M10,70 Q35,55 60,70 T90,70"/></svg>');
  background-size: 300px;
  opacity: 0.2;
  pointer-events: none;
  z-index: 0;
}

.generating-time {
  margin-top: 20px;
  text-align: center;
}

.generating-time p {
  margin: 5px 0;
  color: #666;
}

.wait-tip {
  font-style: italic;
  color: #ff9c9c;
  font-size: 16px;
}

.wait-tip2 {
  font-style: italic;
  color: #888;
  font-size: 16px;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  min-width: 1600px;
  justify-content: space-between;
  align-items: center;
  padding: 15px 25px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  z-index: 10;
  position: relative;
  margin: 0 auto 15px;
  border: 2px solid #e8e8e8;
}

.header-bar-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  position: relative;
  margin: 0 auto;
  border: 2px solid #e8e8e8;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.app-name {
  margin: 0;
  font-family: 'Comic Neue', cursive;
  font-size: 26px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.header-right {
  display: flex;
  gap: 15px;
}

.detail-btn {
  border-radius: 12px;
  font-weight: 600;
  padding: 0 20px;
  height: 40px;
  transition: all 0.3s;
  font-family: 'Nunito', sans-serif;
  border: none;
  background: linear-gradient(135deg, #a8e6cf 0%, #dcedc1 100%);
  color: #2c3e50;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.detail-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(168, 230, 207, 0.4);
}

/* 主要内容区域 */
.main-content {
  flex-direction: row;
  flex: 1;
  display: flex;
  gap: 30px;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  min-height: 400px;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: 2px solid #e8e8e8;
  transition: all 0.3s ease;
}

.chat-section:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  transform: translateY(-3px);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 25px 0;
  gap: 15px;
}

.decorative-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(to right, transparent, #f0f0f0, transparent);
}

.section-title {
  font-family: 'Comic Neue', cursive;
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
  text-align: center;
  padding: 0 20px;
  letter-spacing: -0.5px;
  position: relative;
}

.messages-container {
  flex: 1;
  padding: 0 25px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 30px;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  gap: 12px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;
}

.creative-bubble {
  max-width: 75%;
  padding: 18px 22px;
  border-radius: 16px;
  line-height: 1.6;
  word-wrap: break-word;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  font-family: 'Nunito', sans-serif;
}

.user-message .creative-bubble {
  background: linear-gradient(135deg, #bddeff 0%, #7b94ff 100%);
  color: #2c3e50;
  border-bottom-right-radius: 5px;
  font-weight: 600;
}

.ai-message .creative-bubble {
  background: linear-gradient(135deg, #d8ffef 0%, #eac6ff 100%);
  color: #2c3e50;
  border-bottom-left-radius: 5px;
}

.message-content {
  position: relative;
}

.user-message .message-content::after {
  content: '';
  position: absolute;
  right: -10px;
  bottom: 0;
  border: 10px solid transparent;
  border-bottom: 10px solid #096dd9;
  border-left: 10px solid #096dd9;
}

.ai-message .message-content::before {
  content: '';
  position: absolute;
  left: -10px;
  bottom: 0;
  border: 10px solid transparent;
  border-bottom: 10px solid #e8e8e8;
  border-right: 10px solid #e8e8e8;
}

.avatar-glow {
  box-shadow: 0 0 15px rgba(198, 160, 138, 0.3);
  border: 2px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.avatar-glow:hover {
  transform: scale(1.1);
  box-shadow: 0 0 20px rgba(198, 160, 138, 0.5);
}

.ai-avatar {
  background: #ffffff;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  font-weight: 500;
  margin-top: 10px;
}

/* 输入区域 */
.input-container {
  padding: 25px;
  background: rgba(255, 255, 255, 0.7);
  border-top: 2px solid #f0f0f0;
}

.creative-textarea {
  padding: 20px;
  font-size: 16px;
  border-radius: 16px;
  border: 2px solid #e8e8e8;
  background: #ffffff;
  resize: none;
  transition: all 0.3s ease;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  font-family: 'Nunito', sans-serif;
}

.creative-textarea:focus {
  box-shadow: 0 0 0 3px rgb(204, 247, 255);
  outline: none;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  bottom: 8px;
  right: 8px;
}

.send-btn {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  transition: all 0.3s ease;
  color: white;
}

.send-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(116, 235, 213, 0.4);
}

/* 编辑按钮样式 */
.edit-btn {
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
}

.edit-btn.edit-mode-active {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.edit-btn:not(.edit-mode-active):hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.3);
}

@keyframes editPulse {
  0%, 100% {
    box-shadow: 0 6px 20px rgba(245, 34, 45, 0.4);
  }
  50% {
    box-shadow: 0 8px 25px rgba(245, 34, 45, 0.6), 0 0 15px rgba(255, 77, 79, 0.5);
  }
}

/* 编辑模式状态指示器 */
.edit-mode-indicator {
  margin-right: 15px;
  animation: indicatorPulse 1.5s infinite;
  position: relative;
  z-index: 1000;
}

.edit-mode-tag {
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(245, 34, 45, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

@keyframes indicatorPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

/* 编辑模式全局样式 */
.creative-chat.edit-mode-active {
  position: relative;
  border-radius: 12px;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3);
  transition: box-shadow 0.3s ease;
}

@keyframes editModeBorderPulse {
  0%, 100% {
    box-shadow: 0 0 0 3px rgba(245, 34, 45, 0.3);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(245, 34, 45, 0.6), 0 0 20px rgba(255, 77, 79, 0.4);
  }
}

/* 编辑模式顶部提示条 */
.edit-mode-top-bar {
  position: fixed;
  font-size: 30px;
  top: 0;
  left: 0;
  right: 0;
  height: 85px;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  z-index: 1001;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  animation: topBarSlideDown 0.5s ease-out;

  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.top-bar-content {
  display: flex;
  align-items: center;
  font-weight: 600;
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.exit-edit-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.exit-edit-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
}


@keyframes topBarSlideDown {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(0);
  }
}

.edit-mode-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 77, 79, 0.03) 0%, rgba(255, 169, 0, 0.03) 100%);
  pointer-events: none;
  z-index: 999;
  animation: overlayPulse 3s infinite;
}

@keyframes overlayPulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.5;
  }
}

.input-tip {
  position: absolute;
  left: 15px;
  bottom: -30px;
  font-size: 13px;
  color: #6d6b80;
  font-style: italic;
}

.creator-tip {
  margin-bottom: 15px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border: 2px solid #e8e8e8;
  transition: all 0.3s ease;
  height: 85vh;
  position: relative;
}

.preview-section:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  transform: translateY(-3px);
}

/* 编辑模式下预览内容高亮效果 */
.edit-mode-active .preview-content {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 0 0 15px rgb(147, 203, 255),
  0 0 25px rgb(89, 167, 255),
  inset 0 0 20px rgba(24, 144, 255, 0.4);
  transform: scale(1.01);
  transition: all 0.3s ease;
  z-index: 100;
}

@keyframes previewHighlight {
  0% {
    box-shadow: 0 0 0 5px rgba(24, 144, 255, 0.8),
    0 0 25px rgba(24, 144, 255, 0.6),
    inset 0 0 20px rgba(24, 144, 255, 0.4);
  }
  100% {
    box-shadow: 0 0 0 7px rgba(24, 144, 255, 1),
    0 0 35px rgba(24, 144, 255, 0.8),
    inset 0 0 30px rgba(24, 144, 255, 0.6);
    transform: scale(1.015);
  }
}

.edit-mode-active .preview-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 10;
  animation: overlayPulse 1.5s ease-in-out infinite alternate;
}

@keyframes overlayPulse {
  0% {
    opacity: 0.6;
  }
  100% {
    opacity: 0.9;
  }
}

.edit-mode-active .preview-content::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  pointer-events: none;
  z-index: 11;
  animation: borderPulse 1.8s ease-in-out infinite alternate;
}

@keyframes borderPulse {
  0% {
    opacity: 0.4;
  }
  100% {
    opacity: 1;
  }
}

@keyframes strong-pulse {
  0%, 100% {
    box-shadow: 0 0 0 6px rgba(24, 144, 255, 0.2),
    0 0 40px rgba(24, 144, 255, 0.6),
    inset 0 0 30px rgba(24, 144, 255, 0.15);
    border-width: 5px;
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 144, 255, 0.25),
    0 0 50px rgba(24, 144, 255, 0.8),
    inset 0 0 35px rgba(24, 144, 255, 0.2);
    border-width: 6px;
  }
}

.preview-header {
  padding: 20px 30px 15px;
  background: white;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
}

.creative-preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;
}

.magic-pattern {
  position: relative;
  width: 180px;
  height: 180px;
  margin-bottom: 25px;
}

.pattern-element {
  position: absolute;
  border-radius: 50%;
  opacity: 0.7;
  animation: pulse 5s infinite ease-in-out;
}

.pattern-1 {
  width: 100px;
  height: 100px;
  background: rgba(114, 46, 209, 0.15);
  top: 0;
  left: 40px;
  animation-delay: 0s;
}

.pattern-2 {
  width: 70px;
  height: 70px;
  background: rgba(24, 144, 255, 0.15);
  top: 30px;
  right: 40px;
  animation-delay: 0.5s;
}

.pattern-3 {
  width: 120px;
  height: 120px;
  background: rgba(146, 84, 222, 0.1);
  bottom: 20px;
  left: 30px;
  animation-delay: 1s;
}

.pattern-4 {
  width: 90px;
  height: 90px;
  background: rgba(82, 196, 26, 0.1);
  bottom: 40px;
  right: 30px;
  animation-delay: 1.5s;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.placeholder-text {
  font-size: 20px;
  font-weight: 600;
  color: #5c4a48;
  margin: 15px 0 5px;
}

.placeholder-subtext {
  font-size: 15px;
  color: #6d6b80;
  max-width: 300px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

.preview-footer {
  padding: 15px 30px;
  border-top: 1px solid rgba(200, 180, 170, 0.2);
}

.preview-alert {
  border-radius: 12px;
  background: rgba(255, 0, 0, 0.05);
  border: 1px solid rgba(255, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }

  .header-bar {
    padding: 15px;
  }

  .app-name {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  #appChatPage.creative-chat {
    padding: 15px;
  }

  .header-bar {
    flex-direction: column;
    gap: 10px;
    padding: 15px;
  }

  .header-left,
  .header-right {
    width: 100%;
    justify-content: center;
  }

  .main-content {
    gap: 15px;
  }

  .section-title {
    font-size: 18px;
  }

  .message-content {
    max-width: 80%;
  }
}

.load-more-container {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.load-more-btn {
  color: #1890ff;
  transition: color 0.3s;
}

.load-more-btn:hover {
  color: #096dd9;
}

.tips-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 10px;
  border: 1px solid rgba(24, 144, 255, 0.2);
}

.tips-toggle:hover {
  background: rgba(24, 144, 255, 0.15);
}

.tips-toggle span:first-child {
  font-weight: 600;
  color: #1890ff;
  margin-right: 8px;
}

.toggle-icon {
  transition: transform 0.3s ease;
  font-size: 12px;
  color: #1890ff;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  max-height: 200px;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}

.preview-footer {
  padding: 15px 30px;
  border-top: 1px solid rgba(200, 180, 170, 0.2);
}

.preview-alert {
  border-radius: 12px;
  background: rgba(255, 0, 0, 0.05);
  border: 1px solid rgba(255, 0, 0, 0.1);
}

.detail-btn[target="_blank"] {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.detail-btn[target="_blank"] .ant-btn-loading-icon,
.detail-btn[target="_blank"] .anticon {
  display: flex !important;
  align-items: center !important;
}

.detail-btn[target="_blank"] span {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  line-height: 1 !important;
}

.app-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  background: #f0f0f0;
  color: #666;
  font-weight: 500;
  font-size: 14px;
  border: 1px solid #d9d9d9;
}

.app-tag2 {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 500;
  font-size: 14px;
}

/* 编辑按钮样式 */
.tour-button {
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
}

.tour-button.edit-mode-active {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.tour-button:not(.edit-mode-active):hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.3);
}
</style>
