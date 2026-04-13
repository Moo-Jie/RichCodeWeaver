<template>
  <div class="color-scheme-editor">
    <div class="editor-header">
      <span class="editor-title">UI色系编辑器</span>
      <a-button size="small" @click="addColorScheme">
        <template #icon>
          <PlusOutlined />
        </template>
        添加色系
      </a-button>
    </div>

    <div class="scheme-list">
      <div v-for="(scheme, index) in colorSchemes" :key="index" class="scheme-item">
        <div class="scheme-controls">
          <a-input
            v-model:value="scheme.name"
            placeholder="色系名称，如：天空蓝"
            style="flex: 1; margin-right: 8px;"
          />
          <a-button danger size="small" @click="removeColorScheme(index)">
            <template #icon>
              <DeleteOutlined />
            </template>
          </a-button>
        </div>

        <div class="color-inputs">
          <div class="color-input-group">
            <label>主色</label>
            <div class="color-picker-wrap">
              <input
                v-model="scheme.primary"
                class="color-picker"
                type="color"
              />
              <a-input
                v-model:value="scheme.primary"
                placeholder="#0288d1"
                style="flex: 1; margin-left: 8px;"
              />
            </div>
          </div>

          <div class="color-input-group">
            <label>辅色</label>
            <div class="color-picker-wrap">
              <input
                v-model="scheme.secondary"
                class="color-picker"
                type="color"
              />
              <a-input
                v-model:value="scheme.secondary"
                placeholder="#e1f5fe"
                style="flex: 1; margin-left: 8px;"
              />
            </div>
          </div>
        </div>

        <!-- Real-time preview -->
        <div :style="getPreviewStyle(scheme)" class="scheme-preview">
          <div class="preview-card">
            <div :style="{ background: scheme.primary }" class="preview-header">
              <span style="color: white; font-weight: 600;">{{ scheme.name || '预览' }}</span>
            </div>
            <div :style="{ background: scheme.secondary }" class="preview-body">
              <p :style="{ color: scheme.primary }">这是使用该色系的示例内容</p>
              <button :style="{ background: scheme.primary, color: 'white' }"
                      class="preview-button">
                按钮示例
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="colorSchemes.length === 0" class="empty-state">
      <p>暂无色系，点击"添加色系"开始创建</p>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

interface ColorScheme {
  name: string
  primary: string
  secondary: string
}

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const colorSchemes = ref<ColorScheme[]>([])

// Parse initial value
watch(() => props.modelValue, (val) => {
  if (!val) {
    colorSchemes.value = []
    return
  }

  try {
    const parsed = JSON.parse(val)
    if (Array.isArray(parsed)) {
      colorSchemes.value = parsed.map((item: any) => {
        if (typeof item === 'string') {
          // Parse format: "天空蓝(#0288d1+#e1f5fe)"
          const match = item.match(/^(.+?)\((.+?)\+(.+?)\)$/)
          if (match) {
            return {
              name: match[1],
              primary: match[2],
              secondary: match[3]
            }
          }
        }
        return item
      })
    }
  } catch {
    colorSchemes.value = []
  }
}, { immediate: true })

// Emit changes
watch(colorSchemes, (schemes) => {
  const formatted = schemes.map(s => `${s.name}(${s.primary}+${s.secondary})`)
  emit('update:modelValue', JSON.stringify(formatted))
}, { deep: true })

const addColorScheme = () => {
  colorSchemes.value.push({
    name: '',
    primary: '#0288d1',
    secondary: '#e1f5fe'
  })
}

const removeColorScheme = (index: number) => {
  colorSchemes.value.splice(index, 1)
}

const getPreviewStyle = (scheme: ColorScheme) => {
  return {
    borderColor: scheme.primary
  }
}
</script>

<style lang="less" scoped>
.color-scheme-editor {
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

.editor-title {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a1a;
}

.scheme-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scheme-item {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
}

.scheme-controls {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.color-inputs {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.color-input-group {
  flex: 1;

  label {
    display: block;
    font-size: 12px;
    color: #666;
    margin-bottom: 6px;
    font-weight: 500;
  }
}

.color-picker-wrap {
  display: flex;
  align-items: center;
}

.color-picker {
  width: 40px;
  height: 32px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  padding: 2px;

  &::-webkit-color-swatch-wrapper {
    padding: 0;
  }

  &::-webkit-color-swatch {
    border: none;
    border-radius: 4px;
  }
}

.scheme-preview {
  border: 2px solid;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
}

.preview-card {
  background: white;
}

.preview-header {
  padding: 12px;
  text-align: center;
}

.preview-body {
  padding: 16px;
  text-align: center;

  p {
    margin: 0 0 12px;
    font-size: 13px;
  }
}

.preview-button {
  border: none;
  padding: 6px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.empty-state {
  text-align: center;
  padding: 32px;
  color: #999;
  font-size: 13px;
}
</style>
