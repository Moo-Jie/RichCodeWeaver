<template>
  <a-modal
    :open="open"
    :title="template?.templateName || '编辑模板'"
    cancelText="取消"
    centered
    class="template-dialog"
    okText="确定生成"
    width="560px"
    @cancel="$emit('update:open', false)"
    @ok="handleConfirm"
  >
    <div class="template-dialog-body">
      <p class="template-desc">{{ template?.description }}</p>

      <a-form class="template-form" layout="vertical">
        <div v-for="field in parsedFields" :key="field.key">
          <!-- 下拉选项 -->
          <a-form-item v-if="field.type === 'select'" :label="field.label">
            <a-select
              v-model:value="fieldValues[field.key]"
              :options="buildSelectOptions(field.options)"
              placeholder="请选择"
            />
            <!-- Color scheme visual preview -->
            <div v-if="field.key === 'colorScheme' && fieldValues[field.key]"
                 class="color-scheme-visual-preview">
              <div :style="getColorSchemeStyle(fieldValues[field.key])" class="preview-card-demo">
                <div class="demo-header" />
                <div class="demo-body" />
              </div>
            </div>
          </a-form-item>

          <!-- 文本输入 -->
          <a-form-item v-else-if="field.type === 'text'" :label="field.label">
            <a-input
              v-model:value="fieldValues[field.key]"
              placeholder="请输入"
            />
          </a-form-item>

          <!-- 开关 -->
          <a-form-item v-else-if="field.type === 'switch'" :label="field.label">
            <a-switch v-model:checked="fieldValues[field.key]" />
          </a-form-item>
        </div>
      </a-form>

      <div class="template-preview">
        <div class="preview-label">预览生成的提示词：</div>
        <div class="preview-content">{{ generatedPrompt }}</div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
  template: API.PromptTemplateVO | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'confirm': [prompt: string]
}>()

const fieldValues = reactive<Record<string, any>>({})
const additionalContent = ref('')

const buildSelectOptions = (options?: string[]) => {
  return (options || []).map(o => ({ value: o, label: o }))
}

const parsedFields = computed((): API.TemplateField[] => {
  if (!props.template?.templateFields) return []
  try {
    return JSON.parse(props.template.templateFields)
  } catch {
    return []
  }
})

watch(() => [props.open, props.template], () => {
  if (props.open && props.template) {
    // Reset field values to defaults
    const fields = parsedFields.value
    for (const field of fields) {
      if (field.type === 'switch') {
        fieldValues[field.key] = field.defaultValue ?? false
      } else {
        fieldValues[field.key] = field.defaultValue ?? ''
      }
    }
    // Reset additional content
    additionalContent.value = ''
  }
}, { immediate: true })

const generatedPrompt = computed(() => {
  if (!props.template?.promptContent) return ''
  let content = props.template.promptContent

  for (const field of parsedFields.value) {
    const key = field.key
    let value = fieldValues[key]

    if (field.type === 'switch') {
      value = value ? (field.trueText || '') : (field.falseText || '')
    }

    content = content.replace(new RegExp(`\\{\\{${key}\\}\\}`, 'g'), value ?? '')
  }

  // Append additional content if provided
  if (additionalContent.value.trim()) {
    content += '\n\n网站需要展示的具体内容：' + additionalContent.value.trim()
  }

  return content
})

const handleConfirm = () => {
  emit('confirm', generatedPrompt.value)
  emit('update:open', false)
}

const getColorSchemeStyle = (colorScheme: string) => {
  // Parse color scheme like "天空蓝(#0288d1+#e1f5fe)"
  const match = colorScheme.match(/\((.+?)\+(.+?)\)/)
  if (!match) return {}

  const primary = match[1]
  const secondary = match[2]

  return {
    '--primary-color': primary,
    '--secondary-color': secondary
  }
}
</script>

<style scoped>
.template-dialog-body {
  padding: 4px 0;
}

.template-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px;
  line-height: 1.5;
  padding: 10px 14px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.template-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.template-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 13px;
}

.template-form :deep(.ant-select-selector),
.template-form :deep(.ant-input),
.template-form :deep(.ant-input-textarea-show-count .ant-input) {
  border-radius: 8px !important;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}

.template-preview {
  margin-top: 16px;
  padding: 14px;
  background: #f9f9f9;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}

.preview-label {
  font-size: 12px;
  color: #999;
  font-weight: 600;
  margin-bottom: 8px;
}

.preview-content {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  max-height: 160px;
  overflow-y: auto;
  word-break: break-all;
}

.preview-content::-webkit-scrollbar {
  width: 3px;
}

.preview-content::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.color-scheme-visual-preview {
  margin-top: 12px;
  padding: 16px;
  background: #f9f9f9;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.preview-card-demo {
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid var(--primary-color, #0288d1);
  background: white;
  transition: all 0.3s;
}

.demo-header {
  background: var(--primary-color, #0288d1);
  color: white;
  padding: 12px 16px;
  font-weight: 600;
  font-size: 14px;
  text-align: center;
  min-height: 50px;
}

.demo-body {
  background: var(--secondary-color, #e1f5fe);
  padding: 20px;
  text-align: center;
  min-height: 50px;
}

.demo-body p {
  margin: 0 0 12px;
  color: var(--primary-color, #0288d1);
  font-size: 13px;
}

.demo-button {
  background: var(--primary-color, #0288d1);
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.demo-button:hover {
  opacity: 0.85;
}
</style>
