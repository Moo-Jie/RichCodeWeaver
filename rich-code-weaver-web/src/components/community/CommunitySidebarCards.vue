<template>
  <div class="side-stack">
    <div class="info-card compact-card">
      <div class="info-card-head compact-head">
        <h3 class="info-card-title">热门话题</h3>
        <span class="info-card-tip">Top {{ hotTopics.length }}</span>
      </div>
      <div v-if="hotTopics.length" class="hot-topic-list compact-list">
        <button
          v-for="(topic, index) in hotTopics"
          :key="topic.id"
          class="hot-topic-item"
          @click="$emit('select-post', topic.id)"
        >
          <span class="hot-topic-rank">{{ index + 1 }}</span>
          <span class="hot-topic-main">
            <span class="hot-topic-title">{{ topic.title }}</span>
            <span class="hot-topic-meta">{{ topic.replyCount || 0 }} 回复 · {{ topic.likeCount || 0 }} 点赞</span>
          </span>
        </button>
      </div>
      <div v-else class="empty-inline">当前还没有可展示的话题</div>
    </div>

    <div class="info-card compact-card">
      <div class="info-card-head compact-head">
        <h3 class="info-card-title">分类统计</h3>
        <span class="info-card-tip">当前列表</span>
      </div>
      <div class="stat-list compact-list">
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
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

interface Props {
  posts: API.CommunityPostVO[]
}

const props = defineProps<Props>()

defineEmits<{
  (e: 'select-post', postId?: number | string): void
}>()

const communityAnnouncements = [
  {
    title: '友善交流',
    content: '聚焦问题本身，尽量避免无关争执。'
  },
  {
    title: '描述具体',
    content: '提问建议写清场景、步骤和预期结果。'
  }
]

const categoryOptions = [
  { label: '讨论', value: 'discuss' },
  { label: '问答', value: 'question' },
  { label: '反馈', value: 'feedback' },
  { label: '分享', value: 'share' }
]

const hotTopics = computed(() => {
  return [...(props.posts || [])]
    .sort((a, b) => {
      const scoreA = (a.replyCount || 0) * 3 + (a.likeCount || 0) * 2 + (a.viewCount || 0)
      const scoreB = (b.replyCount || 0) * 3 + (b.likeCount || 0) * 2 + (b.viewCount || 0)
      return scoreB - scoreA
    })
    .slice(0, 3)
})

const categoryStats = computed(() => {
  const total = props.posts.length || 1
  return categoryOptions.map(item => {
    const count = props.posts.filter(post => post.category === item.value).length
    return {
      ...item,
      count,
      percent: Math.max(count > 0 ? Math.round((count / total) * 100) : 0, count > 0 ? 12 : 0)
    }
  })
})
</script>

<style scoped>
.side-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
}

.compact-card {
  padding: 14px;
}

.info-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.compact-head {
  margin-bottom: 10px;
}

.info-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.info-card-tip {
  font-size: 12px;
  color: #999;
}

.compact-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hot-topic-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 8px 0;
  border: none;
  border-bottom: 1px solid #f5f5f5;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.hot-topic-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hot-topic-rank {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f4f4f4;
  color: #666;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.hot-topic-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.hot-topic-title {
  color: #333;
  font-size: 13px;
  line-height: 1.5;
}

.hot-topic-meta,
.announcement-content,
.stat-value,
.empty-inline {
  color: #999;
  font-size: 12px;
}

.announcement-item {
  border-radius: 12px;
  border: 1px solid #f5f5f5;
  background: #fafafa;
}

.compact-announcement {
  padding: 10px;
}

.announcement-title,
.stat-label {
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 600;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-item-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.stat-bar-track {
  width: 100%;
  height: 5px;
  border-radius: 999px;
  background: #f2f2f2;
  overflow: hidden;
}

.stat-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #1a1a1a;
}
</style>
