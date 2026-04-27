<template>
  <div class="system-manage-dashboard">
    <div class="page-shell">
      <div class="page-header">
        <h1>系统管理控制台</h1>
        <p>统一查看核心业务运行概况，并快速进入各管理模块。</p>
      </div>

      <MicroserviceHealthPanel
        class="top-health-panel"
        title="微服务模块状态监测"
        :show-refresh="true"
      />

      <div class="section-block">
        <div class="section-header">
          <div>
            <h2>核心管理模块</h2>
            <p>围绕 Generator 业务主链路组织，点击即可进入独立管理页面。</p>
          </div>
          <a-button class="ghost-btn" @click="refreshDashboard" :loading="refreshing">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新数据
          </a-button>
        </div>

        <div class="module-grid">
          <button
            v-for="item in adminNavItems"
            :key="item.key"
            class="module-card"
            type="button"
            @click="handleAdminNavClick(item)"
          >
            <div class="module-card-head">
              <div class="module-icon-wrap">
                <component :is="item.icon" class="module-icon" />
              </div>
              <div class="module-link">{{ item.url ? '新窗口打开' : '查看详情' }}</div>
            </div>
            <div class="module-title-row">
              <div class="module-title">{{ item.label }}</div>
              <div v-if="item.metricKey" class="module-count-pill">
                {{ formatNumber(metrics[item.metricKey] || 0) }}
              </div>
              <div v-else class="module-count-pill neutral">
                入口
              </div>
            </div>
            <div class="module-desc">{{ item.description }}</div>
            <div class="module-footer">
              <div class="module-metric-line" v-if="item.metricKey">
                <span>主指标</span>
                <strong>{{ formatNumber(metrics[item.metricKey] || 0) }}</strong>
              </div>
              <div class="module-metric-line" v-if="item.secondaryMetricKey && item.secondaryLabel">
                <span>{{ item.secondaryLabel }}</span>
                <strong>{{ formatNumber(metrics[item.secondaryMetricKey] || 0) }}</strong>
              </div>
              <div class="module-metric-line" v-if="!item.metricKey && !item.secondaryMetricKey">
                <span>访问方式</span>
                <strong>{{ item.url ? '外部链接' : '内部页面' }}</strong>
              </div>
            </div>
          </button>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-header detail-section-header">
          <div>
            <h2>产物运营分析总览</h2>
            <p>围绕产物 VO 的生成类型、生成模式、部署状态、评分质量与创作者活跃度展开。</p>
          </div>
        </div>

        <div class="analytics-grid">
          <a-card class="analytics-card analytics-span-2" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>产物运营量化总览</span>
                <span class="card-title-tip">基于产物 VO 汇总当前核心经营指标</span>
              </div>
            </template>
            <div class="detail-table">
              <div class="detail-row detail-head">
                <span>指标项</span>
                <span>数值</span>
                <span>说明</span>
              </div>
              <div
                v-for="item in detailRows"
                :key="item.key"
                class="detail-row"
              >
                <span>{{ item.label }}</span>
                <span>{{ item.displayValue || formatNumber(item.value) }}</span>
                <span>{{ item.description }}</span>
              </div>
            </div>
          </a-card>

          <a-card class="analytics-card" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>产物核心指标对比</span>
                <span class="card-title-tip">围绕部署、评分、素材完整度与近期活跃度展开</span>
              </div>
            </template>
            <div class="bar-chart-list">
              <div v-for="item in scaleChartRows" :key="item.key" class="bar-item">
                <div class="bar-item-head">
                  <span>{{ item.label }}</span>
                  <strong>{{ formatNumber(item.value) }}</strong>
                </div>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    :style="{ width: `${Math.max(item.ratio * 100, item.value ? 10 : 0)}%` }"
                  />
                </div>
              </div>
            </div>
          </a-card>

          <a-card class="analytics-card" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>生成模式分布</span>
                <span class="card-title-tip">Workflow / Agent / 其他模式</span>
              </div>
            </template>
            <div class="donut-panel">
              <div class="donut-chart" :style="appModeDonutStyle">
                <div class="donut-inner">
                  <div class="donut-total">{{ formatNumber(metrics.appTotal) }}</div>
                  <div class="donut-caption">模式覆盖</div>
                </div>
              </div>
              <div class="legend-list">
                <div v-for="item in appModeComposition" :key="item.key" class="legend-item">
                  <span class="legend-dot" :style="{ background: item.color }" />
                  <div class="legend-content">
                    <span>{{ item.label }}</span>
                    <small>{{ formatNumber(item.value) }}</small>
                  </div>
                  <strong>{{ formatPercent(item.share) }}</strong>
                </div>
              </div>
            </div>
          </a-card>

          <a-card class="analytics-card" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>部署状态分布</span>
                <span class="card-title-tip">已部署 / 已配置待部署 / 未配置部署</span>
              </div>
            </template>
            <div class="donut-panel">
              <div class="donut-chart" :style="appDeploymentDonutStyle">
                <div class="donut-inner">
                  <div class="donut-total">{{ formatNumber(metrics.appDeployed) }}</div>
                  <div class="donut-caption">已部署数</div>
                </div>
              </div>
              <div class="legend-list">
                <div v-for="item in appDeploymentComposition" :key="item.key" class="legend-item">
                  <span class="legend-dot" :style="{ background: item.color }" />
                  <div class="legend-content">
                    <span>{{ item.label }}</span>
                    <small>{{ formatNumber(item.value) }}</small>
                  </div>
                  <strong>{{ formatPercent(item.share) }}</strong>
                </div>
              </div>
            </div>
          </a-card>

          <a-card class="analytics-card" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>产物类型分布</span>
                <span class="card-title-tip">单文件结构 / 多文件结构 / Vue 工程</span>
              </div>
            </template>
            <div class="donut-panel">
              <div class="donut-chart" :style="appTypeDonutStyle">
                <div class="donut-inner">
                  <div class="donut-total">{{ formatNumber(metrics.appTotal) }}</div>
                  <div class="donut-caption">产物总量</div>
                </div>
              </div>
              <div class="legend-list">
                <div v-for="item in appTypeComposition" :key="item.key" class="legend-item">
                  <span class="legend-dot" :style="{ background: item.color }" />
                  <div class="legend-content">
                    <span>{{ item.label }}</span>
                    <small>{{ formatNumber(item.value) }}</small>
                  </div>
                  <strong>{{ formatPercent(item.share) }}</strong>
                </div>
              </div>
            </div>
          </a-card>

          <a-card class="analytics-card analytics-span-2" :bordered="false">
            <template #title>
              <div class="card-title-row">
                <span>产物运营指标</span>
                <span class="card-title-tip">部署、质量、模式渗透与创作者活跃度指标</span>
              </div>
            </template>
            <div class="indicator-list">
              <div v-for="item in governanceIndicators" :key="item.key" class="indicator-item">
                <div class="indicator-head">
                  <span>{{ item.label }}</span>
                  <strong>{{ formatPercent(item.percent) }}</strong>
                </div>
                <div class="bar-track slim">
                  <div class="bar-fill accent" :style="{ width: `${item.percent}%` }" />
                </div>
                <div class="indicator-desc">{{ item.description }}</div>
              </div>
            </div>
          </a-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { listAppVoByPageByAdmin } from '@/api/appController'
import { listAppChatHistoryByPageAdmin } from '@/api/chatHistoryController'
import {
  listAllMaterialByPage,
  listEnabledCategories,
  listMaterialCategoryByPage
} from '@/api/materialController'
import { listPromptTemplateByPage } from '@/api/promptTemplateController'
import { listRagDocumentByPage } from '@/api/ragDocumentController'
import { listSystemPromptByPage } from '@/api/systemPromptController'
import { listUserVoByPage } from '@/api/userController'
import { adminNavItems } from '@/constants/adminNavItems'
import MicroserviceHealthPanel from '@/components/admin/MicroserviceHealthPanel.vue'

const router = useRouter()
const refreshing = ref(false)
const appRecords = ref<API.AppVO[]>([])

const metrics = reactive<Record<string, number>>({
  userTotal: 0,
  adminUserTotal: 0,
  appTotal: 0,
  appHtml: 0,
  appMultiFile: 0,
  appVueProject: 0,
  appOtherType: 0,
  appWorkflow: 0,
  appAgent: 0,
  appOtherMode: 0,
  appDeployed: 0,
  appDeployPending: 0,
  appDeployUnconfigured: 0,
  appWithDeployKey: 0,
  appFeatured: 0,
  appRated: 0,
  appRatedRegular: 0,
  appWithCover: 0,
  appWithPrompt: 0,
  appCreatorTotal: 0,
  appCreated7d: 0,
  appUpdated7d: 0,
  appDeployed7d: 0,
  chatHistoryTotal: 0,
  promptTemplateTotal: 0,
  promptTemplateEnabled: 0,
  systemPromptTotal: 0,
  ragDocTotal: 0,
  ragDocEnabled: 0,
  materialTotal: 0,
  materialPublic: 0,
  categoryTotal: 0,
  enabledCategoryTotal: 0
})

const formatNumber = (value?: number) => {
  return new Intl.NumberFormat('zh-CN').format(value || 0)
}

const formatDecimal = (value?: number) => {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1
  }).format(value || 0)
}

const formatPercent = (value?: number) => {
  return `${formatDecimal(value)}%`
}

const clampPercent = (value: number) => {
  return Math.max(0, Math.min(100, value))
}

const calculateShare = (value: number, total: number) => {
  if (!total) {
    return 0
  }
  return Number(((value / total) * 100).toFixed(1))
}

type CompositionItem = {
  key: string
  label: string
  value: number
  color: string
  share: number
}

type DetailRowItem = {
  key: string
  label: string
  value: number
  description: string
  displayValue?: string
}

const buildComposition = (items: Omit<CompositionItem, 'share'>[]): CompositionItem[] => {
  const total = items.reduce((sum, item) => sum + item.value, 0)
  return items.map((item) => ({
    ...item,
    share: calculateShare(item.value, total)
  }))
}

const buildDonutBackground = (segments: CompositionItem[]) => {
  const total = segments.reduce((sum, item) => sum + item.value, 0)
  if (!total) {
    return 'conic-gradient(#edf2ff 0deg 360deg)'
  }
  let current = 0
  const stops = segments.map((item) => {
    const start = current
    current += (item.value / total) * 360
    return `${item.color} ${start}deg ${current}deg`
  })
  return `conic-gradient(${stops.join(', ')})`
}

const hasText = (value?: string | null) => {
  return typeof value === 'string' && value.trim().length > 0
}

const priorityToRateValue = (priority?: number) => {
  if (!priority || priority <= 0) {
    return 0
  }
  if (priority >= 99) {
    return 5
  }
  return Math.min(priority, 4)
}

const isWithinDays = (value?: string, days = 7) => {
  if (!value) {
    return false
  }
  const timestamp = new Date(value).getTime()
  if (Number.isNaN(timestamp)) {
    return false
  }
  return Date.now() - timestamp <= days * 24 * 60 * 60 * 1000
}

const appPerCreator = computed(() => (metrics.appCreatorTotal ? metrics.appTotal / metrics.appCreatorTotal : 0))
const appAveragePromptLength = computed(() => {
  const promptLengths = appRecords.value
    .map((item) => (hasText(item.initPrompt) ? item.initPrompt!.trim().length : 0))
    .filter((length) => length > 0)
  if (!promptLengths.length) {
    return 0
  }
  return promptLengths.reduce((sum, length) => sum + length, 0) / promptLengths.length
})
const appAverageRating = computed(() => {
  const ratings = appRecords.value
    .map((item) => priorityToRateValue(item.priority))
    .filter((value) => value > 0)
  if (!ratings.length) {
    return 0
  }
  return ratings.reduce((sum, value) => sum + value, 0) / ratings.length
})

const detailRows = computed<DetailRowItem[]>(() => [
  {
    key: 'appTotal',
    label: '产物总量',
    value: metrics.appTotal,
    description: '当前纳入后台管理的全部数字产物数量'
  },
  {
    key: 'appCreatorTotal',
    label: '活跃创作者数',
    value: metrics.appCreatorTotal,
    description: `覆盖平台 ${formatPercent(calculateShare(metrics.appCreatorTotal, metrics.userTotal))} 的用户，当前人均 ${formatDecimal(appPerCreator.value)} 个产物`
  },
  {
    key: 'appHtml',
    label: '单文件结构产物',
    value: metrics.appHtml,
    description: `占比 ${formatPercent(calculateShare(metrics.appHtml, metrics.appTotal))}`
  },
  {
    key: 'appMultiFile',
    label: '多文件结构产物',
    value: metrics.appMultiFile,
    description: `占比 ${formatPercent(calculateShare(metrics.appMultiFile, metrics.appTotal))}`
  },
  {
    key: 'appVueProject',
    label: 'Vue 工程产物',
    value: metrics.appVueProject,
    description: `占比 ${formatPercent(calculateShare(metrics.appVueProject, metrics.appTotal))}`
  },
  {
    key: 'appWorkflow',
    label: 'Workflow 模式产物',
    value: metrics.appWorkflow,
    description: `占比 ${formatPercent(calculateShare(metrics.appWorkflow, metrics.appTotal))}`
  },
  {
    key: 'appAgent',
    label: 'Agent 模式产物',
    value: metrics.appAgent,
    description: `占比 ${formatPercent(calculateShare(metrics.appAgent, metrics.appTotal))}`
  },
  {
    key: 'appDeployed',
    label: '已部署产物数',
    value: metrics.appDeployed,
    description: `近 7 日新增部署 ${formatNumber(metrics.appDeployed7d)} 个`
  },
  {
    key: 'appWithDeployKey',
    label: '已配置部署密钥',
    value: metrics.appWithDeployKey,
    description: `待部署 ${formatNumber(metrics.appDeployPending)} / 未配置 ${formatNumber(metrics.appDeployUnconfigured)}`
  },
  {
    key: 'appFeatured',
    label: '星选产物数',
    value: metrics.appFeatured,
    description: `占已评分产物 ${formatPercent(calculateShare(metrics.appFeatured, metrics.appRated))}`
  },
  {
    key: 'appRated',
    label: '已评分产物数',
    value: metrics.appRated,
    description: `未评分 ${formatNumber(metrics.appTotal - metrics.appRated)} 个`
  },
  {
    key: 'appWithCover',
    label: '已配置封面产物',
    value: metrics.appWithCover,
    description: `缺少封面 ${formatNumber(metrics.appTotal - metrics.appWithCover)} 个`
  },
  {
    key: 'appWithPrompt',
    label: '已配置初始提示词',
    value: metrics.appWithPrompt,
    description: `缺少初始提示词 ${formatNumber(metrics.appTotal - metrics.appWithPrompt)} 个`
  },
  {
    key: 'appAveragePromptLength',
    label: '平均提示词长度',
    value: Math.round(appAveragePromptLength.value),
    displayValue: `${formatDecimal(appAveragePromptLength.value)} 字`,
    description: '统计已填写初始提示词的产物平均字符长度'
  },
  {
    key: 'appAverageRating',
    label: '平均评分星级',
    value: Number(appAverageRating.value.toFixed(1)),
    displayValue: `${formatDecimal(appAverageRating.value)} 星`,
    description: '基于后台 priority 字段折算出的平均星级'
  },
  {
    key: 'appCreated7d',
    label: '近 7 日新增产物',
    value: metrics.appCreated7d,
    description: `占总产物 ${formatPercent(calculateShare(metrics.appCreated7d, metrics.appTotal))}`
  },
  {
    key: 'appUpdated7d',
    label: '近 7 日更新产物',
    value: metrics.appUpdated7d,
    description: `占总产物 ${formatPercent(calculateShare(metrics.appUpdated7d, metrics.appTotal))}`
  }
])

const scaleChartRows = computed(() => {
  const items = [
    { key: 'appTotal', label: '产物总量', value: metrics.appTotal },
    { key: 'appDeployed', label: '已部署产物', value: metrics.appDeployed },
    { key: 'appWithDeployKey', label: '已配置部署密钥', value: metrics.appWithDeployKey },
    { key: 'appWithCover', label: '已配置封面', value: metrics.appWithCover },
    { key: 'appWithPrompt', label: '已配置初始提示词', value: metrics.appWithPrompt },
    { key: 'appFeatured', label: '星选产物', value: metrics.appFeatured },
    { key: 'appCreated7d', label: '近 7 日新增', value: metrics.appCreated7d },
    { key: 'appUpdated7d', label: '近 7 日更新', value: metrics.appUpdated7d }
  ]
  const max = Math.max(...items.map((item) => item.value), 1)
  return items.map((item) => ({
    ...item,
    ratio: item.value / max
  }))
})

const appTypeComposition = computed(() => buildComposition([
  { key: 'appHtml', label: '单文件结构', value: metrics.appHtml, color: '#5b8ff9' },
  { key: 'appMultiFile', label: '多文件结构', value: metrics.appMultiFile, color: '#5ad8a6' },
  { key: 'appVueProject', label: 'Vue 工程', value: metrics.appVueProject, color: '#f6bd16' },
  { key: 'appOtherType', label: '其他类型', value: metrics.appOtherType, color: '#9270ca' }
]))

const appTypeDonutStyle = computed(() => ({
  background: buildDonutBackground(appTypeComposition.value)
}))

const appModeComposition = computed(() => buildComposition([
  { key: 'appWorkflow', label: 'Workflow 模式', value: metrics.appWorkflow, color: '#5b8ff9' },
  { key: 'appAgent', label: 'Agent 模式', value: metrics.appAgent, color: '#5ad8a6' },
  { key: 'appOtherMode', label: '其他模式', value: metrics.appOtherMode, color: '#f6bd16' }
]))

const appModeDonutStyle = computed(() => ({
  background: buildDonutBackground(appModeComposition.value)
}))

const appDeploymentComposition = computed(() => buildComposition([
  { key: 'appDeployed', label: '已部署', value: metrics.appDeployed, color: '#5b8ff9' },
  { key: 'appDeployPending', label: '已配置待部署', value: metrics.appDeployPending, color: '#5ad8a6' },
  { key: 'appDeployUnconfigured', label: '未配置部署', value: metrics.appDeployUnconfigured, color: '#f6bd16' }
]))

const appDeploymentDonutStyle = computed(() => ({
  background: buildDonutBackground(appDeploymentComposition.value)
}))

const governanceIndicators = computed(() => {
  const deploymentRate = metrics.appTotal
    ? (metrics.appDeployed / metrics.appTotal) * 100
    : 0
  const deployKeyRate = metrics.appTotal
    ? (metrics.appWithDeployKey / metrics.appTotal) * 100
    : 0
  const coverRate = metrics.appTotal
    ? (metrics.appWithCover / metrics.appTotal) * 100
    : 0
  const promptRate = metrics.appTotal
    ? (metrics.appWithPrompt / metrics.appTotal) * 100
    : 0
  const workflowRate = metrics.appTotal
    ? (metrics.appWorkflow / metrics.appTotal) * 100
    : 0
  const agentRate = metrics.appTotal
    ? (metrics.appAgent / metrics.appTotal) * 100
    : 0
  const featuredRate = metrics.appTotal
    ? (metrics.appFeatured / metrics.appTotal) * 100
    : 0
  const updated7dRate = metrics.appTotal
    ? (metrics.appUpdated7d / metrics.appTotal) * 100
    : 0

  return [
    {
      key: 'deploymentRate',
      label: '部署完成率',
      percent: clampPercent(deploymentRate),
      description: `${formatNumber(metrics.appDeployed)} / ${formatNumber(metrics.appTotal)} 个产物已部署`
    },
    {
      key: 'deployKeyRate',
      label: '部署密钥配置率',
      percent: clampPercent(deployKeyRate),
      description: `${formatNumber(metrics.appWithDeployKey)} / ${formatNumber(metrics.appTotal)} 个产物已配置部署密钥`
    },
    {
      key: 'coverRate',
      label: '封面完善率',
      percent: clampPercent(coverRate),
      description: `${formatNumber(metrics.appWithCover)} / ${formatNumber(metrics.appTotal)} 个产物已配置封面`
    },
    {
      key: 'promptRate',
      label: '提示词完善率',
      percent: clampPercent(promptRate),
      description: `${formatNumber(metrics.appWithPrompt)} / ${formatNumber(metrics.appTotal)} 个产物已配置初始提示词`
    },
    {
      key: 'workflowRate',
      label: 'Workflow 模式占比',
      percent: clampPercent(workflowRate),
      description: `${formatNumber(metrics.appWorkflow)} / ${formatNumber(metrics.appTotal)} 个产物使用 Workflow 模式`
    },
    {
      key: 'agentRate',
      label: 'Agent 模式占比',
      percent: clampPercent(agentRate),
      description: `${formatNumber(metrics.appAgent)} / ${formatNumber(metrics.appTotal)} 个产物使用 Agent 模式`
    },
    {
      key: 'featuredRate',
      label: '星选产物占比',
      percent: clampPercent(featuredRate),
      description: `${formatNumber(metrics.appFeatured)} / ${formatNumber(metrics.appTotal)} 个产物已进入星选`
    },
    {
      key: 'updated7dRate',
      label: '近 7 日更新率',
      percent: clampPercent(updated7dRate),
      description: `${formatNumber(metrics.appUpdated7d)} / ${formatNumber(metrics.appTotal)} 个产物近 7 日发生更新`
    }
  ]
})

const assignMetric = (key: keyof typeof metrics, value?: number) => {
  metrics[key] = value || 0
}

const analyzeAppRecords = (records: API.AppVO[]) => {
  appRecords.value = records

  let appHtml = 0
  let appMultiFile = 0
  let appVueProject = 0
  let appOtherType = 0
  let appWorkflow = 0
  let appAgent = 0
  let appOtherMode = 0
  let appDeployed = 0
  let appDeployPending = 0
  let appDeployUnconfigured = 0
  let appWithDeployKey = 0
  let appFeatured = 0
  let appRated = 0
  let appRatedRegular = 0
  let appWithCover = 0
  let appWithPrompt = 0
  let appCreated7d = 0
  let appUpdated7d = 0
  let appDeployed7d = 0

  const creatorIds = new Set<number>()

  records.forEach((item) => {
    const codeGenType = item.codeGenType === 'html' ? 'single_html' : item.codeGenType
    if (codeGenType === 'single_html') {
      appHtml += 1
    } else if (codeGenType === 'multi_file') {
      appMultiFile += 1
    } else if (codeGenType === 'vue_project') {
      appVueProject += 1
    } else {
      appOtherType += 1
    }

    if (item.genMode === 'workflow') {
      appWorkflow += 1
    } else if (item.genMode === 'agent') {
      appAgent += 1
    } else {
      appOtherMode += 1
    }

    const hasDeployKeyValue = hasText(item.deployKey)
    const isDeployed = Boolean(item.deployedTime)
    if (hasDeployKeyValue) {
      appWithDeployKey += 1
    }
    if (isDeployed) {
      appDeployed += 1
    } else if (hasDeployKeyValue) {
      appDeployPending += 1
    } else {
      appDeployUnconfigured += 1
    }

    const rateValue = priorityToRateValue(item.priority)
    if (rateValue > 0) {
      appRated += 1
    }
    if ((item.priority || 0) >= 99) {
      appFeatured += 1
    } else if (rateValue > 0) {
      appRatedRegular += 1
    }

    if (hasText(item.cover)) {
      appWithCover += 1
    }
    if (hasText(item.initPrompt)) {
      appWithPrompt += 1
    }
    if (typeof item.userId === 'number') {
      creatorIds.add(item.userId)
    }
    if (isWithinDays(item.createTime, 7)) {
      appCreated7d += 1
    }
    if (isWithinDays(item.updateTime, 7)) {
      appUpdated7d += 1
    }
    if (isWithinDays(item.deployedTime, 7)) {
      appDeployed7d += 1
    }
  })

  assignMetric('appHtml', appHtml)
  assignMetric('appMultiFile', appMultiFile)
  assignMetric('appVueProject', appVueProject)
  assignMetric('appOtherType', appOtherType)
  assignMetric('appWorkflow', appWorkflow)
  assignMetric('appAgent', appAgent)
  assignMetric('appOtherMode', appOtherMode)
  assignMetric('appDeployed', appDeployed)
  assignMetric('appDeployPending', appDeployPending)
  assignMetric('appDeployUnconfigured', appDeployUnconfigured)
  assignMetric('appWithDeployKey', appWithDeployKey)
  assignMetric('appFeatured', appFeatured)
  assignMetric('appRated', appRated)
  assignMetric('appRatedRegular', appRatedRegular)
  assignMetric('appWithCover', appWithCover)
  assignMetric('appWithPrompt', appWithPrompt)
  assignMetric('appCreatorTotal', creatorIds.size)
  assignMetric('appCreated7d', appCreated7d)
  assignMetric('appUpdated7d', appUpdated7d)
  assignMetric('appDeployed7d', appDeployed7d)
}

const loadDashboardMetrics = async () => {
  refreshing.value = true
  try {
    const [
      userRes, adminUserRes,
      appRes,
      chatRes,
      templateRes, templateEnabledRes,
      systemPromptRes,
      ragRes, ragEnabledRes,
      materialRes, materialPublicRes,
      categoryRes, enabledCategoryRes
    ] = await Promise.all([
      listUserVoByPage({ pageNum: 1, pageSize: 1 }),
      listUserVoByPage({ pageNum: 1, pageSize: 1, userRole: 'admin' }),
      listAppVoByPageByAdmin({ pageNum: 1, pageSize: 1 }),
      listAppChatHistoryByPageAdmin({ pageNum: 1, pageSize: 1 }),
      listPromptTemplateByPage({ pageNum: 1, pageSize: 1 }),
      listPromptTemplateByPage({ pageNum: 1, pageSize: 1, isEnabled: 1 }),
      listSystemPromptByPage({ pageNum: 1, pageSize: 1 }),
      listRagDocumentByPage({ pageNum: 1, pageSize: 1 }),
      listRagDocumentByPage({ pageNum: 1, pageSize: 1, isEnabled: 1 }),
      listAllMaterialByPage({ pageNum: 1, pageSize: 1 }),
      listAllMaterialByPage({ pageNum: 1, pageSize: 1, isPublic: 1 }),
      listMaterialCategoryByPage({ pageNum: 1, pageSize: 1 }),
      listEnabledCategories()
    ])

    assignMetric('userTotal', userRes.data.data?.totalRow)
    assignMetric('adminUserTotal', adminUserRes.data.data?.totalRow)
    assignMetric('appTotal', appRes.data.data?.totalRow)
    assignMetric('chatHistoryTotal', chatRes.data.data?.totalRow)
    assignMetric('promptTemplateTotal', templateRes.data.data?.totalRow)
    assignMetric('promptTemplateEnabled', templateEnabledRes.data.data?.totalRow)
    assignMetric('systemPromptTotal', systemPromptRes.data.data?.totalRow)
    assignMetric('ragDocTotal', ragRes.data.data?.totalRow)
    assignMetric('ragDocEnabled', ragEnabledRes.data.data?.totalRow)
    assignMetric('materialTotal', materialRes.data.data?.totalRow)
    assignMetric('materialPublic', materialPublicRes.data.data?.totalRow)
    assignMetric('categoryTotal', categoryRes.data.data?.totalRow)
    metrics.enabledCategoryTotal = enabledCategoryRes.data.data?.length || 0

    const appTotal = appRes.data.data?.totalRow || 0
    if (appTotal > 0) {
      const appRecordRes = await listAppVoByPageByAdmin({ pageNum: 1, pageSize: appTotal })
      analyzeAppRecords(appRecordRes.data.data?.records || [])
    } else {
      analyzeAppRecords([])
    }
  } catch (error) {
    console.error('加载系统管理控制台数据失败', error)
    message.error('加载系统管理控制台数据失败')
  } finally {
    refreshing.value = false
  }
}

const refreshDashboard = () => {
  loadDashboardMetrics()
}

const handleAdminNavClick = (item: (typeof adminNavItems)[number]) => {
  if (item.url) {
    window.open(item.url, '_blank')
    return
  }
  if (item.path) {
    router.push(item.path)
  }
}

onMounted(() => {
  loadDashboardMetrics()
})
</script>

<style lang="less" scoped>
.system-manage-dashboard {
  min-height: 100%;
  width: 100%;
  background: #f5f5f5;
}

.page-shell {
  padding: 32px;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.ghost-btn {
  height: 40px;
  border-radius: 12px;
  border-color: #d9d9d9;
  color: #333;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);

  &:hover {
    border-color: #999;
    color: #000;
    background: #fafafa;
  }
}

.top-health-panel {
  margin-bottom: 24px;
}

.section-block {
  margin-bottom: 24px;
  padding: 24px;
  border-radius: 20px;
  border: 1px solid #e8e8e8;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 19px;
    font-weight: 600;
    color: #1a1a1a;
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #999;
  }
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.module-card {
  width: 100%;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e8e8e8;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 188px;

  &:hover {
    border-color: #bbb;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }
}

.module-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.module-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-icon {
  font-size: 18px;
  color: #333;
}

.module-link {
  font-size: 12px;
  color: #999;
}

.module-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.module-title {
  font-size: 16px;
  line-height: 1.3;
  font-weight: 600;
  color: #1a1a1a;
}

.module-count-pill {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f0f0f0;
  color: #333;
  font-size: 12px;
  font-weight: 600;
}

.module-count-pill.neutral {
  background: #f0f0f0;
  color: #888;
}

.module-desc {
  min-height: 38px;
  font-size: 13px;
  line-height: 1.55;
  color: #888;
  margin-bottom: 14px;
}

.module-footer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  margin-top: auto;
}

.module-metric-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;

  span {
    font-size: 12px;
    color: #888;
  }

  strong {
    font-size: 13px;
    color: #222;
    font-weight: 600;
  }
}

.detail-section {
  padding: 24px;
  border-radius: 22px;
  border: 1px solid #e8e8e8;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.detail-section-header {
  margin-bottom: 18px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.analytics-span-2 {
  grid-column: span 2;
}

.analytics-card {
  border-radius: 20px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);

  :deep(.ant-card-head) {
    border-bottom: 1px solid #f0f0f0;
    min-height: 64px;
    background: #fafafa;
  }

  :deep(.ant-card-body) {
    padding: 0;
  }
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.card-title-tip {
  font-size: 12px;
  color: #888;
  font-weight: 400;
}

.detail-table {
  display: flex;
  flex-direction: column;
}

.detail-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 2fr;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;

  &:last-child {
    border-bottom: none;
  }
}

.detail-head {
  background: #fafafa;
  color: #888;
  font-size: 12px;
  letter-spacing: 0.02em;
}

.bar-chart-list,
.indicator-list,
.matrix-grid {
  padding: 20px 22px 22px;
}

.bar-item {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.bar-item-head,
.indicator-head,
.matrix-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;

  span {
    font-size: 13px;
    color: #666;
  }

  strong {
    font-size: 13px;
    color: #222;
    font-weight: 600;
  }
}

.bar-track,
.matrix-track {
  position: relative;
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #f0f0f0;
  overflow: hidden;
}

.bar-track.slim {
  height: 8px;
}

.bar-fill,
.matrix-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b8ff9 0%, #84aefc 100%);
}

.bar-fill.accent {
  background: linear-gradient(90deg, #6f8cff 0%, #9fb7f6 100%);
}

.donut-panel {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 22px;
}

.donut-chart {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}

.donut-inner {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}

.donut-total {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}

.donut-caption {
  font-size: 12px;
  color: #888;
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-content {
  display: flex;
  flex-direction: column;
  gap: 2px;

  span {
    font-size: 13px;
    color: #333;
  }

  small {
    font-size: 12px;
    color: #888;
  }
}

.legend-item strong {
  font-size: 13px;
  color: #222;
}

.indicator-item {
  margin-bottom: 18px;

  &:last-child {
    margin-bottom: 0;
  }
}

.indicator-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #888;
  line-height: 1.6;
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.matrix-item {
  padding: 14px 14px 12px;
  border-radius: 16px;
  border: 1px solid #eee;
  background: #fafafa;
}

.matrix-bottom {
  display: flex;
  align-items: center;
  gap: 12px;

  span {
    min-width: 48px;
    font-size: 12px;
    color: #888;
  }
}

@media (max-width: 1600px) {
  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .analytics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .matrix-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }

  .analytics-span-2 {
    grid-column: span 1;
  }

  .donut-panel,
  .matrix-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-shell {
    padding: 16px;
  }

  .page-header,
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }

  .detail-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .detail-section,
  .section-block,
  .page-header {
    padding: 18px;
  }

  .module-desc {
    min-height: auto;
  }

  .donut-chart {
    width: 144px;
    height: 144px;
  }

  .bar-chart-list,
  .indicator-list,
  .matrix-grid,
  .donut-panel {
    padding: 16px;
  }
}
</style>
