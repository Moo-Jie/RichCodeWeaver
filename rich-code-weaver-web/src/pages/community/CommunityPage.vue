<template>
  <div class="community-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">交流社区</h2>
        <p class="page-desc">提问题、提建议、做分享，在社区里和大家一起交流使用经验</p>
      </div>
      <div class="header-right">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索帖子标题或内容..."
          style="width: 280px"
          @search="handleSearch"
        />
      </div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-group">
        <span class="toolbar-label">分类</span>
        <button
          v-for="item in categoryOptions"
          :key="item.value"
          :class="['filter-chip', { active: activeCategory === item.value }]"
          @click="handleCategoryChange(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="toolbar-group">
        <span class="toolbar-label">排序</span>
        <button
          v-for="item in sortOptions"
          :key="item.value"
          :class="['filter-chip', { active: activeSort === item.value }]"
          @click="handleSortChange(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="content-grid">
      <div class="left-column">
        <div class="composer-card">
          <div class="composer-head">
            <div>
              <h3 class="section-title">发布新帖</h3>
              <p class="section-desc">像反馈社区和问答社区一样发起讨论，但保持本系统统一风格</p>
            </div>
            <div class="composer-meta">{{ loginUserStore.loginUser?.userName || '未登录' }}</div>
          </div>
          <div class="composer-form">
            <a-input
              v-model:value="postForm.title"
              :maxlength="200"
              placeholder="请输入帖子标题，清楚描述你的问题或想法"
            />
            <div class="composer-row">
              <a-select v-model:value="postForm.category" style="width: 160px">
                <a-select-option v-for="item in categoryOptions.slice(1)" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
              <span class="word-count">{{ postForm.content.length }}/10000</span>
            </div>
            <a-textarea
              v-model:value="postForm.content"
              :auto-size="{ minRows: 4, maxRows: 8 }"
              :maxlength="10000"
              placeholder="可以写使用问题、功能建议、经验分享或Bug反馈，描述越具体越容易获得帮助"
            />
            <div class="composer-actions">
              <button class="secondary-btn" @click="resetPostForm">清空</button>
              <button :disabled="submittingPost" class="primary-btn" @click="submitPost">
                {{ submittingPost ? '发布中...' : '发布帖子' }}
              </button>
            </div>
          </div>
        </div>

        <div class="section-header">
          <div>
            <h3 class="section-title">帖子列表</h3>
            <p class="section-desc">优先展示近期活跃内容，便于快速跟进社区讨论</p>
          </div>
          <div class="section-extra">共 {{ postsTotal }} 条</div>
        </div>

        <div v-if="loading && posts.length === 0" class="post-list">
          <div v-for="i in 4" :key="i" class="post-card skeleton-card">
            <a-skeleton active :paragraph="{ rows: 4 }" />
          </div>
        </div>

        <div v-else-if="posts.length === 0" class="empty-card">
          <p class="empty-title">{{ searchText ? '没有找到匹配帖子' : '还没有帖子，来发第一条吧' }}</p>
          <p class="empty-desc">你可以提问题、提建议，或者分享你用织码睿奇做出的作品</p>
        </div>

        <div v-else class="post-list">
          <div
            v-for="post in posts"
            :key="post.id"
            :class="['post-card', { selected: selectedPost?.id === post.id }]"
            @click="selectPost(post.id)"
          >
            <div class="post-head">
              <div class="post-head-left">
                <span v-if="post.isTop" class="top-badge">置顶</span>
                <span class="category-badge">{{ getCategoryLabel(post.category) }}</span>
              </div>
              <span class="post-time">{{ formatTime(post.updateTime || post.createTime) }}</span>
            </div>
            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-content">{{ post.content }}</p>
            <div class="post-author-row">
              <div class="author-info">
                <a-avatar :size="28" :src="post.user?.userAvatar">
                  {{ post.user?.userName?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="author-texts">
                  <span class="author-name">{{ post.user?.userName || '匿名用户' }}</span>
                  <span class="author-meta">{{ post.user?.userIndustry || '社区成员' }}</span>
                </div>
              </div>
              <div class="post-stats">
                <span class="stat-text">{{ post.viewCount || 0 }} 浏览</span>
                <span class="stat-text">{{ post.likeCount || 0 }} 点赞</span>
                <span class="stat-text">{{ post.replyCount || 0 }} 回复</span>
              </div>
            </div>
            <div v-if="post.latestReplies?.length" class="latest-replies">
              <div v-for="reply in post.latestReplies" :key="reply.id" class="latest-reply-item">
                <span class="reply-user">{{ reply.user?.userName || '用户' }}：</span>
                <span class="reply-content">{{ reply.content }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="load-more-wrap">
          <button :disabled="loadingMore" class="load-more-btn" @click="loadMorePosts">
            {{ loadingMore ? '加载中...' : '加载更多帖子' }}
          </button>
        </div>
      </div>

      <div class="right-column">
        <div class="side-stack">
          <div class="info-card">
            <div class="info-card-head">
              <div>
                <h3 class="info-card-title">热门话题</h3>
                <p class="info-card-desc">根据当前社区内容的浏览、点赞与回复热度综合排序</p>
              </div>
            </div>
            <div v-if="hotTopics.length" class="hot-topic-list">
              <button
                v-for="(topic, index) in hotTopics"
                :key="topic.id"
                class="hot-topic-item"
                @click="selectPost(topic.id)"
              >
                <span class="hot-topic-rank">{{ index + 1 }}</span>
                <span class="hot-topic-main">
                  <span class="hot-topic-title">{{ topic.title }}</span>
                  <span class="hot-topic-meta">{{ topic.replyCount || 0 }} 回复 · {{ topic.likeCount || 0 }} 点赞</span>
                </span>
              </button>
            </div>
            <div v-else class="empty-inline">当前还没有可展示的热门话题</div>
          </div>

          <div class="info-card">
            <div class="info-card-head">
              <div>
                <h3 class="info-card-title">分类统计</h3>
                <p class="info-card-desc">基于当前列表结果的分类分布</p>
              </div>
            </div>
            <div class="stat-list">
              <div v-for="item in categoryStats" :key="item.value" class="stat-item">
                <div class="stat-item-top">
                  <span class="stat-label">{{ item.label }}</span>
                  <span class="stat-value">{{ item.count }}</span>
                </div>
                <div class="stat-bar-track">
                  <div class="stat-bar-fill" :style="{ width: `${item.percent}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedPost" class="detail-card">
            <div class="detail-head">
              <div class="detail-head-left">
                <span v-if="selectedPost.isTop" class="top-badge">置顶</span>
                <span class="category-badge">{{ getCategoryLabel(selectedPost.category) }}</span>
              </div>
              <div class="detail-stats-inline">
                <span>{{ selectedPost.viewCount || 0 }} 浏览</span>
                <span>{{ selectedPost.likeCount || 0 }} 点赞</span>
                <span>{{ selectedPost.replyCount || 0 }} 回复</span>
              </div>
            </div>

            <h3 class="detail-title">{{ selectedPost.title }}</h3>

            <div class="detail-author">
              <div class="author-info">
                <a-avatar :size="34" :src="selectedPost.user?.userAvatar">
                  {{ selectedPost.user?.userName?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="author-texts">
                  <span class="author-name">{{ selectedPost.user?.userName || '匿名用户' }}</span>
                  <span class="author-meta">发布于 {{ formatDateTime(selectedPost.createTime) }}</span>
                </div>
              </div>
              <button
                :class="['like-btn', { liked: selectedPost.hasLiked }]"
                @click="handleLikePost"
              >
                <LikeOutlined />
                {{ selectedPost.hasLiked ? '已点赞' : '点赞帖子' }}
              </button>
            </div>

            <div class="detail-content">{{ selectedPost.content }}</div>

            <div class="reply-section">
              <div class="section-header small-gap">
                <div>
                  <h3 class="section-title">全部回复</h3>
                  <p class="section-desc">围绕当前话题继续讨论</p>
                </div>
              </div>

              <div class="reply-composer">
                <a-textarea
                  v-model:value="replyContent"
                  :auto-size="{ minRows: 3, maxRows: 6 }"
                  :maxlength="5000"
                  placeholder="写下你的回复，帮助对方解决问题，或者补充你的经验"
                />
                <div class="composer-actions">
                  <span class="word-count">{{ replyContent.length }}/5000</span>
                  <button :disabled="submittingReply" class="primary-btn" @click="submitReply">
                    {{ submittingReply ? '回复中...' : '发表回复' }}
                  </button>
                </div>
              </div>

              <div v-if="repliesLoading" class="reply-list">
                <div v-for="i in 3" :key="i" class="reply-card skeleton-card">
                  <a-skeleton active :paragraph="{ rows: 2 }" />
                </div>
              </div>

              <div v-else-if="replies.length === 0" class="empty-inline">
                暂无回复，成为第一个参与讨论的人吧
              </div>

              <div v-else class="reply-list">
                <div v-for="reply in replies" :key="reply.id" class="reply-card">
                  <div class="reply-head">
                    <div class="author-info">
                      <a-avatar :size="28" :src="reply.user?.userAvatar">
                        {{ reply.user?.userName?.charAt(0) || 'U' }}
                      </a-avatar>
                      <div class="author-texts">
                        <span class="author-name">{{ reply.user?.userName || '匿名用户' }}</span>
                        <span class="author-meta">{{ formatDateTime(reply.createTime) }}</span>
                      </div>
                    </div>
                    <button :class="['icon-like-btn', { liked: reply.hasLiked }]" @click="handleLikeReply(reply)">
                      <LikeOutlined />
                      {{ reply.likeCount || 0 }}
                    </button>
                  </div>
                  <div class="reply-body">{{ reply.content }}</div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="detail-placeholder">
            <p class="empty-title">选择一条帖子查看详情</p>
            <p class="empty-desc">你可以在这里查看完整内容、点赞帖子并参与回复</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LikeOutlined } from '@ant-design/icons-vue'
import {
  addCommunityPost,
  addCommunityReply,
  getCommunityPostVOById,
  listCommunityPostByPage,
  listCommunityReplyByPage,
  toggleCommunityPostLike,
  toggleCommunityReplyLike
} from '@/api/communityController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const PAGE_SIZE = 10

interface PostFormState {
  title: string
  content: string
  category: string
}

const categoryOptions = [
  { label: '全部', value: '' },
  { label: '讨论', value: 'discuss' },
  { label: '问答', value: 'question' },
  { label: '反馈', value: 'feedback' },
  { label: '分享', value: 'share' }
]

const sortOptions = [
  { label: '最近活跃', value: 'lastActive' },
  { label: '最新发布', value: 'latest' },
  { label: '最多回复', value: 'replies' },
  { label: '最多点赞', value: 'likes' },
  { label: '最多浏览', value: 'views' }
]

const communityAnnouncements = [
  {
    title: '友善交流',
    content: '提问、反馈和分享都欢迎，但请尽量围绕问题本身讨论，避免无关争执。'
  },
  {
    title: '描述尽量具体',
    content: '提问时建议写清场景、操作步骤和预期结果，越具体越容易获得高质量回复。'
  },
  {
    title: '鼓励分享经验',
    content: '欢迎分享你使用织码睿奇做出的项目、提示词技巧和踩坑总结。'
  }
]

const activeCategory = ref('')
const activeSort = ref('lastActive')
const searchText = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(false)
const postsTotal = ref(0)
const posts = ref<API.CommunityPostVO[]>([])
const selectedPost = ref<API.CommunityPostVO>()
const replies = ref<API.CommunityReplyVO[]>([])
const repliesLoading = ref(false)
const submittingPost = ref(false)
const submittingReply = ref(false)
const replyContent = ref('')
const pendingIncreaseView = ref(false)

const postForm = reactive<PostFormState>({
  title: '',
  content: '',
  category: 'question'
})

const selectedPostId = computed(() => {
  const id = route.query.postId
  if (!id) return undefined
  const postId = Number(id)
  return Number.isNaN(postId) ? undefined : postId
})

const normalizePostId = (postId?: number | string) => {
  if (postId == null || postId === '') {
    return undefined
  }
  const normalized = Number(postId)
  return Number.isNaN(normalized) ? undefined : normalized
}

const getCategoryLabel = (category?: string) => {
  return categoryOptions.find(item => item.value === category)?.label || '讨论'
}

const hotTopics = computed(() => {
  return [...posts.value]
    .sort((a, b) => {
      const scoreA = (a.replyCount || 0) * 3 + (a.likeCount || 0) * 2 + (a.viewCount || 0)
      const scoreB = (b.replyCount || 0) * 3 + (b.likeCount || 0) * 2 + (b.viewCount || 0)
      return scoreB - scoreA
    })
    .slice(0, 5)
})

const categoryStats = computed(() => {
  const total = posts.value.length || 1
  return categoryOptions.slice(1).map(item => {
    const count = posts.value.filter(post => post.category === item.value).length
    return {
      ...item,
      count,
      percent: Math.max(count > 0 ? Math.round((count / total) * 100) : 0, count > 0 ? 12 : 0)
    }
  })
})

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const formatDateTime = (time?: string) => {
  return formatTime(time)
}

const ensureLogin = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录后再操作')
    router.push(`/user/login?redirect=${encodeURIComponent(window.location.href)}`)
    return false
  }
  return true
}

const resetPostForm = () => {
  postForm.title = ''
  postForm.content = ''
  postForm.category = 'question'
}

const handleCategoryChange = (value: string) => {
  activeCategory.value = value
  fetchPosts(1)
}

const handleSortChange = (value: string) => {
  activeSort.value = value
  fetchPosts(1)
}

const handleSearch = () => {
  fetchPosts(1)
}

const fetchPosts = async (pageNum = 1) => {
  if (pageNum === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const res = await listCommunityPostByPage({
      pageNum,
      pageSize: PAGE_SIZE,
      category: activeCategory.value || undefined,
      searchText: searchText.value.trim() || undefined,
      sortField: activeSort.value,
      sortOrder: 'descend'
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      postsTotal.value = res.data.data.totalRow || 0
      if (pageNum === 1) {
        posts.value = records
      } else {
        posts.value = [...posts.value, ...records]
      }
      page.value = pageNum
      hasMore.value = records.length === PAGE_SIZE
      const currentSelectedExists = posts.value.some(item => item.id === selectedPost.value?.id)
      const querySelectedExists = posts.value.some(item => item.id === selectedPostId.value)
      if (selectedPostId.value && !selectedPost.value?.id && querySelectedExists) {
        await selectPost(selectedPostId.value, false, true)
      } else if (!currentSelectedExists) {
        selectedPost.value = undefined
        replies.value = []
      }
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const fetchPostDetail = async (postId: number | string, increaseView = false) => {
  const normalizedPostId = normalizePostId(postId)
  if (!normalizedPostId) return
  const res = await getCommunityPostVOById({ id: normalizedPostId, increaseView })
  if (res.data.code === 0 && res.data.data) {
    selectedPost.value = res.data.data
    const index = posts.value.findIndex(item => item.id === normalizedPostId)
    if (index >= 0) {
      posts.value[index] = {
        ...posts.value[index],
        ...res.data.data
      }
    }
  }
}

const fetchReplies = async (postId: number | string) => {
  const normalizedPostId = normalizePostId(postId)
  if (!normalizedPostId) return
  repliesLoading.value = true
  try {
    const res = await listCommunityReplyByPage({
      postId: normalizedPostId,
      pageNum: 1,
      pageSize: 50
    })
    if (res.data.code === 0 && res.data.data) {
      replies.value = res.data.data.records || []
    }
  } finally {
    repliesLoading.value = false
  }
}

const selectPost = async (postId?: number | string, updateRoute = true, increaseView = true) => {
  const normalizedPostId = normalizePostId(postId)
  if (!normalizedPostId) return
  if (updateRoute) {
    if (Number(route.query.postId) !== normalizedPostId) {
      pendingIncreaseView.value = increaseView
      await router.replace({ query: { ...route.query, postId: String(normalizedPostId) } })
      return
    }
  }
  await fetchPostDetail(normalizedPostId, increaseView)
  await fetchReplies(normalizedPostId)
}

const loadMorePosts = async () => {
  await fetchPosts(page.value + 1)
}

const submitPost = async () => {
  if (!ensureLogin()) return
  if (!postForm.title?.trim()) {
    message.warning('请输入帖子标题')
    return
  }
  if (!postForm.content?.trim()) {
    message.warning('请输入帖子内容')
    return
  }
  submittingPost.value = true
  try {
    const res = await addCommunityPost({
      title: postForm.title.trim(),
      content: postForm.content.trim(),
      category: postForm.category
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('发布成功')
      const postId = res.data.data
      resetPostForm()
      await fetchPosts(1)
      await selectPost(postId)
    } else {
      message.error(res.data.message || '发布失败')
    }
  } catch {
    message.error('发布失败')
  } finally {
    submittingPost.value = false
  }
}

const submitReply = async () => {
  if (!ensureLogin()) return
  if (!selectedPost.value?.id) {
    message.warning('请先选择帖子')
    return
  }
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容')
    return
  }
  submittingReply.value = true
  try {
    const res = await addCommunityReply({
      postId: selectedPost.value.id,
      content: replyContent.value.trim()
    })
    if (res.data.code === 0) {
      message.success('回复成功')
      replyContent.value = ''
      await fetchPostDetail(selectedPost.value.id, false)
      await fetchReplies(selectedPost.value.id)
      const index = posts.value.findIndex(item => item.id === selectedPost.value?.id)
      if (index >= 0 && posts.value[index].replyCount != null) {
        posts.value[index].replyCount = selectedPost.value.replyCount
      }
    } else {
      message.error(res.data.message || '回复失败')
    }
  } catch {
    message.error('回复失败')
  } finally {
    submittingReply.value = false
  }
}

const handleLikePost = async () => {
  if (!ensureLogin() || !selectedPost.value?.id) return
  try {
    const res = await toggleCommunityPostLike({ postId: selectedPost.value.id })
    if (res.data.code === 0) {
      const liked = !!res.data.data
      selectedPost.value.hasLiked = liked
      selectedPost.value.likeCount = Math.max(0, (selectedPost.value.likeCount || 0) + (liked ? 1 : -1))
      const index = posts.value.findIndex(item => item.id === selectedPost.value?.id)
      if (index >= 0) {
        posts.value[index].hasLiked = liked
        posts.value[index].likeCount = selectedPost.value.likeCount
      }
    }
  } catch {
    message.error('操作失败')
  }
}

const handleLikeReply = async (reply: API.CommunityReplyVO) => {
  if (!ensureLogin() || !reply.id) return
  try {
    const res = await toggleCommunityReplyLike({ replyId: reply.id })
    if (res.data.code === 0) {
      const liked = !!res.data.data
      reply.hasLiked = liked
      reply.likeCount = Math.max(0, (reply.likeCount || 0) + (liked ? 1 : -1))
    }
  } catch {
    message.error('操作失败')
  }
}

watch(
  () => route.query.postId,
  async (value) => {
    if (!value) return
    const postId = Number(value)
    if (!Number.isNaN(postId) && selectedPost.value?.id !== postId) {
      const increaseView = pendingIncreaseView.value
      pendingIncreaseView.value = false
      await selectPost(postId, false, increaseView)
    }
  }
)

onMounted(async () => {
  await fetchPosts(1)
})
</script>

<style scoped>
.community-page {
  padding: 32px;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
}

.header-right {
  flex-shrink: 0;
}

.page-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.toolbar-card,
.composer-card,
.detail-card,
.empty-card,
.detail-placeholder {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
}

.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-label {
  font-size: 13px;
  color: #888;
}

.filter-chip {
  padding: 6px 14px;
  border: 1px solid #ececec;
  border-radius: 999px;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  border-color: #d9d9d9;
  color: #333;
  background: #fafafa;
}

.filter-chip.active {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 20px;
  align-items: start;
}

.left-column,
.right-column {
  min-width: 0;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 24px;
}

.info-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 16px;
}

.info-card-head {
  margin-bottom: 14px;
}

.info-card-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.info-card-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}

.hot-topic-list,
.announcement-list,
.stat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-topic-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 10px 0;
  border: none;
  border-bottom: 1px solid #f5f5f5;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.hot-topic-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hot-topic-item:hover .hot-topic-title {
  color: #111;
}

.hot-topic-rank {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f3f3f3;
  color: #666;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hot-topic-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.hot-topic-title {
  font-size: 13px;
  line-height: 1.6;
  color: #333;
  transition: color 0.2s ease;
}

.hot-topic-meta {
  font-size: 12px;
  color: #999;
}

.announcement-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: #fafafa;
  border: 1px solid #f3f3f3;
}

.announcement-title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.announcement-content {
  font-size: 12px;
  line-height: 1.7;
  color: #666;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stat-label {
  font-size: 13px;
  color: #333;
}

.stat-value {
  font-size: 12px;
  color: #999;
}

.stat-bar-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: #f2f2f2;
  overflow: hidden;
}

.stat-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #1a1a1a;
}

.composer-card {
  padding: 18px;
  margin-bottom: 20px;
}

.composer-head,
.section-header,
.reply-head,
.post-author-row,
.detail-author,
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.composer-meta,
.section-extra,
.word-count,
.post-time,
.stat-text,
.author-meta,
.detail-stats-inline {
  font-size: 12px;
  color: #999;
}

.composer-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.composer-row,
.composer-actions,
.detail-stats-inline,
.post-head,
.post-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.composer-actions {
  justify-content: flex-end;
}

.primary-btn,
.secondary-btn,
.load-more-btn,
.like-btn,
.icon-like-btn {
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary-btn {
  padding: 9px 18px;
  border: 1px solid #1a1a1a;
  background: #1a1a1a;
  color: #fff;
}

.primary-btn:hover:not(:disabled) {
  background: #333;
  border-color: #333;
}

.secondary-btn {
  padding: 9px 16px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.secondary-btn:hover {
  background: #fafafa;
  border-color: #d0d0d0;
}

.primary-btn:disabled,
.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.section-header {
  margin-bottom: 14px;
}

.small-gap {
  margin-bottom: 12px;
}

.section-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
}

.section-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.post-list,
.reply-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-card,
.reply-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
}

.post-card {
  padding: 18px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.post-card:hover {
  border-color: #d8d8d8;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}

.post-card.selected {
  border-color: #d0d0d0;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.06);
}

.skeleton-card {
  padding: 18px;
}

.top-badge,
.category-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.top-badge {
  background: #1a1a1a;
  color: #fff;
}

.category-badge {
  background: #f5f5f5;
  color: #666;
}

.post-title,
.detail-title {
  margin: 12px 0 10px;
  color: #1a1a1a;
  line-height: 1.5;
}

.post-title {
  font-size: 18px;
}

.detail-title {
  font-size: 22px;
}

.post-content,
.detail-content,
.reply-body,
.reply-content {
  color: #555;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-content {
  margin: 0 0 14px;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.author-info,
.author-texts {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-texts {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.author-name {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 600;
}

.post-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.latest-replies {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.latest-reply-item {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-user {
  color: #1a1a1a;
  font-weight: 500;
}

.detail-card {
  padding: 20px;
}

.detail-head {
  margin-bottom: 4px;
}

.detail-author {
  padding-bottom: 16px;
  margin-bottom: 18px;
  border-bottom: 1px solid #f5f5f5;
}

.detail-content {
  font-size: 14px;
  margin-bottom: 24px;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.like-btn:hover,
.icon-like-btn:hover {
  border-color: #d0d0d0;
  background: #fafafa;
}

.like-btn.liked,
.icon-like-btn.liked {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.reply-composer {
  padding: 14px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  margin-bottom: 14px;
}

.reply-card {
  padding: 14px;
}

.reply-head {
  margin-bottom: 10px;
}

.icon-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid #ececec;
  background: #fff;
  color: #666;
}

.empty-card,
.detail-placeholder {
  padding: 56px 24px;
  text-align: center;
}

.empty-title {
  margin: 0 0 8px;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.empty-desc,
.empty-inline {
  color: #999;
  font-size: 14px;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 18px 0 4px;
}

.load-more-btn {
  padding: 9px 22px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.load-more-btn:hover:not(:disabled) {
  background: #fafafa;
  border-color: #d0d0d0;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-stack {
    position: static;
  }
}

@media (max-width: 768px) {
  .community-page {
    padding: 20px;
  }

  .page-header,
  .composer-head,
  .section-header,
  .detail-author,
  .post-author-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }
}
</style>
