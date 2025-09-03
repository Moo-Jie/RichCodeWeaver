<template>
  <a-modal v-model:open="visible" title="应用详情" :footer="null" width="500px">
    <div class="app-detail-content">
      <!-- 基础信息 -->
      <a-descriptions :column="1" class="meta-grid">
        <a-descriptions-item label="应用ID">
          <div class="meta-item">
            <a-tag color="blue">{{ app?.id || '--' }}</a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="应用名称">
          <div class="meta-item">
            <appstore-outlined />
            <span>{{ app?.appName?.substring(0, 20)+'...' || '--' }}</span>
          </div>
        </a-descriptions-item>


        <a-descriptions-item label="创建者">
          <div class="meta-item">
            <user-outlined />
            <UserInfo :user="app?.user" size="small" />
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="用户ID">
          <div class="meta-item">
            <idcard-outlined />
            <span>{{ app?.userId || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="创建时间">
          <div class="meta-item">
            <calendar-outlined />
            <span>{{ formatTime(app?.createTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="更新时间">
          <div class="meta-item">
            <sync-outlined />
            <span>{{ formatTime(app?.updateTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署状态">
          <div class="meta-item">
            <cloud-server-outlined />
            <template v-if="app?.deployKey">
              <a-tag color="green">已部署</a-tag>
            </template>
            <a-tag v-else color="red">未部署</a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署时间">
          <div class="meta-item">
            <cloud-server-outlined />
            <template v-if="app?.deployKey">
              <span>{{ formatTime(app.deployedTime) }}</span>
            </template>
            <a-tag v-else color="red"> --</a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="生成类型">
          <div class="meta-item">
            <a-tag :color="getTypeColor(app?.codeGenType)">
              {{ formatCodeGenType(app?.codeGenType) }}
            </a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="应用封面">
          <div class="meta-item">
            <picture-outlined />
            <img v-if="app?.cover" :src="app.cover" alt="应用封面" class="cover-img" />
            <span v-else>--</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="初始提示">
          <div class="meta-item">
            <message-outlined />
            <a-typography-paragraph :ellipsis="{ rows: 3, expandable: true }">
              {{ app?.initPrompt || '--' }}
            </a-typography-paragraph>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="优先级">
          <div class="meta-item">
            <flag-outlined />
            <a-tag :color="getPriorityColor(app?.priority)">{{ app?.priority || '--' }}</a-tag>
          </div>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 操作栏 -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button @click="handleEdit">
            <template #icon>
              <EditOutlined />
            </template>
            前往完善应用信息
          </a-button>
          <a-popconfirm
            title="警告：确定要删除这个应用吗？（删除后将无法恢复）"
            @confirm="handleDelete"
            ok-text="确定"
            cancel-text="取消"
          >
            <a-button danger>
              <template #icon>
                <DeleteOutlined />
              </template>
              删除当前应用（谨慎选择）
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  CalendarOutlined,
  SyncOutlined,
  CloudServerOutlined,
  AppstoreOutlined,
  PictureOutlined,
  MessageOutlined,
  FlagOutlined,
  IdcardOutlined
} from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import AppVO = API.AppVO
import { formatTime } from '../utils/timeUtil.ts'
import { formatCodeGenType } from '../enums/codeGenTypes.ts'

interface Props {
  open: boolean
  app?: AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void

  (e: 'edit'): void

  (e: 'delete'): void
}

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

const props = withDefaults(defineProps<Props>(), {
  showActions: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

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

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.meta-grid {
  padding: 15px;
  background: rgba(255, 253, 248, 0.92);
  border-radius: 8px;
  border: 1px solid rgba(198, 180, 165, 0.15);
  box-shadow: 0 3px 10px rgba(155, 140, 125, 0.08);
  margin-bottom: 20px;
}

:deep(.ant-descriptions-item) {
  padding-bottom: 16px;
  border-bottom: 1px solid #f6f3ee;
}

:deep(.ant-descriptions-item-label) {
  width: 90px;
  color: #7a787c;
  font-weight: 500;
  padding-right: 16px;
  vertical-align: top;
}

:deep(.ant-descriptions-item-content) {
  font-weight: 500;
  color: #5c4a48;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 3px 0;

  .anticon {
    font-size: 16px;
    color: #c6a08a;
  }
}

.cover-img {
  max-width: 120px;
  height: auto;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.app-actions {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}
</style>
