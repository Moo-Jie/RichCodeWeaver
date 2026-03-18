<template>
  <transition name="panel-slide">
    <aside v-if="visible" class="right-panel">
      <div class="panel-header">
        <span class="panel-title">操作面板</span>
        <button class="panel-close" @click="$emit('toggle')">✕</button>
      </div>

      <div class="panel-body">
        <!-- App Info -->
        <div v-if="app" class="panel-section">
          <div class="app-brief">
            <img v-if="app.cover" :src="app.cover" alt="" class="brief-cover" />
            <img v-else alt="" class="brief-cover brief-cover--default" src="@/assets/logo.png" />
            <div class="brief-info">
              <span class="brief-name">{{ app.appName || '未命名应用' }}</span>
              <span class="brief-type">{{ typeLabel }}</span>
            </div>
          </div>
        </div>

        <!-- App Mode Actions -->
        <div v-if="mode === 'app'" class="panel-section">
          <div class="section-label">应用操作</div>
          <button class="panel-btn" @click="$emit('toggleEdit')">
            <EditOutlined />
            <span>{{ isEditMode ? '退出编辑' : '可视化编辑' }}</span>
          </button>
          <button v-if="previewUrl" class="panel-btn" @click="$emit('previewFullscreen')">
            <ExpandOutlined />
            <span>全屏预览</span>
          </button>
          <button :disabled="isGenerating" class="panel-btn" @click="$emit('download')">
            <DownloadOutlined />
            <span>{{ downloading ? '下载中...' : '下载代码' }}</span>
          </button>
          <template v-if="isDeployed">
            <button :disabled="isGenerating" class="panel-btn" @click="$emit('reDeploy')">
              <CloudUploadOutlined />
              <span>{{ deploying ? '部署中...' : '重新部署' }}</span>
            </button>
          </template>
          <button v-else :disabled="isGenerating" class="panel-btn" @click="$emit('deploy')">
            <CloudUploadOutlined />
            <span>{{ deploying ? '部署中...' : '部署网站' }}</span>
          </button>
        </div>

        <!-- Common Actions -->
        <div class="panel-section">
          <div class="section-label">通用操作</div>
          <button v-if="isOwner || isAdmin" class="panel-btn" @click="$emit('showDetail')">
            <InfoCircleOutlined />
            <span>应用详情</span>
          </button>
          <button v-if="isDeployed" class="panel-btn" @click="$emit('visitSite')">
            <ExportOutlined />
            <span>访问网站</span>
          </button>
        </div>
      </div>
    </aside>
  </transition>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExpandOutlined,
  ExportOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'

interface Props {
  visible: boolean
  mode: 'chat' | 'app'
  app?: API.AppVO | null
  isOwner: boolean
  isAdmin: boolean
  isDeployed: boolean
  isGenerating: boolean
  previewUrl: string
  deploying: boolean
  downloading: boolean
  isEditMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isEditMode: false
})

defineEmits([
  'toggle', 'showDetail', 'previewFullscreen', 'download',
  'deploy', 'reDeploy', 'visitSite', 'toggleEdit'
])

const typeLabel = computed(() => {
  const t = props.app?.codeGenType
  if (t === 'single_html') return '单文件结构'
  if (t === 'multi_file') return '多文件结构'
  if (t === 'vue_project') return 'VUE 项目工程'
  return t || '未知类型'
})
</script>

<style scoped>
.right-panel {
  width: 220px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.panel-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #bbb;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.15s;
}

.panel-close:hover {
  background: #f5f5f5;
  color: #666;
}

.panel-body {
  padding: 8px 12px;
  flex: 1;
}

.panel-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 11px;
  color: #bbb;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 0 4px 8px;
  font-weight: 500;
}

/* App Brief */
.app-brief {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fafafa;
  border-radius: 10px;
  margin-bottom: 4px;
}

.brief-cover {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.brief-cover--default {
  opacity: 0.5;
}

.brief-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brief-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brief-type {
  font-size: 11px;
  color: #999;
}

/* Panel Buttons */
.panel-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  margin-bottom: 4px;
  background: none;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: all 0.15s ease;
}

.panel-btn:hover:not(:disabled) {
  background: #f5f5f5;
  color: #1a1a1a;
}

.panel-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Panel Slide Transition */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  width: 0;
  opacity: 0;
  overflow: hidden;
}
</style>
