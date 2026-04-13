<template>
  <section class="health-panel">
    <div class="panel-header">
      <div>
        <h2 class="panel-title">{{ title }}</h2>
        <p v-if="description" class="panel-desc">{{ description }}</p>
      </div>
      <a-button v-if="showRefresh" size="small" class="refresh-btn" :loading="loading" @click="loadModuleHealth">
        刷新检测
      </a-button>
    </div>

    <div class="modules-grid">
      <div v-for="item in moduleItems" :key="item.key" class="module-card">
        <div class="module-header">
          <span class="module-name">{{ item.name }}</span>
          <span :class="['module-status', getStatusClass(item.key)]">
            {{ getStatusText(item.key) }}
          </span>
        </div>
        <p class="module-desc">{{ item.description }}</p>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { checkAllModulesHealth } from '@/api/healthController'

type ModuleHealthKey = 'user' | 'generator' | 'ai' | 'file' | 'prompt' | 'social'

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  showRefresh?: boolean
}>(), {
  title: '微服务模块',
  description: '',
  showRefresh: false
})

const loading = ref(false)
const moduleHealth = reactive<Record<ModuleHealthKey, boolean | null>>({
  user: null,
  generator: null,
  ai: null,
  file: null,
  prompt: null,
  social: null
})

const moduleItems = computed(() => [
  { key: 'user' as ModuleHealthKey, name: '用户模块', description: '用户管理、认证授权、Session管理' },
  { key: 'generator' as ModuleHealthKey, name: '产物生成模块', description: 'AI代码生成核心引擎、工作流编排' },
  { key: 'ai' as ModuleHealthKey, name: 'AI模块', description: 'AI服务封装、多模型支持、对话管理' },
  { key: 'file' as ModuleHealthKey, name: '文件模块', description: '文件上传、存储管理、访问控制' },
  { key: 'prompt' as ModuleHealthKey, name: '提示词模板模块', description: '模板管理、智能匹配、动态渲染' },
  { key: 'social' as ModuleHealthKey, name: '社交模块', description: '点赞、收藏、评论、分享、热门排行' }
])

const getStatusText = (key: ModuleHealthKey) => {
  const status = moduleHealth[key]
  if (status === null) {
    return loading.value ? '检测中' : '待检测'
  }
  return status ? '运行中' : '未运行'
}

const getStatusClass = (key: ModuleHealthKey) => {
  const status = moduleHealth[key]
  if (status === null) {
    return 'status-pending'
  }
  return status ? 'status-running' : 'status-stopped'
}

const loadModuleHealth = async () => {
  loading.value = true
  try {
    const healthStatus = await checkAllModulesHealth()
    moduleItems.value.forEach((item) => {
      moduleHealth[item.key] = Boolean(healthStatus[item.key])
    })
  } catch (error) {
    moduleItems.value.forEach((item) => {
      moduleHealth[item.key] = false
    })
    console.error('Failed to load module health status:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadModuleHealth()
})
</script>

<style scoped>
.health-panel {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 24px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.panel-desc {
  font-size: 14px;
  color: #999;
  margin: 8px 0 0;
}

.refresh-btn {
  border-radius: 8px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.module-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s ease;
}

.module-card:hover {
  border-color: #e5e5e5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.module-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.module-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.status-running {
  color: #52c41a;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.status-stopped {
  color: #ff4d4f;
  background: #fff1f0;
  border: 1px solid #ffccc7;
}

.status-pending {
  color: #8c8c8c;
  background: #fafafa;
  border: 1px solid #d9d9d9;
}

.module-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

@media (max-width: 768px) {
  .health-panel {
    padding: 18px;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .modules-grid {
    grid-template-columns: 1fr;
  }
}
</style>
