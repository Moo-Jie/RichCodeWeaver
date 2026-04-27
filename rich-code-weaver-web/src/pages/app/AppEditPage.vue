<template>
  <div id="appEditPage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>数字产物编辑中心</h1>
      <p>管理数字产物的详细信息与配置</p>
    </div>

    <!-- 编辑面板 -->
    <div class="edit-panel">
      <a-card :loading="loading" class="info-card">
        <div class="card-header">
          <div class="card-title">
            <h2>数字产物信息</h2>
            <span v-if="ownershipLabel" :class="['ownership-pill', `ownership-pill--${appInfo?.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
              {{ ownershipLabel }}
            </span>
          </div>
          <a-button class="view-chat" type="link" @click="goToChat">
            <MessageOutlined />
            <span>进入对话</span>
          </a-button>
        </div>

        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item class="form-item" label="数字产物名称" name="appName">
            <a-input
              v-model:value="formData.appName"
              :maxlength="50"
              allow-clear
              placeholder="为您的数字产物输入一个有意义的名称"
            >
              <template #prefix>
                <TagOutlined />
              </template>
              <template #suffix>
                <span class="max-length">{{ formData.appName.length }}/50</span>
              </template>
            </a-input>
            <div class="form-tip">
              好的数字产物名称应该简洁明确，能准确反映数字产物的核心功能
            </div>
          </a-form-item>

          <!-- 管理员专属字段 -->
          <template v-if="isAdmin">
            <a-form-item
              class="form-item"
              extra="支持外部图片链接或上传本地图片，建议尺寸：400x300"
              label="数字产物封面"
              name="cover"
            >
              <div class="cover-input-group">
                <a-input
                  v-model:value="formData.cover"
                  allow-clear
                  class="cover-input"
                  placeholder="输入封面图片URL链接"
                >
                  <template #prefix>
                    <PaperClipOutlined />
                  </template>
                </a-input>
                <a-upload
                  :before-upload="beforeUpload"
                  :custom-request="handleCustomUpload"
                  :show-upload-list="false"
                  accept="image/jpeg,image/png"
                >
                  <a-button class="upload-btn" type="primary">
                    <UploadOutlined />
                    上传图片
                  </a-button>
                </a-upload>
              </div>

              <div v-if="formData.cover" class="cover-preview">
                <div class="preview-title">封面预览</div>
                <a-image
                  :fallback="fallbackImage"
                  :height="150"
                  :src="formData.cover"
                  :width="200"
                />
                <a-button
                  class="clear-btn"
                  type="link"
                  @click="clearCover"
                >
                  清除封面
                </a-button>
              </div>
            </a-form-item>
          </template>

          <!-- 不可编辑字段 -->
          <a-form-item class="form-item" label="初始提示词" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              :maxlength="1000"
              :rows="5"
              disabled
              placeholder="数字产物的初始生成提示词"
            />
            <div class="form-tip">
              此字段为数字产物的初始生成指令，创建后不可修改
            </div>
          </a-form-item>

          <a-form-item class="form-item" label="生成类型" name="codeGenType">
            <a-tag :color="getTypeColor(appInfo?.codeGenType || '')">
              {{
                appInfo?.codeGenType === 'single_html' ? '单文件结构' :
                  appInfo?.codeGenType === 'multi_file' ? '多文件结构' :
                    appInfo?.codeGenType === 'vue_project' ? 'VUE 项目工程' :
                      formatCodeGenType(appInfo?.codeGenType)
              }}
            </a-tag>
            <div class="form-tip">
              生成类型决定了数字产物的框架和技术栈，创建后不可变更
            </div>
          </a-form-item>

          <a-form-item
            v-if="formData.deployKey"
            class="form-item"
            label="部署密钥"
            name="deployKey"
          >
            <a-input v-model:value="formData.deployKey" disabled />
            <div class="form-tip">
              此密钥用于数字产物部署，请勿随意分享给他人
            </div>
          </a-form-item>

          <a-form-item class="form-actions">
            <a-space>
              <a-button
                :loading="submitting"
                class="submit-btn"
                html-type="submit"
                size="large"
                type="primary"
              >
                <SaveOutlined />
                <span>保存修改</span>
              </a-button>
              <a-button class="reset-btn" size="large" @click="resetForm">
                <UndoOutlined />
                <span>重置表单</span>
              </a-button>
              <a-button
                v-if="isDeployed"
                :href="deployedSiteUrl"
                class="detail-btn"
                size="large"
                target="_blank"
                type="primary"
              >
                <ExportOutlined />
                <span>访问已部署网站</span>
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 元信息卡片 -->
      <a-card class="meta-card">
        <div class="card-header">
          <div class="card-title card-title--stack">
            <h2>元信息</h2>
            <span class="card-subtitle">查看归属、创建者与部署状态</span>
          </div>
          <span class="meta-id-pill">ID {{ appInfo?.id || '--' }}</span>
        </div>
        <div class="meta-summary">
          <span v-if="ownershipLabel" :class="['ownership-pill', `ownership-pill--${appInfo?.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
            {{ ownershipLabel }}
          </span>
          <span :class="['ownership-pill', appInfo?.deployKey ? 'ownership-pill--success' : 'ownership-pill--muted']">
            {{ appInfo?.deployKey ? '已部署' : '未部署' }}
          </span>
          <span class="meta-summary-text">{{ collaborators.length }} 位协作者</span>
        </div>
        <a-descriptions :column="1" class="meta-grid">
          <a-descriptions-item label="归属类型">
            <div class="meta-item">
              <span v-if="ownershipLabel" :class="['ownership-pill', `ownership-pill--${appInfo?.ownershipType === 'collaborator' ? 'collaborator' : 'mine'}`]">
                {{ ownershipLabel }}
              </span>
              <span v-else>--</span>
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="创建者">
            <div class="meta-item">
              <UserOutlined />
              <UserInfo :user="appInfo?.user" size="small" />
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="创建时间">
            <div class="meta-item">
              <CalendarOutlined />
              <span>{{ formatTime(appInfo?.createTime) || '--' }}</span>
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="更新时间">
            <div class="meta-item">
              <SyncOutlined />
              <span>{{ formatTime(appInfo?.updateTime) || '--' }}</span>
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="部署状态">
            <div class="meta-item">
              <CloudServerOutlined />
              <span :class="['ownership-pill', appInfo?.deployKey ? 'ownership-pill--success' : 'ownership-pill--muted']">
                {{ appInfo?.deployKey ? '已部署' : '未部署' }}
              </span>
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="部署时间">
            <div class="meta-item">
              <CloudServerOutlined />
              <template v-if="appInfo?.deployKey">
                <span class="time">{{ formatTime(appInfo.deployedTime) }}</span>
              </template>
              <span v-else>--</span>
            </div>
          </a-descriptions-item>
        </a-descriptions>
        <AppTeamCard :app="appInfo" :collaborators="collaborators" class="edit-team-card" />
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import {
  CalendarOutlined,
  CloudServerOutlined,
  ExportOutlined,
  MessageOutlined,
  PaperClipOutlined,
  SaveOutlined,
  SyncOutlined,
  TagOutlined,
  UndoOutlined,
  UploadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { DEPLOY_DOMAIN } from '@/config/env'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { listCollaborators } from '@/api/collaboratorController'
import { formatCodeGenType } from '@/enums/codeGenTypes'
import { formatTime } from '@/utils/timeUtil'
import UserInfo from '@/components/UserInfo.vue'
import AppTeamCard from '@/components/AppTeamCard.vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const formRef = ref<FormInstance>()

// 数字产物信息
const appInfo = ref<API.AppVO>()
const collaborators = ref<API.AppCollaboratorVO[]>([])
const loading = ref(false)
const submitting = ref(false)

// 表单数据
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: ''
})

// 默认封面图像
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#f5f5f5"/><text x="50%" y="50%" font-family="Arial" font-size="20" text-anchor="middle" fill="#999">未提供封面</text></svg>'

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 判断数字产物是否已部署
const isDeployed = computed(() => {
  return !!appInfo.value?.deployKey
})

const ownershipLabel = computed(() => {
  if (appInfo.value?.ownershipType === 'mine') return '我的'
  if (appInfo.value?.ownershipType === 'collaborator') return '协作'
  return ''
})

// 获取部署后的访问URL
const deployedSiteUrl = computed(() => {
  if (appInfo.value?.deployKey) {
    return `${DEPLOY_DOMAIN}/${appInfo.value.deployKey}`
  }
  return ''
})

// 表单验证规则
const rules = {
  appName: [
    { required: true, message: '请填写数字产物名称', trigger: 'blur' },
    { min: 1, max: 50, message: '名称长度应为1-50个字符', trigger: 'blur' }
  ],
  cover: [{ type: 'url', message: '请提供有效的图片URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围为0-99', trigger: 'blur' }]
}

// 根据生成类型获取标签颜色
const getTypeColor = (type?: string) => {
  const colors: Record<string, string> = {
    'react': '#61dafb',
    'vue': '#42b883',
    'angular': '#dd0031',
    'html': '#e34c26',
    'nodejs': '#68a063',
    'flutter': '#04599C',
    'swift': '#ff2d55'
  }
  return type ? (colors[type] || 'blue') : 'blue'
}

// 获取数字产物信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('数字产物ID无效')
    await router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      await fetchCollaborators(res.data.data.id)

      // 检查权限
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此数字产物:' + res.data.message)
        router.push('/')
        return
      }

      // 填充表单数据
      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('获取数字产物信息失败: ' + res.data.message)
      router.push('/')
    }
  } catch (error) {
    console.error('获取数字产物信息失败:', error)
    message.error('获取数字产物信息失败')
    collaborators.value = []
    router.push('/')
  } finally {
    loading.value = false
  }
}

const fetchCollaborators = async (targetAppId?: number) => {
  if (!targetAppId) {
    collaborators.value = []
    return
  }
  try {
    const res = await listCollaborators({ appId: targetAppId })
    collaborators.value = res.data.code === 0 ? (res.data.data || []) : []
  } catch (error) {
    console.error('获取协作者列表失败:', error)
    collaborators.value = []
  }
}

// 清除封面
const clearCover = () => {
  formData.cover = ''
}

// 上传封面图片
const handleCoverUpload = async (file: File) => {
  try {
    const uploadFormData = new FormData()
    uploadFormData.append('file', file)

    // 直接使用fetch API上传文件，因为自动生成的API方法使用了JSON格式
    const res = await fetch(`/api/app/upload/cover?appId=${appInfo.value?.id}`, {
      method: 'POST',
      body: uploadFormData
    })

    const result = await res.json()

    if (result.code === 0) {
      formData.cover = result.data + '?t=' + Date.now() // 添加时间戳强制刷新图片
      message.success('封面图片上传成功')
    } else {
      message.error('上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传过程中出现错误')
  }
}

const handleCustomUpload = ({ file }: { file: File }) => {
  return handleCoverUpload(file)
}

// 图片上传前检查
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG 格式的图片!')
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
  }
  return isJpgOrPng && isLt2M
}

// 提交表单
const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority
      })
    } else {
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName
      })
    }

    if (res.data.code === 0) {
      message.success('修改已保存')
      await fetchAppInfo()
    } else {
      message.error('修改失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('修改失败:', error)
    message.error('提交过程中出现错误')
    console.error('Error details:', error)
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

// 进入对话页面
const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

// 页面加载时获取数字产物信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style lang="less" scoped>
#appEditPage {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 48px);
  position: relative;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #999;
    max-width: 600px;
    margin: 0 auto;
  }
}

.edit-panel {
  display: flex;
  gap: 30px;
  max-width: 1400px;
  margin: 0 auto;

  @media (max-width: 1200px) {
    flex-direction: column;
  }
}

.info-card, .meta-card {
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfd 100%);
  border-radius: 20px;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.03);
  border: 1px solid #e8edf2;
  overflow: hidden;
  position: relative;
  z-index: 1;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: #dbe3ea;
    box-shadow: 0 18px 32px rgba(15, 23, 42, 0.08);
  }
}

.info-card {
  flex: 1;
  padding: 30px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 18px;
    border-bottom: 1px solid #eef2f5;
    margin-bottom: 25px;

    h2 {
      font-size: 18px;
      color: #1a1a1a;
      margin: 0;
      font-weight: 600;
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .app-id {
      color: #999;
      font-size: 14px;

      .ant-tag {
        margin-left: 8px;
      }
    }

    .view-chat {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #374151;
      transition: all 0.2s ease;
      border-radius: 10px;
      padding: 0 8px;

      &:hover {
        color: #111827;
        background: #f6f8fa;
      }
    }
  }

  .form-item {
    margin-bottom: 25px;

    :deep(.ant-form-item-label) {
      font-weight: 600;
      color: #24292f;
      padding-bottom: 5px;
    }

    .ant-input-prefix, .ant-input-suffix {
      color: #8c959f;
    }

    .max-length {
      font-size: 12px;
      color: #8c959f;
      padding-right: 8px;
    }

    .gen-type-tag {
      padding: 4px 12px;
      font-size: 1rem;
    }
  }

  .form-tip {
    font-size: 12px;
    color: #8c959f;
    margin-top: 8px;
    padding: 0 3px;
  }

  .form-actions {
    margin-top: 15px;
    padding-top: 20px;
    border-top: 1px solid #eef2f5;

    .ant-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 45px;
      padding: 0 25px;
      font-weight: 600;
      transition: all 0.3s ease;
    }
  }
}

.cover-input-group {
  display: flex;
  gap: 12px;
  align-items: center;

  .cover-input {
    flex: 1;
  }

  .upload-btn {
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 12px;
    background: #1f2328;
    border: 1px solid #1f2328;
    color: white;

    &:hover {
      background: #2f363d;
      transform: translateY(-1px);
      box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
    }
  }
}

.cover-preview {
  margin-top: 15px;
  padding: 15px;
  background: #f8fafc;
  border-radius: 16px;
  border: 1px dashed #d9e0e8;

  .preview-title {
    font-size: 14px;
    color: #8c959f;
    margin-bottom: 10px;
  }

  .ant-image {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  .clear-btn {
    display: block;
    margin-top: 10px;
    text-align: center;
    color: #374151;
    font-size: 14px;
    transition: all 0.2s ease;

    &:hover {
      color: #111827;
    }
  }
}

.meta-card {
  width: 380px;
  padding: 30px;

  @media (max-width: 1200px) {
    width: 100%;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding-bottom: 18px;
    border-bottom: 1px solid #eef2f5;
    margin-bottom: 16px;

    h2 {
      font-size: 18px;
      color: #1a1a1a;
      margin: 0;
      font-weight: 600;
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .card-title--stack {
      align-items: flex-start;
      flex-direction: column;
      gap: 4px;
    }

    .card-subtitle {
      font-size: 12px;
      color: #8c959f;
      line-height: 1.5;
    }
  }

  .meta-summary {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 18px;
  }

  .meta-summary-text {
    font-size: 12px;
    color: #8c959f;
  }

  .meta-grid {
    :deep(.ant-descriptions-item) {
      padding-bottom: 18px;
      border-bottom: 1px solid #eef2f5;
    }

    :deep(.ant-descriptions-item-label) {
      font-weight: 600;
      color: #8c959f;
      width: 100px;
      padding-right: 20px;
    }

    :deep(.ant-descriptions-item-content) {
      font-weight: 400;
      color: #1a1a1a;
    }
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 5px 0;

    .anticon {
      font-size: 16px;
      color: #6b7280;
    }

    .time {
      margin-left: 10px;
      font-size: 14px;
      color: #8c959f;
    }
  }
}

.edit-team-card {
  margin-top: 24px;
}

.ownership-pill,
.meta-id-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
  font-size: 11px;
  font-weight: 600;
}

.ownership-pill--mine {
  background: #eef6ff;
  border-color: #c6ddff;
  color: #245ea6;
}

.ownership-pill--collaborator {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

.ownership-pill--success {
  background: #ecfdf3;
  border-color: #b7ebc6;
  color: #137333;
}

.ownership-pill--muted {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

/* 按钮样式 */
.submit-btn {
  background: #1f2328;
  border: 1px solid #1f2328;
  color: white;

  &:hover {
    background: #2f363d;
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
  }

  &:active {
    transform: translateY(0);
  }
}

.reset-btn {
  background: #fff;
  border-color: #d8dee4;
  color: #57606a;

  &:hover {
    background: #f6f8fa;
    border-color: #d0d7de;
    color: #24292f;
  }
}

.detail-btn {
  background: #fff;
  border-color: #d8dee4;
  color: #57606a;

  &:hover {
    background: #f6f8fa;
    border-color: #d0d7de;
    color: #24292f;
  }
}
</style>
