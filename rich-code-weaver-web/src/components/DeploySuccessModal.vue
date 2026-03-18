<template>
  <a-modal v-model:open="visible" :footer="null" title="部署成功" width="600px">
    <div class="deploy-success">
      <div class="success-icon">
        <CheckCircleOutlined style="color: #52c41a; font-size: 48px"/>
      </div>
      <h3>网站部署成功！</h3>
      <p>你的网站已经成功部署，可以通过以下链接访问：</p>
      <div class="deploy-url">
        <a-input :value="deployUrl" readonly>
          <template #suffix>
            <a-button type="text" @click="handleCopyUrl">
              <CopyOutlined/>
            </a-button>
          </template>
        </a-input>
      </div>
      <div class="deploy-actions">
        <a-button type="primary" @click="handleOpenSite">访问网站</a-button>
        <a-button @click="handleClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void

  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-success {
  text-align: center;
  padding: 24px;
}

.success-icon {
  margin-bottom: 16px;
}

.deploy-success h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.deploy-success p {
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
}

.deploy-url {
  margin-bottom: 20px;
}

.deploy-url :deep(.ant-input) {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  font-size: 14px;
}

.deploy-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.deploy-actions :deep(.ant-btn) {
  border-radius: 8px;
  padding: 0 20px;
  height: 36px;
  font-weight: 500;
  font-size: 14px;
}

.deploy-actions :deep(.ant-btn-primary) {
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.deploy-actions :deep(.ant-btn-primary:hover) {
  background: #333;
  border-color: #333;
}

.deploy-actions :deep(.ant-btn-default) {
  border-color: #e5e5e5;
  color: #666;
}

.deploy-actions :deep(.ant-btn-default:hover) {
  background: #fafafa;
  border-color: #d0d0d0;
  color: #333;
}
</style>
