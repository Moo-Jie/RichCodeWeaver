<template>
  <div class="all-apps-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">热门产物</h2>
        <p class="page-desc">探索社区中最受欢迎的数字产物</p>
      </div>
      <div class="header-right">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索产物名称..."
          style="width: 260px"
          @search="handleSearch"
        />
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tabs">
      <div
        :class="{ active: activeTab === 'hot' }"
        class="tab-item"
        @click="activeTab = 'hot'"
      >
        <FireOutlined />
        社区热门
      </div>
      <div
        :class="{ active: activeTab === 'star' }"
        class="tab-item"
        @click="activeTab = 'star'"
      >
        <CrownOutlined />
        精选推荐
      </div>
    </div>

    <!-- 热门产物（社区模块） -->
    <div v-show="activeTab === 'hot'" class="section">
      <!-- Skeleton loading -->
      <div v-if="hotLoading && hotApps.length === 0" class="apps-masonry">
        <div v-for="i in 6" :key="'skeleton-' + i" class="skeleton-card">
          <a-skeleton-image class="skeleton-image" />
          <div class="skeleton-content">
            <a-skeleton :paragraph="{ rows: 2 }" active />
          </div>
        </div>
      </div>
      <div v-else-if="filteredHotApps.length === 0" class="empty-wrap">
        <p class="empty-text">{{ searchKeyword ? '未找到匹配的产物' : '暂无社区热门产物' }}</p>
      </div>
      <template v-else>
        <div class="apps-masonry">
          <div
            v-for="item in filteredHotApps"
            :key="'hot-' + item.appId"
            class="app-card"
            @click="goToApp(item.appVO)"
          >
            <div class="card-cover">
              <img v-if="item.appVO?.cover" :alt="item.appVO?.appName" :src="item.appVO?.cover" />
              <img v-else alt="默认" src="@/assets/logo.png" style="opacity:0.5" />
              <div class="cover-overlay">
                <span class="view-btn">查看详情</span>
              </div>
            </div>
            <div class="card-body">
              <span :title="item.appVO?.appName"
                    class="card-name">{{ item.appVO?.appName || '未命名数字产物'
                }}</span>
              <div class="card-stats-row">
                <div class="stat-item">
                  <LikeOutlined />
                  <span>{{ formatNumber(item.stat.likeCount || 0) }}</span>
                </div>
                <div class="stat-item">
                  <StarOutlined />
                  <span>{{ formatNumber(item.stat.favoriteCount || 0) }}</span>
                </div>
                <div class="stat-item">
                  <CommentOutlined />
                  <span>{{ formatNumber(item.stat.commentCount || 0) }}</span>
                </div>
              </div>
              <div class="card-footer">
                <span class="card-user">{{ item.appVO?.user?.userName || '--' }}</span>
                <span v-if="item.appVO?.deployKey" class="deploy-tag">已部署</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="hotHasMore" class="load-more-wrap">
          <button :disabled="hotLoadingMore" class="load-more-btn" @click="loadMoreHot">
            {{ hotLoadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </template>
    </div>

    <!-- 精选推荐（生成器星选推荐） -->
    <div v-show="activeTab === 'star'" class="section">
      <!-- Skeleton loading -->
      <div v-if="starLoading && starApps.length === 0" class="apps-masonry">
        <div v-for="i in 6" :key="'skeleton-' + i" class="skeleton-card">
          <a-skeleton-image class="skeleton-image" />
          <div class="skeleton-content">
            <a-skeleton :paragraph="{ rows: 2 }" active />
          </div>
        </div>
      </div>
      <div v-else-if="filteredStarApps.length === 0" class="empty-wrap">
        <p class="empty-text">{{ searchKeyword ? '未找到匹配的产物' : '暂无精选推荐产物' }}</p>
      </div>
      <template v-else>
        <div class="apps-masonry">
          <div
            v-for="app in filteredStarApps"
            :key="'star-' + app.id"
            class="app-card"
            @click="goToApp(app)"
          >
            <div class="card-cover">
              <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
              <img v-else alt="默认" src="@/assets/logo.png" style="opacity:0.5" />
              <div class="cover-overlay">
                <span class="view-btn">查看详情</span>
              </div>
            </div>
            <div class="card-body">
              <span :title="app.appName" class="card-name">{{ app.appName || '未命名数字产物'
                }}</span>
              <div class="card-desc">{{ formatTime(app.createTime) }}</div>
              <div class="card-footer">
                <span class="card-user">{{ app.user?.userName || '--' }}</span>
                <span v-if="app.deployKey" class="deploy-tag">已部署</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="starHasMore" class="load-more-wrap">
          <button :disabled="starLoadingMore" class="load-more-btn" @click="loadMoreStar">
            {{ starLoadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAppStore} from '@/stores/appStore'
import {getAppVoById, listStarAppVoByPage} from '@/api/appController'
import {listHotApps} from '@/api/socialController'
import {
  CommentOutlined,
  CrownOutlined,
  FireOutlined,
  LikeOutlined,
  StarOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const appStore = useAppStore()

const activeTab = ref<'hot' | 'star'>('hot')
const searchKeyword = ref('')

// === 热门产物（社区模块）===
interface HotAppItem {
  appId: number
  stat: API.AppHotStat
  appVO?: API.AppVO | null
}

const hotApps = ref<HotAppItem[]>([])
const hotLoading = ref(false)
const hotLoadingMore = ref(false)
const hotPage = ref(1)
const hotHasMore = ref(false)

// === 精选推荐 ===
const starApps = ref<API.AppVO[]>([])
const starLoading = ref(false)
const starLoadingMore = ref(false)
const starPage = ref(1)
const starHasMore = ref(false)

const PAGE_SIZE = 12

const goToApp = (app?: API.AppVO | null) => {
  if (app?.id) {
    appStore.selectApp(app)
    router.push(`/app/chat/${app.id}`)
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.substring(0, 10)
}

const formatNumber = (num: number) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const handleSearch = () => {
  // 搜索逻辑已通过 computed 实现
}

// 过滤热门产物
const filteredHotApps = computed(() => {
  if (!searchKeyword.value.trim()) return hotApps.value
  const keyword = searchKeyword.value.toLowerCase()
  return hotApps.value.filter(item =>
    item.appVO?.appName?.toLowerCase().includes(keyword)
  )
})

// 过滤精选推荐
const filteredStarApps = computed(() => {
  if (!searchKeyword.value.trim()) return starApps.value
  const keyword = searchKeyword.value.toLowerCase()
  return starApps.value.filter(app =>
    app.appName?.toLowerCase().includes(keyword)
  )
})

// 加载热门产物
const fetchHotApps = async (pageNum = 1) => {
  if (pageNum === 1) hotLoading.value = true
  else hotLoadingMore.value = true

  try {
    const res = await listHotApps({ pageNum, pageSize: PAGE_SIZE })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      const items: HotAppItem[] = records
        .filter((r) => r.appId)
        .map((r) => ({ appId: r.appId!, stat: r, appVO: null }))

      // 批量加载产物信息
      await Promise.allSettled(
        items.map(async (item) => {
          try {
            const appRes = await getAppVoById({ id: item.appId as any })
            if (appRes.data.code === 0 && appRes.data.data) {
              item.appVO = appRes.data.data
            }
          } catch { /* 跳过加载失败的产物 */
          }
        })
      )

      // 过滤掉加载失败的
      const validItems = items.filter((i) => i.appVO)

      if (pageNum === 1) {
        hotApps.value = validItems
      } else {
        hotApps.value = [...hotApps.value, ...validItems]
      }
      hotHasMore.value = records.length === PAGE_SIZE
      hotPage.value = pageNum
    }
  } finally {
    hotLoading.value = false
    hotLoadingMore.value = false
  }
}

// 加载精选推荐
const fetchStarApps = async (pageNum = 1) => {
  if (pageNum === 1) starLoading.value = true
  else starLoadingMore.value = true

  try {
    const res = await listStarAppVoByPage({
      pageNum,
      pageSize: PAGE_SIZE,
      sortField: 'priority',
      sortOrder: 'asc'
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      if (pageNum === 1) {
        starApps.value = records
      } else {
        starApps.value = [...starApps.value, ...records]
      }
      starHasMore.value = records.length === PAGE_SIZE
      starPage.value = pageNum
    }
  } finally {
    starLoading.value = false
    starLoadingMore.value = false
  }
}

const loadMoreHot = () => fetchHotApps(hotPage.value + 1)
const loadMoreStar = () => fetchStarApps(starPage.value + 1)

onMounted(() => {
  fetchHotApps()
  fetchStarApps()
})
</script>

<style scoped>
.all-apps-page {
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

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.tab-item {
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-item:hover {
  color: #1a1a1a;
}

.tab-item.active {
  color: #1a1a1a;
  border-bottom-color: #1a1a1a;
}

.section {
  margin-bottom: 36px;
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
}

.empty-text {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.apps-masonry {
  columns: 4 260px;
  column-gap: 20px;
}

@media (max-width: 900px) {
  .apps-masonry {
    columns: 2 200px;
    column-gap: 14px;
  }
}

.skeleton-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  overflow: hidden;
  break-inside: avoid;
  margin-bottom: 20px;
}

.skeleton-image {
  width: 100%;
  height: 140px;
}

.skeleton-content {
  padding: 12px;
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  break-inside: avoid;
  margin-bottom: 20px;
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
  min-height: 180px;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
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
  gap: 8px;
  flex: 1;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-stats-row {
  display: flex;
  gap: 12px;
  margin-top: auto;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.stat-item span {
  font-weight: 500;
}

.card-desc {
  font-size: 12px;
  color: #bbb;
  margin-top: -4px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.card-user {
  font-size: 12px;
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deploy-tag {
  padding: 2px 8px;
  background: #f0f0f0;
  color: #666;
  font-size: 11px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
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
