<template>
  <a-modal
    :open="open"
    title="评论区"
    :footer="null"
    width="680px"
    centered
    @cancel="$emit('update:open', false)"
  >
    <!-- Comment Input -->
    <div class="comment-input-section">
      <a-avatar :size="36" :src="loginUserStore.loginUser.userAvatar">
        {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
      </a-avatar>
      <div class="comment-input-wrap">
        <textarea
          v-model="newComment"
          class="comment-textarea"
          placeholder="写下你的评论..."
          rows="2"
          maxlength="500"
        />
        <div class="comment-input-footer">
          <span class="char-count">{{ newComment.length }}/500</span>
          <button
            class="comment-submit-btn"
            :disabled="!newComment.trim() || submitting"
            @click="handleSubmitComment"
          >
            {{ submitting ? '发布中...' : '发布评论' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Comment Stats -->
    <div class="comment-stats">
      <span class="comment-total">共 {{ totalComments }} 条评论</span>
    </div>

    <!-- Comment List -->
    <div class="comment-list" ref="commentListRef">
      <div v-if="loading && comments.length === 0" class="comment-loading">
        <a-spin size="small" />
        <span>加载中...</span>
      </div>

      <div v-else-if="comments.length === 0" class="comment-empty">
        暂无评论，来发表第一条评论吧
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <a-avatar :size="32" :src="comment.user?.userAvatar">
          {{ comment.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
        <div class="comment-body">
          <div class="comment-header">
            <span class="comment-author">{{ comment.user?.userName || '匿名用户' }}</span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-actions">
            <button
              :class="['action-btn', { active: comment.hasLiked }]"
              @click="handleToggleCommentLike(comment)"
            >
              <LikeOutlined v-if="!comment.hasLiked" />
              <LikeFilled v-else />
              <span>{{ comment.likeCount || 0 }}</span>
            </button>
            <button
              v-if="canDeleteComment(comment)"
              class="action-btn delete-btn"
              @click="handleDeleteComment(comment)"
            >
              <DeleteOutlined />
              <span>删除</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Load More -->
      <div v-if="hasMore && comments.length > 0" class="comment-load-more">
        <button class="load-more-btn" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, LikeFilled, LikeOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  addAppComment,
  deleteAppComment,
  listAppCommentByPage,
  toggleCommentLike
} from '@/api/socialController'

interface Props {
  open: boolean
  appId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:open', 'commentCountChange'])

const loginUserStore = useLoginUserStore()

const comments = ref<API.AppCommentVO[]>([])
const totalComments = ref(0)
const loading = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const newComment = ref('')
const submitting = ref(false)
const commentListRef = ref<HTMLElement>()

/**
 * 加载评论列表
 */
const fetchComments = async (reset = true) => {
  if (!props.appId) return
  if (reset) {
    pageNum.value = 1
    comments.value = []
  }
  loading.value = true
  try {
    const res = await listAppCommentByPage({
      appId: props.appId,
      pageNum: pageNum.value,
      pageSize
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      if (reset) {
        comments.value = records
      } else {
        comments.value = [...comments.value, ...records]
      }
      totalComments.value = res.data.data.totalRow || 0
      hasMore.value = comments.value.length < totalComments.value
    }
  } catch (e) {
    console.error('加载评论失败:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 加载更多评论
 */
const loadMore = () => {
  pageNum.value++
  fetchComments(false)
}

/**
 * 发布评论
 */
const handleSubmitComment = async () => {
  if (!newComment.value.trim() || !props.appId) return
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  submitting.value = true
  try {
    const res = await addAppComment({
      appId: props.appId,
      content: newComment.value.trim()
    })
    if (res.data.code === 0) {
      message.success('评论发布成功')
      newComment.value = ''
      await fetchComments(true)
      emit('commentCountChange', 1)
    } else {
      message.error('评论失败：' + (res.data.message || ''))
    }
  } catch (e) {
    message.error('评论失败，请重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 切换评论点赞
 */
const handleToggleCommentLike = async (comment: API.AppCommentVO) => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  if (!comment.id) return
  try {
    const res = await toggleCommentLike({ commentId: comment.id })
    if (res.data.code === 0) {
      const liked = res.data.data
      comment.hasLiked = liked
      comment.likeCount = (comment.likeCount || 0) + (liked ? 1 : -1)
    }
  } catch (e) {
    message.error('操作失败')
  }
}

/**
 * 判断当前用户是否可以删除评论
 */
const canDeleteComment = (comment: API.AppCommentVO): boolean => {
  const user = loginUserStore.loginUser
  if (!user.id) return false
  return comment.userId === user.id || user.userRole === 'admin'
}

/**
 * 删除评论
 */
const handleDeleteComment = (comment: API.AppCommentVO) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      if (!comment.id) return
      try {
        const res = await deleteAppComment({ id: comment.id })
        if (res.data.code === 0) {
          message.success('评论已删除')
          await fetchComments(true)
          emit('commentCountChange', -1)
        } else {
          message.error('删除失败：' + (res.data.message || ''))
        }
      } catch (e) {
        message.error('删除失败，请重试')
      }
    }
  })
}

/**
 * 格式化时间
 */
const formatTime = (time?: string): string => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString()
}

// 监听弹窗打开时加载评论
watch(() => props.open, (val) => {
  if (val && props.appId) {
    fetchComments(true)
  }
})
</script>

<style scoped>
.comment-input-section {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-input-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.comment-textarea:focus {
  border-color: #1a1a1a;
}

.comment-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 12px;
  color: #bbb;
}

.comment-submit-btn {
  padding: 6px 16px;
  background: #1a1a1a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.comment-submit-btn:hover:not(:disabled) {
  background: #333;
}

.comment-submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.comment-stats {
  padding: 12px 0 8px;
}

.comment-total {
  font-size: 13px;
  color: #999;
  font-weight: 500;
}

.comment-list {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.comment-list::-webkit-scrollbar { width: 4px; }
.comment-list::-webkit-scrollbar-thumb { background: #e0e0e0; border-radius: 4px; }

.comment-loading,
.comment-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: #bbb;
  font-size: 14px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #bbb;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  color: #bbb;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.15s;
}

.action-btn:hover {
  color: #666;
  background: #f5f5f5;
}

.action-btn.active {
  color: #1a1a1a;
}

.delete-btn:hover {
  color: #ff4d4f;
  background: #fff1f0;
}

.comment-load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.load-more-btn {
  padding: 6px 20px;
  background: none;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.load-more-btn:hover:not(:disabled) {
  border-color: #d0d0d0;
  background: #fafafa;
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
