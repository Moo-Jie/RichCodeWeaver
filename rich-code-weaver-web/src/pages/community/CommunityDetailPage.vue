<template>
  <div class="community-detail-page">
    <div class="page-shell">
      <div class="detail-main-card">
        <div class="detail-topbar">
          <button class="back-btn" @click="goBack">返回社区</button>
          <div class="topbar-right">
            <button
              v-if="canDelete"
              :disabled="deleting"
              class="delete-btn"
              @click="handleDeletePost"
            >
              {{ deleting ? '删除中...' : '删除帖子' }}
            </button>
            <button
              v-if="isAdmin && post?.id"
              :disabled="togglingTop"
              class="admin-top-btn"
              @click="handleToggleTop"
            >
              {{ togglingTop ? '处理中...' : post?.isTop ? '取消置顶' : '设为置顶' }}
            </button>
            <span class="detail-time">最后活跃 {{ formatDateTime(post?.updateTime || post?.createTime) }}</span>
          </div>
        </div>

        <div v-if="loading" class="detail-loading">
          <a-skeleton active :paragraph="{ rows: 8 }" />
        </div>

        <template v-else-if="post">
          <div class="detail-badges">
            <span v-if="post.isTop" class="top-badge">置顶</span>
            <span class="category-badge">{{ getCategoryLabel(post.category) }}</span>
          </div>

          <h1 class="detail-title">{{ post.title }}</h1>

          <div class="detail-author-row">
            <div class="author-info">
              <a-avatar :size="42" :src="post.user?.userAvatar">
                {{ post.user?.userName?.charAt(0) || 'U' }}
              </a-avatar>
              <div class="author-texts">
                <span class="author-name">{{ post.user?.userName || '匿名用户' }}</span>
                <span class="author-meta">发布于 {{ formatDateTime(post.createTime) }}</span>
              </div>
            </div>
            <div class="detail-actions">
              <span class="stat-text">{{ post.viewCount || 0 }} 浏览</span>
              <span class="stat-text">{{ post.replyCount || 0 }} 回复</span>
              <button :class="['like-btn', { liked: post.hasLiked }]" @click="handleLikePost">
                {{ post.hasLiked ? '已点赞' : '点赞帖子' }} · {{ post.likeCount || 0 }}
              </button>
            </div>
          </div>

          <div class="detail-summary-strip">
            <div class="summary-card">
              <span class="summary-label">讨论状态</span>
              <strong class="summary-value">{{ post.replyCount || 0 }} 条回复</strong>
              <span class="summary-desc">欢迎围绕当前主题继续补充经验与方案</span>
            </div>
            <div class="summary-card">
              <span class="summary-label">阅读热度</span>
              <strong class="summary-value">{{ post.viewCount || 0 }} 次浏览</strong>
              <span class="summary-desc">浏览与互动会持续帮助更多人发现这条内容</span>
            </div>
          </div>

          <div class="markdown-shell">
            <MarkdownRenderer :content="post.content || ''" />
          </div>

          <div class="reply-section">
            <div class="section-header">
              <div>
                <h3 class="section-title">全部回复</h3>
                <p class="section-desc">围绕当前话题继续讨论</p>
              </div>
              <div class="reply-guide">
                <span class="reply-guide-chip">先说结论，再补步骤</span>
                <span class="reply-guide-chip">尽量描述你的实际场景</span>
              </div>
            </div>

            <div class="reply-composer">
              <a-textarea
                v-model:value="replyContent"
                :auto-size="{ minRows: 4, maxRows: 8 }"
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
              <div v-for="i in 3" :key="i" class="reply-card loading-card">
                <a-skeleton active :paragraph="{ rows: 2 }" />
              </div>
            </div>

            <div v-else-if="replies.length === 0" class="empty-card">
              <p class="empty-title">暂无回复</p>
              <p class="empty-desc">成为第一个参与讨论的人吧</p>
            </div>

            <div v-else class="reply-list">
              <div v-for="reply in replies" :key="reply.id" class="reply-card">
                <div class="reply-head">
                  <div class="author-info">
                    <a-avatar :size="30" :src="reply.user?.userAvatar">
                      {{ reply.user?.userName?.charAt(0) || 'U' }}
                    </a-avatar>
                    <div class="author-texts">
                      <span class="author-name">{{ reply.user?.userName || '匿名用户' }}</span>
                      <span class="author-meta">{{ formatDateTime(reply.createTime) }}</span>
                    </div>
                  </div>
                  <button :class="['icon-like-btn', { liked: reply.hasLiked }]" @click="handleLikeReply(reply)">
                    {{ reply.likeCount || 0 }} 赞
                  </button>
                </div>
                <div class="reply-body">
                  <MarkdownRenderer :content="reply.content || ''" />
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="empty-card">
          <p class="empty-title">帖子不存在或已被删除</p>
          <p class="empty-desc">请返回社区列表查看其他内容</p>
        </div>
      </div>

      <div class="detail-side">
        <CommunitySidebarCards :posts="relatedPosts" @select-post="handleSelectSidebarPost" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  addCommunityReply,
  deleteCommunityPost,
  getCommunityPostVOById,
  listCommunityPostByPage,
  listCommunityReplyByPage,
  toggleCommunityPostLike,
  toggleCommunityReplyLike,
  updateCommunityPostTopStatus
} from '@/api/communityController'
import CommunitySidebarCards from '@/components/community/CommunitySidebarCards.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const repliesLoading = ref(false)
const submittingReply = ref(false)
const replyContent = ref('')
const togglingTop = ref(false)
const deleting = ref(false)
const post = ref<API.CommunityPostVO>()
const replies = ref<API.CommunityReplyVO[]>([])
const relatedPosts = ref<API.CommunityPostVO[]>([])

const categoryOptions = [
  { label: '讨论', value: 'discuss' },
  { label: '问答', value: 'question' },
  { label: '反馈', value: 'feedback' },
  { label: '分享', value: 'share' }
]

const postId = computed(() => {
  const raw = route.params.id as string
  return raw || undefined
})

const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

const canDelete = computed(() => {
  if (!post.value?.id) return false
  const user = loginUserStore.loginUser
  if (!user?.id) return false
  return String(post.value.userId) === String(user.id) || user.userRole === 'admin'
})

const getCategoryLabel = (category?: string) => {
  return categoryOptions.find(item => item.value === category)?.label || '讨论'
}

const formatDateTime = (time?: string) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const ensureLogin = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录后再操作')
    router.push(`/user/login?redirect=${encodeURIComponent(window.location.href)}`)
    return false
  }
  return true
}

const fetchRelatedPosts = async () => {
  const res = await listCommunityPostByPage({
    pageNum: 1,
    pageSize: 10,
    sortField: 'lastActive',
    sortOrder: 'descend'
  })
  if (res.data.code === 0 && res.data.data) {
    relatedPosts.value = (res.data.data.records || []).filter(item => String(item.id) !== String(postId.value))
  }
}

const fetchPostDetail = async (increaseView = false) => {
  if (!postId.value) return
  loading.value = true
  try {
    const res = await getCommunityPostVOById({ id: postId.value, increaseView })
    if (res.data.code === 0) {
      post.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const fetchReplies = async () => {
  if (!postId.value) return
  repliesLoading.value = true
  try {
    const res = await listCommunityReplyByPage({
      postId: postId.value,
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

const loadPageData = async (increaseView = false) => {
  await Promise.all([fetchPostDetail(increaseView), fetchReplies(), fetchRelatedPosts()])
}

const handleLikePost = async () => {
  if (!ensureLogin() || !post.value?.id) return
  try {
    const res = await toggleCommunityPostLike({ postId: post.value.id })
    if (res.data.code === 0) {
      const liked = !!res.data.data
      post.value.hasLiked = liked
      post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (liked ? 1 : -1))
    }
  } catch {
    message.error('操作失败')
  }
}

const handleDeletePost = async () => {
  if (!ensureLogin() || !post.value?.id || !canDelete.value) return
  deleting.value = true
  try {
    const res = await deleteCommunityPost({ postId: post.value.id })
    if (res.data.code === 0) {
      message.success('帖子已删除')
      router.replace('/community')
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  } finally {
    deleting.value = false
  }
}

const handleToggleTop = async () => {
  if (!ensureLogin() || !post.value?.id || !isAdmin.value) return
  togglingTop.value = true
  try {
    const nextTop = post.value.isTop ? 0 : 1
    const res = await updateCommunityPostTopStatus({ postId: post.value.id, isTop: nextTop })
    if (res.data.code === 0) {
      post.value.isTop = nextTop
      message.success(nextTop ? '已设为置顶' : '已取消置顶')
      await fetchRelatedPosts()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    togglingTop.value = false
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

const submitReply = async () => {
  if (!ensureLogin() || !post.value?.id) return
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容')
    return
  }
  submittingReply.value = true
  try {
    const res = await addCommunityReply({
      postId: post.value.id,
      content: replyContent.value.trim()
    })
    if (res.data.code === 0) {
      message.success('回复成功')
      replyContent.value = ''
      await Promise.all([fetchPostDetail(false), fetchReplies(), fetchRelatedPosts()])
    } else {
      message.error(res.data.message || '回复失败')
    }
  } catch {
    message.error('回复失败')
  } finally {
    submittingReply.value = false
  }
}

const handleSelectSidebarPost = (id?: number | string) => {
  if (!id) return
  router.push(`/community/post/${id}`)
}

const goBack = () => {
  router.push('/community')
}

watch(
  () => route.params.id,
  async (value, oldValue) => {
    if (!value) return
    await loadPageData(value !== oldValue)
  }
)

onMounted(async () => {
  await loadPageData(true)
})
</script>

<style scoped>
.community-detail-page {
  padding: 32px;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
}

.page-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 320px;
  gap: 20px;
  align-items: start;
}

.detail-main-card,
.empty-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
}

.detail-main-card {
  padding: 24px;
}

.detail-side {
  position: sticky;
  top: 24px;
}

.detail-topbar,
.detail-author-row,
.reply-head,
.composer-actions,
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.detail-topbar {
  margin-bottom: 18px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.back-btn,
.primary-btn,
.like-btn,
.icon-like-btn,
.admin-top-btn {
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn {
  padding: 8px 14px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.back-btn:hover,
.icon-like-btn:hover,
.like-btn:hover,
.admin-top-btn:hover {
  background: #fafafa;
  border-color: #d0d0d0;
}

.admin-top-btn {
  padding: 8px 14px;
  border: 1px solid #d9deea;
  background: #f7f9fc;
  color: #384152;
}

.delete-btn {
  padding: 8px 14px;
  border: 1px solid #f0d0d0;
  background: #fef7f7;
  color: #a04040;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background: #fdecea;
  border-color: #e0b0b0;
}

.delete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-time,
.stat-text,
.author-meta,
.word-count,
.section-desc {
  font-size: 12px;
  color: #999;
}

.detail-badges {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
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

.detail-title {
  margin: 0 0 18px;
  color: #1a1a1a;
  font-size: 28px;
  line-height: 1.45;
}

.author-info,
.author-texts,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-texts {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.author-name,
.section-title {
  color: #1a1a1a;
  font-weight: 600;
}

.author-name {
  font-size: 14px;
}

.detail-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-summary-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 20px 0 0;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid #eef1f5;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 65%, #f4f6fb 100%);
}

.summary-label,
.summary-desc {
  color: #8f8f8f;
  font-size: 12px;
}

.summary-value {
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 700;
}

.like-btn {
  padding: 8px 14px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.like-btn.liked,
.icon-like-btn.liked {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.markdown-shell {
  margin: 24px 0 30px;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f5f5f5;
  background: #fcfcfc;
}

.reply-section {
  border-top: 1px solid #f5f5f5;
  padding-top: 24px;
}

.section-header {
  margin-bottom: 14px;
}

.reply-guide {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reply-guide-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #eceff4;
  background: #fafbfd;
  color: #7d8594;
  font-size: 12px;
}

.section-title {
  margin: 0 0 4px;
  font-size: 18px;
}

.reply-composer {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fafafa 0%, #f7f8fa 100%);
  border-radius: 14px;
}

.composer-actions {
  margin-top: 12px;
}

.primary-btn {
  padding: 9px 18px;
  border: 1px solid #1a1a1a;
  background: #1a1a1a;
  color: #fff;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reply-card {
  padding: 14px;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  background: #fff;
}

.loading-card {
  padding: 18px;
}

.reply-head {
  margin-bottom: 10px;
}

.icon-like-btn {
  padding: 6px 10px;
  border: 1px solid #ececec;
  background: #fff;
  color: #666;
}

.reply-body {
  color: #555;
  line-height: 1.8;
  word-break: break-word;
}

.detail-loading,
.empty-card {
  padding: 56px 24px;
}

.empty-card {
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

@media (max-width: 1180px) {
  .page-shell {
    grid-template-columns: 1fr;
  }

  .detail-side {
    position: static;
  }

  .detail-summary-strip {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .community-detail-page {
    padding: 20px;
  }

  .detail-topbar,
  .detail-author-row,
  .reply-head,
  .section-header,
  .topbar-right {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
