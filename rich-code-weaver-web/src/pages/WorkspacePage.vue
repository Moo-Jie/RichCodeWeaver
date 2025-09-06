<template>
  <div class="workspace">
    <!-- Default view: no app selected -->
    <template v-if="!appStore.hasSelectedApp && !appId">
      <div class="workspace-home-layout">
        <div class="workspace-home">
          <div class="home-scroll">
          <!-- Greeting -->
          <div ref="greetingRef" class="greeting">
            <img alt="Logo" class="greeting-logo" src="@/assets/logo.png" />
            <h1 class="greeting-title">RichCodeWeaver - 织码睿奇</h1>
            <p class="greeting-sub">工作大幅提效，成果触手可及</p>
          </div>

          <!-- 热门数字产物 horizontal scrollable cards -->
          <div v-if="appStore.hotApps.length > 0" ref="hotSectionRef" class="section">
            <div class="section-header">
              <span class="section-title">热门数字产物</span>
              <button class="section-more" @click="router.push('/all/apps')">查看更多
                <RightOutlined />
              </button>
            </div>
            <div class="app-scroll-wrap">
              <div class="app-scroll">
                <div
                  v-for="app in appStore.hotApps.slice(0, 10)"
                  :key="app.id"
                  class="app-card"
                  @click="goToApp(app)"
                >
                  <div class="app-card-cover">
                    <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
                    <img v-else alt="默认" src="@/assets/logo.png" style="opacity:0.5" />
                  </div>
                  <span class="app-card-name">{{ app.appName || '未命名数字产物' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Dynamic prompt templates -->
          <div v-if="matchedTemplates.length > 0" ref="templateSectionRef" class="section">
            <div class="section-header">
              <span class="section-title">推荐模板</span>
              <span v-if="loginUserStore.loginUser.userIndustry"
                    class="section-tag">{{ loginUserStore.loginUser.userIndustry }}</span>
            </div>
            <div class="prompt-grid">
              <div
                v-for="tpl in matchedTemplates.slice(0, 8)"
                :key="tpl.id"
                class="prompt-card"
                @click="openTemplateDialog(tpl)"
              >
                <div v-if="getColorSchemePreview(tpl)" class="prompt-color-preview">
                  <div
                    v-for="(color, idx) in getColorSchemePreview(tpl)"
                    :key="idx"
                    :style="{ background: color }"
                    :title="color"
                    class="color-dot"
                  />
                </div>
                <span class="prompt-label">{{ tpl.templateName }}</span>
                <span class="prompt-desc">{{ tpl.description }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Chat Input at bottom -->
        <ChatInput
          ref="chatInputWrapRef"
          v-model="userPrompt"
          :generator-mode="useAgentMode"
          :optimizing="optimizing"
          :sending="creating"
          :show-mode-selector="true"
          :show-optimize-button="true"
          :show-tour-button="true"
          :show-material-button="true"
          :selected-materials="selectedMaterials"
          placeholder="描述您想要创建的数字产物..."
          @optimize="handleOptimizePrompt"
          @send="handleCreate"
          @update:generator-mode="useAgentMode = $event"
          @start-tour="startTour"
          @open-material-selector="openMaterialSelector"
          @remove-material="removeMaterial"
          @clear-materials="clearMaterials"
        />
        <div class="home-meta">
          <div class="home-meta-inner">
            <span class="home-meta-item home-meta-filing">
              <img alt="备案标识" class="home-meta-icon" src="@/assets/ICPFiling.png" />
              <span>鲁ICP备2024125764号-2</span>
            </span>
            <span class="home-meta-divider">|</span>
            <span class="home-meta-item">版权所有 © MOJIE TEAM</span>
            <span class="home-meta-divider">|</span>
            <span class="home-meta-item">作品内容由AI生成，详情请查看</span>
            <router-link class="home-meta-link" to="/other/ai-generated-content">《AI 生成内容服务使用规范与权利义务声明》</router-link>
          </div>
        </div>
        </div>
        <CustomerServicePanel />
      </div>

    </template>

    <!-- App selected view -->
    <div v-else class="workspace-app">
      <!-- Edit mode top bar -->
      <div v-if="isEditMode" class="edit-mode-bar">
        <span class="edit-bar-text">
          <EditOutlined style="margin-right: 6px;" />
          可视化编辑模式已启用 — 点击预览页面中的元素并输入修改需求
        </span>
        <button class="edit-bar-exit" @click="toggleEditMode">退出编辑</button>
      </div>

      <!-- Mode switcher -->
      <ModeSwitch :mode="appStore.currentMode" @update:mode="handleModeSwitch" />

      <!-- Body: content + right panel side by side -->
      <div class="workspace-body">
        <!-- Chat mode: full-width chat page (code response) -->
        <div v-show="appStore.currentMode === 'chat'" class="pane-full">
          <!-- Task Plan Panel (real-time rendering during generation) -->
          <TaskPlanPanel
            v-if="taskPlanData && taskPlanData.tasks"
            :tasks="taskPlanData.tasks"
            :summary="taskPlanData.summary || { total: 0, completed: 0, inProgress: 0, pending: 0 }"
          />
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
            :is-owner="canEditApp"
            :selected-element="selectedElement"
            :sending="isGenerating"
            placeholder="✨  描述越详尽，创作越符合您的预期"
            @send="sendMessage"
            @clear-selection="clearSelection"
          />
        </div>

        <!-- App mode: full-width preview -->
        <div v-show="appStore.currentMode === 'app'" class="pane-full">
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
          :collaborators="collaborators"
          :deploying="deploying"
          :downloading="downloading"
          :refreshing="refreshing"
          :hot-stat="hotStat"
          :is-admin="isAdmin"
          :can-edit-app="canEditApp"
          :is-deployed="isDeployed"
          :is-edit-mode="isEditMode"
          :is-generating="isGenerating"
          :is-owner="isOwner"
          :mode="appStore.currentMode"
          :preview-url="previewUrl"
          :visible="appStore.rightPanelVisible"
          @deploy="deployApp"
          @download="downloadCode"
          @toggle="appStore.toggleRightPanel"
          @do-share="handleShare"
          @open-comment="commentDialogOpen = true"
          @preview-fullscreen="openInNewTab"
          @re-deploy="confirmReDeploy"
          @refresh-app="refreshArtifact"
          @show-detail="showAppDetail"
          @toggle-edit="toggleEditMode"
          @toggle-favorite="handleToggleFavorite"
          @toggle-like="handleToggleLike"
          @visit-site="visitDeployedSite"
          @invite-collab="inviteCollabOpen = true"
        />
      </div>
    </div>

    <!-- Modals -->
    <AppInfo
      v-model:open="appDetailVisible"
      :app="appStore.selectedApp ?? undefined"
      :collaborators="collaborators"
      :show-actions="isOwner || isAdmin"
      @delete="handleDeleteApp"
      @edit="handleEditApp"
    />
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="visitDeployedSite"
    />

    <!-- Identity setup modal -->
    <IdentitySetupModal />

    <!-- Prompt template edit dialog -->
    <PromptTemplateDialog
      v-model:open="templateDialogOpen"
      :template="selectedTemplate"
      @confirm="handleTemplateConfirm"
    />

    <!-- Comment dialog -->
    <CommentDialog
      v-model:open="commentDialogOpen"
      :app-id="appStore.selectedApp?.id ?? null"
      @comment-count-change="onCommentCountChange"
    />

    <!-- Tour 漫游引导 -->
    <a-tour v-model:open="tourOpen" :steps="tourSteps" @close="tourOpen = false"
            @finish="tourOpen = false" />

    <!-- Material Selector Modal -->
    <MaterialSelector
      v-model:open="materialSelectorOpen"
      :selected="selectedMaterials"
      @confirm="handleMaterialConfirm"
    />

    <!-- Invite Collaborator Modal -->
    <InviteCollabModal
      v-model:open="inviteCollabOpen"
      :app-id="appStore.selectedApp?.id ?? null"
    />
    <ForwardAppModal
      v-model:open="forwardAppOpen"
      :app="appStore.selectedApp"
      :copy-loading="forwardCopying"
      :share-link="forwardShareLink"
      :submitting="forwardSubmitting"
      @copy-share="handleCopyShare"
      @confirm="handleForwardConfirm"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, RightOutlined } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/appStore'
import { useChatStore } from '@/stores/chatStore'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  addApp,
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  refreshApp as refreshAppApi,
  getAppVoById,
  optimizePrompt
} from '@/api/appController'
import { listAppChatHistoryByPage } from '@/api/chatHistoryController'
import { CodeGenTypeEnum } from '@/enums/codeGenTypes'
import {
  API_BASE_URL,
  DEPLOY_DOMAIN,
  getStaticPreviewUrl,
  getWebProjectStaticPreviewUrl
} from '@/config/env'
import { type ElementInfo, visualEditorUtil } from '@/utils/visualEditorUtil'
import { listMatchedTemplates } from '@/api/promptTemplateController'
import {
  parseBatchContent,
  StreamChunkParserContext,
  type TaskPlanData
} from '@/utils/streamChunkParser'
import request from '@/request'
import { checkCollaborator, listCollaborators } from '@/api/collaboratorController'
import ChatInput from '@/components/workspace/ChatInput.vue'
import CustomerServicePanel from '@/components/workspace/CustomerServicePanel.vue'
import ChatMessages from '@/components/workspace/ChatMessages.vue'
import ModeSwitch from '@/components/workspace/ModeSwitch.vue'
import AppPreview from '@/components/workspace/AppPreview.vue'
import RightPanel from '@/components/workspace/RightPanel.vue'
import AppInfo from '@/components/AppInfo.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import IdentitySetupModal from '@/components/IdentitySetupModal.vue'
import PromptTemplateDialog from '@/components/PromptTemplateDialog.vue'
import CommentDialog from '@/components/CommentDialog.vue'
import TaskPlanPanel from '@/components/workspace/TaskPlanPanel.vue'
import MaterialSelector from '@/components/workspace/MaterialSelector.vue'
import InviteCollabModal from '@/components/workspace/InviteCollabModal.vue'
import ForwardAppModal from '@/components/workspace/ForwardAppModal.vue'
import { doAppShare, getAppHotStat, toggleAppFavorite, toggleAppLike } from '@/api/socialController'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const chatStore = useChatStore()
const loginUserStore = useLoginUserStore()

// === Create Mode State ===
const userPrompt = ref('')
const useAgentMode = ref(true)
const creating = ref(false)
const optimizing = ref(false)

// === Material Selector State ===
const materialSelectorOpen = ref(false)
/** 协作邀请弹窗可见状态 */
const inviteCollabOpen = ref(false)
const forwardAppOpen = ref(false)
const forwardCopying = ref(false)
const forwardSubmitting = ref(false)
const selectedMaterials = ref<API.MaterialVO[]>([])

const openMaterialSelector = () => {
  materialSelectorOpen.value = true
}

const handleMaterialConfirm = (materials: API.MaterialVO[]) => {
  selectedMaterials.value = materials
}

const removeMaterial = (index: number) => {
  selectedMaterials.value.splice(index, 1)
}

const clearMaterials = () => {
  selectedMaterials.value = []
}

// === Tour State ===
const tourOpen = ref(false)
const greetingRef = ref<HTMLElement | null>(null)
const hotSectionRef = ref<HTMLElement | null>(null)
const templateSectionRef = ref<HTMLElement | null>(null)
const chatInputWrapRef = ref<any>(null)

const tourSteps = computed(() => [
  {
    title: '欢迎使用 RichCodeWeaver',
    description: '识码睭奇是一个 AI 驱动的数字产物生成平台，只需描述您的需求，AI 即可自动生成完整的网页应用。',
    target: () => greetingRef.value
  },
  {
    title: '输入您的想法',
    description: '在输入框中描述您想要创建的数字产物，越详细越好。支持 Agent 智能模式，具备更强的理解能力。',
    target: () => chatInputWrapRef.value?.$el ?? chatInputWrapRef.value
  },
  ...(appStore.hotApps.length > 0 ? [{
    title: '热门数字产物',
    description: '浏览社区中最受欢迎的数字产物，点击卡片即可进入对话体验。',
    target: () => hotSectionRef.value
  }] : []),
  ...(matchedTemplates.value.length > 0 ? [{
    title: '推荐模板',
    description: '根据您的身份和行业智能推荐模板，点击模板可快速填充提示词。',
    target: () => templateSectionRef.value
  }] : []),
  {
    title: '我的产物',
    description: '点击左侧边栏的「我的产物」可查看您创建的所有数字产物，支持编辑、删除、部署等操作。',
    target: null
  }
])

const startTour = () => {
  tourOpen.value = true
}

// === Dynamic Template State ===
const matchedTemplates = ref<API.PromptTemplateVO[]>([])
const templateDialogOpen = ref(false)
const selectedTemplate = ref<API.PromptTemplateVO | null>(null)
const loadingTemplates = ref(false)

const fetchMatchedTemplates = async () => {
  const user = loginUserStore.loginUser
  if (!user?.id) return
  loadingTemplates.value = true
  try {
    const res = await listMatchedTemplates({
      userIdentity: user.userIdentity || undefined,
      userIndustry: user.userIndustry || undefined
    })
    if (res.data.code === 0 && res.data.data) {
      matchedTemplates.value = res.data.data
    }
  } catch (e) {
    console.error('Failed to load templates:', e)
  } finally {
    loadingTemplates.value = false
  }
}

const openTemplateDialog = (tpl: API.PromptTemplateVO) => {
  selectedTemplate.value = tpl
  templateDialogOpen.value = true
}

const handleTemplateConfirm = (prompt: string) => {
  userPrompt.value = prompt
  nextTick(() => {
    chatInputWrapRef.value?.resetHeight?.()
  })
}

const refreshArtifact = () => {
  if (!appId.value) return
  if (refreshing.value) return

  if (refreshDebounceTimer) {
    clearTimeout(refreshDebounceTimer)
  }

  refreshDebounceTimer = setTimeout(() => {
    void doRefreshArtifact()
  }, 450)
}

const doRefreshArtifact = async () => {
  if (!appId.value || refreshing.value) return

  const now = Date.now()
  if (now - lastRefreshTriggerAt.value < 1000) {
    return
  }
  lastRefreshTriggerAt.value = now

  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  refreshing.value = true
  try {
    const res = await refreshAppApi({ appId: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      message.success('产物刷新成功（已重新构建）')
      updatePreview()
    } else {
      message.error('产物刷新失败：' + (res.data.message || ''))
    }
  } catch (error) {
    message.error('产物刷新失败，请重试')
  } finally {
    refreshing.value = false
  }
}

const getColorSchemePreview = (tpl: API.PromptTemplateVO): string[] => {
  if (!tpl.templateFields) return []
  try {
    const fields = JSON.parse(tpl.templateFields)
    const colorSchemeField = fields.find((f: any) => f.key === 'colorScheme' && f.type === 'select')
    if (!colorSchemeField?.options) return []

    // Extract colors from options like "天空蓝(#0288d1+#e1f5fe)"
    const colors: string[] = []
    for (const option of colorSchemeField.options.slice(0, 3)) {
      const match = option.match(/\((.+?)\+(.+?)\)/)
      if (match) {
        colors.push(match[1], match[2])
      }
    }
    return colors.slice(0, 6) // Max 6 colors
  } catch {
    return []
  }
}

const quickSendPrompts = [
  {
    title: '企业官网首页',
    description: '适合快速生成品牌介绍、产品亮点与联系方式页面。',
    prompt: '帮我生成一个简洁高级的企业官网首页，包含品牌介绍、核心服务、成功案例、客户评价和联系我们模块。'
  },
  {
    title: '活动报名落地页',
    description: '适合发布活动信息、亮点日程和报名入口。',
    prompt: '帮我生成一个活动报名落地页，包含活动亮点、议程安排、嘉宾介绍、报名表单和常见问题模块。'
  },
  {
    title: '产品展示页',
    description: '适合展示 SaaS、工具产品或解决方案能力。',
    prompt: '帮我生成一个科技感产品展示页，突出产品功能、使用流程、价格方案、用户评价和行动按钮。'
  },
  {
    title: '个人作品集',
    description: '适合设计师、开发者或自由职业者展示案例。',
    prompt: '帮我生成一个个人作品集网站，包含个人介绍、代表作品、技能清单、服务内容和联系入口。'
  }
]

const handleQuickSend = async (prompt: string) => {
  if (creating.value || optimizing.value) {
    return
  }
  userPrompt.value = prompt
  await nextTick()
  chatInputWrapRef.value?.resetHeight?.()
  await handleCreate()
}

// === App Chat State ===
const appId = ref<string>(route.params.id as string || '')

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  userId?: number
  senderName?: string
  senderAvatar?: string
  senderRoleLabel?: string
  isSelf?: boolean
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
const refreshing = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')
const lastRefreshTriggerAt = ref(0)
let refreshDebounceTimer: ReturnType<typeof setTimeout> | null = null

// === Visual Editor State ===
const visualEditor = ref<visualEditorUtil | null>(null)
const isEditMode = ref(false)
const selectedElement = ref<ElementInfo | null>(null)

const ELEMENT_TYPE_MAP: Record<string, string> = {
  H1: '大标题', H2: '二级标题', H3: '三级标题', H4: '四级标题', H5: '五级标题', H6: '六级标题',
  P: '段落文本', A: '链接', BUTTON: '按钮', IMG: '图片', VIDEO: '视频', AUDIO: '音频',
  NAV: '导航栏', HEADER: '页头区域', FOOTER: '页脚区域', MAIN: '主要内容', ASIDE: '侧边栏',
  SECTION: '内容区块', ARTICLE: '文章区块', DIV: '区块容器', SPAN: '文本片段',
  UL: '无序列表', OL: '有序列表', LI: '列表项', TABLE: '表格', FORM: '表单', INPUT: '输入框'
}
const getElementTypeLabel = (tagName: string): string => ELEMENT_TYPE_MAP[tagName?.toUpperCase()] || '页面元素'

// === UI Refs ===
const chatMessagesRef = ref<InstanceType<typeof ChatMessages>>()
const appPreviewRef = ref<InstanceType<typeof AppPreview>>()
const appDetailVisible = ref(false)

// === Social State ===
const hotStat = ref<API.AppHotStatVO | null>(null)
const commentDialogOpen = ref(false)
const collaborators = ref<API.AppCollaboratorVO[]>([])

const getForwardTypeLabel = (codeGenType?: string) => {
  if (codeGenType === 'single_html') return '单文件结构'
  if (codeGenType === 'multi_file') return '多文件结构'
  if (codeGenType === 'vue_project') return 'Vue 项目工程'
  return '数字产物'
}

const forwardShareLink = computed(() => {
  if (typeof window === 'undefined') {
    return ''
  }
  const currentApp = appStore.selectedApp
  if (!currentApp?.id) {
    return window.location.href
  }
  const resolved = router.resolve(`/app/chat/${currentApp.id}`)
  return new URL(resolved.href, window.location.origin).toString()
})

// === Task Plan State ===
const taskPlanData = ref<TaskPlanData | null>(null)

// === Timer ===
const generatingTime = ref(0)
const timer = ref<ReturnType<typeof setInterval> | null>(null)

// === SSE Reconnect ===
const lastEventId = ref<string | null>(null)
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 5
const isAcceptedCollaborator = ref(false)

// === Computed ===
const isOwner = computed(() => {
  return appStore.selectedApp?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const canEditApp = computed(() => {
  return isOwner.value || isAdmin.value || isAcceptedCollaborator.value
})

const getSenderMeta = (userId?: number) => {
  const currentUserId = loginUserStore.loginUser.id
  const owner = appStore.selectedApp?.user
  const collaborator = collaborators.value.find((item) => item.userId === userId)

  if (userId && owner?.id === userId) {
    return {
      senderName: owner.userName || '主创',
      senderAvatar: owner.userAvatar,
      senderRoleLabel: '主创',
      isSelf: userId === currentUserId
    }
  }

  if (collaborator) {
    return {
      senderName: collaborator.userName || '协作者',
      senderAvatar: collaborator.userAvatar,
      senderRoleLabel: collaborator.role === 'viewer' ? '协作者' : '编辑者',
      isSelf: collaborator.userId === currentUserId
    }
  }

  return {
    senderName: userId === currentUserId ? loginUserStore.loginUser.userName || '我' : '协作者',
    senderAvatar: userId === currentUserId ? loginUserStore.loginUser.userAvatar : undefined,
    senderRoleLabel: userId === currentUserId ? (isOwner.value ? '主创' : '编辑者') : '协作者',
    isSelf: userId === currentUserId
  }
}

const createUserMessage = (content: string, overrides: Partial<Message> = {}): Message => {
  const senderMeta = getSenderMeta(loginUserStore.loginUser.id)
  return {
    type: 'user',
    content,
    userId: loginUserStore.loginUser.id,
    senderName: senderMeta.senderName,
    senderAvatar: senderMeta.senderAvatar,
    senderRoleLabel: senderMeta.senderRoleLabel,
    isSelf: true,
    ...overrides
  }
}

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

// === Navigate to app ===
const goToApp = (app: API.AppVO) => {
  if (app.id) {
    appStore.selectApp(app)
    router.push(`/app/chat/${app.id}`)
  }
}

// === Create App ===
const handleCreate = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入数字产物描述')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  // 确认提示词
  Modal.confirm({
    title: '确认创建',
    content: '请确认您的提示词是否已经检查完毕？',
    okText: '确认创建',
    cancelText: '再检查一下',
    onOk: async () => {
      creating.value = true
      try {
        const res = await addApp({
          initPrompt: userPrompt.value.trim(),
          generatorType: 'AI_STRATEGY',
          genMode: useAgentMode.value ? 'workflow' : 'agent'
        })
        if (res.data.code === 0 && res.data.data) {
          const newAppId = res.data.data
          message.success('数字产物创建成功')
          userPrompt.value = ''
          // 将素材ID传递到路由query中
          const materialIds = selectedMaterials.value.map(m => m.id).filter(Boolean).join(',')
          const query: Record<string, string> = { useAgent: String(useAgentMode.value) }
          if (materialIds) query.materialIds = materialIds
          selectedMaterials.value = [] // 清空已选素材
          await router.push({ path: `/app/chat/${newAppId}`, query })
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
  })
}

// === Optimize Prompt ===
const handleOptimizePrompt = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请先输入提示词')
    return
  }

  optimizing.value = true
  try {
    const res = await optimizePrompt(userPrompt.value.trim())
    if (res.data.code === 0 && res.data.data) {
      userPrompt.value = res.data.data
      nextTick(() => {
        chatInputWrapRef.value?.resetHeight?.()
      })
      message.success('提示词优化成功')
    } else {
      message.error('优化失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (e: any) {
    message.error('优化失败：' + (e.message || '请稍后重试'))
  } finally {
    optimizing.value = false
  }
}

// === Fetch App Info ===
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) return

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appStore.refreshSelectedApp(res.data.data)
      appStore.rightPanelVisible = true
      await fetchCollaborators(id as unknown as number)
      await refreshCollaboratorPermission(id as unknown as number)

      await fetchChatHistory()
      updatePreview()
      fetchHotStat()

      // 智能判断默认模式：检测是否有正在传输的SSE流
      const hasActiveSSE = !!getGeneratingInfo()
      if (hasActiveSSE) {
        // 有正在传输的SSE流，进入对话模式
        appStore.setMode('chat')
      } else if (appStore.selectedApp?.initPrompt && messages.value.length === 0) {
        // 新创建的产物（有初始提示词但无消息历史），即将自动发送初始消息，进入对话模式
        appStore.setMode('chat')
      } else {
        // 没有正在传输的SSE流，展示数字产物模式
        appStore.setMode('app')
      }

      // Auto-send initial prompt if new app with init prompt
      // 但不在部署或其他操作后自动发送，只在首次加载且无消息时发送
      if (appStore.selectedApp?.initPrompt && isOwner.value && messages.value.length === 0 && !isGenerating.value) {
        await sendInitialMessage(appStore.selectedApp.initPrompt)
      } else if (messages.value.length >= 2) {
        updatePreview()
      }
    } else {
      message.error('获取数字产物信息失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (error) {
    console.error('获取数字产物信息失败：', error)
    collaborators.value = []
  }
}

const fetchCollaborators = async (targetAppId: number) => {
  try {
    const res = await listCollaborators({ appId: targetAppId })
    collaborators.value = res.data.code === 0 ? (res.data.data || []) : []
  } catch (error) {
    console.error('获取协作者列表失败：', error)
    collaborators.value = []
  }
}

// === Chat History ===
const fetchChatHistory = async (loadMore = false) => {
  if (!appId.value || loadingHistory.value) return

  // Reset cursor for fresh load to avoid stale pagination
  if (!loadMore) {
    lastCreateTime.value = null
  }

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
      const newMessages: Message[] = historyData.map((item: API.ChatHistory) => {
        if (item.messageType === 'user') {
          const senderMeta = getSenderMeta(item.userId)
          return {
            type: 'user',
            content: item.message || '',
            createTime: item.createTime,
            userId: item.userId,
            senderName: senderMeta.senderName,
            senderAvatar: senderMeta.senderAvatar,
            senderRoleLabel: senderMeta.senderRoleLabel,
            isSelf: senderMeta.isSelf
          }
        }

        return {
          type: 'ai',
          content: parseBatchContent(item.message || ''),
          createTime: item.createTime
        }
      })

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
      chatMessagesRef.value?.scrollToBottom(true)
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
  if (!canEditApp.value) return
  const generatingInfo = getGeneratingInfo()
  if (!generatingInfo) return

  const lastMessage = messages.value[messages.value.length - 1]

  // 检查是否已部署（deployKey 存在说明生成肯定已完成）
  if (appStore.selectedApp?.deployKey) {
    console.log('检测到已部署数字产物，清除过期标记')
    markGeneratingEnd()
    return
  }

  // 检查最后一条 AI 消息是否包含工作流完成/错误标记（系统分步执行模式的可靠完成判断）
  if (lastMessage?.type === 'ai' && lastMessage.content && !lastMessage.loading) {
    const content = lastMessage.content
    if (content.includes('WORKFLOW_COMPLETE') || content.includes('WORKFLOW_ERROR') || content.includes('代码生成任务完成')) {
      console.log('检测到已完成的生成任务（含完成标记），清除过期标记')
      markGeneratingEnd()
      return
    }
  }

  console.log('检测到未完成的生成任务（来自 localStorage），准备恢复...', generatingInfo.message.substring(0, 50))
  message.info('检测到未完成的任务，正在恢复生成...')
  let aiMessageIndex: number

  if (lastMessage?.type === 'ai') {
    aiMessageIndex = messages.value.length - 1
    messages.value[aiMessageIndex].loading = true
  } else {
    aiMessageIndex = messages.value.length
    messages.value.push({ type: 'ai', content: '', loading: true })
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
  if (!ensureEditPermission()) return
  messages.value.push(createUserMessage(prompt))
  const aiMessageIndex = messages.value.length
  messages.value.push({ type: 'ai', content: '', loading: true })
  await nextTick()
  chatMessagesRef.value?.scrollToBottom(true)
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) return
  if (!ensureEditPermission()) return

  const msg = userInput.value.trim()
  userInput.value = ''

  let displayMsg = msg
  let prompt = msg
  if (selectedElement.value?.selector) {
    const el = selectedElement.value
    const typeLabel = getElementTypeLabel(el.tagName)
    const textPreview = el.textContent ? el.textContent.substring(0, 40) : ''
    displayMsg = `修改选中的「${typeLabel}」${textPreview ? `（${textPreview}...）` : ''}：${msg}`
    prompt = `[可视化编辑] 我在页面中选中了一个「${typeLabel}」元素${textPreview ? `，内容为“${textPreview}”` : ''}。\n元素定位选择器: \`${el.selector}\`\n我的修改需求：${msg}`
  }

  messages.value.push(createUserMessage(displayMsg))
  const aiMessageIndex = messages.value.length
  messages.value.push({ type: 'ai', content: '', loading: true })
  await nextTick()
  chatMessagesRef.value?.scrollToBottom(true)

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
  if (!canEditApp.value) {
    if (messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].content = '你还没有该产物的编辑权限，请先接受协作邀请后再操作。'
      messages.value[aiMessageIndex].loading = false
    }
    isGenerating.value = false
    return
  }
  generatingTime.value = 0
  timer.value = setInterval(() => generatingTime.value++, 1000)
  reconnectAttempts.value = 0
  lastEventId.value = null

  let eventSource: EventSource | null = null
  let streamCompleted = false
  let fullContent = ''
  const existingContentLength = isReconnect ? (messages.value[aiMessageIndex]?.content?.length || 0) : 0
  // 创建流式 JSON 消息块解析上下文，用于解析 VUE_PROJECT 模式下的 ai_response / tool_request / tool_executed 类型
  const chunkParser = new StreamChunkParserContext()
  // 设置任务计划更新回调，用于实时渲染任务列表
  chunkParser.setTaskPlanCallback((data: TaskPlanData) => {
    taskPlanData.value = data
  })

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
    // 清理任务计划状态（生成结束后不再显示任务面板）
    taskPlanData.value = null
    if (fullContent) {
      messages.value[aiMessageIndex].content = fullContent
    }
    messages.value[aiMessageIndex].loading = false
    // 延迟后刷新数字产物信息 + 从数据库重新加载对话历史，确保 SSE 流内容与 DB 持久化内容平滑过渡
    setTimeout(async () => {
      await refreshAppInfoOnly()
      appPreviewRef.value?.refresh()
      // 重新加载对话历史，用 DB 持久化数据替换内存中的流式数据，保证数据一致性
      await fetchChatHistory()
    }, 3000)
  }

  // Agent mode from route query
  const useAgent = route.query.useAgent !== 'false'
  // 获取当前选中的素材ID列表（优先使用当前UI选择，回退到路由query）
  const currentMaterialIds = selectedMaterials.value.map(m => m.id).filter(Boolean).join(',')
  const materialIdsFromQuery = route.query.materialIds as string || ''
  const materialIdsToSend = currentMaterialIds || materialIdsFromQuery
  let reconnectMode = isReconnect

  const connectSSE = () => {
    try {
      const baseURL = request.defaults.baseURL || API_BASE_URL
      const params = new URLSearchParams({
        appId: appId.value || '',
        message: userMessage,
        isWorkflow: String(useAgent),
        reconnect: String(reconnectMode)
      })
      if (lastEventId.value) {
        params.set('lastEventId', lastEventId.value)
      }
      // 添加素材ID参数（首次发送或有选中素材时使用，重连时不需要）
      if (materialIdsToSend && !reconnectMode) {
        params.set('materialIds', materialIdsToSend)
      }
      const url = `${baseURL}/generator/app/gen/code/stream?${params}`
      eventSource = new EventSource(url, { withCredentials: true })

      eventSource.onmessage = function(event) {
        if (streamCompleted) return
        if (event.lastEventId) lastEventId.value = event.lastEventId
        try {
          const parsed = JSON.parse(event.data)
          const rawContent = parsed.b
          if (rawContent !== undefined && rawContent !== null) {
            // 解析 JSON 消息块（ai_response / tool_request / tool_executed），提取有效展示内容
            const content = chunkParser.parseChunk(rawContent)
            if (content) {
              fullContent += content
              if (fullContent.length >= existingContentLength) {
                messages.value[aiMessageIndex].content = fullContent
                messages.value[aiMessageIndex].loading = false
              }
              chatMessagesRef.value?.scrollToBottom()
            }
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

// === Lightweight refresh: only updates app info, no chat reload or auto-send ===
const refreshAppInfoOnly = async () => {
  const id = route.params.id as string
  if (!id) return
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appStore.refreshSelectedApp(res.data.data)
      updatePreview()
    }
  } catch (error) {
    console.error('刷新数字产物信息失败：', error)
  }
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
  if (!ensureEditPermission()) return
  if (visualEditor.value) {
    isEditMode.value = visualEditor.value.toggleEditMode()
    if (!isEditMode.value) clearSelection()
  }
}

const refreshCollaboratorPermission = async (targetAppId: number) => {
  if (isOwner.value || isAdmin.value) {
    isAcceptedCollaborator.value = false
    return
  }
  try {
    const res = await checkCollaborator({ appId: targetAppId })
    isAcceptedCollaborator.value = !!(res.data.code === 0 && res.data.data)
  } catch (error) {
    isAcceptedCollaborator.value = false
  }
}

const ensureEditPermission = () => {
  if (canEditApp.value) {
    return true
  }
  message.warning('当前仅产物所有者或已接受协作者可以编辑，请先接受协作邀请。')
  return false
}

const handleModeSwitch = (newMode: 'chat' | 'app') => {
  // Auto-exit visual edit mode when switching to chat mode
  if (newMode === 'chat' && isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }
  appStore.setMode(newMode)
}

const clearSelection = () => {
  selectedElement.value = null
  visualEditor.value?.clearSelection()
}

// === Deploy ===
const deployApp = async () => {
  if (!appId.value) return
  if (isDeployed.value) return

  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  deploying.value = true
  try {
    const res = await deployAppApi({ appId: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      await refreshAppInfoOnly()
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
  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  Modal.confirm({
    title: '重复部署警告',
    content: '请勿频繁部署，若违反则系统自动封号处理！确定要继续部署吗？',
    okText: '确定部署',
    cancelText: '取消',
    onOk: async () => {
      deploying.value = true
      try {
        const res = await deployAppApi({ appId: appId.value as unknown as number })
        if (res.data.code === 0 && res.data.data) {
          deployUrl.value = res.data.data
          deployModalVisible.value = true
          message.success('重新部署成功')
          await refreshAppInfoOnly()
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

  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  downloading.value = true
  try {
    const res = await request.get(`/generator/download/code/zip/${appId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/zip' })
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
  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  if (previewUrl.value) window.open(previewUrl.value, '_blank')
}

const visitDeployedSite = () => {
  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

  if (deployedSiteUrl.value) window.open(deployedSiteUrl.value, '_blank')
  else if (deployUrl.value) window.open(deployUrl.value, '_blank')
}

const showAppDetail = () => {
  // Auto-exit visual edit mode
  if (isEditMode.value) {
    isEditMode.value = false
    visualEditor.value?.disableEditMode()
    clearSelection()
  }

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
    const res = await deleteAppApi({ id: appStore.selectedApp.id })
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

// === Social Handlers ===
const fetchHotStat = async () => {
  if (!appId.value) return
  try {
    const res = await getAppHotStat({ appId: appId.value as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      hotStat.value = res.data.data
    }
  } catch (e) {
    console.error('获取热点数据失败:', e)
  }
}

const handleToggleLike = async () => {
  if (!appId.value) return
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  try {
    const res = await toggleAppLike({ appId: appId.value as unknown as number })
    if (res.data.code === 0) {
      await fetchHotStat()
    }
  } catch (e) {
    message.error('操作失败')
  }
}

const handleToggleFavorite = async () => {
  if (!appId.value) return
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  try {
    const res = await toggleAppFavorite({ appId: appId.value as unknown as number })
    if (res.data.code === 0) {
      const favorited = res.data.data
      message.success(favorited ? '已收藏' : '已取消收藏')
      await fetchHotStat()
    }
  } catch (e) {
    message.error('操作失败')
  }
}

const handleShare = async () => {
  if (!appId.value) return
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }

  if (!appStore.selectedApp?.id) {
    message.warning('当前产物不存在')
    return
  }

  forwardAppOpen.value = true
}

const bumpLocalShareCount = () => {
  if (!hotStat.value) {
    return
  }
  hotStat.value = {
    ...hotStat.value,
    shareCount: (hotStat.value.shareCount || 0) + 1
  }
}

const copyTextToClipboard = async (text: string) => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!copied) {
    throw new Error('copy failed')
  }
}

const handleForwardConfirm = async (friendIds: number[]) => {
  const currentApp = appStore.selectedApp
  if (!currentApp?.id) {
    message.warning('当前产物不存在')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }

  forwardSubmitting.value = true
  try {
    const content = JSON.stringify({
      appId: currentApp.id,
      appName: currentApp.appName,
      appCover: currentApp.cover,
      codeGenType: currentApp.codeGenType
    })

    let successCount = 0
    for (const friendId of friendIds) {
      try {
        await chatStore.sendWsMessage(friendId, content, 'app_forward')
        successCount++
      } catch (error) {
        console.error('转发给好友失败', friendId, error)
      }
    }

    if (successCount <= 0) {
      message.error('转发失败')
      return
    }

    try {
      const res = await doAppShare({ appId: currentApp.id })
      if (res.data.code === 0) {
        await fetchHotStat()
      }
    } catch (error) {
      console.error('更新转发统计失败', error)
    }

    forwardAppOpen.value = false
    if (successCount === friendIds.length) {
      message.success(`已转发给 ${successCount} 位好友`)
    } else {
      message.warning(`已转发给 ${successCount} 位好友，部分好友发送失败`)
    }
  } finally {
    forwardSubmitting.value = false
  }
}

const onCommentCountChange = (_delta: number) => {
  fetchHotStat()
}

const handleCopyShare = async () => {
  const currentApp = appStore.selectedApp
  if (!currentApp?.id) {
    message.warning('当前产物不存在')
    return
  }
  forwardCopying.value = true
  try {
    const shareLink = forwardShareLink.value
    if (!shareLink) {
      message.warning('当前分享连接暂不可用')
      return
    }
    const typeLabel = getForwardTypeLabel(currentApp.codeGenType)
    const authorName = currentApp.user?.userName || '平台创作者'
    const deployKey = currentApp.deployKey
    const deployLink = deployKey ? `${DEPLOY_DOMAIN}/${deployKey}` : ''
    const lines = [
      `【${currentApp.appName || '未命名数字产物'}】`,
      `产物类型：${typeLabel}`,
      `创作者：${authorName}`,
      '',
      `在线查看：${shareLink}`,
      ...(deployLink ? [`部署地址：${deployLink}`] : []),
      '',
      '—— 来自 RichCodeWeaver · 织码睿奇',
      'AI 驱动的数字产物生成平台，工作大幅提效，成果触手可及',
      `平台首页：${window.location.origin}`
    ]
    const shareContent = lines.join('\n')
    await copyTextToClipboard(shareContent)
    bumpLocalShareCount()
    try {
      const res = await doAppShare({ appId: currentApp.id })
      if (res.data.code === 0) {
        await fetchHotStat()
      }
    } catch (error) {
      console.error('更新转发统计失败', error)
    }
    forwardAppOpen.value = false
    message.success('分享连接已复制，可直接发送到微信等聊天工具')
  } catch (error) {
    console.error('复制分享文案失败', error)
    message.error('复制失败，请稍后重试')
  } finally {
    forwardCopying.value = false
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
    appId.value = ''
    messages.value = []
    previewUrl.value = ''
    previewReady.value = false
    appStore.selectedApp = null
  }
}, { immediate: false })

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
  hotStat.value = null
  commentDialogOpen.value = false
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
      // Auto-switch to chat mode when element is selected
      if (appStore.currentMode === 'app') {
        appStore.setMode('chat')
      }
      // Prompt user to input modification requirements
      nextTick(() => {
        message.info('已选中元素，请在下方输入框中描述您想要的修改需求', 3)
      })
    }
  })
  window.addEventListener('message', handleIframeMessage)

  // If route has app id, load it
  if (route.params.id) {
    fetchAppInfo()
  }

  // Load user's apps and hot apps
  if (loginUserStore.loginUser.id) {
    appStore.loadMyApps()
    fetchMatchedTemplates()
  }
  appStore.loadHotApps()
})

// Re-fetch templates when user identity/industry changes
watch(
  () => [loginUserStore.loginUser.userIdentity, loginUserStore.loginUser.userIndustry],
  () => {
    if (loginUserStore.loginUser.id) {
      fetchMatchedTemplates()
    }
  }
)

onUnmounted(() => {
  window.removeEventListener('message', handleIframeMessage)
  visualEditor.value?.disableEditMode()
  if (refreshDebounceTimer) {
    clearTimeout(refreshDebounceTimer)
    refreshDebounceTimer = null
  }
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
  flex-direction: row;
  height: 100%;
  overflow: hidden;
  background: #fff;
  position: relative;
}

/* ====== Home view (no app) ====== */
.workspace-home-layout {
  flex: 1;
  display: flex;
  gap: 16px;
  min-width: 0;
  overflow: hidden;
  padding: 16px 16px 16px 0;
}

.workspace-home {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.home-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  scrollbar-width: none;
}

.home-scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.greeting {
  text-align: center;
}

.greeting-logo {
  width: 100px;
  height: 100px;
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

/* Section */
.section {
  width: 100%;
  max-width: 760px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.section-tag {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 2px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.section-more {
  display: flex;
  align-items: center;
  gap: 3px;
  border: none;
  background: none;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 0;
  transition: color 0.15s;
}

.section-more:hover {
  color: #666;
}

/* Horizontal scrollable app cards */
.app-scroll-wrap {
  overflow: hidden;
  margin: 0 -4px;
}

.app-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 4px 12px;
  scroll-snap-type: x mandatory;
}

.app-scroll::-webkit-scrollbar {
  height: 3px;
}

.app-scroll::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 3px;
}

.app-card {
  flex-shrink: 0;
  width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  scroll-snap-align: start;
}

.app-card:hover {
  background: #f5f5f5;
  border-color: #e5e5e5;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.app-card-cover {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #f0f0f0;
}

.app-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card-name {
  font-size: 12px;
  color: #333;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  line-height: 1.3;
}

/* Prompt grid */
.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
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

.prompt-color-preview {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.color-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.color-dot:hover {
  transform: scale(1.2);
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

.quick-send-section {
  margin-top: -4px;
}

.quick-send-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.quick-send-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px;
  border: 1px solid #ececec;
  border-radius: 14px;
  background: #fcfcfc;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.quick-send-card:hover {
  background: #fff;
  border-color: #dcdcdc;
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.quick-send-kicker {
  font-size: 11px;
  line-height: 1.2;
  color: #8c8c8c;
}

.quick-send-title {
  font-size: 15px;
  line-height: 1.35;
  font-weight: 600;
  color: #1a1a1a;
}

.quick-send-desc {
  font-size: 13px;
  line-height: 1.55;
  color: #666;
}

.quick-send-action {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 500;
  color: #1a1a1a;
}

.home-meta {
  display: flex;
  justify-content: center;
  padding: 12px 0 6px;
}

.home-meta-inner {
  width: 100%;
  max-width: 1120px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  padding: 10px 8px;
}

.home-meta-item,
.home-meta-link,
.home-meta-divider {
  font-size: 12px;
  line-height: 1.6;
  white-space: nowrap;
}

.home-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #8c8c8c;
}

.home-meta-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  flex-shrink: 0;
}

.home-meta-divider {
  color: #d0d0d0;
}

.home-meta-link {
  color: #666;
  text-decoration: none;
  transition: color 0.15s ease;
}

.home-meta-link:hover {
  color: #1a1a1a;
}

@media (max-width: 900px) {
  .quick-send-grid {
    grid-template-columns: 1fr;
  }

  .home-meta {
    padding-top: 10px;
  }

  .home-meta-inner {
    max-width: 100%;
    padding: 12px 0;
  }

  .home-meta-link,
  .home-meta-item {
    white-space: normal;
    text-align: center;
  }
}

/* ====== App selected view ====== */
.workspace-app {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.workspace-body {
  flex: 1;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  min-height: 0;
}

.pane-full {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  min-width: 0;
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
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
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
