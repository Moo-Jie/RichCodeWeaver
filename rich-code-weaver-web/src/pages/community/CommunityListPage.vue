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
        <button class="primary-btn" @click="openPostEditor">发布帖子</button>
      </div>
    </div>

    <div class="overview-strip">
      <div v-for="item in overviewCards" :key="item.label" class="overview-card">
        <span class="overview-label">{{ item.label }}</span>
        <strong class="overview-value">{{ item.value }}</strong>
        <span class="overview-desc">{{ item.desc }}</span>
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
        <div class="section-header">
          <div>
            <h3 class="section-title">帖子列表</h3>
            <p class="section-desc">主体内容优先突出展示，评论预览作为次级信息辅助浏览</p>
          </div>
          <div class="section-extra">共 {{ postsTotal }} 条</div>
        </div>

        <div v-if="loading && posts.length === 0" class="post-list">
          <div v-for="i in 4" :key="i" class="post-card skeleton-card">
            <a-skeleton active :paragraph="{ rows: 5 }" />
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
            class="post-card"
            @click="selectPost(post.id)"
          >
            <div class="post-head">
              <div class="post-head-left">
                <span v-if="post.isTop" class="top-badge">置顶</span>
                <span class="category-badge">{{ getCategoryLabel(post.category) }}</span>
              </div>
              <span class="post-time">{{ formatTime(post.updateTime || post.createTime) }}</span>
            </div>

            <div class="post-main">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-content">{{ getPostPreview(post.content) }}</p>
            </div>

            <div class="post-footer">
              <div class="author-info">
                <a-avatar :size="30" :src="post.user?.userAvatar">
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
                <span class="detail-link">查看详情</span>
              </div>
            </div>

            <div v-if="post.latestReplies?.length" class="latest-replies">
              <div class="latest-replies-title">最新评论</div>
              <div v-for="reply in post.latestReplies.slice(0, 2)" :key="reply.id" class="latest-reply-item">
                <a-avatar :size="20" :src="reply.user?.userAvatar" class="reply-avatar">
                  {{ reply.user?.userName?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="latest-reply-content">
                  <span class="reply-user">{{ reply.user?.userName || '用户' }}</span>
                  <span class="reply-content">{{ getReplyPreview(reply.content) }}</span>
                </div>
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
        <CommunitySidebarCards :posts="posts" @select-post="selectPost" />
      </div>
    </div>

    <a-modal
      v-model:open="editorOpen"
      :body-style="{ padding: '0', height: 'calc(90vh - 55px)', overflow: 'hidden' }"
      :footer="null"
      centered
      destroy-on-close
      width="95vw"
    >
      <div class="editor-wrapper">
        <div class="editor-intro">
          <div>
            <h3 class="editor-intro-title">发布新帖子</h3>
            <p class="editor-intro-desc">先选模块，再补充标题和内容，会更容易获得高质量回复。</p>
          </div>
        </div>
        <div class="editor-toolbar">
          <div class="editor-meta">
            <div class="module-picker">
              <span class="editor-meta-label">请选择发布模块</span>
              <div class="module-chip-list">
                <button
                  v-for="item in publishCategoryOptions"
                  :key="item.value"
                  :class="['module-chip', { active: postForm.category === item.value }]"
                  @click="selectPublishCategory(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
            <span class="editor-char-count">{{ postForm.content.length }}/10000</span>
          </div>
          <div class="editor-actions">
            <button class="secondary-btn" @click="closePostEditor">取消</button>
            <button :disabled="submittingPost" class="primary-btn" @click="submitPost">
              {{ submittingPost ? '发布中...' : '确认发布' }}
            </button>
          </div>
        </div>
        <div class="editor-form">
          <a-input
            v-model:value="postForm.title"
            :maxlength="200"
            class="editor-title-input"
            placeholder="请输入帖子标题，清楚描述你的问题或想法"
          />
          <div class="editor-assist-row">
            <div class="editor-assist-main">
              <span class="editor-assist-label">不知道怎么写？</span>
              <button class="assist-btn" @click="applyTemplate">帖子模板</button>
            </div>
            <span class="editor-assist-text">建议写清楚场景、步骤、现象或你的结论，别人会更容易理解。</span>
          </div>
          <div class="editor-container">
            <MdEditor
              v-model="postForm.content"
              :language="'zh-CN'"
              :preview="true"
              :show-code-row-number="true"
              :theme="'light'"
              style="height: 100%"
            />
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { addCommunityPost, listCommunityPostByPage } from '@/api/communityController'
import CommunitySidebarCards from '@/components/community/CommunitySidebarCards.vue'
import { useLoginUserStore } from '@/stores/loginUser'

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

const publishCategoryOptions = categoryOptions.slice(1)

const templateMap: Record<string, { title: string; content: string }> = {
  question: {
    title: '请教一个使用问题：',
    content: '### 当前场景\n- 我在做什么：\n- 相关配置 / 步骤：\n\n### 遇到的问题\n- 实际现象：\n- 已尝试的方法：\n\n### 希望得到的帮助\n- 期望结果：\n'
  },
  feedback: {
    title: '功能反馈建议：',
    content: '### 反馈背景\n- 我使用的场景：\n\n### 当前问题\n- 具体不便之处：\n\n### 建议方案\n- 希望如何优化：\n'
  },
  share: {
    title: '分享一个使用经验：',
    content: '### 分享内容\n- 我完成了什么：\n\n### 关键做法\n- 核心步骤 / 提示词：\n\n### 经验总结\n- 适合哪些人参考：\n'
  },
  discuss: {
    title: '一起讨论一个想法：',
    content: '### 想讨论的话题\n- 背景：\n\n### 我的看法\n- 目前思路：\n\n### 欢迎交流\n- 想听大家怎么看：\n'
  }
}

const activeCategory = ref('')
const activeSort = ref('lastActive')
const searchText = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(false)
const postsTotal = ref(0)
const editorOpen = ref(false)
const submittingPost = ref(false)
const posts = ref<API.CommunityPostVO[]>([])

const postForm = reactive<PostFormState>({
  title: '',
  content: '',
  category: ''
})

const overviewCards = computed(() => {
  const questionCount = posts.value.filter(item => item.category === 'question').length
  const shareCount = posts.value.filter(item => item.category === 'share').length
  const topCount = posts.value.filter(item => item.isTop).length
  return [
    {
      label: '当前话题',
      value: postsTotal.value || posts.value.length,
      desc: '当前筛选条件下的帖子总量'
    },
    {
      label: '问答求助',
      value: questionCount,
      desc: '社区内正在寻求帮助的问题帖'
    },
    {
      label: '经验分享',
      value: shareCount,
      desc: `其中置顶内容 ${topCount} 条`
    }
  ]
})

const ensureLogin = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录后再操作')
    router.push(`/user/login?redirect=${encodeURIComponent(window.location.href)}`)
    return false
  }
  return true
}

const stripMarkdown = (content?: string) => {
  if (!content) return ''
  return content
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[>*_~|-]/g, ' ')
    .replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const getPostPreview = (content?: string) => {
  return stripMarkdown(content).slice(0, 180) || '暂无内容预览'
}

const getReplyPreview = (content?: string) => {
  return stripMarkdown(content).slice(0, 80) || '暂无评论内容'
}

const getCategoryLabel = (category?: string) => {
  return categoryOptions.find(item => item.value === category)?.label || '讨论'
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const resetPostForm = () => {
  postForm.title = ''
  postForm.content = ''
  postForm.category = ''
}

const selectPublishCategory = (value: string) => {
  postForm.category = value
}

const applyTemplate = () => {
  if (!postForm.category) {
    message.warning('请先选择发布模块')
    return
  }
  const template = templateMap[postForm.category]
  if (!template) return
  if (!postForm.title.trim()) {
    postForm.title = template.title
  }
  postForm.content = postForm.content.trim()
    ? `${postForm.content.trim()}\n\n${template.content}`
    : template.content
  message.success('已插入参考结构')
}

const openPostEditor = () => {
  if (!ensureLogin()) return
  editorOpen.value = true
}

const closePostEditor = () => {
  editorOpen.value = false
  resetPostForm()
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
      posts.value = pageNum === 1 ? records : [...posts.value, ...records]
      page.value = pageNum
      hasMore.value = records.length === PAGE_SIZE
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMorePosts = async () => {
  await fetchPosts(page.value + 1)
}

const selectPost = (postId?: number | string) => {
  if (!postId) return
  router.push(`/community/post/${postId}`)
}

const submitPost = async () => {
  if (!ensureLogin()) return
  if (!postForm.category) {
    message.warning('请选择发布模块')
    return
  }
  if (!postForm.title.trim()) {
    message.warning('请输入帖子标题')
    return
  }
  if (!postForm.content.trim()) {
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
      closePostEditor()
      await fetchPosts(1)
      selectPost(postId)
    } else {
      message.error(res.data.message || '发布失败')
    }
  } catch {
    message.error('发布失败')
  } finally {
    submittingPost.value = false
  }
}

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

.page-header,
.section-header,
.post-head,
.post-footer,
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header {
  margin-bottom: 24px;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 68%, #f4f6fb 100%);
}

.overview-label,
.overview-desc {
  color: #8f8f8f;
  font-size: 12px;
}

.overview-value {
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 700;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.page-desc,
.toolbar-label,
.section-desc,
.section-extra,
.post-time,
.author-meta,
.stat-text,
.latest-replies-title,
.detail-link,
.editor-char-count {
  color: #999;
  font-size: 12px;
}

.toolbar-card,
.empty-card,
.post-card {
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
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-chip,
.primary-btn,
.secondary-btn,
.load-more-btn {
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip {
  padding: 6px 14px;
  border: 1px solid #ececec;
  background: #fff;
  color: #666;
  font-size: 13px;
}

.filter-chip.active,
.primary-btn {
  background: #1a1a1a;
  border: 1px solid #1a1a1a;
  color: #fff;
}

.filter-chip:hover,
.secondary-btn:hover,
.load-more-btn:hover:not(:disabled) {
  background: #fafafa;
  border-color: #d0d0d0;
}

.filter-chip.active:hover,
.primary-btn:hover:not(:disabled) {
  background: #333;
  border-color: #333;
}

.primary-btn,
.secondary-btn,
.load-more-btn {
  padding: 9px 18px;
}

.secondary-btn,
.load-more-btn {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.primary-btn:disabled,
.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 320px;
  gap: 20px;
  align-items: start;
}

.left-column,
.right-column {
  min-width: 0;
}

.right-column {
  position: sticky;
  top: 24px;
}

.section-header {
  margin-bottom: 14px;
}

.section-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.post-card:hover {
  border-color: #d8d8d8;
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(16, 24, 40, 0.08);
}

.skeleton-card {
  padding: 18px;
}

.post-head-left,
.post-stats,
.author-info,
.author-texts,
.latest-reply-item,
.latest-reply-content,
.editor-meta,
.editor-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.post-main {
  margin: 14px 0 16px;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fcfcfc 0%, #f8f8f8 100%);
  border: 1px solid #f6f6f6;
}

.post-title {
  margin: 0 0 10px;
  color: #1a1a1a;
  font-size: 20px;
  line-height: 1.5;
}

.post-content {
  margin: 0;
  color: #555;
  font-size: 14px;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.author-texts,
.latest-reply-content {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.author-name,
.reply-user {
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 600;
}

.post-stats {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-link {
  color: #1a1a1a;
  font-weight: 600;
}

.latest-replies {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.latest-replies-title {
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.latest-reply-item {
  align-items: flex-start;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-content {
  color: #888;
  font-size: 12px;
  line-height: 1.5;
}

.empty-card {
  padding: 56px 24px;
  text-align: center;
}

.empty-title {
  margin: 0 0 8px;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.empty-desc {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 18px 0 4px;
}

.editor-wrapper {
  height: calc(90vh - 55px);
  display: flex;
  flex-direction: column;
}

.editor-intro {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 60%, #f4f6fb 100%);
}

.editor-intro-title {
  margin: 0 0 6px;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 700;
}

.editor-intro-desc {
  margin: 0;
  color: #7a7a7a;
  font-size: 13px;
}

.editor-tip-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.editor-tip-item {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #eceff4;
  color: #666;
  font-size: 12px;
}

.editor-toolbar {
  padding: 16px 18px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.editor-meta {
  flex: 1;
  flex-wrap: wrap;
  align-items: flex-start;
}

.editor-meta-label,
.editor-assist-label {
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 600;
}

.module-picker,
.editor-assist-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.module-chip,
.assist-btn {
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid #e7e7e7;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.module-chip:hover,
.assist-btn:hover {
  border-color: #d5d9e2;
  background: #fafbfc;
}

.module-chip.active {
  border-color: #1a1a1a;
  background: #1a1a1a;
  color: #fff;
}

.editor-actions {
  justify-content: flex-end;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  height: 100%;
}

.editor-title-input {
  flex-shrink: 0;
}

.editor-assist-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  background: #fafafa;
}

.editor-assist-text {
  color: #909090;
  font-size: 12px;
}

.editor-container {
  min-height: 0;
  flex: 1;
}

@media (max-width: 1180px) {
  .overview-strip {
    grid-template-columns: 1fr;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .right-column {
    position: static;
  }
}

@media (max-width: 768px) {
  .community-page {
    padding: 20px;
  }

  .page-header,
  .section-header,
  .post-head,
  .post-footer,
  .editor-toolbar,
  .editor-intro,
  .editor-assist-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .editor-tip-list {
    justify-content: flex-start;
  }
}
</style>
