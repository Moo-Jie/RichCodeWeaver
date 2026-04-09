<template>
  <a-modal
    v-model:open="visible"
    :footer="null"
    class="app-detail-modal"
    title="产物详情"
    width="600px"
  >
    <div class="app-detail-content">
      <div class="detail-hero">
        <img v-if="app?.cover" :src="app.cover" alt="数字产物封面" class="detail-cover" />
        <div v-else class="detail-cover detail-cover--placeholder">
          <AppstoreOutlined />
        </div>
        <div class="detail-hero-body">
          <div class="detail-hero-topline">
            <h3 class="detail-title">{{ app?.appName || '未命名数字产物' }}</h3>
            <span v-if="ownershipLabel" :class="['detail-pill', `detail-pill--${app?.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
              {{ ownershipLabel }}
            </span>
          </div>
          <div class="detail-hero-meta">
            <span>{{ formatCodeGenType(app?.codeGenType) || '未知类型' }}</span>
            <span>·</span>
            <span>{{ collaborators.length }} 位协作者</span>
          </div>
          <div class="detail-hero-status">
            <span :class="['detail-pill', app?.deployKey ? 'detail-pill--success' : 'detail-pill--muted']">
              {{ app?.deployKey ? '已部署' : '未部署' }}
            </span>
            <span class="detail-id">ID {{ app?.id || '--' }}</span>
          </div>
        </div>
      </div>

      <!-- 基础信息 -->
      <a-descriptions :column="1" class="meta-grid">
        <a-descriptions-item label="数字产物ID">
          <div class="meta-item">
            <span class="inline-pill">{{ app?.id || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="数字产物名称">
          <div class="meta-item">
            <AppstoreOutlined />
            <span class="app-name">{{ app?.appName || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="归属类型">
          <div class="meta-item">
            <span v-if="ownershipLabel" :class="['detail-pill', `detail-pill--${app?.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
              {{ ownershipLabel }}
            </span>
            <span v-else>--</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="创建者">
          <div class="meta-item">
            <UserOutlined />
            <UserInfo :user="app?.user" size="small" />
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="用户ID">
          <div class="meta-item">
            <IdcardOutlined />
            <span>{{ app?.userId || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="创建时间">
          <div class="meta-item">
            <CalendarOutlined />
            <span>{{ formatTime(app?.createTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="更新时间">
          <div class="meta-item">
            <SyncOutlined />
            <span>{{ formatTime(app?.updateTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署状态">
          <div class="meta-item">
            <CloudServerOutlined />
            <span :class="['detail-pill', app?.deployKey ? 'detail-pill--success' : 'detail-pill--muted']">
              {{ app?.deployKey ? '已部署' : '未部署' }}
            </span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署时间">
          <div class="meta-item">
            <CloudServerOutlined />
            <template v-if="app?.deployKey">
              <span>{{ formatTime(app.deployedTime) }}</span>
            </template>
            <span v-else>--</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="生成类型">
          <div class="meta-item">
            <a-tag :color="getTypeColor(app?.codeGenType)">
              {{
              app?.codeGenType === 'single_html' ? '单文件结构' :
              app?.codeGenType === 'multi_file' ? '多文件结构' :
              app?.codeGenType === 'vue_project' ? 'VUE 项目工程' :
              formatCodeGenType(app?.codeGenType)
              }}
            </a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="数字产物封面">
          <div class="meta-item">
            <PaperClipOutlined />
            <img v-if="app?.cover" :src="app.cover" alt="数字产物封面" class="cover-img" />
            <span v-else>--</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="初始提示词">
          <div class="meta-item">
            <MessageOutlined />
            <a-typography-paragraph :ellipsis="{ rows: 3, expandable: true }">
              {{ app?.initPrompt || '--' }}
            </a-typography-paragraph>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="优先级">
          <div class="meta-item">
            <FlagOutlined />
            <a-tag :color="getPriorityColor(app?.priority)">{{ app?.priority || '--' }}</a-tag>
          </div>
        </a-descriptions-item>
      </a-descriptions>

      <AppTeamCard :app="app" :collaborators="collaborators" class="team-card-wrap" />

      <!-- 操作栏 -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button
            class="edit-btn"
            type="primary"
            @click="handleEdit"
          >
            <template #icon>
              <EditOutlined />
            </template>
            前往完善数字产物信息
          </a-button>
          <a-popconfirm
            cancel-text="取消"
            ok-text="确定"
            title="警告：确定要删除这个数字产物吗？（删除后将无法恢复）"
            @confirm="handleDelete"
          >
            <a-button class="delete-btn" danger>
              <template #icon>
                <DeleteOutlined />
              </template>
              删除当前数字产物
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import {
  AppstoreOutlined,
  CalendarOutlined,
  CloudServerOutlined,
  DeleteOutlined,
  EditOutlined,
  FlagOutlined,
  IdcardOutlined,
  MessageOutlined,
  PaperClipOutlined,
  SyncOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import AppTeamCard from './AppTeamCard.vue'
import { formatTime } from '../utils/timeUtil'
import { formatCodeGenType } from '../enums/codeGenTypes'

type AppVO = API.AppVO

interface Props {
  open: boolean
  app?: AppVO
  collaborators?: API.AppCollaboratorVO[]
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void

  (e: 'edit'): void

  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  collaborators: () => [],
  showActions: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const ownershipLabel = computed(() => {
  if (props.app?.ownershipType === 'mine') return '我的'
  if (props.app?.ownershipType === 'collaborator') return '协作'
  return ''
})

const getPriorityColor = (priority?: number) => {
  if (!priority) return 'default'
  switch (priority) {
    case 1:
      return 'green'
    case 2:
      return 'blue'
    case 3:
      return 'orange'
    case 4:
      return 'red'
    default:
      return 'default'
  }
}

const getTypeColor = (type?: string) => {
  if (!type) return 'blue'
  const colors: Record<string, string> = {
    'react': '#61dafb',
    'vue': '#42b883',
    'angular': '#dd0031',
    'html': '#e34c26',
    'nodejs': '#68a063',
    'flutter': '#04599C',
    'swift': '#ff2d55'
  }
  return colors[type] || 'blue'
}

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style lang="less" scoped>
.app-detail-modal {
  :deep(.ant-modal-content) {
    background: #fff;
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
    border: 1px solid #e8edf2;
  }

  :deep(.ant-modal-header) {
    background: #fff;
    border-bottom: 1px solid #eef2f5;
    border-radius: 20px 20px 0 0;
    padding: 18px 24px;
  }

  :deep(.ant-modal-title) {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
  }

  :deep(.ant-modal-body) {
    padding: 0;
  }
}

.app-detail-content {
  padding: 22px 24px 24px;
}

.detail-hero {
  display: flex;
  gap: 18px;
  padding: 16px;
  margin-bottom: 18px;
  border: 1px solid #e8edf2;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f7f9fb 100%);
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.02);
}

.detail-cover {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.detail-cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #64748b;
  font-size: 28px;
}

.detail-hero-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.detail-hero-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 700;
  color: #24292f;
}

.detail-hero-meta,
.detail-hero-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #656d76;
}

.detail-id {
  color: #8c959f;
}

.meta-grid {
  background: #fbfcfd;
  border-radius: 18px;
  padding: 18px 20px;
  border: 1px solid #e8edf2;
  margin-bottom: 18px;
}

:deep(.ant-descriptions-item) {
  padding-bottom: 14px;
  border-bottom: 1px solid #edf1f4;
}

:deep(.ant-descriptions-item:last-child) {
  border-bottom: none;
  padding-bottom: 0;
}

:deep(.ant-descriptions-item-label) {
  width: 100px;
  color: #8c959f;
  font-weight: 600;
  padding-right: 16px;
  vertical-align: top;
  font-size: 13px;
}

:deep(.ant-descriptions-item-content) {
  font-weight: 400;
  color: #1a1a1a;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 3px 0;

  .anticon {
    font-size: 14px;
    color: #6b7280;
    flex-shrink: 0;
  }
}

.app-name {
  font-weight: 500;
  color: #1a1a1a;
}

.cover-img {
  max-width: 120px;
  height: auto;
  border-radius: 12px;
  border: 1px solid #e6ebf1;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.detail-pill,
.inline-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
  font-size: 11px;
  font-weight: 600;
}

.detail-pill--mine {
  background: #eef6ff;
  border-color: #c6ddff;
  color: #245ea6;
}

.detail-pill--collaborator {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

.detail-pill--success {
  background: #ecfdf3;
  border-color: #b7ebc6;
  color: #137333;
}

.detail-pill--muted {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

.app-actions {
  padding: 22px 0 0;
  border-top: 1px solid #eef2f5;
  display: flex;
  justify-content: flex-start;
  gap: 10px;
}

.team-card-wrap {
  margin: 0 0 18px;
}

.edit-btn {
  background: #1f2328;
  border: 1px solid #1f2328;
  color: #fff;
  border-radius: 12px;
  height: 40px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s ease;
  padding: 0 20px;

  &:hover {
    background: #2f363d;
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
  }

  &:active {
    transform: translateY(0);
  }
}

.delete-btn {
  border-radius: 12px;
  height: 40px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s ease;
  border-color: #f1c0c0;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(239, 68, 68, 0.12);
  }

  &:active {
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .detail-hero {
    flex-direction: column;
  }

  .app-actions {
    justify-content: stretch;
  }
}
</style>
