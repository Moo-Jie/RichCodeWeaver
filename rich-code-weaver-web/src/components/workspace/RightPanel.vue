<template>
  <transition name="panel-slide">
    <aside v-if="visible" class="right-panel">
      <div class="panel-header">
        <span class="panel-title">操作面板</span>
        <button class="panel-close" @click="$emit('toggle')">✕</button>
      </div>

      <div class="panel-body">
        <!-- App Info -->
        <div v-if="app" class="panel-section panel-section-hero">
          <div class="app-brief">
            <img v-if="app.cover" :src="app.cover" alt="" class="brief-cover" />
            <img v-else alt="" class="brief-cover brief-cover--default" src="@/assets/logo.png" />
            <div class="brief-info">
              <div class="brief-topline">
                <span class="brief-name">{{ app.appName || '未命名数字产物' }}</span>
                <span v-if="ownershipLabel" :class="['brief-pill', `brief-pill--${app.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
                  {{ ownershipLabel }}
                </span>
              </div>
              <span class="brief-type">{{ typeLabel }}</span>
              <span class="brief-meta">{{ deployLabel }} · {{ collaboratorSummary }}</span>
            </div>
          </div>
        </div>

        <!-- App Mode Actions -->
        <div v-if="mode === 'app'" class="panel-section">
          <div class="section-label">数字产物操作</div>
          <button :disabled="!canEditApp" class="panel-btn" @click="$emit('toggleEdit')">
            <EditOutlined />
            <span>{{ canEditApp ? (isEditMode ? '退出编辑' : '可视化编辑') : '暂无编辑权限' }}</span>
          </button>
          <button v-if="previewUrl" class="panel-btn" @click="$emit('previewFullscreen')">
            <ExpandOutlined />
            <span>全屏预览</span>
          </button>
          <button :disabled="isGenerating" class="panel-btn" @click="$emit('download')">
            <DownloadOutlined />
            <span>{{ downloading ? '下载中...' : '下载代码' }}</span>
          </button>
          <button
            v-if="app?.codeGenType === 'vue_project'"
            :disabled="isGenerating || deploying || refreshing"
            class="panel-btn"
            @click="$emit('refreshApp')"
          >
            <ReloadOutlined />
            <span>{{ refreshing ? '刷新中...' : '刷新产物' }}</span>
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

        <!-- Social Actions -->
        <div class="panel-section">
          <div class="section-label">社交互动</div>
          <button
            :class="['panel-btn', { 'panel-btn--active': hotStat?.hasLiked }]"
            @click="$emit('toggleLike')"
          >
            <LikeFilled v-if="hotStat?.hasLiked" />
            <LikeOutlined v-else />
            <span>点赞</span>
            <span class="btn-count">{{ hotStat?.likeCount || 0 }}</span>
          </button>
          <button
            :class="['panel-btn', { 'panel-btn--active': hotStat?.hasFavorited }]"
            @click="$emit('toggleFavorite')"
          >
            <StarFilled v-if="hotStat?.hasFavorited" />
            <StarOutlined v-else />
            <span>收藏</span>
            <span class="btn-count">{{ hotStat?.favoriteCount || 0 }}</span>
          </button>
          <button class="panel-btn" @click="$emit('doShare')">
            <ShareAltOutlined />
            <span>转发</span>
            <span class="btn-count">{{ hotStat?.shareCount || 0 }}</span>
          </button>
          <button v-if="mode === 'app'" class="panel-btn" @click="$emit('openComment')">
            <CommentOutlined />
            <span>评论</span>
            <span class="btn-count">{{ hotStat?.commentCount || 0 }}</span>
          </button>
        </div>

        <!-- Collaboration Actions -->
        <div v-if="isOwner" class="panel-section">
          <div class="section-label">团队协作</div>
          <button class="panel-btn" @click="$emit('inviteCollab')">
            <TeamOutlined />
            <span>邀请协作</span>
          </button>
        </div>

        <!-- Common Actions -->
        <div class="panel-section">
          <div class="section-label">通用操作</div>
          <button v-if="app" class="panel-btn" @click="$emit('showDetail')">
            <InfoCircleOutlined />
            <span>产物详情</span>
          </button>
          <button v-if="isDeployed" class="panel-btn" @click="$emit('visitSite')">
            <ExportOutlined />
            <span>访问网站</span>
          </button>
        </div>

        <div v-if="app" class="panel-section panel-section-bottom">
          <AppTeamCard :app="app" :collaborators="collaborators" compact />
        </div>
      </div>
    </aside>
  </transition>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import {
  CloudUploadOutlined,
  CommentOutlined,
  DownloadOutlined,
  EditOutlined,
  ExpandOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  LikeFilled,
  LikeOutlined,
  ReloadOutlined,
  ShareAltOutlined,
  StarFilled,
  StarOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import AppTeamCard from '@/components/AppTeamCard.vue'

interface Props {
  visible: boolean
  mode: 'chat' | 'app'
  app?: API.AppVO | null
  collaborators?: API.AppCollaboratorVO[]
  isOwner: boolean
  isAdmin: boolean
  canEditApp?: boolean
  isDeployed: boolean
  isGenerating: boolean
  previewUrl: string
  deploying: boolean
  downloading: boolean
  refreshing: boolean
  isEditMode?: boolean
  hotStat?: API.AppHotStatVO | null
}

const props = withDefaults(defineProps<Props>(), {
  isEditMode: false,
  canEditApp: false,
  collaborators: () => []
})

defineEmits([
  'toggle', 'showDetail', 'previewFullscreen', 'download',
  'deploy', 'reDeploy', 'refreshApp', 'visitSite', 'toggleEdit',
  'toggleLike', 'toggleFavorite', 'doShare', 'openComment',
  'inviteCollab'
])

const typeLabel = computed(() => {
  const t = props.app?.codeGenType
  if (t === 'single_html') return '单文件结构'
  if (t === 'multi_file') return '多文件结构'
  if (t === 'vue_project') return 'VUE 项目工程'
  return t || '未知类型'
})

const ownershipLabel = computed(() => {
  if (props.app?.ownershipType === 'mine') return '我的'
  if (props.app?.ownershipType === 'collaborator') return '协作'
  return ''
})

const deployLabel = computed(() => props.isDeployed ? '已部署网站' : '未部署网站')

const collaboratorSummary = computed(() => {
  const count = props.collaborators.length
  return count > 0 ? `${count} 位协作者` : '暂无协作者'
})
</script>

<style scoped>
.right-panel {
  width: 280px;
  height: 100%;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfc 100%);
  border-left: 1px solid #eceff3;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #eef1f4;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.panel-close {
  background: #f6f8fa;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  color: #6b7280;
  font-size: 14px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  transition: all 0.18s ease;
}

.panel-close:hover {
  background: #ffffff;
  color: #24292f;
  border-color: #d0d7de;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.panel-body {
  padding: 14px;
  flex: 1;
}

.panel-section {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #edf1f4;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.02);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.panel-section:hover {
  transform: translateY(-1px);
  border-color: #dfe6ee;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
}

.panel-section-hero {
  padding: 0;
  overflow: hidden;
}

.section-label {
  font-size: 11px;
  color: #8c959f;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0 2px 8px;
  font-weight: 600;
}

/* App Brief */
.app-brief {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
}

.brief-cover {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}

.brief-cover--default {
  opacity: 0.7;
}

.brief-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.brief-topline {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.brief-name {
  font-size: 14px;
  font-weight: 600;
  color: #24292f;
  line-height: 1.4;
}

.brief-type {
  font-size: 11px;
  color: #57606a;
}

.brief-meta {
  font-size: 11px;
  color: #8c959f;
}

.brief-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
}

.brief-pill--mine {
  background: #eef6ff;
  border-color: #c6ddff;
  color: #245ea6;
}

.brief-pill--collaborator {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

/* Panel Buttons */
.panel-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 12px;
  margin-bottom: 6px;
  background: #ffffff;
  border: 1px solid #e6ebf1;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  color: #4b5563;
  transition: all 0.18s ease;
}

.panel-btn:hover:not(:disabled) {
  background: #ffffff;
  color: #1f2937;
  border-color: #d0d7de;
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.06);
}

.panel-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.panel-btn--active {
  color: #111827;
  font-weight: 500;
  background: #f6f8fa;
  border-color: #d8dee4;
}

.btn-count {
  margin-left: auto;
  font-size: 12px;
  color: #8c959f;
  font-weight: 500;
}

.panel-section-bottom {
  margin-top: 16px;
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

.panel-section-bottom:hover {
  transform: none;
  box-shadow: none;
}

/* Panel Slide Transition */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  width: 0;
  opacity: 0;
  overflow: hidden;
}

@media (max-width: 1440px) {
  .right-panel {
    width: 256px;
  }
}
</style>
