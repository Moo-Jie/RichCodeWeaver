<template>
  <div class="chat-input-wrap">
    <!-- Selected element hint -->
    <div v-if="selectedElement" class="selected-hint">
      <div class="hint-content">
        <div class="hint-title">
          <span class="hint-icon">✓</span>
          <span class="hint-label">已选中元素</span>
        </div>
        <div class="hint-details">
          <span class="detail-item">
            <span class="detail-label">标签:</span>
            <code class="detail-value">{{ selectedElement.tagName }}</code>
          </span>
          <span v-if="selectedElement.className" class="detail-item">
            <span class="detail-label">类名:</span>
            <code class="detail-value">{{ selectedElement.className }}</code>
          </span>
          <span class="detail-item">
            <span class="detail-label">选择器:</span>
            <code class="detail-value detail-selector">{{ selectedElement.selector }}</code>
          </span>
        </div>
      </div>
      <button class="hint-close" @click="$emit('clearSelection')" title="取消选择">✕</button>
    </div>

    <!-- Not owner tip -->
    <div v-if="!isOwner && isAppMode" class="not-owner-tip">
      <span>这是别人的创作作品，如需对话请创建您自己的项目</span>
    </div>

    <!-- Input Area -->
    <div v-else class="input-area">
      <!-- Capsule mode selector (above input, only for create mode) -->
      <div v-if="showModeSelector" class="capsule-switch">
        <button
          :class="['capsule-item', { active: generatorMode }]"
          @click="$emit('update:generatorMode', true)"
        >分步执行</button>
        <button
          :class="['capsule-item', { active: !generatorMode }]"
          @click="$emit('update:generatorMode', false)"
        >Agent 智能生成</button>
      </div>

      <div class="input-row">
        <textarea
          ref="inputRef"
          :disabled="sending"
          :placeholder="placeholder"
          :value="modelValue"
          class="chat-textarea"
          rows="1"
          @input="handleInput"
          @keydown.enter.exact.prevent="handleSend"
        ></textarea>
        <button :class="['send-btn', { disabled: sending || !modelValue?.trim() }]" :disabled="sending" @click="handleSend">
          <template v-if="sending">
            <a-spin :indicator="indicator" size="small" />
          </template>
          <template v-else>
            <SendOutlined />
          </template>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {h, ref, nextTick, onMounted} from 'vue'
import {SendOutlined, LoadingOutlined} from '@ant-design/icons-vue'

interface ElementInfo {
  tagName: string
  className: string
  selector: string
}

interface Props {
  modelValue: string
  sending?: boolean
  isOwner?: boolean
  isAppMode?: boolean
  selectedElement?: ElementInfo | null
  showModeSelector?: boolean
  generatorMode?: boolean
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  sending: false,
  isOwner: true,
  isAppMode: false,
  showModeSelector: false,
  generatorMode: true,
  placeholder: '描述您想要创建的应用...'
})

const emit = defineEmits(['update:modelValue', 'send', 'clearSelection', 'update:generatorMode'])

const inputRef = ref<HTMLTextAreaElement>()
const baseHeight = ref(0)
const indicator = h(LoadingOutlined, {style: {fontSize: '16px', color: '#999'}, spin: true})

onMounted(() => {
  if (inputRef.value) {
    baseHeight.value = inputRef.value.scrollHeight || 22
  }
})

const handleInput = (e: Event) => {
  const target = e.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  autoResize(target)
}

const autoResize = (el: HTMLTextAreaElement) => {
  el.style.height = 'auto'
  const maxH = (baseHeight.value || 22) * 2
  el.style.height = Math.min(el.scrollHeight, maxH) + 'px'
}

const handleSend = () => {
  emit('send')
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
    }
  })
}

defineExpose({focus: () => inputRef.value?.focus()})
</script>

<style scoped>
.chat-input-wrap {
  padding: 12px 24px 20px;
  flex-shrink: 0;
}

.selected-hint {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #f0f9ff;
  border: 1px solid #bae7ff;
  border-radius: 10px;
  font-size: 13px;
  gap: 12px;
}

.hint-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hint-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1a1a1a;
}

.hint-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: #52c41a;
  color: #fff;
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
}

.hint-label {
  font-size: 14px;
}

.hint-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.detail-label {
  color: #666;
  min-width: 50px;
}

.detail-value {
  background: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #1a1a1a;
}

.detail-selector {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hint-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  font-size: 16px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.15s;
  flex-shrink: 0;
  line-height: 1;
}

.hint-close:hover {
  background: #f0f0f0;
}

.not-owner-tip {
  text-align: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  color: #999;
  font-size: 14px;
}

.input-area {
  max-width: 720px;
  margin: 0 auto;
}

/* Capsule mode switch */
.capsule-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 10px;
  background: #f0f0f0;
  border-radius: 20px;
  padding: 3px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

.capsule-item {
  padding: 5px 16px;
  border: none;
  border-radius: 18px;
  background: transparent;
  color: #999;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.capsule-item.active {
  background: #1a1a1a;
  color: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.10);
}

.capsule-item:not(.active):hover {
  color: #666;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #f7f7f8;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  padding: 10px 12px 10px 18px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-row:focus-within {
  border-color: #d0d0d0;
  box-shadow: 0 0 0 3px rgba(0,0,0,0.03);
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  color: #1a1a1a;
  font-family: inherit;
  min-height: 22px;
  overflow-y: auto;
}

.chat-textarea::placeholder {
  color: #bbb;
}

.chat-textarea:disabled {
  opacity: 0.5;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;
  font-size: 15px;
}

.send-btn:hover:not(.disabled) {
  background: #333;
  transform: scale(1.04);
}

.send-btn.disabled {
  background: #e5e5e5;
  color: #bbb;
  cursor: not-allowed;
}
</style>
