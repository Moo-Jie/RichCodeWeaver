<template>
  <div id="appChatPage" class="creative-chat">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName?.substring(0, 10) + '  ...' || '网站生成器' }}</h1>
      </div>
      <div class="header-right">
        <a-button type="default" class="detail-btn" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button type="primary" class="deploy-btn" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署网站
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
            type="text"
            @click="loadMoreHistory"
            class="load-more-btn"
          >
            加载更多历史消息
          </a-button>
          <a-spin v-else size="small" />
        </div>


        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
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
          <div class="input-wrapper">
            <div v-if="!isOwner" class="creator-tip">
              <a-alert message="这是别人的创作作品" description="如需对话请创建您自己的项目" type="info" show-icon />
            </div>
            <a-textarea
              v-else
              v-model:value="userInput"
              placeholder="✨ 请用丰富语言描述您的网站愿景 —— 描述越详尽，创作越精彩 ✨"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
              class="creative-textarea"
            />
            <div class="input-actions">
              <a-tooltip v-if="!isOwner" title="请创建自己的作品来与AI对话" placement="top">
                <a-button
                  type="primary"
                  shape="circle"
                  size="large"
                  :disabled="!isOwner"
                  class="send-btn"
                >
                  <template #icon>
                    <SendOutlined />
                  </template>
                </a-button>
              </a-tooltip>
              <a-button
                v-else
                type="primary"
                shape="circle"
                size="large"
                @click="sendMessage"
                :loading="isGenerating"
                class="send-btn"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
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
          <div class="preview-actions">
            <a-button v-if="previewUrl" type="primary" ghost @click="openInNewTab" class="preview-action-btn">
              <template #icon>
                <ExportOutlined />
              </template>
              全屏查看
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder creative-preview-placeholder">
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
            <a-spin size="large" :tip="generatingTip" />
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
        <div v-if="previewUrl" class="preview-footer">
          <a-alert
            type="info"
            message="预览提示"
            description="预览为静态页面效果，部署后可体验完整功能"
            show-icon
            class="preview-alert"
          />
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppInfo
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import request from '@/request'
import {
  listAppChatHistoryByPage
} from '@/api/chatHistoryController'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppInfo from '@/components/AppInfo.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

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

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

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

      // 自动发送初始提示词（除非是查看模式或已经进行过初始对话）
      if (appInfo.value.initPrompt && isOwner.value && messages.value.length === 0) {
        hasInitialConversation.value = true
        await sendInitialMessage(appInfo.value.initPrompt)
      } else if (messages.value.length >= 2) {
        // 如果有至少2条对话记录，更新预览
        updatePreview()
      }
    } else {
      message.error('获取应用信息失败')
      await router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    await router.push('/')
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
      const historyData = res.data.data.records || []
      const newMessages = historyData.map(item => ({
        type: item.messageType === 'USER' ? 'user' : 'ai',
        content: item.message,
        createTime: item.createTime
      }))

      // 处理加载更多
      if (loadMore) {
        // 加载更多时添加到前面
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
    }
  } catch (error) {
    console.error('获取对话历史失败：', error)
    message.error('获取对话历史失败')
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
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
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

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/gen/code/stream?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
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
    eventSource.addEventListener('end', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
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
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

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

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
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

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Source+Sans+Pro:wght@300;400;600&family=Caveat:wght@700&display=swap');

#appChatPage.creative-chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px 30px;
  background: #fcf9f2;
  background-image: radial-gradient(circle at 5% 10%, rgba(255, 230, 204, 0.3) 0%, transparent 25%),
  radial-gradient(circle at 95% 90%, rgba(204, 230, 255, 0.3) 0%, transparent 25%),
  linear-gradient(125deg, transparent 60%, rgba(255, 245, 230, 0.5) 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Source Sans Pro', sans-serif;
}

/* 水彩纹理背景 */
#appChatPage.creative-chat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><path fill="none" stroke="rgba(180,170,255,0.1)" stroke-width="1" d="M20,20 Q40,5 60,20 T100,20 M20,40 Q30,30 40,40 T80,40 M10,70 Q35,55 60,70 T90,70"/></svg>');
  background-size: 300px;
  opacity: 0.3;
  pointer-events: none;
  z-index: 0;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 25px;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 20px;
  box-shadow: 0 5px 20px rgba(155, 140, 125, 0.1);
  z-index: 10;
  position: relative;
  margin-bottom: 25px;
  border: 1px solid rgba(198, 160, 138, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.app-name {
  margin: 0;
  font-family: 'Playfair Display', serif;
  font-size: 26px;
  font-weight: 700;
  color: #5c4a48;
  letter-spacing: -0.5px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.05);
}

.header-right {
  display: flex;
  gap: 15px;
}

.detail-btn {
  border-radius: 50px;
  font-weight: 600;
  padding: 0 20px;
  height: 40px;
  transition: all 0.3s;
}

.detail-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.deploy-btn {
  border-radius: 50px;
  font-weight: 600;
  padding: 0 25px;
  height: 40px;
  background: linear-gradient(to right, #1890ff, #69c0ff);
  border: none;
  transition: all 0.3s;
}

.deploy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

/* 主要内容区域 */
.main-content {
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
  flex-direction: column;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(155, 140, 125, 0.12);
  overflow: hidden;
  border: 1px solid rgba(198, 160, 138, 0.15);
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
  background: linear-gradient(to right, transparent, rgba(198, 160, 138, 0.4), transparent);
}

.section-title {
  font-family: 'Playfair Display', serif;
  font-size: 22px;
  font-weight: 700;
  color: #5c4a48;
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
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
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
  border-radius: 18px;
  line-height: 1.6;
  word-wrap: break-word;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.08);
}

.user-message .creative-bubble {
  background: linear-gradient(135deg, #1890ff, #096dd9);
  color: white;
  border-bottom-right-radius: 5px;
}

.ai-message .creative-bubble {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #1a1a1a;
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
  border-top: 1px solid rgba(200, 180, 170, 0.2);
}

.input-wrapper {
  position: relative;
  padding-bottom: 35px;
}

.creative-textarea {
  padding: 18px;
  font-size: 16px;
  border-radius: 18px;
  border: 1px solid rgba(198, 160, 138, 0.3);
  background: rgba(255, 251, 245, 0.9);
  resize: none;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.05);
}

.creative-textarea:focus {
  border-color: rgba(24, 144, 255, 0.7);
  box-shadow: inset 0 2px 8px rgba(24, 144, 255, 0.1), 0 0 0 2px rgba(24, 144, 255, 0.2);
  background: white;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.send-btn {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #98d0ff, #e9ccff);
  border: none;
  transition: all 0.3s ease;
}

.send-btn:hover {
  transform: scale(1.1) rotate(8deg);
  box-shadow: 0 8px 20px rgb(255, 255, 255);
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
  flex: 1.5;
  display: flex;
  flex-direction: column;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(155, 140, 125, 0.12);
  border: 1px solid rgba(198, 160, 138, 0.15);
}

.preview-header {
  padding: 20px 30px 15px;
}

.preview-action-btn {
  border-radius: 50px;
  font-weight: 600;
  padding: 0 20px;
  height: 36px;
  transition: all 0.3s;
}

.preview-action-btn:hover {
  background: rgba(24, 144, 255, 0.1);
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
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
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
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
  background: rgba(24, 144, 255, 0.05);
  border: 1px solid rgba(24, 144, 255, 0.1);
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
</style>
