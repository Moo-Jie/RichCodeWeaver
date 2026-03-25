<template>
  <div class="my-favorites-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的收藏</h2>
        <p class="page-desc">您收藏的所有数字产物</p>
      </div>
      <div class="header-right">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索收藏的产物..."
          style="width: 240px; margin-right: 12px"
        />
        <a-select v-model:value="sortBy" style="width: 140px">
          <a-select-option value="time">按收藏时间</a-select-option>
          <a-select-option value="name">按名称排序</a-select-option>
        </a-select>
      </div>
    </div>

    <!-- Skeleton loading -->
    <div v-if="loading && apps.length === 0" class="apps-grid">
      <div v-for="i in 6" :key="'skeleton-' + i" class="skeleton-card">
        <a-skeleton-image class="skeleton-image" />
        <div class="skeleton-content">
          <a-skeleton :paragraph="{ rows: 2 }" active />
        </div>
      </div>
    </div>

    <div v-else-if="filteredApps.length === 0" class="empty-wrap">
      <p class="empty-text">{{ searchKeyword ? '未找到匹配的收藏' : '暂无收藏，去发现更多优秀产物吧' }}</p>
      <button class="explore-btn" @click="router.push('/all/apps')">
        <GlobalOutlined /> 浏览全部产物
      </button>
    </div>

    <template v-else>
      <div class="apps-grid">
        <div
          v-for="app in filteredApps"
          :key="app.id"
          class="app-card"
        >
          <div class="card-cover" @click="goToApp(app)">
            <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
            <img v-else alt="默认" src="@/assets/logo.png" style="opacity:0.5" />
            <div class="cover-overlay">
              <span class="view-btn">查看详情</span>
            </div>
          </div>
          <div class="card-body" @click="goToApp(app)">
            <span class="card-name" :title="app.appName">{{ app.appName || '未命名数字产物' }}</span>
            <span class="card-time">收藏于 {{ formatTime(app._favoriteTime) }}</span>
          </div>
          <div class="card-actions">
            <button class="action-btn" @click.stop="handleUnfavorite(app)" title="取消收藏">
              <HeartFilled style="color: #ff4d4f" />
            </button>
            <span v-if="app.deployKey" class="deploy-tag">已部署</span>
          </div>
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
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { GlobalOutlined, HeartFilled } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/appStore'
import { getAppVoById } from '@/api/appController'
import { listMyFavorites, toggleAppFavorite } from '@/api/socialController'

const router = useRouter()
const appStore = useAppStore()

const searchKeyword = ref('')
const sortBy = ref<'time' | 'name'>('time')
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(false)
const apps = ref<(API.AppVO & { _favoriteTime?: string })[]>([])

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
  let result = apps.value
  
  // 搜索过滤
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(app => app.appName?.toLowerCase().includes(keyword))
  }
  
  // 排序
  if (sortBy.value === 'time') {
    result = [...result].sort((a, b) => {
      const timeA = a._favoriteTime || ''
      const timeB = b._favoriteTime || ''
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

// 取消收藏
const handleUnfavorite = async (app: API.AppVO) => {
  try {
    const res = await toggleAppFavorite({ appId: app.id })
    if (res.data.code === 0) {
      message.success('已取消收藏')
      // 从列表中移除
      apps.value = apps.value.filter(a => a.id !== app.id)
    }
  } catch (error) {
    message.error('操作失败')
  }
}

/**
 * 获取收藏列表并加载产物信息
 */
const fetchFavorites = async (pageNum = 1) => {
  if (pageNum === 1) loading.value = true
  else loadingMore.value = true

  try {
    const res = await listMyFavorites({ pageNum, pageSize: 20 })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      // 批量获取产物信息
      const appList: (API.AppVO & { _favoriteTime?: string })[] = []
      for (const fav of records) {
        if (!fav.appId) continue
        try {
          const appRes = await getAppVoById({ id: fav.appId })
          if (appRes.data.code === 0 && appRes.data.data) {
            appList.push({ ...appRes.data.data, _favoriteTime: fav.createTime })
          }
        } catch {
          // 产物可能已被删除，跳过
        }
      }
      if (pageNum === 1) {
        apps.value = appList
      } else {
        apps.value = [...apps.value, ...appList]
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
  fetchFavorites(page.value + 1)
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.my-favorites-page {
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

.explore-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.explore-btn:hover {
  background: #333;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

@media (max-width: 768px) {
  .apps-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
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
}

.app-card:hover {
  border-color: #d0d0d0;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.app-card:hover .cover-overlay {
  opacity: 1;
}

.card-cover {
  position: relative;
  width: 100%;
  height: 140px;
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
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  cursor: pointer;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-time {
  font-size: 12px;
  color: #bbb;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 14px;
  gap: 8px;
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
}

.action-btn:hover {
  background: #fff1f0;
  border-color: #ffccc7;
}

.deploy-tag {
  padding: 4px 10px;
  background: #f0f0f0;
  color: #666;
  font-size: 11px;
  border-radius: 6px;
  font-weight: 500;
  flex: 1;
  text-align: center;
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
