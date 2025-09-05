<template>
  <div id="appEditPage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>应用编辑中心</h1>
      <p>管理应用的详细信息与配置</p>
    </div>

    <!-- 编辑面板 -->
    <div class="edit-panel">
      <a-card class="info-card" :loading="loading">
        <div class="card-header">
          <h2>应用信息</h2>
          <a-button type="link" class="view-chat" @click="goToChat">
            <MessageOutlined />
            <span>进入对话</span>
          </a-button>
        </div>

        <a-form
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
          ref="formRef"
        >
          <a-form-item label="应用名称" name="appName" class="form-item">
            <a-input
              v-model:value="formData.appName"
              placeholder="为您的应用输入一个有意义的名称"
              :maxlength="50"
              allow-clear
            >
              <template #prefix>
                <TagOutlined />
              </template>
              <template #suffix>
                <span class="max-length">{{ formData.appName.length }}/50</span>
              </template>
            </a-input>
            <div class="form-tip">
              好的应用名称应该简洁明确，能准确反映应用的核心功能
            </div>
          </a-form-item>

          <!-- 管理员专属字段 -->
          <template v-if="isAdmin">
            <a-form-item
              label="应用封面"
              name="cover"
              class="form-item"
              extra="支持外部图片链接，建议尺寸：400x300"
            >
              <a-input
                v-model:value="formData.cover"
                placeholder="输入封面图片URL链接"
                allow-clear
              >
                <template #prefix>
                  <PictureOutlined />
                </template>
              </a-input>

              <div v-if="formData.cover" class="cover-preview">
                <div class="preview-title">封面预览</div>
                <a-image
                  :src="formData.cover"
                  :width="200"
                  :height="150"
                  :fallback="fallbackImage"
                />
                <a-button
                  type="link"
                  @click="clearCover"
                  class="clear-btn"
                >
                  清除封面
                </a-button>
              </div>
            </a-form-item>
            <template v-if="isAdmin">
              <a-form-item
                label="应用优先级"
                name="priority"
                class="form-item"
                extra="设置为99可标记为星选应用"
              >
                <a-tooltip
                  title="星选应用会展示在首页供所有用户查看"
                  placement="right"
                >
                  <a-input-number
                    v-model:value="formData.priority"
                    :min="0"
                    :max="99"
                    style="width: 100%"
                  >
                    <template #prefix>
                      <StarOutlined />
                    </template>
                    <template #addonBefore>
                      <span style="color: #2c3e50">星选等级：</span>
                    </template>
                    <template #addonAfter>
                    <span v-if="formData.priority === 99" style="color: #d48806">
                      (星选应用)
                    </span>
                    </template>
                  </a-input-number>
                </a-tooltip>
              </a-form-item>
            </template>
          </template>

          <!-- 不可编辑字段 -->
          <a-form-item label="初始提示词" name="initPrompt" class="form-item">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="应用的初始生成提示词"
              :rows="5"
              :maxlength="1000"
              disabled
            />
            <div class="form-tip">
              此字段为应用的初始生成指令，创建后不可修改
            </div>
          </a-form-item>

          <a-form-item label="生成类型" name="codeGenType" class="form-item">
            <a-tag :color="getTypeColor(appInfo?.codeGenType)">
              {{
                appInfo?.codeGenType === 'single_html' ? '单文件结构' :
                  appInfo?.codeGenType === 'multi_file' ? '多文件结构' :
                    appInfo?.codeGenType === 'vue_project' ? 'VUE 项目工程' :
                      formatCodeGenType(appInfo?.codeGenType)
              }}
            </a-tag>
            <div class="form-tip">
              生成类型决定了应用的框架和技术栈，创建后不可变更
            </div>
          </a-form-item>

          <a-form-item
            v-if="formData.deployKey"
            label="部署密钥"
            name="deployKey"
            class="form-item"
          >
            <a-input v-model:value="formData.deployKey" disabled />
            <div class="form-tip">
              此密钥用于应用部署，请勿随意分享给他人
            </div>
          </a-form-item>

          <a-form-item class="form-actions">
            <a-space>
              <a-button
                type="primary"
                html-type="submit"
                :loading="submitting"
                size="large"
                class="submit-btn"
              >
                <SaveOutlined />
                <span>保存修改</span>
              </a-button>
              <a-button @click="resetForm" size="large" class="reset-btn">
                <UndoOutlined />
                <span>重置表单</span>
              </a-button>
              <a-button
                v-if="isDeployed"
                type="primary"
                class="detail-btn"
                target="_blank"
                :href="deployedSiteUrl"
                size="large"
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
          <h2>元信息</h2>
        </div>
        <div class="card-header">
        <span class="app-id">
            应用ID：<a-tag color="blue">{{ appInfo?.id || '--' }}</a-tag>
          </span>
        </div>
        <a-descriptions :column="1" class="meta-grid">
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
              <template v-if="appInfo?.deployKey">
                <a-tag color="green">已部署</a-tag>
              </template>
              <a-tag v-else color="red">未部署</a-tag>
            </div>
          </a-descriptions-item>

          <a-descriptions-item label="部署时间">
            <div class="meta-item">
              <CloudServerOutlined />
              <template v-if="appInfo?.deployKey">
                <span class="time">{{ formatTime(appInfo.deployedTime) }}</span>
              </template>
              <a-tag v-else color="red"> ——</a-tag>
            </div>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import {
  CalendarOutlined,
  CloudServerOutlined,
  ExportOutlined,
  MessageOutlined,
  PictureOutlined,
  SaveOutlined,
  StarOutlined,
  SyncOutlined,
  TagOutlined,
  UndoOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { DEPLOY_DOMAIN } from '@/config/env'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { formatCodeGenType } from '@/enums/codeGenTypes.ts'
import { formatTime } from '@/utils/timeUtil.ts'
import UserInfo from '@/components/UserInfo.vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const formRef = ref<FormInstance>()

// 应用信息
const appInfo = ref<API.AppVO>()
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

// 判断应用是否已部署
const isDeployed = computed(() => {
  return !!appInfo.value?.deployKey
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
    { required: true, message: '请填写应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '名称长度应为1-50个字符', trigger: 'blur' }
  ],
  cover: [{ type: 'url', message: '请提供有效的图片URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围为0-99', trigger: 'blur' }]
}

// 根据生成类型获取标签颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'react': '#61dafb',
    'vue': '#42b883',
    'angular': '#dd0031',
    'html': '#e34c26',
    'nodejs': '#68a063',
    'flutter': '#04599C',
    'swift': '#ff2d55'
  }
  return colors[type] || 'blue'
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID无效')
    await router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查权限
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
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
      message.error('获取应用信息失败: ' + res.data.message)
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败:', error)
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 清除封面
const clearCover = () => {
  formData.cover = ''
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

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped lang="less">
#appEditPage {
  padding: 24px;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  min-height: calc(100vh - 48px);
  position: relative;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"><path fill="none" stroke="rgba(180,200,220,0.05)" stroke-width="1" d="M0,400 Q200,300 400,400 M0,0 Q200,100 400,0"/></svg>');
    background-size: 400px;
    opacity: 0.3;
    pointer-events: none;
    z-index: 0;
  }
}

.page-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-family: 'Comic Neue', cursive;
    font-size: 2.6rem;
    font-weight: 700;
    color: #2c3e50;
    margin-bottom: 8px;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  }

  p {
    font-family: 'Nunito', sans-serif;
    font-size: 1.1rem;
    color: #7f8c8d;
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
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(204, 230, 255, 0.2);
  overflow: hidden;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  }
}

.info-card {
  flex: 1;
  padding: 30px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 15px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 25px;

    h2 {
      font-family: 'Comic Neue', cursive;
      font-size: 1.3rem;
      color: #2c3e50;
      margin: 0;
      font-weight: 700;
    }

    .app-id {
      color: #7f8c8d;
      font-size: 0.95rem;

      .ant-tag {
        margin-left: 8px;
      }
    }

    .view-chat {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.95rem;
      color: #2c3e50;
      transition: all 0.2s ease;

      &:hover {
        color: #00c4ff;
      }
    }
  }

  .form-item {
    margin-bottom: 25px;

    :deep(.ant-form-item-label) {
      font-weight: 600;
      color: #2c3e50;
      padding-bottom: 5px;
    }

    .ant-input-prefix, .ant-input-suffix {
      color: #00c4ff;
    }

    .max-length {
      font-size: 0.85rem;
      color: #7f8c8d;
      padding-right: 8px;
    }

    .gen-type-tag {
      padding: 4px 12px;
      font-size: 1rem;
    }
  }

  .form-tip {
    font-family: 'Nunito', sans-serif;
    font-size: 0.85rem;
    color: #7f8c8d;
    margin-top: 8px;
    padding: 0 3px;
  }

  .form-actions {
    margin-top: 15px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;

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

.cover-preview {
  margin-top: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px dashed #e6e1da;

  .preview-title {
    font-size: 0.95rem;
    color: #7f8c8d;
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
    color: #00c4ff;
    font-size: 0.95rem;
    transition: all 0.2s ease;

    &:hover {
      color: #5adbc8;
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
    padding-bottom: 15px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 25px;

    h2 {
      font-family: 'Comic Neue', cursive;
      font-size: 1.3rem;
      color: #2c3e50;
      margin: 0;
      font-weight: 700;
    }
  }

  .meta-grid {
    :deep(.ant-descriptions-item) {
      padding-bottom: 18px;
      border-bottom: 1px solid #f0f0f0;
    }

    :deep(.ant-descriptions-item-label) {
      font-weight: 600;
      color: #7f8c8d;
      width: 100px;
      padding-right: 20px;
    }

    :deep(.ant-descriptions-item-content) {
      font-weight: 500;
      color: #2c3e50;
    }
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 5px 0;

    .anticon {
      font-size: 1.1rem;
      color: #00c4ff;
    }

    .time {
      margin-left: 10px;
      font-size: 0.95rem;
      color: #7f8c8d;
    }
  }
}

/* 按钮样式 */
.submit-btn {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  color: white;
  box-shadow: 0 8px 16px rgba(116, 235, 213, 0.25);

  &:hover {
    background: linear-gradient(135deg, #5adbc8 0%, #8b9de6 100%);
    transform: translateY(-3px);
    box-shadow: 0 12px 20px rgba(116, 235, 213, 0.35);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 4px 8px rgba(116, 235, 213, 0.3);
  }
}

.reset-btn {
  background: #f0f2f5;
  border-color: #d9d9d9;

  &:hover {
    background: #e6f7ff;
    border-color: #00c4ff;
    color: #00c4ff;
  }
}

.detail-btn {
  background: #f0f2f5;
  border-color: #d9d9d9;

  &:hover {
    background: #e6f7ff;
    border-color: #00c4ff;
    color: #00c4ff;
  }
}
</style>
