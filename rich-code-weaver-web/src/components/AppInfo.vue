<template>
  <a-modal
    v-model:open="visible"
    :footer="null"
    class="app-detail-modal"
    title="应用详情"
    width="600px"
  >
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
            <AppstoreOutlined/>
            <span class="app-name">{{ app?.appName || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="创建者">
          <div class="meta-item">
            <UserOutlined/>
            <UserInfo :user="app?.user" size="small"/>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="用户ID">
          <div class="meta-item">
            <IdcardOutlined/>
            <span>{{ app?.userId || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="创建时间">
          <div class="meta-item">
            <CalendarOutlined/>
            <span>{{ formatTime(app?.createTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="更新时间">
          <div class="meta-item">
            <SyncOutlined/>
            <span>{{ formatTime(app?.updateTime) || '--' }}</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署状态">
          <div class="meta-item">
            <CloudServerOutlined/>
            <template v-if="app?.deployKey">
              <a-tag color="green">已部署</a-tag>
            </template>
            <a-tag v-else color="red">未部署</a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署时间">
          <div class="meta-item">
            <CloudServerOutlined/>
            <template v-if="app?.deployKey">
              <span>{{ formatTime(app.deployedTime) }}</span>
            </template>
            <a-tag v-else color="red"> --</a-tag>
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

        <a-descriptions-item label="应用封面">
          <div class="meta-item">
            <PictureOutlined/>
            <img v-if="app?.cover" :src="app.cover" alt="应用封面" class="cover-img"/>
            <span v-else>--</span>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="初始提示词">
          <div class="meta-item">
            <MessageOutlined/>
            <a-typography-paragraph :ellipsis="{ rows: 3, expandable: true }">
              {{ app?.initPrompt || '--' }}
            </a-typography-paragraph>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="优先级">
          <div class="meta-item">
            <FlagOutlined/>
            <a-tag :color="getPriorityColor(app?.priority)">{{ app?.priority || '--' }}</a-tag>
          </div>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 操作栏 -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button
            class="edit-btn"
            type="primary"
            @click="handleEdit"
          >
            <template #icon>
              <EditOutlined/>
            </template>
            前往完善应用信息
          </a-button>
          <a-popconfirm
            cancel-text="取消"
            ok-text="确定"
            title="警告：确定要删除这个应用吗？（删除后将无法恢复）"
            @confirm="handleDelete"
          >
            <a-button class="delete-btn" danger>
              <template #icon>
                <DeleteOutlined/>
              </template>
              删除当前应用
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
  PictureOutlined,
  SyncOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import { formatTime } from '../utils/timeUtil.ts'
import { formatCodeGenType } from '../enums/codeGenTypes.ts'
import AppVO = API.AppVO

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

const props = withDefaults(defineProps<Props>(), {
  showActions: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
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
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid #f0f0f0;
  }

  :deep(.ant-modal-header) {
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    border-radius: 12px 12px 0 0;
    padding: 16px 24px;
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
  padding: 20px 24px;
}

.meta-grid {
  background: #fafafa;
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

:deep(.ant-descriptions-item) {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-descriptions-item:last-child) {
  border-bottom: none;
  padding-bottom: 0;
}

:deep(.ant-descriptions-item-label) {
  width: 100px;
  color: #999;
  font-weight: 500;
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
  gap: 8px;
  padding: 2px 0;

  .anticon {
    font-size: 14px;
    color: #666;
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
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.app-actions {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.edit-btn {
  background: #1a1a1a;
  border: none;
  color: #fff;
  border-radius: 10px;
  height: 36px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
  padding: 0 20px;

  &:hover {
    background: #333;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  }

  &:active {
    transform: translateY(0);
  }
}

.delete-btn {
  border-radius: 10px;
  height: 36px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
  border-color: #ff4d4f;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 77, 79, 0.15);
  }

  &:active {
    transform: translateY(0);
  }
}
</style>
