<template>
  <div class="chat-input-wrap">
    <!-- Selected element hint -->
    <div v-if="selectedElement" class="selected-hint">
      <div class="hint-content">
        <div class="hint-title">
          <span class="hint-icon">✓</span>
          <span class="hint-label">已选中：{{ getElementTypeLabel(selectedElement.tagName) }}</span>
        </div>
        <div v-if="selectedElement.textContent" class="hint-text-preview">
          "{{ selectedElement.textContent.length > 60 ? selectedElement.textContent.substring(0, 60) + '...' : selectedElement.textContent }}"
        </div>
        <div class="hint-tip">请在下方输入框描述您想要的修改</div>
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
        >系统分步执行</button>
        <button
          :class="['capsule-item', { active: !generatorMode }]"
          @click="$emit('update:generatorMode', false)"
        >Agent 智能生成</button>
      </div>

      <div class="input-row">
        <div class="textarea-wrapper">
          <div class="textarea-container">
            <textarea
              ref="inputRef"
              :disabled="sending || optimizing"
              :placeholder="placeholder"
              :value="modelValue"
              class="chat-textarea"
              :class="{ optimizing: optimizing }"
              rows="1"
              @input="handleInput"
            ></textarea>
            <div v-if="optimizing" class="mist-overlay"></div>
          </div>
        </div>
        <button
          v-if="showOptimizeButton"
          :class="['optimize-btn', { disabled: optimizing || !modelValue?.trim() }]"
          :disabled="optimizing || !modelValue?.trim()"
          @click="$emit('optimize')"
          title="AI 优化提示词"
        >
          <template v-if="optimizing">
            <a-spin :indicator="indicator" size="small" />
          </template>
          <template v-else>
            <img :class="['optimize-btn', { disabled: optimizing || !modelValue?.trim() }]" src="@/assets/AiOptimize.png"/>
          </template>
        </button>
        <button :class="['send-btn', { disabled: sending || !modelValue?.trim() }]" :disabled="sending || optimizing" @click="handleSend">
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
import { h, nextTick, onMounted, ref } from 'vue'
import { LoadingOutlined, SendOutlined } from '@ant-design/icons-vue'

interface ElementInfo {
  tagName: string
  className: string
  selector: string
  textContent?: string
}

const ELEMENT_TYPE_MAP: Record<string, string> = {
  H1: '大标题', H2: '二级标题', H3: '三级标题', H4: '四级标题', H5: '五级标题', H6: '六级标题',
  P: '段落文本', A: '链接', BUTTON: '按钮', IMG: '图片', VIDEO: '视频', AUDIO: '音频',
  NAV: '导航栏', HEADER: '页头区域', FOOTER: '页脚区域', MAIN: '主要内容', ASIDE: '侧边栏',
  SECTION: '内容区块', ARTICLE: '文章区块', DIV: '区块容器', SPAN: '文本片段',
  UL: '无序列表', OL: '有序列表', LI: '列表项', TABLE: '表格', FORM: '表单', INPUT: '输入框',
  LABEL: '标签', SELECT: '下拉选择', TEXTAREA: '文本域', IFRAME: '嵌入框架',
}

function getElementTypeLabel(tagName: string): string {
  return ELEMENT_TYPE_MAP[tagName?.toUpperCase()] || '页面元素'
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
  showOptimizeButton?: boolean
  optimizing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sending: false,
  isOwner: true,
  isAppMode: false,
  showModeSelector: false,
  generatorMode: true,
  placeholder: '描述您想要创建的数字产物...',
  showOptimizeButton: false,
  optimizing: false
})

const emit = defineEmits(['update:modelValue', 'send', 'clearSelection', 'update:generatorMode', 'optimize'])

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
  const maxH = (baseHeight.value || 22) * 5
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

const resetHeight = () => {
  if (inputRef.value) {
    nextTick(() => {
      if (inputRef.value) {
        autoResize(inputRef.value)
      }
    })
  }
}

defineExpose({
  focus: () => inputRef.value?.focus(),
  resetHeight
})
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

.hint-text-preview {
  font-size: 13px;
  color: #555;
  font-style: italic;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.hint-tip {
  font-size: 12px;
  color: #999;
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

.textarea-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: flex-start;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.textarea-wrapper:focus-within {
  border-color: #1a1a1a;
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  padding: 0;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  color: #1a1a1a;
  font-family: inherit;
  min-height: 22px;
  overflow-y: auto;
  position: relative;
  z-index: 2;
  transition: height 0.3s ease;
}

.chat-textarea::placeholder {
  color: #bbb;
}

.chat-textarea:disabled {
  opacity: 0.5;
}

.textarea-container {
  position: relative;
  flex: 1;
  display: flex;
}

.mist-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(147, 197, 253, 0.25) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(196, 181, 253, 0.2) 0%, transparent 50%),
    radial-gradient(circle at 50% 80%, rgba(253, 186, 116, 0.18) 0%, transparent 50%),
    radial-gradient(circle at 40% 20%, rgba(134, 239, 172, 0.15) 0%, transparent 50%);
  background-size: 200% 200%;
  animation: mistMove 6s ease-in-out infinite, mistFlow 3s ease-in-out infinite;
  pointer-events: none;
  border-radius: 12px;
  z-index: 1;
}

@keyframes mistMove {
  0% {
    background-position: 0% 50%, 100% 50%, 50% 100%, 20% 30%;
    opacity: 0.5;
  }
  33% {
    background-position: 100% 30%, 0% 70%, 30% 0%, 80% 50%;
    opacity: 0.6;
  }
  66% {
    background-position: 50% 100%, 50% 0%, 100% 50%, 0% 80%;
    opacity: 0.55;
  }
  100% {
    background-position: 0% 50%, 100% 50%, 50% 100%, 20% 30%;
    opacity: 0.5;
  }
}

@keyframes mistFlow {
  0%, 100% {
    filter: brightness(1) saturate(1);
  }
  50% {
    filter: brightness(0.99) saturate(1.05);
  }
}

.optimize-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e5e5e5;
  background: #fafafa;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;
  font-size: 16px;
}

.optimize-btn:hover:not(.disabled) {
  background: #f5f5f5;
  border-color: #d0d0d0;
  transform: scale(1.04);
}

.optimize-btn.disabled {
  background: #fafafa;
  border-color: #f0f0f0;
  color: #bbb;
  cursor: not-allowed;
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
