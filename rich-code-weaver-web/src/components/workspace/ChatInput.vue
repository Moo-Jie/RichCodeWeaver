<template>
  <div class="chat-input-wrap">
    <!-- Selected element hint -->
    <div v-if="selectedElement" class="selected-hint">
      <span class="hint-tag">已选择元素: {{ selectedElement.tagName }}</span>
      <button class="hint-close" @click="$emit('clearSelection')">✕</button>
    </div>

    <!-- Not owner tip -->
    <div v-if="!isOwner && isAppMode" class="not-owner-tip">
      <span>这是别人的创作作品，如需对话请创建您自己的项目</span>
    </div>

    <!-- Input Area -->
    <div v-else class="input-area">
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

      <!-- Mode selector (only for create mode, not app chat) -->
      <div v-if="showModeSelector" class="mode-selector">
        <a-select
          :value="generatorMode"
          class="mode-select"
          size="small"
          @change="(val) => $emit('update:generatorMode', val)"
        >
          <a-select-option :value="true">分步执行模式</a-select-option>
          <a-select-option :value="false">Agent 智能生成</a-select-option>
        </a-select>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {h, ref, nextTick} from 'vue'
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

withDefaults(defineProps<Props>(), {
  sending: false,
  isOwner: true,
  isAppMode: false,
  showModeSelector: false,
  generatorMode: true,
  placeholder: '描述您想要创建的应用...'
})

const emit = defineEmits(['update:modelValue', 'send', 'clearSelection', 'update:generatorMode'])

const inputRef = ref<HTMLTextAreaElement>()
const indicator = h(LoadingOutlined, {style: {fontSize: '16px', color: '#999'}, spin: true})

const handleInput = (e: Event) => {
  const target = e.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  autoResize(target)
}

const autoResize = (el: HTMLTextAreaElement) => {
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
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
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  margin-bottom: 8px;
  background: #f9f9f9;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 13px;
  color: #666;
}

.hint-tag {
  color: #52c41a;
  font-weight: 500;
}

.hint-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.15s;
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
  max-height: 120px;
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

.mode-selector {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.mode-select {
  min-width: 160px;
}

:deep(.mode-select .ant-select-selector) {
  border-radius: 8px !important;
  border-color: #e5e5e5 !important;
  font-size: 12px !important;
}
</style>
