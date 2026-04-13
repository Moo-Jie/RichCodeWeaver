<template>
  <div class="mode-switch">
    <div class="switch-track">
      <div
        :class="['switch-item', { active: mode === 'chat' }]"
        @click="$emit('update:mode', 'chat')"
      >
        对话模式
      </div>
      <div
        :class="['switch-item', { active: mode === 'app' }]"
        @click="$emit('update:mode', 'app')"
      >
        产物模式
      </div>
      <div :class="['switch-indicator', { right: mode === 'app' }]"></div>
    </div>
  </div>
</template>

<script lang="ts" setup>
interface Props {
  mode: 'chat' | 'app'
}

defineProps<Props>()
defineEmits(['update:mode'])
</script>

<style scoped>
.mode-switch {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
  flex-shrink: 0;
}

.switch-track {
  display: flex;
  position: relative;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 3px;
  gap: 2px;
}

.switch-item {
  position: relative;
  z-index: 1;
  padding: 8px 28px;
  font-size: 14px;
  font-weight: 500;
  color: #999;
  cursor: pointer;
  border-radius: 10px;
  transition: color 0.25s ease;
  user-select: none;
  white-space: nowrap;
}

.switch-item.active {
  color: #1a1a1a;
}

.switch-item:hover:not(.active) {
  color: #666;
}

.switch-indicator {
  position: absolute;
  top: 3px;
  left: 3px;
  width: calc(50% - 4px);
  height: calc(100% - 6px);
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.switch-indicator.right {
  transform: translateX(calc(100% + 2px));
}
</style>
