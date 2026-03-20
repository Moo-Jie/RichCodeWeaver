<template>
  <div class="all-apps-page">
    <div class="page-header">
      <h2 class="page-title">全部产物</h2>
      <p class="page-desc">探索社区中的热门创作</p>
    </div>

    <div v-if="loading" class="loading-wrap">
      <a-spin />
    </div>

    <div v-else-if="apps.length === 0" class="empty-wrap">
      <div class="empty-icon">🌍</div>
      <p class="empty-text">暂无热门数字产物</p>
    </div>

    <template v-else>
      <div class="apps-grid">
        <div
          v-for="app in apps"
          :key="app.id"
          class="app-card"
          @click="goToApp(app)"
        >
          <div class="card-cover">
            <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
            <img v-else alt="默认" src="@/assets/logo.png" style="opacity:0.5" />
          </div>
          <div class="card-body">
            <span class="card-name">{{ app.appName || '未命名数字产物' }}</span>
            <div class="card-meta">
              <span class="card-user">{{ app.user?.userName || '--' }}</span>
              <span class="card-time">{{ formatTime(app.createTime) }}</span>
            </div>
          </div>
          <div v-if="app.deployKey" class="card-badge">已部署</div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more-wrap">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/appStore'
import { listStarAppVoByPage } from '@/api/appController'

const router = useRouter()
const appStore = useAppStore()

const apps = ref<API.AppVO[]>([])
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

const fetchApps = async (pageNum = 1) => {
  if (pageNum === 1) loading.value = true
  else loadingMore.value = true

  try {
    const res = await listStarAppVoByPage({
      pageNum,
      pageSize: 20,
      sortField: 'priority',
      sortOrder: 'asc'
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      if (pageNum === 1) {
        apps.value = records
      } else {
        apps.value = [...apps.value, ...records]
      }
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
.all-apps-page {
  padding: 32px;
  width: 100%;
}

.page-header {
  margin-bottom: 28px;
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

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 12px;
}

.empty-icon {
  font-size: 40px;
}

.empty-text {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.app-card:hover {
  border-color: #e5e5e5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.card-cover {
  width: 100%;
  height: 120px;
  background: #f0f0f0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-user {
  font-size: 12px;
  color: #999;
}

.card-time {
  font-size: 12px;
  color: #bbb;
}

.card-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  background: #1a1a1a;
  color: #fff;
  font-size: 11px;
  border-radius: 6px;
  font-weight: 500;
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
