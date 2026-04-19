<template>
  <div class="field-definition-editor">
    <div class="editor-header">
      <span class="editor-title">字段定义编辑器</span>
      <div class="header-actions">
        <a-dropdown v-if="fields.length > 0">
          <template #overlay>
            <a-menu @click="handleLoadTemplate">
              <a-menu-item key="colorLayout">
                <ThunderboltOutlined />
                色系+布局模板
              </a-menu-item>
              <a-menu-item key="logoSwitch">
                <ThunderboltOutlined />
                LOGO开关模板
              </a-menu-item>
              <a-menu-item key="identityText">
                <ThunderboltOutlined />
                身份描述模板
              </a-menu-item>
            </a-menu>
          </template>
          <a-button size="small">
            <template #icon>
              <ThunderboltOutlined />
            </template>
            快速模板
          </a-button>
        </a-dropdown>
        <a-dropdown>
          <template #overlay>
            <a-menu @click="handleAddField">
              <a-menu-item key="select">
                <SelectOutlined />
                下拉选择
              </a-menu-item>
              <a-menu-item key="text">
                <EditOutlined />
                文本输入
              </a-menu-item>
              <a-menu-item key="switch">
                <CheckOutlined />
                开关选项
              </a-menu-item>
            </a-menu>
          </template>
          <a-button size="small" type="primary">
            <template #icon>
              <PlusOutlined />
            </template>
            添加字段
            <DownOutlined />
          </a-button>
        </a-dropdown>
      </div>
    </div>

    <div class="field-list">
      <div v-for="(field, index) in fields" :key="index" class="field-item">
        <div class="field-header">
          <a-tag :color="getFieldTypeColor(field.type)">
            {{ getFieldTypeLabel(field.type) }}
          </a-tag>
          <div class="field-actions">
            <a-button :disabled="index === 0" size="small" @click="moveField(index, -1)">
              <template #icon>
                <UpOutlined />
              </template>
            </a-button>
            <a-button :disabled="index === fields.length - 1" size="small"
                      @click="moveField(index, 1)">
              <template #icon>
                <DownOutlined />
              </template>
            </a-button>
            <a-button danger size="small" @click="removeField(index)">
              <template #icon>
                <DeleteOutlined />
              </template>
            </a-button>
          </div>
        </div>

        <div class="field-body">
          <a-row :gutter="12">
            <a-col :span="12">
              <div class="field-input-group">
                <label>变量名 (key)</label>
                <a-input
                  v-model:value="field.key"
                  placeholder="例如：colorScheme"
                  @change="emitChange"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="field-input-group">
                <label>显示标签</label>
                <a-input
                  v-model:value="field.label"
                  placeholder="例如：UI色系"
                  @change="emitChange"
                />
              </div>
            </a-col>
          </a-row>

          <!-- Select type specific fields -->
          <template v-if="field.type === 'select'">
            <div class="field-input-group">
              <label>选项列表</label>
              <div class="options-editor">
                <div v-for="(option, optIndex) in field.options ?? []" :key="optIndex"
                     class="option-item">
                  <a-input
                    :value="option"
                    placeholder="选项值"
                    @update:value="updateOption(field, optIndex, $event)"
                  />
                  <a-button
                    danger
                    size="small"
                    @click="removeOption(field, optIndex)"
                  >
                    <template #icon>
                      <DeleteOutlined />
                    </template>
                  </a-button>
                </div>
                <a-button size="small" style="width: 100%;" @click="addOption(field)">
                  <template #icon>
                    <PlusOutlined />
                  </template>
                  添加选项
                </a-button>
              </div>
            </div>
            <div class="field-input-group">
              <label>默认值</label>
              <a-select
                v-model:value="field.defaultValue"
                :options="field.options?.map(o => ({ value: o, label: o }))"
                allow-clear
                placeholder="选择默认值"
                @change="emitChange"
              />
            </div>
          </template>

          <!-- Text type specific fields -->
          <template v-if="field.type === 'text'">
            <div class="field-input-group">
              <label>默认值</label>
              <a-input
                v-model:value="field.defaultValue"
                placeholder="默认文本"
                @change="emitChange"
              />
            </div>
          </template>

          <!-- Switch type specific fields -->
          <template v-if="field.type === 'switch'">
            <a-row :gutter="12">
              <a-col :span="12">
                <div class="field-input-group">
                  <label>开启时文本</label>
                  <a-textarea
                    v-model:value="field.trueText"
                    :rows="2"
                    placeholder="开关打开时替换的文本"
                    @change="emitChange"
                  />
                </div>
              </a-col>
              <a-col :span="12">
                <div class="field-input-group">
                  <label>关闭时文本</label>
                  <a-textarea
                    v-model:value="field.falseText"
                    :rows="2"
                    placeholder="开关关闭时替换的文本（通常为空）"
                    @change="emitChange"
                  />
                </div>
              </a-col>
            </a-row>
            <div class="field-input-group">
              <label>默认状态</label>
              <a-switch
                v-model:checked="field.defaultValue"
                checked-children="开"
                un-checked-children="关"
                @change="emitChange"
              />
            </div>
          </template>

          <!-- Preview for this field -->
          <div class="field-preview">
            <span class="preview-label">预览：</span>
            <span class="preview-content">变量 <code>{{ formatVariable(field.key) }}</code> 将显示为 "{{ field.label }}"</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="fields.length === 0" class="empty-state">
      <p>暂无字段，点击"添加字段"开始创建</p>
    </div>

    <!-- JSON Preview -->
    <div v-if="fields.length > 0" class="json-preview">
      <div class="json-header">
        <span>JSON 预览</span>
        <a-button size="small" @click="copyJSON">
          <template #icon>
            <CopyOutlined />
          </template>
          复制
        </a-button>
      </div>
      <pre class="json-content">{{ formattedJSON }}</pre>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  CopyOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  PlusOutlined,
  SelectOutlined,
  ThunderboltOutlined,
  UpOutlined
} from '@ant-design/icons-vue'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'fields-updated': [fields: API.TemplateField[]]
}>()

const fields = ref<API.TemplateField[]>([])

// Parse initial value
watch(() => props.modelValue, (val) => {
  if (!val) {
    fields.value = []
    return
  }

  try {
    const parsed = JSON.parse(val)
    if (Array.isArray(parsed)) {
      fields.value = parsed
    }
  } catch {
    fields.value = []
  }
}, { immediate: true })

const formattedJSON = computed(() => {
  return JSON.stringify(fields.value, null, 2)
})

const emitChange = () => {
  const json = JSON.stringify(fields.value)
  emit('update:modelValue', json)
  emit('fields-updated', fields.value)
}

const handleAddField = ({ key }: { key: string }) => {
  const newField: any = {
    type: key,
    key: '',
    label: ''
  }

  if (key === 'select') {
    newField.options = []
    newField.defaultValue = ''
  } else if (key === 'text') {
    newField.defaultValue = ''
  } else if (key === 'switch') {
    newField.defaultValue = false
    newField.trueText = ''
    newField.falseText = ''
  }

  fields.value.push(newField)
  emitChange()
}

const removeField = (index: number) => {
  fields.value.splice(index, 1)
  emitChange()
}

const moveField = (index: number, direction: number) => {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= fields.value.length) return

  const temp = fields.value[index]
  fields.value[index] = fields.value[newIndex]
  fields.value[newIndex] = temp
  emitChange()
}

const addOption = (field: any) => {
  if (!field.options) field.options = []
  field.options.push('')
  emitChange()
}

const updateOption = (field: API.TemplateField, index: number, value: string) => {
  if (!field.options) {
    field.options = []
  }
  field.options[index] = value
  emitChange()
}

const removeOption = (field: any, index: number) => {
  field.options.splice(index, 1)
  emitChange()
}

const getFieldTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    select: '下拉选择',
    text: '文本输入',
    switch: '开关选项'
  }
  return map[type] || type
}

const getFieldTypeColor = (type: string) => {
  const map: Record<string, string> = {
    select: 'blue',
    text: 'green',
    switch: 'orange'
  }
  return map[type] || 'default'
}

const copyJSON = () => {
  navigator.clipboard.writeText(formattedJSON.value)
  message.success('已复制到剪贴板')
}

const handleLoadTemplate = ({ key }: { key: string }) => {
  if (key === 'colorLayout') {
    fields.value.push(
      {
        type: 'select',
        key: 'colorScheme',
        label: 'UI色系',
        options: ['天空蓝(#0288d1+#e1f5fe)', '自然绿(#388e3c+#e8f5e9)', '温暖橙(#f57c00+#fff3e0)'],
        defaultValue: '天空蓝(#0288d1+#e1f5fe)'
      },
      {
        type: 'select',
        key: 'layout',
        label: '布局建议',
        options: ['卡片瀑布流', '时间线列表', '网格画廊'],
        defaultValue: '卡片瀑布流'
      }
    )
  } else if (key === 'logoSwitch') {
    fields.value.push({
      type: 'switch',
      key: 'logoOption',
      label: '是否生成LOGO',
      defaultValue: true,
      trueText: '请设计一个旅行主题SVG LOGO。',
      falseText: ''
    })
  } else if (key === 'identityText') {
    fields.value.push({
      type: 'text',
      key: 'identity',
      label: '身份描述',
      defaultValue: '个体'
    })
  }
  emitChange()
  message.success('已添加模板字段')
}

const formatVariable = (key: string) => {
  return `{{${key}}}`
}
</script>

<style lang="less" scoped>
.field-definition-editor {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e0e0e0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.editor-title {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a1a;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.field-item {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f9f9f9;
  border-bottom: 1px solid #e0e0e0;
}

.field-actions {
  display: flex;
  gap: 6px;
}

.field-body {
  padding: 12px;
}

.field-input-group {
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }

  label {
    display: block;
    font-size: 12px;
    color: #666;
    margin-bottom: 6px;
    font-weight: 500;
  }
}

.options-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.field-preview {
  margin-top: 12px;
  padding: 10px;
  background: #f0f7ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  font-size: 12px;
}

.preview-label {
  color: #666;
  font-weight: 500;
  margin-right: 6px;
}

.preview-content {
  color: #1a1a1a;

  code {
    background: #fff;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Consolas', 'Monaco', monospace;
    color: #d63384;
    font-size: 11px;
  }
}

.empty-state {
  text-align: center;
  padding: 32px;
  color: #999;
  font-size: 13px;
  background: white;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
}

.json-preview {
  margin-top: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.json-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f9f9f9;
  border-bottom: 1px solid #e0e0e0;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.json-content {
  margin: 0;
  padding: 12px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #333;
  background: #fafafa;
  max-height: 300px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #ddd;
    border-radius: 3px;
  }
}
</style>
