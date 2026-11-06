<template>
  <div class="library-page my-apps-page">
    <header class="library-header">
      <div class="library-heading">
        <h2>我的产物</h2>
        <p>管理自己创建和参与协作的数字产物，查看状态、继续编辑或进入对话。</p>
      </div>
      <div class="library-toolbar">
        <a-input-search
          v-model:value="searchKeyword"
          allow-clear
          class="library-search"
          placeholder="搜索已加载的产物..."
        />
        <a-select v-model:value="sortBy" class="library-select">
          <a-select-option value="time">按创建时间</a-select-option>
          <a-select-option value="name">按名称排序</a-select-option>
        </a-select>
        <button class="library-primary-btn" @click="router.push('/')">
          <PlusOutlined />
          新建
        </button>
      </div>
    </header>

    <div class="library-filter-row">
      <div class="library-tabs" role="tablist" aria-label="产物归属">
        <button type="button" class="library-tab" :class="{ 'is-active': ownershipFilter === 'all' }" @click="ownershipFilter = 'all'">全部</button>
        <button type="button" class="library-tab" :class="{ 'is-active': ownershipFilter === 'owned' }" @click="ownershipFilter = 'owned'">我创建的</button>
        <button type="button" class="library-tab" :class="{ 'is-active': ownershipFilter === 'collab' }" @click="ownershipFilter = 'collab'">协作参与</button>
      </div>
      <span class="library-count">{{ filteredApps.length }} 个产物</span>
    </div>

    <div v-if="loading && appStore.myApps.length === 0" class="library-skeleton-grid">
      <div v-for="i in 6" :key="'skeleton-' + i" class="library-skeleton"></div>
    </div>

    <div v-else-if="filteredApps.length === 0" class="library-empty">
      <h3>{{ searchKeyword ? '没有匹配的产物' : '还没有数字产物' }}</h3>
      <p>{{ searchKeyword ? '搜索只作用于当前已加载内容。' : '从一个清晰的想法开始，创建第一个数字产物。' }}</p>
      <button v-if="!searchKeyword" class="library-primary-btn" @click="router.push('/')">
        <PlusOutlined />
        新建数字产物
      </button>
    </div>

    <template v-else>
      <div class="library-app-grid">
        <AppLibraryCard
          v-for="(app, index) in filteredApps"
          :key="app.id"
          :app="app"
          :variant="index === 0 ? 'hero' : 'grid'"
          :status="app.deployKey ? '已部署' : '未部署'"
          :meta="[app.ownershipType === 'collaborator' ? '协作参与' : '我创建的', formatTime(app.createTime)]"
          @open="goToApp"
        >
          <template #actions>
            <button
              v-if="app.ownershipType !== 'collaborator'"
              type="button"
              class="app-action-btn"
              title="编辑产物"
              aria-label="编辑产物"
              @click="handleEdit(app)"
            >
              <EditOutlined />
            </button>
            <button
              v-if="app.ownershipType !== 'collaborator'"
              type="button"
              class="app-action-btn"
              title="删除产物"
              aria-label="删除产物"
              @click="handleDelete(app)"
            >
              <DeleteOutlined />
            </button>
          </template>
        </AppLibraryCard>
      </div>

      <div v-if="hasMore" class="library-load-more-wrap">
        <button :disabled="loadingMore" class="library-load-more" @click="loadMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/appStore'
import { deleteApp, listMyAppVoByPage } from '@/api/appController'
import AppLibraryCard from '@/components/library/AppLibraryCard.vue'

const router = useRouter()
const appStore = useAppStore()

const searchKeyword = ref('')
const sortBy = ref<'time' | 'name'>('time')
const ownershipFilter = ref<'all' | 'owned' | 'collab'>('all')
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(false)

const goToApp = (app: API.AppVO) => {
  if (app.id) {
    appStore.selectApp(app)
    router.push(`/app/chat/${app.id}`)
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.substring(0, 10)
}

// 过滤和排序
const filteredApps = computed(() => {
  let result = appStore.myApps

  // 搜索过滤
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(app => app.appName?.toLowerCase().includes(keyword))
  }

  if (ownershipFilter.value === 'owned') {
    result = result.filter(app => app.ownershipType !== 'collaborator')
  } else if (ownershipFilter.value === 'collab') {
    result = result.filter(app => app.ownershipType === 'collaborator')
  }

  // 排序
  if (sortBy.value === 'time') {
    result = [...result].sort((a, b) => {
      const timeA = a.createTime || ''
      const timeB = b.createTime || ''
      return timeB.localeCompare(timeA)
    })
  } else if (sortBy.value === 'name') {
    result = [...result].sort((a, b) => {
      const nameA = a.appName || ''
      const nameB = b.appName || ''
      return nameA.localeCompare(nameB)
    })
  }

  return result
})

// 编辑产物
const handleEdit = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

// 删除产物
const handleDelete = (app: API.AppVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除产物「${app.appName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await deleteApp({ id: app.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 从列表中移除
          appStore.myApps = appStore.myApps.filter(a => a.id !== app.id)
          appStore.myAppsTotal = Math.max(0, appStore.myAppsTotal - 1)
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const fetchApps = async (pageNum = 1) => {
  if (pageNum === 1) loading.value = true
  else loadingMore.value = true

  try {
    const res = await listMyAppVoByPage({
      pageNum,
      pageSize: 20,
      sortField: 'createTime',
      sortOrder: 'desc'
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      if (pageNum === 1) {
        appStore.myApps = records
      } else {
        appStore.myApps = [...appStore.myApps, ...records]
      }
      appStore.myAppsTotal = res.data.data.totalRow || 0
      hasMore.value = records.length === 20
      page.value = pageNum
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  fetchApps(page.value + 1)
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.my-apps-page {
  padding: 32px;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.skeleton-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
}

.skeleton-image {
  width: 100%;
  height: 140px;
}

.skeleton-content {
  padding: 12px;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 12px;
}

.empty-text {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  background: #1a1a1a;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.create-btn:hover {
  background: #333;
}

.create-btn-large {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.create-btn-large:hover {
  background: #333;
}

.apps-masonry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 900px) {
  .apps-masonry {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  break-inside: avoid;
  margin-bottom: 16px;
}

.app-card:hover {
  border-color: #d0d0d0;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.app-card:hover .cover-overlay {
  opacity: 1;
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  min-height: 160px;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.app-card:hover .card-cover img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.view-btn {
  padding: 8px 20px;
  background: #fff;
  color: #1a1a1a;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

.card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  cursor: pointer;
}

.card-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-time {
  font-size: 12px;
  color: #bbb;
}

.ownership-tag {
  margin-inline-end: 0;
  flex-shrink: 0;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  padding-inline: 8px;
}

.skeleton-card {
  break-inside: avoid;
  margin-bottom: 20px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.action-btn:hover {
  background: #fafafa;
  border-color: #d0d0d0;
}

.deploy-tag {
  padding: 4px 10px;
  background: #f0f0f0;
  color: #52c41a;
  font-size: 11px;
  border-radius: 6px;
  font-weight: 500;
  margin-left: auto;
}

.collab-tip {
  font-size: 12px;
  color: #57606a;
  background: #f6f8fa;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  padding: 4px 10px;
}

.status-tag {
  padding: 4px 10px;
  background: #f5f5f5;
  color: #999;
  font-size: 11px;
  border-radius: 6px;
  font-weight: 500;
  margin-left: auto;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.load-more-btn {
  padding: 8px 28px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #fff;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.load-more-btn:hover:not(:disabled) {
  background: #fafafa;
  border-color: #d0d0d0;
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
<style scoped>
.app-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #666;
  background: transparent;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease;
}

.app-action-btn:hover {
  border-color: #e5e5e5;
  color: #1a1a1a;
  background: #fafafa;
}

.library-page.my-apps-page {
  max-width: 1480px;
  padding: clamp(18px, 3vw, 32px);
}
</style>
