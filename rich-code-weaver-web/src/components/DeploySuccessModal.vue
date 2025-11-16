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
import {computed} from 'vue'
import {message} from 'ant-design-vue'
import {CheckCircleOutlined, CopyOutlined} from '@ant-design/icons-vue'

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
  padding: 32px;
  background: linear-gradient(135deg, rgba(250, 248, 216, 0.09) 0%, rgba(250, 248, 216, 0.68) 100%);
  border-radius: 8px;
}

.success-icon {
  margin-bottom: 20px;
}

.deploy-success h3 {
  margin: 0 0 20px;
  font-size: 24px;
  font-weight: 700;
  color: #62d591;
}

.deploy-success p {
  margin: 0 0 28px;
  color: #595959;
  font-size: 16px;
}

.deploy-url {
  margin-bottom: 28px;
}

.deploy-url :deep(.ant-input) {
  border-radius: 6px;
  border: 1px solid #d9d9d9;
}

.deploy-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.deploy-actions :deep(.ant-btn) {
  border-radius: 6px;
  padding: 0 20px;
  height: 36px;
  font-weight: 500;
}

.deploy-actions :deep(.ant-btn-default) {
  border-color: #ffffff;
  color: #000000;
}

.deploy-actions :deep(.ant-btn-default:hover) {
  background-color: #c2cbff;
  color: white;
}
</style>
