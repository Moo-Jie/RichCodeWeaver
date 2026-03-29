<template>
  <div v-if="tasks.length > 0" class="task-plan-panel">
    <div class="panel-header">
      <span class="panel-title">📋 任务计划</span>
      <span class="panel-progress">{{ summary.completed }}/{{ summary.total }}</span>
    </div>
    <div class="task-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-item"
        :class="`task-item--${task.status}`"
      >
        <span class="task-icon">{{ getStatusIcon(task.status) }}</span>
        <span class="task-step">{{ task.step }}</span>
        <span v-if="task.notes" class="task-notes">({{ task.notes }})</span>
      </div>
    </div>
    <div class="panel-footer">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="progress-text">
        {{ summary.inProgress > 0 ? `${summary.inProgress} 进行中` : '' }}
        {{ summary.pending > 0 ? `${summary.pending} 待处理` : '' }}
      </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

interface Task {
  id: number
  step: string
  status: 'pending' | 'in_progress' | 'completed'
  notes?: string
}

interface Summary {
  total: number
  completed: number
  inProgress: number
  pending: number
}

interface Props {
  tasks: Task[]
  summary: Summary
}

const props = withDefaults(defineProps<Props>(), {
  tasks: () => [],
  summary: () => ({ total: 0, completed: 0, inProgress: 0, pending: 0 })
})

const progressPercent = computed(() => {
  if (props.summary.total === 0) return 0
  return Math.round((props.summary.completed / props.summary.total) * 100)
})

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'completed':
      return '✅'
    case 'in_progress':
      return '🔄'
    default:
      return '⏳'
  }
}
</script>

<style scoped>
.task-plan-panel {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin: 12px 0;
  font-size: 13px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.panel-title {
  font-weight: 600;
  color: #1a1a1a;
}

.panel-progress {
  color: #666;
  font-size: 12px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.task-item--pending {
  color: #999;
}

.task-item--in_progress {
  background: #e6f7ff;
  color: #1890ff;
}

.task-item--completed {
  color: #52c41a;
}

.task-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.task-step {
  flex: 1;
}

.task-notes {
  color: #999;
  font-size: 12px;
}

.panel-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #e8e8e8;
}

.progress-bar {
  height: 4px;
  background: #e8e8e8;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #52c41a, #73d13d);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  color: #999;
  font-size: 12px;
}
</style>
