<template>
  <div v-if="tasks.length > 0" class="task-plan-panel">
    <div class="panel-header">
      <span class="panel-title"><UnorderedListOutlined class="panel-title-icon" />执行计划</span>
      <span class="panel-progress">{{ summary.completed }}/{{ summary.total }}</span>
    </div>
    <div class="task-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-item"
        :class="`task-item--${task.status}`"
      >
        <span class="task-status" :aria-label="getStatusLabel(task.status)">
          <span class="task-dot"></span>
        </span>
        <span class="task-step">{{ task.step }}</span>
        <span v-if="task.notes" class="task-notes">{{ task.notes }}</span>
        <span class="task-label">{{ getStatusLabel(task.status) }}</span>
      </div>
    </div>
    <div class="panel-footer">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="progress-text">
        {{ summary.inProgress > 0 ? `${summary.inProgress} 项进行中` : '等待下一阶段' }}
      </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { UnorderedListOutlined } from '@ant-design/icons-vue'

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

const getStatusLabel = (status: Task['status']) => {
  if (status === 'completed') return '完成'
  if (status === 'in_progress') return '进行中'
  return '待处理'
}
</script>

<style scoped>
.task-plan-panel {
  margin: 12px 0;
  padding: 12px 14px;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #fafafa;
  font-size: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ececec;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #333;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.panel-title-icon {
  color: #888;
  font-size: 12px;
}

.panel-progress {
  color: #999;
  font-size: 11px;
}

.task-list {
  display: flex;
  flex-direction: column;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 6px 0;
  border-bottom: 1px solid #f2f2f2;
}

.task-item:last-child {
  border-bottom: 0;
}

.task-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 8px;
}

.task-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c8c8c8;
}

.task-item--completed .task-dot {
  background: #999;
}

.task-item--in_progress .task-dot {
  background: #1a1a1a;
}

.task-step {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #555;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-item--completed .task-step {
  color: #888;
}

.task-item--in_progress .task-step {
  color: #1f1f1f;
  font-weight: 500;
}

.task-notes {
  max-width: 180px;
  overflow: hidden;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-label {
  flex-shrink: 0;
  color: #aaa;
  font-size: 10px;
}

.task-item--in_progress .task-label {
  color: #555;
}

.panel-footer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #ececec;
}

.progress-bar {
  height: 2px;
  margin-bottom: 7px;
  overflow: hidden;
  border-radius: 2px;
  background: #e8e8e8;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  background: #1a1a1a;
  transition: width 220ms ease;
}

.progress-text {
  color: #999;
  font-size: 10.5px;
}
</style>
