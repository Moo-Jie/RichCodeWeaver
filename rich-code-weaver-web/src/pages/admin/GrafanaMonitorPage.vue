<template>
  <div class="grafana-monitor-page">
    <div class="page-header">
      <h2>AI 模型系统监控</h2>
      <p class="page-description">嵌入 Grafana 仪表盘，实时查看 AI 模型请求量、Token 消耗、响应时间与错误率等核心指标。</p>
    </div>

    <AdminBackToDashboardButton class="page-back-entry" />

    <div class="iframe-container">
      <div v-if="loading" class="loading-overlay">
        <a-spin size="large" />
        <p>正在加载监控面板...</p>
      </div>

      <iframe
        ref="monitorIframe"
        :src="grafanaUrl"
        class="management-iframe"
        frameborder="0"
        allow="fullscreen"
        @load="handleIframeLoad"
        @error="handleIframeError"
      />

      <div v-if="error" class="error-overlay">
        <a-result
          status="error"
          title="加载失败"
          sub-title="无法连接到 Grafana 监控面板，请检查网络或服务状态。"
        >
          <template #extra>
            <a-button type="primary" @click="reloadIframe">
              重新加载
            </a-button>
            <a-button @click="openInNewTab">
              在新标签页中打开
            </a-button>
          </template>
        </a-result>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'

const baseGrafanaUrl = import.meta.env.VITE_GRAFANA_URL
  || 'https://rubyyan.cn/grafana/d/ai-model-monitoring/richcodeweaver'

const grafanaParams = new URLSearchParams({
  orgId: '1',
  from: 'now-7d',
  to: 'now',
  timezone: 'browser',
  'var-DS_PROMETHEUS': 'cfj5oyy7f74zkc',
  refresh: '5s',
  theme: 'light',  // 之前设置的浅色主题
  // 以下三选一，根据需求选择
  // kiosk: 'tv',     // 推荐：只隐藏左侧边栏，保留顶部菜单
  // kiosk: 'full', // 隐藏侧边栏，保留顶部菜单和下拉框
  kiosk: '',     // 隐藏侧边栏和顶部菜单（极简模式）
})

const grafanaUrl = `${baseGrafanaUrl}?${grafanaParams.toString()}`

const loading = ref(true)
const error = ref(false)
const monitorIframe = ref<HTMLIFrameElement>()

const handleIframeLoad = () => {
  loading.value = false
  error.value = false
  message.success('监控面板加载成功')
}

const handleIframeError = () => {
  loading.value = false
  error.value = true
  message.error('监控面板加载失败')
}

const reloadIframe = () => {
  if (monitorIframe.value) {
    loading.value = true
    error.value = false
    monitorIframe.value.src = grafanaUrl
  }
}

const openInNewTab = () => {
  const externalUrl = grafanaUrl.replace('&kiosk=', '')
  window.open(externalUrl, '_blank')
}

onMounted(() => {
  setTimeout(() => {
    if (loading.value) {
      handleIframeError()
    }
  }, 15000)
})
</script>

<style scoped>
.grafana-monitor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.page-header {
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.page-back-entry {
  padding: 0 24px;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.iframe-container {
  flex: 1;
  position: relative;
  margin: 16px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.management-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 10;
}

.loading-overlay p {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}

.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 10;
}
</style>
