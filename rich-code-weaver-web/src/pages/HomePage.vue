<script lang="ts" setup>
import {onMounted, reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import type {TourProps} from 'ant-design-vue'
import {message, Tour} from 'ant-design-vue'
import {useLoginUserStore} from '@/stores/loginUser'
import {addApp, listMyAppVoByPage, listStarAppVoByPage} from '@/api/appController'
import {getDeployUrl} from '@/config/env'
import AppCard from '@/components/AppCard.vue'
// 导入图标
import {
  AppstoreOutlined,
  CheckCircleFilled,
  CodeOutlined,
  FileOutlined,
  FolderOutlined,
  PlayCircleOutlined,
  QuestionCircleOutlined,
  RobotOutlined,
  RocketOutlined
} from '@ant-design/icons-vue'
import {promptOptions} from '@/constants/prompts.ts'

// 当前激活的选项卡状态
const activeTab = ref('featured')

// 导入 Tour 相关状态
const tourOpen = ref(false)
const tourCurrent = ref(0)
const tourSteps = ref<TourProps['steps']>([
  {
    title: '创意输入',
    description: '在这里输入您的应用创意描述，系统会根据描述生成完整应用',
    target: () => document.querySelector('.prompt-input') as HTMLElement,
    placement: 'bottom'
  },
  {
    title: '热门提示词',
    description: '点击这里可以选择热门应用的提示词模板，快速开始创作',
    target: () => document.querySelector('.rich-select-button') as HTMLElement,
    placement: 'bottom'
  },
  {
    title: '创建作品',
    description: '输入创意后点击这里创建您的应用作品',
    target: () => document.querySelector('.create-button') as HTMLElement,
    placement: 'bottom'
  },
  {
    title: '我的创作空间',
    description: '这里展示您已创建的所有应用作品，可以随时查看和编辑',
    target: () => document.querySelector('.my-workspace') as HTMLElement,
    placement: 'top'
  }
])

// 开始引导
const startTour = () => {
  tourOpen.value = true
  tourCurrent.value = 0
}

// 关闭引导
const handleTourClose = () => {
  tourOpen.value = false
}

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 控制下拉框可见性
const showPromptDropdown = ref(true)
// 处理选择的方法
const handlePromptSelect = (value: string) => {
  setPrompt(value)
  showPromptDropdown.value = false
}

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 生成器类型
const generatorType = ref<API.AppAddRequest['generatorType']>('AI_STRATEGY')
const generatorOptions = ref([
  {label: 'AI 自主规划模式', value: 'AI_STRATEGY'},
  {label: '工程项目模式', value: 'VUE_PROJECT'},
  {label: '多文件模式', value: 'MULTI_FILE'},
  {label: '单文件模式', value: 'HTML'}
])

// 模式选择
const useAgentMode = ref(true)
const agentModeOptions = ref([
  {
    label: '节点智能生成模式',
    value: true,
    desc: '基于一套工作流的节点生成模式会让 AI 有更强的分析和决策能，构建出的应用更加完善、稳定'
  },
  {
    label: '快速生成模式',
    value: false,
    desc: '基于训练后的 AI 模型直接构建应用，速度更快但可能不够完善'
  }
])

// 监听 Agent 模式变化
watch(useAgentMode, (newVal) => {
  if (newVal) {
    generatorType.value = 'VUE_PROJECT'
  }
}, { immediate: true })

// 处理生成器类型选择
const handleGeneratorTypeSelect = (value: any) => {
  if (useAgentMode.value && value !== 'VUE_PROJECT') {
    message.warning('节点智能生成模式下，需使用工程项目模式')
    return
  }
  generatorType.value = value
}

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0
})

// 星选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 创建应用
const createApp = async () => {
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
      generatorType: generatorType.value
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      // 传递Agent模式参数
      await router.push({
        path: `/app/chat/${appId}`,
        query: {useAgent: useAgentMode.value.toString()}
      })
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试:' + res.data.message)
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc'
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载星选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listStarAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc'
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载星选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div id="creativeStudio">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title">RichCodeWeaver - 织码睿奇</h1>
        <p class="hero-description">< 只需一句话，让创意触手可及 ></p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <div class="generator-content">
          <a-textarea
            v-model:value="userPrompt"
            :maxlength="1000"
            :rows="4"
            class="prompt-input"
            placeholder="描述您想要创建的创意作品..."
          />

          <!-- 按钮组 -->
          <div class="action-buttons">
            <!-- 快速入门按钮 -->
            <a-button
              class="action-button tour-button"
              size="large"
              type="primary"
              @click="startTour"
            >
              <template #icon>
                <PlayCircleOutlined/>
              </template>
              快速入门
            </a-button>

            <!-- 自动填入提示词按钮 -->
            <a-button
              class="action-button rich-select-button"
              size="large"
              type="primary"
              @click="showPromptDropdown = !showPromptDropdown"
            >
              <template #icon>
                <AppstoreOutlined/>
              </template>
              热门提示词
            </a-button>


            <!-- 创建作品按钮 -->
            <a-button
              :loading="creating"
              class="action-button create-button"
              size="large"
              target="_blank"
              type="primary"
              @click="createApp"
            >
              <template #icon>
                <RocketOutlined/>
              </template>
              开始生成
            </a-button>
          </div>
        </div>

        <!-- 热门提示词滑动模块 -->
        <div v-if="showPromptDropdown" class="prompt-slider-container">
          <div class="prompt-slider-header">
            <h3 class="slider-title">🔥 热门提示词</h3>
            <a-button
              class="close-button"
              size="small"
              type="text"
              @click="showPromptDropdown = false"
            >
              ✕
            </a-button>
          </div>
          <div class="prompt-slider">
            <div
              v-for="option in promptOptions"
              :key="option.value"
              class="prompt-card"
              @click="handlePromptSelect(option.value)"
            >
              <div :style="{backgroundColor: option.color}" class="prompt-card-icon">
                <component :is="option.icon"/>
              </div>
              <div class="prompt-card-content">
                <div class="prompt-card-title">{{ option.label }}</div>
                <div class="prompt-card-desc">{{ option.desc }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 代码生成模式选择器  -->
        <div class="generator-selector">
          <div class="selector-title">
            <span class="title-icon">⚙️</span>
            <span>选择应用架构</span>
            <a-tooltip placement="top">
              <template #title>
                <div style="max-width: 300px">
                  选择代码生成模式，不同模式适合不同的应用场景，生成速度、部署成本也不同。
                </div>
              </template>
              <question-circle-outlined class="help-icon"/>
            </a-tooltip>
          </div>

          <div class="mode-cards">
            <div
              v-for="option in generatorOptions"
              :key="option.value"
              :class="{ active: generatorType === option.value, disabled: useAgentMode && option.value !== 'VUE_PROJECT' }"
              class="mode-card"
              @click="handleGeneratorTypeSelect(option.value)"
            >
              <div class="card-icon">
                <robot-outlined v-if="option.value === 'AI_STRATEGY'"/>
                <code-outlined v-else-if="option.value === 'VUE_PROJECT'"/>
                <folder-outlined v-else-if="option.value === 'MULTI_FILE'"/>
                <file-outlined v-else/>
              </div>
              <div class="card-content">
                <div class="card-title">{{ option.label }}</div>
                <div class="card-desc">
                  <span
                    v-if="option.value === 'AI_STRATEGY'">AI 智能分析您的需求并生成完整应用
                  </span>
                  <span
                    v-else-if="option.value === 'VUE_PROJECT'">生成完整的 VUE 工程项目，适合复杂的应用，但生成时间更长
                  </span>
                  <span
                    v-else-if="option.value === 'MULTI_FILE'">多文件模式会生成多个文件的应用结构
                  </span>
                  <span v-else>单文件模式会生成单个 HTML 文件，适合简单应用，极速生成
                  </span>
                </div>
              </div>
              <div class="card-check">
                <check-circle-filled v-if="generatorType === option.value"/>
              </div>
            </div>
          </div>
        </div>

        <!-- Agent模式选择器 -->
        <div class="generator-selector agent-selector">
          <div class="selector-title">
            <span class="title-icon">🔄</span>
            <span>选择生成模式</span>
            <a-tooltip placement="top">
              <template #title>
                <div style="max-width: 300px">
                  基于一套工作流的 Agent 模式会让 AI 有更强的分析和决策能，构建出的应用更加完善、稳定；
                  基于训练后的 AI 模型直接构建应用，速度更快但可能不够完善。
                </div>
              </template>
              <question-circle-outlined class="help-icon"/>
            </a-tooltip>
          </div>

          <div class="mode-cards agent-mode-cards">
            <div
              v-for="option in agentModeOptions"
              :key="option.value"
              :class="{ active: useAgentMode === option.value }"
              class="mode-card"
              @click="useAgentMode = option.value"
            >
              <div class="card-icon">
                <robot-outlined v-if="option.value"/>
                <code-outlined v-else/>
              </div>
              <div class="card-content">
                <div class="card-title">{{ option.label }}</div>
                <div class="card-desc">
                  {{ option.desc }}
                </div>
              </div>
              <div class="card-check">
                <check-circle-filled v-if="useAgentMode === option.value"/>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 创作区容器 -->
      <div class="meituan-workspace-card">
        <div class="card-header">
          <div class="header-tabs">
            <div
              :class="{ active: activeTab === 'featured' }"
              class="tab"
              @click="activeTab = 'featured'"
            >
              <span class="tab-icon">⭐</span>
              <span class="tab-text">星选灵感工坊</span>
            </div>
            <div
              :class="{ active: activeTab === 'my' }"
              class="tab"
              @click="activeTab = 'my'"
            >
              <span class="tab-icon">🎨</span>
              <span class="tab-text">我的创作空间</span>
            </div>
          </div>
        </div>

        <div class="card-content">
          <!-- 星选案例区域 -->
          <div v-show="activeTab === 'featured'" class="workspace-section featured-workspace">
            <div class="app-grid-wrapper">
              <transition-group appear name="fade">
                <AppCard
                  v-for="app in featuredApps"
                  :key="app.id"
                  :app="app"
                  :featured="true"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </transition-group>
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="featuredAppsPage.current"
                v-model:page-size="featuredAppsPage.pageSize"
                :show-size-changer="false"
                :show-total="(total: number) => `共 ${total} 个作品`"
                :total="featuredAppsPage.total"
                @change="loadFeaturedApps"
              />
            </div>
          </div>

          <!-- 我的作品区域 -->
          <div v-show="activeTab === 'my'" class="workspace-section my-workspace">
            <div class="app-grid-wrapper">
              <transition-group appear name="fade">
                <AppCard
                  v-for="app in myApps"
                  :key="app.id"
                  :app="app"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </transition-group>
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="myAppsPage.current"
                v-model:page-size="myAppsPage.pageSize"
                :show-size-changer="false"
                :show-total="(total: number) => `共 ${total} 个项目`"
                :total="myAppsPage.total"
                @change="loadMyApps"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!--  Tour 组件 -->
    <Tour
      v-model:current="tourCurrent"
      v-model:open="tourOpen"
      :steps="tourSteps"
      @close="handleTourClose"
    />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@300;400;700&family=Nunito:wght@300;400;600;700&display=swap');

#creativeStudio {
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  width: 100%;
  min-height: 100vh;
  background: #f8f9fa;
  position: relative;
  overflow: hidden;
  color: #333333;
}

/* 美团风格背景 */
#creativeStudio::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  pointer-events: none;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 2;
  box-sizing: border-box;
}

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: 60px 0 30px;
  margin-bottom: 30px;
}

.hero-title {
  font-family: 'Comic Neue', cursive;
  font-size: 3.5rem;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.1;
  letter-spacing: -1px;
  color: #2c3e50;
}

.hero-description {
  font-size: 1.4rem;
  margin: 0;
  color: #7f8c8d;
  font-weight: 400;
  font-family: 'Comic Neue', cursive;
}

/* 输入区域 */
.input-section {
  position: relative;
  margin: 0 auto 40px;
}

.prompt-input {
  border-radius: 16px;
  border: 2px solid #e8e8e8;
  font-size: 16px;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  font-family: 'Nunito', sans-serif;
  transition: all 0.3s ease;
  max-width: 800px;
}

.prompt-input:focus {
  border-color: #f2fdac;
  box-shadow: 0 0 0 3px rgb(255, 255, 255);
  outline: none;
}

.generator-selector {
  margin-top: 25px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 20px;
  border: 2px solid #e8f4fd;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.generator-selector:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.generator-content {
  padding: 15px;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  margin: 0 auto;
  max-width: 800px;
}

.generator-content:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.selector-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-family: 'Comic Neue', cursive;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
}

.title-icon {
  font-size: 1.4rem;
  animation: bounce 2s infinite;
}

.help-icon {
  color: #1890ff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
}

.help-icon:hover {
  color: #40a9ff;
  transform: scale(1.1);
}

.mode-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 1200px) {
  .mode-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .mode-cards {
    grid-template-columns: 1fr;
  }
}

/* Agent模式选择器特殊样式 */
.agent-mode-cards {
  grid-template-columns: repeat(2, 1fr) !important;
}

@media (max-width: 768px) {
  .agent-mode-cards {
    grid-template-columns: 1fr !important;
  }
}

.mode-card {
  display: flex;
  align-items: center;
  padding: 18px 16px;
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  min-height: 90px;
}

.mode-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(24, 144, 255, 0.1), transparent);
  transition: left 0.5s ease;
}

.mode-card:hover::before {
  left: 100%;
}

.mode-card:hover {
  border-color: #1890ff;
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.15);
  transform: translateY(-3px);
}

.mode-card.active {
  border-color: #1890ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.2);
}

.mode-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f5f5f5;
}

.mode-card.disabled:hover {
  border-color: #f0f0f0;
  box-shadow: none;
  transform: none;
}

.card-icon {
  font-size: 1.8rem;
  margin-right: 16px;
  min-width: 50px;
  text-align: center;
  animation: pulse 2s infinite;
}

.card-content {
  flex: 1;
}

.card-title {
  font-weight: 600;
  font-size: 1rem;
  color: #2c3e50;
  margin-bottom: 4px;
  font-family: 'Nunito', sans-serif;
  line-height: 1.2;
}

.card-desc {
  font-size: 12px;
  color: #7f8c8d;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-check {
  color: #52c41a;
  font-size: 1.2rem;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.mode-card.active .card-check {
  opacity: 1;
  animation: scaleIn 0.3s ease;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-5px);
  }
  60% {
    transform: translateY(-3px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 按钮组样式 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 20px;
  flex-wrap: wrap;
  position: relative;
}

.action-button {
  border-radius: 12px;
  padding: 0 25px;
  height: 48px;
  font-family: 'Nunito', sans-serif;
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tour-button {
  background: linear-gradient(135deg, #00d9ff 0%, #ccd3ff 100%);
  border: none;
  color: #ffffff;
}

.tour-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(168, 230, 207, 0.4);
}

.create-button {
  background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
  border: none;
  color: white;
  margin-left: auto;
}

.create-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(255, 154, 158, 0.4);
}

.rich-select-button {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  color: white;
  position: relative;
}

.rich-select-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(116, 235, 213, 0.4);
}

/* 热门提示词滑动模块样式 */
.prompt-slider-container {
  margin-top: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 2px solid #e8f4fd;
  animation: slideDown 0.3s ease-out;
}

.prompt-slider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.slider-title {
  margin: 0;
  font-family: 'Comic Neue', cursive;
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
}

.close-button {
  color: #999;
  font-size: 16px;
  padding: 4px 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-button:hover {
  background: #f0f0f0;
  color: #666;
}

.prompt-slider {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 8px 0;
  scrollbar-width: thin;
  scrollbar-color: #ddd transparent;
}

.prompt-slider::-webkit-scrollbar {
  height: 6px;
}

.prompt-slider::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.prompt-slider::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.prompt-slider::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.prompt-card {
  min-width: 280px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid #f0f0f0;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.prompt-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #1890ff;
}

.prompt-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.prompt-card-content {
  flex: 1;
}

.prompt-card-title {
  font-weight: 600;
  font-size: 1rem;
  color: #2c3e50;
  margin-bottom: 8px;
  font-family: 'Nunito', sans-serif;
  line-height: 1.2;
}

.prompt-card-desc {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.4;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.meituan-workspace-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  margin-bottom: 40px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.meituan-workspace-card:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  transform: translateY(-3px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 2px solid #f0f0f0;
  background: #fafafa;
}

.header-tabs {
  display: flex;
  gap: 24px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  font-family: 'Comic Neue', cursive;
  font-weight: 600;
}

.tab.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 700;
}

.tab:hover {
  background: #f5f5f5;
}

.tab-icon {
  font-size: 20px;
}

.tab-text {
  font-size: 18px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.card-content {
  padding: 24px;
}

.workspace-section {
  margin-bottom: 0;
}

.app-grid-wrapper {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.app-grid-wrapper :deep(.app-card) {
  border-radius: 20px !important;
  transition: all 0.3s ease;
}

.app-grid-wrapper :deep(.app-card):hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.pagination-wrapper :deep(.ant-pagination-item) {
  border-radius: 10px;
}

.pagination-wrapper :deep(.ant-pagination-prev .ant-pagination-item-link),
.pagination-wrapper :deep(.ant-pagination-next .ant-pagination-item-link) {
  border-radius: 10px;
}


/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .hero-description {
    font-size: 1.2rem;
  }

  .prompt-input {
    padding: 16px;
    border-radius: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .header-tabs {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 8px;
    gap: 12px;
  }

  .tab {
    padding: 8px 16px;
  }

  .app-grid-wrapper {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }

  .action-button {
    width: 100%;
    max-width: 280px;
    justify-content: center;
  }
}

/* Agent选择器特殊样式 */
.agent-selector {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 2px solid #bae6fd;
}

.agent-selector:hover {
  box-shadow: 0 12px 40px rgba(14, 165, 233, 0.12);
}

.agent-selector .mode-card {
  border: 2px solid #e0f2fe;
}

.agent-selector .mode-card:hover {
  border-color: #0ea5e9;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.15);
}

.agent-selector .mode-card.active {
  border-color: #0ea5e9;
  background: linear-gradient(135deg, #e0f2fe 0%, #ffffff 100%);
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.2);
}

.agent-selector .selector-title {
  color: #0c4a6e;
}

.agent-selector .help-icon {
  color: #0ea5e9;
}

.agent-selector .help-icon:hover {
  color: #0284c7;
}

/* 移动端样式调整 */
@media (max-width: 768px) {
  .prompt-dropdown {
    width: 90%;
    left: 5%;
    transform: translateX(0);
  }

  .generator-selector {
    padding: 16px;
    margin-top: 20px;
  }

  .selector-title {
    font-size: 1.1rem;
  }

  .mode-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .mode-card {
    padding: 16px;
    flex-direction: column;
    text-align: center;
  }

  .card-icon {
    margin-right: 0;
    margin-bottom: 12px;
    font-size: 1.8rem;
  }

  .card-content {
    text-align: center;
  }

  .card-title {
    font-size: 0.95rem;
  }

  .card-desc {
    font-size: 0.8rem;
  }

  .card-check {
    position: absolute;
    top: 12px;
    right: 12px;
  }
}
</style>
