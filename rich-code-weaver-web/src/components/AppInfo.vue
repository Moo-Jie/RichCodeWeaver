<template>
  <a-modal
    v-model:open="visible"
    title="应用详情"
    :footer="null"
    width="600px"
    class="app-detail-modal"
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
            <AppstoreOutlined />
            <span class="app-name">{{ app?.appName || '--' }}</span>
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
            <template v-if="app?.deployKey">
              <a-tag color="green">已部署</a-tag>
            </template>
            <a-tag v-else color="red">未部署</a-tag>
          </div>
        </a-descriptions-item>

        <a-descriptions-item label="部署时间">
          <div class="meta-item">
            <CloudServerOutlined />
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
            <PictureOutlined />
            <img v-if="app?.cover" :src="app.cover" alt="应用封面" class="cover-img" />
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

      <!-- 操作栏 -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button
            type="primary"
            class="edit-btn"
            @click="handleEdit"
          >
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
            <a-button danger class="delete-btn">
              <template #icon>
                <DeleteOutlined />
              </template>
              删除当前应用
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

<style scoped lang="less">
.app-detail-modal {
  :deep(.ant-modal-content) {
    background: linear-gradient(135deg, rgba(255, 248, 206, 0.95) 0%, rgba(147, 203, 255, 0.95) 100%);
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(204, 230, 255, 0.3);
  }

  :deep(.ant-modal-header) {
    background: rgba(255, 255, 255, 0.85);
    border-bottom: 1px solid rgba(204, 230, 255, 0.5);
    border-radius: 16px 16px 0 0;
    padding: 16px 24px;
  }

  :deep(.ant-modal-title) {
    font-family: 'Comic Neue', cursive;
    font-size: 1.5rem;
    font-weight: 700;
    color: #2c3e50;
    text-align: center;
  }

  :deep(.ant-modal-body) {
    padding: 0;
  }
}

.app-detail-content {
  padding: 20px;
  font-family: 'Nunito', sans-serif;
}

.meta-grid {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(204, 230, 255, 0.3);
  margin-bottom: 20px;
}

:deep(.ant-descriptions-item) {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(204, 230, 255, 0.3);
}

:deep(.ant-descriptions-item-label) {
  width: 100px;
  color: #7f8c8d;
  font-weight: 600;
  padding-right: 20px;
  vertical-align: top;
}

:deep(.ant-descriptions-item-content) {
  font-weight: 500;
  color: #2c3e50;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 5px 0;

  .anticon {
    font-size: 16px;
    color: #00c4ff;
    flex-shrink: 0;
  }
}

.app-name {
  font-weight: 600;
  color: #2c3e50;
}

.cover-img {
  max-width: 150px;
  height: auto;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.app-actions {
  padding-top: 20px;
  border-top: 1px solid rgba(204, 230, 255, 0.5);
  display: flex;
  justify-content: center;
}

.edit-btn {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  color: white;
  border-radius: 12px;
  height: 40px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 6px 12px rgba(116, 235, 213, 0.25);

  &:hover {
    background: linear-gradient(135deg, #5adbc8 0%, #8b9de6 100%);
    transform: translateY(-3px);
    box-shadow: 0 8px 16px rgba(116, 235, 213, 0.35);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 4px 8px rgba(116, 235, 213, 0.3);
  }
}

.delete-btn {
  border-radius: 12px;
  height: 40px;
  font-weight: 600;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 16px rgba(255, 77, 79, 0.2);
  }

  &:active {
    transform: translateY(0);
  }
}
</style>
