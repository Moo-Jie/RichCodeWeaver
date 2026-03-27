<template>
  <div class="prompt-content-editor">
    <div class="editor-toolbar">
      <span class="toolbar-title">提示词内容编辑器</span>
      <div class="toolbar-actions">
        <a-button v-if="!localContent" size="small" @click="loadExample">
          <template #icon>
            <BulbOutlined />
          </template>
          加载示例
        </a-button>
        <a-dropdown>
          <template #overlay>
            <a-menu @click="handleInsertVariable">
              <a-menu-item v-for="field in availableFields" :key="field.key">
                插入 {{ formatVariable(field.key) }} - {{ field.label }}
              </a-menu-item>
              <a-menu-divider v-if="availableFields.length > 0" />
              <a-menu-item key="custom">
                <EditOutlined />
                自定义变量...
              </a-menu-item>
            </a-menu>
          </template>
          <a-button size="small">
            <template #icon>
              <PlusOutlined />
            </template>
            插入变量
            <DownOutlined />
          </a-button>
        </a-dropdown>
      </div>
    </div>

    <a-textarea
      ref="textareaRef"
      v-model:value="localContent"
      :rows="8"
      class="content-textarea"
      placeholder="输入提示词内容，使用 {{变量名}} 作为可替换占位符&#10;&#10;示例：为一位{{identity}}用户创建网站。色系：{{colorScheme}}，布局：{{layout}}。"
      @change="handleChange"
    />

    <div class="editor-hints">
      <div class="hint-item">
        <span class="hint-label">已使用变量：</span>
        <a-tag v-for="variable in usedVariables" :key="variable" color="blue" style="margin: 2px;">
          {{ variable }}
        </a-tag>
        <span v-if="usedVariables.length === 0" class="hint-empty">暂无</span>
      </div>
      <div class="hint-item">
        <span class="hint-label">字符数：</span>
        <span class="hint-value">{{ localContent.length }}</span>
      </div>
    </div>

    <!-- Custom variable dialog -->
    <a-modal
      v-model:open="customVarVisible"
      title="插入自定义变量"
      width="400px"
      @ok="insertCustomVariable"
    >
      <a-form layout="vertical" style="margin-top: 16px;">
        <a-form-item label="变量名（英文）">
          <a-input
            v-model:value="customVarName"
            placeholder="例如：colorScheme"
          />
        </a-form-item>
        <a-form-item label="变量说明">
          <a-input
            v-model:value="customVarLabel"
            placeholder="例如：UI色系"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { PlusOutlined, DownOutlined, EditOutlined, BulbOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  modelValue: string
  fields?: API.TemplateField[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const localContent = ref('')
const textareaRef = ref()
const customVarVisible = ref(false)
const customVarName = ref('')
const customVarLabel = ref('')

watch(() => props.modelValue, (val) => {
  localContent.value = val || ''
}, { immediate: true })

const handleChange = () => {
  emit('update:modelValue', localContent.value)
}

const availableFields = computed(() => {
  return props.fields || []
})

const usedVariables = computed(() => {
  const regex = /\{\{(\w+)\}\}/g
  const matches = localContent.value.matchAll(regex)
  const vars = new Set<string>()
  for (const match of matches) {
    vars.add(match[1])
  }
  return Array.from(vars)
})

const handleInsertVariable = ({ key }: { key: string }) => {
  if (key === 'custom') {
    customVarVisible.value = true
    customVarName.value = ''
    customVarLabel.value = ''
    return
  }

  insertAtCursor(`{{${key}}}`)
}

const insertCustomVariable = () => {
  if (!customVarName.value) return
  insertAtCursor(`{{${customVarName.value}}}`)
  customVarVisible.value = false
}

const insertAtCursor = (text: string) => {
  const textarea = textareaRef.value?.$el?.querySelector('textarea')
  if (!textarea) {
    localContent.value += text
    handleChange()
    return
  }

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = localContent.value.substring(0, start)
  const after = localContent.value.substring(end)

  localContent.value = before + text + after
  handleChange()

  // Restore cursor position
  setTimeout(() => {
    textarea.focus()
    const newPos = start + text.length
    textarea.setSelectionRange(newPos, newPos)
  }, 0)
}

const loadExample = () => {
  localContent.value = '为一位{{identity}}旅游/酒店爱好者创建旅行游记静态网站。色系：{{colorScheme}}，布局：{{layout}}。包含旅行者介绍、游记文章列表（含目的地、日期、封面图占位）、旅行地图标记、旅行数据统计（去过的城市数、国家数等）、旅行Tips分享区。{{logoOption}}页面需有地图交互和文章卡片动效。所有内容使用真实可信的旅行场景文本。'
  handleChange()
}

const formatVariable = (key: string) => {
  return `{{${key}}}`
}
</script>

<style lang="less" scoped>
.prompt-content-editor {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.toolbar-title {
  font-weight: 600;
  font-size: 13px;
  color: #1a1a1a;
}

.content-textarea {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;

  :deep(textarea) {
    border-radius: 6px;
    background: white;
  }
}

.editor-hints {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hint-item {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.hint-label {
  color: #666;
  font-weight: 500;
  margin-right: 8px;
  min-width: 80px;
}

.hint-value {
  color: #1a1a1a;
  font-weight: 600;
}

.hint-empty {
  color: #bbb;
  font-size: 12px;
}
</style>
