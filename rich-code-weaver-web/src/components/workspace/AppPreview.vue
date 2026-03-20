<template>
  <div class="app-preview">
    <!-- Generating state -->
    <div v-if="isGenerating && !previewUrl" class="preview-loading">
      <a-spin size="large" />
      <div class="loading-info">
        <p class="loading-tip">支持断线重连，刷新页面后可继续接收生成内容</p>
        <p class="loading-time">已思考时间: {{ generatingTime }}秒</p>
        <p class="loading-sub">为了生成美观完善的页面，请耐心等待...</p>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!previewUrl" class="preview-empty">
      <div class="empty-icon">
        <div class="dot dot-1"></div>
        <div class="dot dot-2"></div>
        <div class="dot dot-3"></div>
      </div>
      <p class="empty-text">灵感火花即将绽放</p>
      <p class="empty-sub">描述您的创意，AI 将为您编织网站</p>
    </div>

    <!-- Preview iframe -->
    <iframe
      v-else
      ref="iframeRef"
      :src="previewUrl"
      class="preview-iframe"
      frameborder="0"
      @load="$emit('iframeLoad', $event)"
    ></iframe>

    <!-- Tips toggle -->
    <div v-if="previewUrl" class="preview-tips">
      <button class="tips-btn" @click="showTips = !showTips">
        <span>查看提示</span>
        <span :class="['arrow', { expanded: showTips }]">▾</span>
      </button>
      <transition name="slide">
        <div v-if="showTips" class="tips-content">
          <p>1. 预览为静态页面效果，部署后可体验完整功能</p>
          <p>2. 数字产物封面将在部署后自动生成，请在部署后静待 1-5s</p>
          <p>3. 若未生成预览页面请刷新页面</p>
          <p>4. 请勿频繁部署，若违反则系统自动封号处理</p>
        </div>
      </transition>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

interface Props {
  previewUrl: string
  isGenerating: boolean
  generatingTime: number
}

defineProps<Props>()
defineEmits(['iframeLoad'])

const iframeRef = ref<HTMLIFrameElement>()
const showTips = ref(false)

defineExpose({
  getIframe: () => iframeRef.value,
  refresh: () => {
    if (iframeRef.value && iframeRef.value.src) {
      iframeRef.value.src = iframeRef.value.src
    }
  }
})
</script>

<style scoped>
.app-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  min-height: 0;
}

.preview-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 40px;
}

.loading-info {
  text-align: center;
}

.loading-tip {
  color: #e8a85a;
  font-size: 13px;
  margin: 0 0 8px;
}

.loading-time {
  color: #999;
  font-size: 14px;
  margin: 0 0 4px;
}

.loading-sub {
  color: #bbb;
  font-size: 13px;
  margin: 0;
}

.preview-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.empty-icon {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.dot {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
  animation: float 4s infinite ease-in-out;
}

.dot-1 {
  width: 40px;
  height: 40px;
  background: #e0e0e0;
  top: 0;
  left: 20px;
}

.dot-2 {
  width: 30px;
  height: 30px;
  background: #d5d5d5;
  bottom: 5px;
  left: 5px;
  animation-delay: 0.5s;
}

.dot-3 {
  width: 25px;
  height: 25px;
  background: #ccc;
  bottom: 10px;
  right: 5px;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-text {
  font-size: 16px;
  font-weight: 500;
  color: #999;
  margin: 0 0 6px;
}

.empty-sub {
  font-size: 14px;
  color: #ccc;
  margin: 0;
}

.preview-iframe {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

.preview-tips {
  border-top: 1px solid #f0f0f0;
  padding: 0 16px;
  flex-shrink: 0;
}

.tips-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 0;
  background: none;
  border: none;
  cursor: pointer;
  color: #606060;
  font-size: 13px;
  transition: color 0.15s;
}

.tips-btn:hover {
  color: #2e2e2e;
}

.arrow {
  transition: transform 0.2s ease;
  font-size: 12px;
}

.arrow.expanded {
  transform: rotate(180deg);
}

.tips-content {
  padding: 0 0 12px;
}

.tips-content p {
  margin: 0 0 4px;
  font-size: 12px;
  color: #616161;
  line-height: 1.6;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
  max-height: 120px;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
