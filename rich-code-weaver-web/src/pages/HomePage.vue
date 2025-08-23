<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Tour } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listStarAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'
import type { TourProps } from 'ant-design-vue'

// 导入 Tour 相关状态
const tourOpen = ref(false)
const tourCurrent = ref(0)
const tourSteps = ref<TourProps['steps']>([
  {
    title: '创意输入',
    description: '在这里输入您的应用创意描述，系统会根据描述生成完整应用',
    target: () => document.querySelector('.prompt-input') as HTMLElement,
    placement: 'bottom',
  },
  {
    title: '热门提示词',
    description: '点击这里可以选择热门应用的提示词模板，快速开始创作',
    target: () => document.querySelector('.rich-select-button') as HTMLElement,
    placement: 'bottom',
  },
  {
    title: '创建作品',
    description: '输入创意后点击这里创建您的应用作品',
    target: () => document.querySelector('.create-button') as HTMLElement,
    placement: 'bottom',
  },
  {
    title: '我的创作空间',
    description: '这里展示您已创建的所有应用作品，可以随时查看和编辑',
    target: () => document.querySelector('.my-workspace') as HTMLElement,
    placement: 'top',
  },
  {
    title: '星选灵感工坊',
    description: '浏览社区精选的优秀应用作品，获取创作灵感',
    target: () => document.querySelector('.featured-workspace') as HTMLElement,
    placement: 'top',
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

// 导入图标
import {
  PictureOutlined,
  ReadOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  SecurityScanOutlined,
  ShopOutlined,
  MedicineBoxOutlined,
  RocketOutlined,
  ExperimentOutlined,
  BankOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import { promptOptions } from '@/constants/prompts.ts'

// 控制下拉框可见性
const showPromptDropdown = ref(false)
// 处理选择的方法
const handlePromptSelect = (value: string) => {
  setPrompt(value)
  showPromptDropdown.value = false
}

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

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
      initPrompt: userPrompt.value.trim()
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
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

  // 艺术动效
  const canvas = document.createElement('canvas')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  canvas.style.position = 'fixed'
  canvas.style.top = '0'
  canvas.style.left = '0'
  canvas.style.zIndex = '1'
  canvas.style.pointerEvents = 'none'
  document.body.appendChild(canvas)

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let particles: any[] = []

  class Particle {
    x: number
    y: number
    size: number
    speedX: number
    speedY: number
    color: string

    constructor() {
      this.x = Math.random() * canvas.width
      this.y = Math.random() * canvas.height
      this.size = Math.random() * 3 + 1
      this.speedX = Math.random() * 1 - 0.5
      this.speedY = Math.random() * 1 - 0.5
      this.color = `hsl(${Math.random() * 360}, 70%, 60%)`
    }

    update() {
      this.x += this.speedX
      this.y += this.speedY

      if (this.x < 0 || this.x > canvas.width) this.speedX = -this.speedX
      if (this.y < 0 || this.y > canvas.height) this.speedY = -this.speedY
    }

    draw() {
      ctx!.fillStyle = this.color
      ctx!.beginPath()
      ctx!.arc(this.x, this.y, this.size, 0, Math.PI * 2)
      ctx!.fill()
    }
  }

  const createParticles = () => {
    for (let i = 0; i < 10; i++) {
      particles.push(new Particle())
    }
  }

  const animateParticles = () => {
    ctx!.clearRect(0, 0, canvas.width, canvas.height)

    for (let i = 0; i < particles.length; i++) {
      particles[i].update()
      particles[i].draw()

      for (let j = i; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < 100) {
          ctx!.beginPath()
          ctx!.strokeStyle = `hsla(${(i * j) % 360}, 70%, 60%, 0.1)`
          ctx!.lineWidth = 0.3
          ctx!.moveTo(particles[i].x, particles[i].y)
          ctx!.lineTo(particles[j].x, particles[j].y)
          ctx!.stroke()
        }
      }
    }

    requestAnimationFrame(animateParticles)
  }

  createParticles()
  animateParticles()
})
</script>

<template>
  <div id="creativeStudio">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title">RichCodeWeaver - 织码睿奇</h1>
        <p class="hero-description">< 将创意灵感转化为数字作品 ></p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="描述您想要创建的创意作品..."
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="input-actions">
          <!-- 快速入门按钮 -->
          <a-button
            size="large"
            @click="startTour"
          >
            <template #icon>
              <span class="create-icon">快速入门</span>
            </template>
          </a-button>

          <!-- 创建作品按钮 -->
          <a-button
            size="large"
            @click="createApp"
            :loading="creating"
          >
            <template #icon>
              <span class="create-icon">创建作品</span>
            </template>
          </a-button>
        </div>
      </div>

      <!-- 自动填入提示词按钮 -->
      <div class="quick-actions">
        <a-button
          type="default"
          size="large"
          class="rich-select-button"
          @click="showPromptDropdown = !showPromptDropdown"
        >
          <template #icon>
            <AppstoreOutlined />
          </template>
          点击选择热门应用提示词
        </a-button>

        <div v-if="showPromptDropdown" class="prompt-dropdown">
          <a-list :bordered="false" class="prompt-list">
            <a-list-item
              v-for="option in promptOptions"
              :key="option.value"
              class="prompt-item"
              @click="handlePromptSelect(option.value)"
            >
              <div class="option-content">
                <span class="option-icon" :style="{backgroundColor: option.color}">
                  <component :is="option.icon" />
                </span>
                <div class="option-text">
                  <span class="option-title">{{ option.label }}</span>
                  <span class="option-desc">{{ option.desc }}</span>
                </div>
              </div>
            </a-list-item>
          </a-list>
        </div>
      </div>

      <!-- 创作区容器 -->
      <div class="workspace-container">
        <!-- 我的作品 -->
        <div class="workspace-section my-workspace">
          <div class="section-header">
            <div class="decorative-line"></div>
            <h2 class="section-title">我的创作空间</h2>
            <div class="decorative-line"></div>
          </div>
          <div class="app-grid-wrapper">
            <transition-group name="fade" appear>
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
              :total="myAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个项目`"
              @change="loadMyApps"
            />
          </div>
        </div>

        <div class="section-divider"></div>

        <!-- 星选案例 -->
        <div class="workspace-section featured-workspace">
          <div class="section-header">
            <div class="decorative-line"></div>
            <h2 class="section-title">星选灵感工坊</h2>
            <div class="decorative-line"></div>
          </div>
          <div class="app-grid-wrapper">
            <transition-group name="fade" appear>
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
              :total="featuredAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个作品`"
              @change="loadFeaturedApps"
            />
          </div>
        </div>
      </div>
    </div>

    <!--  Tour 组件 -->
    <Tour
      v-model:open="tourOpen"
      v-model:current="tourCurrent"
      :steps="tourSteps"
      @close="handleTourClose"
    />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Source+Sans+Pro:wght@300;400;600&family=Caveat:wght@700&display=swap');

#creativeStudio {
  font-family: 'Source Sans Pro', sans-serif;
  width: 100%;
  min-height: 100vh;
  background: #fcf9f2;
  background-image: radial-gradient(circle at 5% 10%, rgba(255, 230, 204, 0.3) 0%, transparent 25%),
  radial-gradient(circle at 95% 90%, rgba(204, 230, 255, 0.3) 0%, transparent 25%),
  linear-gradient(125deg, transparent 60%, rgba(255, 245, 230, 0.5) 100%);
  position: relative;
  overflow: hidden;
  color: #3a3a44;
}

/* 水彩纹理背景 */
#creativeStudio::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><path fill="none" stroke="rgba(180,170,255,0.1)" stroke-width="1" d="M20,20 Q40,5 60,20 T100,20 M20,40 Q30,30 40,40 T80,40 M10,70 Q35,55 60,70 T90,70"/></svg>');
  background-size: 300px;
  opacity: 0.5;
  pointer-events: none;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
  position: relative;
  z-index: 2;
  box-sizing: border-box;
}

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: 80px 0 40px;
  margin-bottom: 40px;
}

.hero-title {
  font-family: 'Playfair Display', serif;
  font-size: 4.5rem;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.1;
  color: #5c4a48;
  text-shadow: 3px 3px 0px rgba(200, 180, 170, 0.3);
  letter-spacing: -1px;
}

.hero-description {
  font-family: 'Caveat', cursive;
  font-size: 2.4rem;
  margin: 0;
  color: #6d6b80;
}

/* 输入区域 */
.input-section {
  position: relative;
  margin: 0 auto 40px;
  max-width: 900px;
}

.prompt-input {
  border-radius: 20px;
  border: 2px solid #e4d7c1;
  font-size: 18px;
  padding: 25px 25px;
  background: rgba(255, 251, 245, 0.9);
  box-shadow: 0 8px 32px rgba(155, 140, 125, 0.15);
  font-family: 'Source Sans Pro', sans-serif;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.prompt-input:focus {
  border-color: #c9b097;
  box-shadow: 0 15px 45px rgba(155, 140, 125, 0.25);
  background: #fffefb;
}

.input-actions {
  position: absolute;
  bottom: -25px;
  right: 25px;
  display: flex;
}

.input-actions .ant-btn {
  background: #c6a08a;
  min-width: 200px;
  border: none;
  border-radius: 30px;
  padding: 0 35px;
  height: 50px;
  font-family: 'Playfair Display', serif;
  font-size: 1.2rem;
  letter-spacing: 1px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 15px rgba(155, 110, 90, 0.3);
}

.input-actions .ant-btn:hover {
  background: #b38e77;
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 12px 20px rgba(155, 110, 90, 0.4);
}

.create-icon {
  font-family: 'Source Sans Pro', sans-serif;
  padding-left: 0;
  font-weight: 600;
}

/* 快捷按钮 */
.quick-actions {
  position: relative;
  margin-bottom: 60px;
}

.rich-select-button {
  width: 50%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 16px;
  background: rgba(255, 250, 240, 0.95);
  border: none;
  box-shadow: 0 10px 25px rgba(155, 140, 125, 0.12);
  transition: all 0.3s ease-out;
  position: relative;
  overflow: hidden;
  color: #5c4a48;
  margin: 0 auto;
}

.rich-select-button:hover {
  transform: translateY(-8px) rotate(-1deg);
  box-shadow: 0 15px 35px rgba(155, 140, 125, 0.25);
  background: rgba(255, 250, 240, 0.95);
  color: #5c4a48;
  z-index: 10;
}

.prompt-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background: white;
  border-radius: 16px;
  margin-top: 12px;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 450px;
  overflow-y: auto;
  padding: 16px 0;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.prompt-item {
  padding: 12px 24px;
  cursor: pointer;
  transition: background 0.2s;
}

.prompt-item:hover {
  background: rgba(198, 160, 138, 0.1);
}

.prompt-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 0;
}

/* =================== 创作区域优化 =================== */
.workspace-container {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 30px;
  margin-bottom: 50px;
  position: relative;
  width: 100%;
}

.section-divider {
  position: relative;
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent,
    #c6a08a,
    transparent
  );
  height: 100%;
  opacity: 0.5;
}

.workspace-section {
  background: rgba(255, 252, 248, 0.95);
  border-radius: 24px;
  padding: 30px;
  border: 1px solid rgba(198, 160, 138, 0.2);
  box-shadow: 0 15px 35px rgba(155, 140, 125, 0.12);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.workspace-section:hover {
  box-shadow: 0 20px 45px rgba(155, 140, 125, 0.18);
  transform: translateY(-5px);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 35px;
  gap: 15px;
}

.decorative-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(198, 160, 138, 0.4),
    transparent
  );
}

.section-title {
  font-family: 'Playfair Display', serif;
  font-size: 2.6rem;
  font-weight: 700;
  color: #5c4a48;
  text-align: center;
  padding: 0 20px;
  text-shadow: 1px 1px 1px rgba(200, 180, 170, 0.2);
  letter-spacing: -0.5px;
  position: relative;
}

.app-grid-wrapper {
  flex: 1;
}

.app-grid-wrapper :deep(.app-card) {
  transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.app-grid-wrapper :deep(.app-card):hover {
  transform: translateY(-12px) scale(1.03);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15),
  0 0 15px rgba(198, 160, 138, 0.2);
  z-index: 10;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 网格布局 */
.app-grid-wrapper .app-grid,
.app-grid-wrapper .featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
  position: relative;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: auto;
  padding-top: 20px;
}

/* 选项样式 */
.option-content {
  display: flex;
  align-items: center;
}

.option-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 12px;
  color: white;
  font-size: 18px;
}

.option-text {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.option-title {
  font-weight: 600;
  font-size: 16px;
  color: #5c4a48;
}

.option-desc {
  font-size: 12px;
  color: #7a787c;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .workspace-container {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto;
    grid-template-areas:
      "myWorkspace"
      "featuredWorkspace";
  }

  .workspace-section:nth-child(1) {
    grid-area: myWorkspace;
  }

  .workspace-section:nth-child(3) {
    grid-area: featuredWorkspace;
  }

  .section-divider {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.8rem;
  }

  .hero-description {
    font-size: 1.8rem;
  }

  .prompt-input {
    padding: 18px;
  }

  .section-title {
    font-size: 2.2rem;
  }

  .workspace-section {
    padding: 20px;
  }

  .app-grid-wrapper .app-grid,
  .app-grid-wrapper .featured-grid {
    grid-template-columns: 1fr;
  }

  .rich-select-button {
    height: 56px;
    font-size: 16px;
    padding: 0 16px;
  }

  .input-actions {
    position: static;
    margin-top: 20px;
    justify-content: center;
  }

  .input-actions .ant-btn {
    min-width: 150px;
  }
}
</style>
