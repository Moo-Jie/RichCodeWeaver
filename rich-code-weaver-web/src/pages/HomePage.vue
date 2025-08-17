<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listStarAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

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

// TODO 测试数据，待优化
const promptOptions = ref([
  {
    value: '设计一个数字艺术画廊网站，展示现代艺术家的混合媒介作品。网站应包含：沉浸式全屏画廊视图、按艺术家/风格/媒介分类的展览馆、艺术家个人简介页面、虚拟展览功能（含时间轴导览）、社交分享功能、在线商店（支持数字藏品NFT购买）、访客留言墙。采用简约高级的设计风格，突出艺术作品本身，支持深色模式切换。',
    label: '数字艺术画廊',
    desc: '沉浸式艺术展示平台',
    icon: PictureOutlined,
    color: '#ff9f7f'
  },
  {
    value: '创建一个互动故事讲述平台，允许用户创作带有多分支情节的互动小说。功能包括：可视化情节树编辑器、角色数据库（含关系图谱）、场景/道具资源库、多语言支持、读者选择影响故事结局系统、作品分类标签（科幻/奇幻/悬疑）、作者与读者讨论社区、作品章节订阅通知、阅读进度同步。采用沉浸式的故事书界面设计，支持声音效果添加。',
    label: '互动小说平台',
    desc: '多分支故事创作系统',
    icon: ReadOutlined,
    color: '#7eb8ff'
  },
  {
    value: '设计一个城市文化探索应用，结合AR技术展示城市历史地标的数字重建。功能包括：GPS定位探索城市文化遗产点、AR实景叠加历史画面对比、数字城市时间轴展示变迁、语音导览多语言支持、用户创建自定义文化路线、社区分享探索发现、主题活动日历、数字印章收集系统。界面需直观呈现城市地图与文化热点的层次关系。',
    label: '文化探索应用',
    desc: 'AR增强现实导览系统',
    icon: EnvironmentOutlined,
    color: '#91d2a3'
  },
  {
    value: '创建一个环保生活社区平台，促进可持续生活实践。功能包含：碳足迹计算工具、可持续替代品数据库、二手物品交换市场、共享技能活动日历、社区花园种植计划管理、环保挑战任务系统、绿色商家地图导航、环保知识库、个人绿色成就展示墙、社区环境监测数据可视化。采用有机自然的视觉风格，突出环保主题色系。',
    label: '可持续生活社区',
    desc: '生态友好型服务平台',
    icon: SecurityScanOutlined,
    color: '#a98bf9'
  },
  {
    value: '开发一个智能家居控制面板，包含房间3D可视化、设备状态监控、能耗统计、情景模式设置、语音控制集成、安全警报系统。采用现代极简风格，支持暗黑模式',
    label: '智能家居面板',
    desc: '全屋智能控制系统',
    icon: HomeOutlined,
    color: '#ffcc5c'
  },
  {
    value: '创建一个虚拟商务洽谈平台，支持3D会议室、数字名片交换、实时文档协作、多语言翻译、会议记录AI助手、合同模板库、交易安全防护。界面需专业商务风格',
    label: '商务洽谈平台',
    desc: '企业级远程协作工具',
    icon: ShopOutlined,
    color: '#ff7c98'
  },
  {
    value: '开发一个健康管理系统，包含健康数据仪表盘、用药提醒、预约管理、AI症状分析、电子病历管理、家庭健康共享功能。符合医疗行业UI规范',
    label: '健康管理系统',
    desc: '个人健康数据追踪器',
    icon: MedicineBoxOutlined,
    color: '#6cd9e6'
  },
  {
    value: '设计航天科普教育平台，含火箭模拟器、星座导航系统、太空任务体验、航天新闻聚合、专家讲座预约。采用深空主题设计',
    label: '航天科普平台',
    desc: '太空探索教育系统',
    icon: RocketOutlined,
    color: '#c17ce0'
  },
  {
    value: '创建一个科研协作系统，支持实验数据可视化、论文协作编辑、学术资源索引、引用管理、会议投稿追踪。符合学术出版规范',
    label: '科研协作系统',
    desc: '学术研究管理工具',
    icon: ExperimentOutlined,
    color: '#8dd1b2'
  },
  {
    value: '设计一个文化旅游预约平台，整合景点门票、特色住宿、当地导游、文化体验工作坊预约。支持多支付方式和多语言界面',
    label: '文化旅游平台',
    desc: '深度旅游体验服务',
    icon: BankOutlined,
    color: '#f9a166'
  }
])

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

  // 添加艺术动效
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
          <a-button type="default" size="large" @click="createApp" :loading="creating">
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
        <div class="workspace-section">
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
        <div class="workspace-section">
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
</style>
