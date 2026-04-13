<template>
  <div class="higress-manage-page">
    <div class="page-header">
      <h2>Higress AI 网关管理</h2>
      <p class="page-description">管理Higress AI网关配置</p>
    </div>

    <AdminBackToDashboardButton class="page-back-entry" />

    <div class="iframe-container">
      <div v-if="loading" class="loading-overlay">
        <a-spin size="large" />
        <p>正在加载Higress管理界面...</p>
      </div>

      <iframe
        ref="higressIframe"
        :src="higressUrl"
        class="management-iframe"
        frameborder="0"
        @load="handleIframeLoad"
        @error="handleIframeError"
      />

      <div v-if="error" class="error-overlay">
        <a-result
          status="error"
          title="加载失败"
          :sub-title="`无法连接到Higress管理界面: ${higressUrl}`"
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
import {onMounted, ref} from 'vue'
import {message} from 'ant-design-vue'
import AdminBackToDashboardButton from '@/components/admin/AdminBackToDashboardButton.vue'

const higressUrl = import.meta.env.VITE_HIGRESS_URL || 'http://localhost:8001'
const loading = ref(true)
const error = ref(false)
const higressIframe = ref<HTMLIFrameElement>()

const handleIframeLoad = () => {
  loading.value = false
  error.value = false
  message.success('Higress管理界面加载成功')
}

const handleIframeError = () => {
  loading.value = false
  error.value = true
  message.error('Higress管理界面加载失败')
}

const reloadIframe = () => {
  if (higressIframe.value) {
    loading.value = true
    error.value = false
    higressIframe.value.src = higressUrl
  }
}

const openInNewTab = () => {
  window.open(higressUrl, '_blank')
}

onMounted(() => {
  // Set a timeout to show error if iframe doesn't load within 10 seconds
  setTimeout(() => {
    if (loading.value) {
      handleIframeError()
    }
  }, 10000)
})
</script>

<style scoped>
.higress-manage-page {
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
