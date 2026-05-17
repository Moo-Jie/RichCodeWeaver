<template>
  <div id="userCenterPage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>用户个人中心</h1>
      <p>管理您的账户信息和设置</p>
    </div>

    <div class="content-cards">
      <!-- 个人信息卡片 -->
      <a-card class="info-card">
        <div class="card-header">
          <h2>
            <UserOutlined />
            个人信息
          </h2>
          <a-button class="gradient-button" type="primary" @click="showEditModal">
            <template #icon>
              <EditOutlined />
            </template>
            编辑资料
          </a-button>
        </div>

        <div class="user-profile">
          <div class="avatar-section">
            <a-avatar :size="120" :src="userInfo.userAvatar" class="user-avatar">
              <template v-if="!userInfo.userAvatar" #icon>
                <UserOutlined style="font-size: 48px" />
              </template>
            </a-avatar>
            <a-upload
              :before-upload="handleAvatarUpload"
              :custom-request="handleAvatarCustomRequest"
              :show-upload-list="false"
              class="avatar-uploader"
              name="avatar"
            >
              <a-button class="upload-btn gradient-button">
                <UploadOutlined />
                更换头像
              </a-button>
            </a-upload>
          </div>

          <div class="user-details">
            <div class="detail-row">
              <label>用户账号</label>
              <span>{{ userInfo.userAccount }}</span>
            </div>
            <div class="detail-row">
              <label>用户昵称</label>
              <span>{{ userInfo.userName }}</span>
            </div>
            <div class="detail-row">
              <label>用户简介</label>
              <span>{{ userInfo.userProfile }}</span>
            </div>
            <div class="detail-row">
              <label>用户角色</label>
              <a-tag :color="userInfo.userRole === 'admin' ? 'green' : 'blue'">
                {{ userInfo.userRole === 'admin' ? '管理员' : '普通用户' }}
              </a-tag>
            </div>
            <div class="detail-row">
              <label>用户身份</label>
              <span>{{ identityLabelMap[userInfo.userIdentity] || userInfo.userIdentity || '未设置'
                }}</span>
            </div>
            <div class="detail-row">
              <label>行业领域</label>
              <span>{{ userInfo.userIndustry || '未设置' }}</span>
            </div>
            <div class="detail-row">
              <label>注册时间</label>
              <span>
                <CalendarOutlined />
                {{ dayjs(userInfo.createTime).format('YYYY-MM-DD HH:mm') }}
              </span>
            </div>
          </div>
        </div>
      </a-card>

      <!-- 账户安全卡片 -->
      <a-card class="security-card">
        <h2>
          <LockOutlined />
          账户安全
        </h2>

        <div class="security-settings">
          <div class="setting-item">
            <div>
              <h3>登录密码</h3>
              <p>定期更改密码可提升账户安全</p>
            </div>
            <a-button class="gradient-button" type="primary" @click="showPasswordModal">
              修改密码
            </a-button>
          </div>

          <div class="setting-item">
            <div>
              <h3>邮箱验证</h3>
              <p>{{
                  userInfo.email ? `已绑定邮箱: ${maskEmail(userInfo.email)}` : '未绑定邮箱'
                }}</p>
            </div>
            <a-button class="gradient-button" type="primary" @click="handleEmail">
              {{ userInfo.email ? '更换邮箱' : '绑定邮箱' }}
            </a-button>
          </div>
        </div>

        <div class="logout-section">
          <a-button class="logout-button" danger @click="handleLogout">
            <template #icon>
              <LogoutOutlined />
            </template>
            退出登录
          </a-button>
        </div>
      </a-card>

      <a-card class="ai-profile-card">
        <div class="card-header">
          <h2>
            <RobotOutlined />
            AI 用户结构化画像
          </h2>
          <a-button class="gradient-button" type="primary" @click="showAiProfileModal">
            <template #icon>
              <EditOutlined />
            </template>
            我来添加/完善
          </a-button>
        </div>

        <div class="ai-profile-tip">
          <p>
            这份结构化画像会作为 AI 个性化生成的长期参考；当你在对话中明确表达稳定偏好时，AI 也可能自动增量完善它。
          </p>
        </div>

        <a-spin :spinning="loadingAiProfile">
          <div v-if="hasAiProfileContent" class="ai-profile-grid">
            <div v-for="item in aiProfileDisplayItems" :key="item.label" class="ai-profile-item">
              <span class="ai-profile-label">{{ item.label }}</span>
              <span :class="['ai-profile-value', { 'is-empty': !item.value }]">{{ item.value || '未设置' }}</span>
            </div>
          </div>
          <div v-else class="ai-profile-empty">
            <div class="ai-profile-empty-title">你还没有完善 AI 用户画像</div>
            <div class="ai-profile-empty-desc">
              点击“我来添加/完善”，告诉 AI 你偏好的项目类型、技术栈、设计风格与使用目标。
            </div>
          </div>
        </a-spin>
      </a-card>
    </div>

    <!-- 编辑信息模态框 -->
    <a-modal
      v-model:visible="editVisible"
      :cancel-text="'取消'"
      :keyboard="false"
      :mask-closable="false"
      :ok-text="'提交'"
      class="custom-modal"
      title="编辑个人信息"
      @ok="handleEditSubmit"
    >
      <br>
      <a-form :label-col="{ span: 6 }" :model="editForm" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户昵称">
          <a-input v-model:value="editForm.userName" placeholder="请输入新昵称" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            :maxlength="200"
            :rows="4"
            placeholder="介绍一下你自己..."
          />
        </a-form-item>
        <a-form-item label="用户身份">
          <a-select
            v-model:value="editForm.userIdentity"
            :options="identitySelectOptions"
            placeholder="请选择您的身份"
          />
        </a-form-item>
        <a-form-item label="行业领域">
          <a-auto-complete
            v-model:value="editForm.userIndustry"
            :filter-option="filterIndustry"
            :options="industryAutoOptions"
            placeholder="请选择或输入您的行业领域"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码模态框 -->
    <a-modal
      v-model:visible="passwordVisible"
      class="custom-modal"
      title="修改登录密码"
      @ok="handlePasswordSubmit"
    >
      <br>
      <a-form :label-col="{ span: 6 }" :model="passwordForm" :wrapper-col="{ span: 18 }">
        <a-form-item label="原密码">
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码">
          <a-input-password v-model:value="passwordForm.confirmPassword"
                            placeholder="请确认新密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 绑定邮箱模态框 -->
    <a-modal
      v-model:visible="emailVisible"
      class="custom-modal"
      title="绑定邮箱"
      @ok="handleEmailSubmit"
    >
      <br>
      <a-form :label-col="{ span: 6 }" :model="emailForm" :wrapper-col="{ span: 18 }">
        <a-form-item label="邮箱地址">
          <a-input
            v-model:value="emailForm.email"
            placeholder="请输入邮箱地址"
          />
        </a-form-item>
        <a-form-item label="图形验证码">
          <div style="display: flex; gap: 8px; align-items: center;">
            <a-input v-model:value="emailForm.captchaAnswer" placeholder="请输入计算结果" style="flex: 1;" />
            <img
              v-if="emailCaptchaImage"
              :src="emailCaptchaImage"
              alt="验证码"
              style="height: 40px; cursor: pointer; border-radius: 4px; border: 1px solid #f0f0f0;"
              @click="refreshEmailCaptcha"
            />
            <a-button v-else size="small" @click="refreshEmailCaptcha">获取验证码</a-button>
          </div>
        </a-form-item>
        <a-form-item label="邮箱验证码">
          <div style="display: flex; gap: 8px;">
            <a-input v-model:value="emailForm.emailCode" placeholder="请输入邮箱验证码" style="flex: 1;" />
            <a-button
              :disabled="emailCountdown > 0 || emailSendingCode"
              :loading="emailSendingCode"
              @click="handleSendEmailCode"
            >
              {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="aiProfileVisible"
      :cancel-text="'取消'"
      :confirm-loading="savingAiProfile"
      :ok-text="'保存'"
      class="custom-modal"
      title="完善 AI 用户结构化画像"
      @ok="handleAiProfileSubmit"
    >
      <div class="ai-profile-form-note">
        AI 会把这里的信息作为个性化生成参考。当前保存为增量完善模式，已记录字段建议直接修改为新值。
      </div>
      <a-form :label-col="{ span: 7 }" :model="aiProfileForm" :wrapper-col="{ span: 17 }">
        <a-form-item label="偏好项目类型">
          <a-select
            v-model:value="aiProfileForm.preferredCodeGenType"
            :options="codeGenTypeOptions"
            placeholder="请选择更常用的项目类型"
          />
        </a-form-item>
        <a-form-item label="偏好生成模式">
          <a-select
            v-model:value="aiProfileForm.preferredGenMode"
            :options="genModeOptions"
            placeholder="请选择更偏好的生成模式"
          />
        </a-form-item>
        <a-form-item label="技术栈偏好">
          <a-textarea
            v-model:value="aiProfileForm.techPreferences"
            :rows="3"
            placeholder="例如：偏好 Vue3、组件化、简洁依赖"
          />
        </a-form-item>
        <a-form-item label="设计风格偏好">
          <a-textarea
            v-model:value="aiProfileForm.stylePreferences"
            :rows="3"
            placeholder="例如：白底、浅灰边框、深色文字、克制动效"
          />
        </a-form-item>
        <a-form-item label="不喜欢的模式">
          <a-textarea
            v-model:value="aiProfileForm.dislikedPatterns"
            :rows="3"
            placeholder="例如：避免大面积渐变、避免复杂动效、避免 jQuery"
          />
        </a-form-item>
        <a-form-item label="使用目标">
          <a-input v-model:value="aiProfileForm.usageGoal" placeholder="例如：个人作品集、企业官网、课程作业" />
        </a-form-item>
        <a-form-item label="技术水平">
          <a-select
            v-model:value="aiProfileForm.experienceLevel"
            :options="experienceLevelOptions"
            placeholder="请选择当前技术水平"
          />
        </a-form-item>
        <a-form-item label="偏好语言">
          <a-select
            v-model:value="aiProfileForm.preferredLanguage"
            :options="languageOptions"
            placeholder="请选择偏好语言"
          />
        </a-form-item>
        <a-form-item label="补充备注">
          <a-textarea
            v-model:value="aiProfileForm.customNotes"
            :rows="4"
            placeholder="可补充你希望 AI 长期记住的偏好、约束或习惯"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  CalendarOutlined,
  EditOutlined,
  LockOutlined,
  LogoutOutlined,
  RobotOutlined,
  UploadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { getMyAiUserProfile, updateMyAiUserProfile } from '@/api/aiUserProfileController'
import {
  bindEmail,
  getLoginUser,
  getMathCaptcha,
  sendEmailCode,
  updateUser,
  updateUserAvatar,
  updateUserPassword,
  userLogout
} from '@/api/userController'
import { identityLabelMap, identityOptions, industryOptions } from '@/constants/identityOptions'
import { validateEmail } from '@/utils/emailUtil'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const codeGenTypeLabelMap: Record<string, string> = {
  single_html: '单页 HTML',
  multi_file: '多文件项目',
  vue_project: 'Vue 工程项目'
}

const genModeLabelMap: Record<string, string> = {
  workflow: '工作流模式',
  agent: 'Agent 模式'
}

const experienceLevelLabelMap: Record<string, string> = {
  beginner: '初学者',
  intermediate: '中级',
  advanced: '高级'
}

const buildEmptyAiProfile = (): API.AiUserProfile => ({
  preferredCodeGenType: '',
  preferredGenMode: '',
  techPreferences: '',
  stylePreferences: '',
  dislikedPatterns: '',
  usageGoal: '',
  experienceLevel: '',
  preferredLanguage: '',
  customNotes: ''
})

// 用户信息
const userInfo = reactive({
  userAccount: '',
  userName: '未命名',
  userRole: 'user',
  userAvatar: '',
  userProfile: '',
  userIdentity: '',
  userIndustry: '',
  email: '',
  phone: '',
  createTime: new Date().toISOString()
})

// 模态框状态
const editVisible = ref(false)
const passwordVisible = ref(false)
const emailVisible = ref(false)
const aiProfileVisible = ref(false)
const emailCaptchaId = ref('')
const emailCaptchaImage = ref('')
const emailCountdown = ref(0)
const emailSendingCode = ref(false)
const loadingAiProfile = ref(false)
const savingAiProfile = ref(false)
let emailCountdownTimer: ReturnType<typeof setInterval> | null = null

const aiProfileInfo = reactive<API.AiUserProfile>(buildEmptyAiProfile())

const aiProfileForm = reactive<API.AiUserProfileUpdateRequest>({
  preferredCodeGenType: '',
  preferredGenMode: '',
  techPreferences: '',
  stylePreferences: '',
  dislikedPatterns: '',
  usageGoal: '',
  experienceLevel: '',
  preferredLanguage: '',
  customNotes: ''
})

// 编辑表单
const editForm = reactive({
  userName: '',
  userProfile: '',
  userIdentity: '',
  userIndustry: ''
})

const identitySelectOptions = identityOptions.map(item => ({
  value: item.value,
  label: item.label
}))

const codeGenTypeOptions = Object.entries(codeGenTypeLabelMap).map(([value, label]) => ({
  value,
  label
}))

const genModeOptions = Object.entries(genModeLabelMap).map(([value, label]) => ({
  value,
  label
}))

const experienceLevelOptions = Object.entries(experienceLevelLabelMap).map(([value, label]) => ({
  value,
  label
}))

const languageOptions = [
  { value: '中文', label: '中文' },
  { value: '英文', label: '英文' },
  { value: '中英双语', label: '中英双语' }
]

const industryAutoOptions = industryOptions.map(item => ({
  value: item
}))

const filterIndustry = (input: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

const aiProfileDisplayItems = computed(() => [
  {
    label: '偏好项目类型',
    value: codeGenTypeLabelMap[aiProfileInfo.preferredCodeGenType || ''] || aiProfileInfo.preferredCodeGenType || ''
  },
  {
    label: '偏好生成模式',
    value: genModeLabelMap[aiProfileInfo.preferredGenMode || ''] || aiProfileInfo.preferredGenMode || ''
  },
  {
    label: '技术栈偏好',
    value: aiProfileInfo.techPreferences || ''
  },
  {
    label: '设计风格偏好',
    value: aiProfileInfo.stylePreferences || ''
  },
  {
    label: '不喜欢的模式',
    value: aiProfileInfo.dislikedPatterns || ''
  },
  {
    label: '使用目标',
    value: aiProfileInfo.usageGoal || ''
  },
  {
    label: '技术水平',
    value: experienceLevelLabelMap[aiProfileInfo.experienceLevel || ''] || aiProfileInfo.experienceLevel || ''
  },
  {
    label: '偏好语言',
    value: aiProfileInfo.preferredLanguage || ''
  },
  {
    label: '补充备注',
    value: aiProfileInfo.customNotes || ''
  }
])

const hasAiProfileContent = computed(() => {
  return aiProfileDisplayItems.value.some(item => !!item.value && item.value.trim().length > 0)
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 邮箱绑定表单
const emailForm = reactive({
  email: '',
  captchaAnswer: '',
  emailCode: ''
})

const applyAiProfileData = (profile?: API.AiUserProfile) => {
  Object.assign(aiProfileInfo, buildEmptyAiProfile(), profile || {})
}

const syncAiProfileForm = () => {
  Object.assign(aiProfileForm, buildEmptyAiProfile(), {
    preferredCodeGenType: aiProfileInfo.preferredCodeGenType || '',
    preferredGenMode: aiProfileInfo.preferredGenMode || '',
    techPreferences: aiProfileInfo.techPreferences || '',
    stylePreferences: aiProfileInfo.stylePreferences || '',
    dislikedPatterns: aiProfileInfo.dislikedPatterns || '',
    usageGoal: aiProfileInfo.usageGoal || '',
    experienceLevel: aiProfileInfo.experienceLevel || '',
    preferredLanguage: aiProfileInfo.preferredLanguage || '',
    customNotes: aiProfileInfo.customNotes || ''
  })
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(userInfo, res.data.data)
      Object.assign(editForm, {
        userName: res.data.data.userName,
        userProfile: res.data.data.userProfile,
        userIdentity: res.data.data.userIdentity || '',
        userIndustry: res.data.data.userIndustry || ''
      })
    } else {
      message.error('获取用户信息失败:' + res.data.message)
    }
  } catch (error) {
    console.error('获取用户信息失败：', error)
  }
}

const fetchAiUserProfile = async () => {
  loadingAiProfile.value = true
  try {
    const res = await getMyAiUserProfile()
    if (res.data.code === 0) {
      applyAiProfileData(res.data.data)
    } else {
      message.error('获取 AI 用户画像失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (error) {
    console.error('获取 AI 用户画像失败：', error)
    message.error('获取 AI 用户画像失败，请稍后重试')
  } finally {
    loadingAiProfile.value = false
  }
}

// 显示编辑模态框
const showEditModal = () => {
  editVisible.value = true
}

const showAiProfileModal = () => {
  syncAiProfileForm()
  aiProfileVisible.value = true
}

const normalizeAiProfilePayload = (): API.AiUserProfileUpdateRequest => {
  const payload: API.AiUserProfileUpdateRequest = {}
  Object.entries(aiProfileForm).forEach(([key, value]) => {
    const normalizedValue = typeof value === 'string' ? value.trim() : value
    if (normalizedValue) {
      payload[key as keyof API.AiUserProfileUpdateRequest] = normalizedValue
    }
  })
  return payload
}

const handleAiProfileSubmit = async () => {
  const payload = normalizeAiProfilePayload()
  if (Object.keys(payload).length === 0) {
    message.warning('请至少完善一项结构化画像信息')
    return
  }
  savingAiProfile.value = true
  try {
    const res = await updateMyAiUserProfile(payload)
    if (res.data.code === 0) {
      applyAiProfileData(res.data.data)
      aiProfileVisible.value = false
      message.success('AI 用户画像更新成功')
    } else {
      message.error('更新失败：' + (res.data.message || '请稍后重试'))
    }
  } catch (error) {
    console.error('更新 AI 用户画像失败：', error)
    message.error('更新失败，请稍后重试')
  } finally {
    savingAiProfile.value = false
  }
}

// 提交编辑信息
const handleEditSubmit = async () => {
  try {
    const res = await updateUser({
      id: loginUserStore.loginUser.id,
      ...editForm
    })

    if (res.data.code === 0) {
      message.success('用户信息更新成功')
      Object.assign(userInfo, editForm)
      editVisible.value = false
      // 刷新全局登录用户缓存，确保身份等变更立即生效
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新用户信息失败：', error)
    message.error('更新失败，请稍后重试')
  }
}

// 显示修改密码模态框
const showPasswordModal = () => {
  passwordVisible.value = true
}

// 提交修改密码
const handlePasswordSubmit = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }

  if (passwordForm.newPassword.length < 6) {
    message.error('密码长度不能小于6位')
    return
  }

  try {
    const res = await updateUserPassword({
      userId: loginUserStore.loginUser.id,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    if (res.data.code === 0) {
      message.success('密码修改成功')
      passwordVisible.value = false
      // 重置表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (error) {
    console.error('修改密码失败：', error)
    message.error('修改失败，请稍后重试')
  }
}

// 处理邮箱操作
const handleEmail = async () => {
  emailForm.email = userInfo.email || ''
  emailForm.captchaAnswer = ''
  emailForm.emailCode = ''
  emailVisible.value = true
  await refreshEmailCaptcha()
}

// 刷新邮箱绑定的图形验证码
const refreshEmailCaptcha = async () => {
  try {
    const res = await getMathCaptcha()
    if (res.data.code === 0 && res.data.data) {
      emailCaptchaId.value = res.data.data.captchaId || ''
      emailCaptchaImage.value = res.data.data.captchaImage || ''
    }
  } catch {
    message.error('获取验证码失败')
  }
}

// 发送邮箱验证码
const handleSendEmailCode = async () => {
  const emailResult = validateEmail(emailForm.email)
  if (!emailResult.valid) {
    message.warning(emailResult.message || '邮箱格式不正确')
    return
  }
  if (!emailForm.captchaAnswer) {
    message.warning('请先输入计算结果')
    return
  }
  if (!emailCaptchaId.value) {
    message.warning('请先获取图形验证码')
    return
  }
  emailSendingCode.value = true
  try {
    const res = await sendEmailCode({
      email: emailResult.normalizedEmail,
      captchaId: emailCaptchaId.value,
      captchaAnswer: emailForm.captchaAnswer
    })
    if (res.data.code === 0) {
      message.success('验证码已发送，请查看邮箱')
      emailCountdown.value = 60
      emailCountdownTimer = setInterval(() => {
        emailCountdown.value--
        if (emailCountdown.value <= 0) {
          clearInterval(emailCountdownTimer!)
          emailCountdownTimer = null
        }
      }, 1000)
    } else {
      message.error(res.data.message || '发送失败')
      await refreshEmailCaptcha()
    }
  } catch {
    message.error('发送验证码失败')
    await refreshEmailCaptcha()
  } finally {
    emailSendingCode.value = false
  }
}

// 提交邮箱绑定
const handleEmailSubmit = async () => {
  const emailResult = validateEmail(emailForm.email)
  if (!emailResult.valid) {
    message.error(emailResult.message || '请输入正确的邮箱格式')
    return
  }
  if (!emailForm.emailCode) {
    message.error('请输入邮箱验证码')
    return
  }
  const email = emailResult.normalizedEmail

  try {
    const res = await bindEmail({ email, emailCode: emailForm.emailCode })
    if (res.data.code === 0) {
      message.success('邮箱绑定成功')
      userInfo.email = email
      emailVisible.value = false
      emailForm.email = ''
      emailForm.captchaAnswer = ''
      emailForm.emailCode = ''
    } else {
      message.error('绑定失败：' + res.data.message)
    }
  } catch (error) {
    console.error('绑定邮箱失败：', error)
    message.error('绑定失败，请稍后重试')
  }
}

// 处理头像上传
const handleAvatarUpload = async (file: File) => {
  try {
    // 创建FormData对象
    const formData = new FormData()
    formData.append('file', file)

    // 直接使用request发送multipart请求
    const res = await updateUserAvatar(formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (res.data.code === 0) {
      message.success('头像上传成功')
      // 重新获取用户信息以更新头像
      await fetchUserInfo()
    } else {
      message.error('头像上传失败：' + res.data.message)
    }
  } catch (error) {
    console.error('头像上传失败：', error)
    message.error('头像上传失败，请稍后重试')
  }
  return false // 阻止默认上传行为
}

const handleAvatarCustomRequest = async (options: { file: File }) => {
  await handleAvatarUpload(options.file)
}

// 退出登录
const handleLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录'
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 邮箱脱敏处理
const maskEmail = (email: string) => {
  if (!email) return email
  const [username, domain] = email.split('@')
  if (!username || !domain) return email
  return `${username.substring(0, 2)}***@${domain}`
}

onMounted(() => {
  fetchUserInfo()
  fetchAiUserProfile()
})
</script>

<style lang="less" scoped>
#userCenterPage {
  padding: 32px;
  width: 100%;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.content-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 992px) {
    grid-template-columns: 1fr;
  }
}

.info-card, .security-card, .ai-profile-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  h2 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 8px;

    .anticon {
      color: #666;
      font-size: 16px;
    }
  }
}

.info-card {
  padding: 20px 24px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      border: none;
      padding-bottom: 0;
    }
  }

  .user-profile {
    display: flex;
    gap: 40px;

    @media (max-width: 768px) {
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }
  }

  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;

    .user-avatar {
      border: 2px solid #f0f0f0;
      background: #fafafa;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);

      .ant-avatar-icon {
        color: #999;
      }
    }

    .upload-btn {
      border-radius: 8px;
      font-size: 14px;
      padding: 0 16px;
      height: 36px;
      display: flex;
      align-items: center;
      gap: 6px;
      background: #1a1a1a;
      border-color: #1a1a1a;
      color: #fff;

      &:hover {
        background: #333;
        border-color: #333;
      }
    }
  }

  .user-details {
    flex: 1;

    .detail-row {
      display: flex;
      padding: 14px 0;
      border-bottom: 1px dashed #eee;

      &:last-child {
        border-bottom: none;
      }

      label {
        width: 100px;
        color: #999;
        font-weight: 500;
        text-align: right;
        padding-right: 15px;
        font-size: 13px;
      }

      span {
        flex: 1;
        color: #1a1a1a;
        font-size: 14px;

        .anticon {
          margin-right: 6px;
          color: #666;
        }
      }
    }
  }
}

.security-card {
  padding: 20px 24px;

  .security-settings {
    margin-bottom: 30px;

    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 18px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      h3 {
        margin: 0;
        font-size: 15px;
        color: #1a1a1a;
        font-weight: 600;
      }

      p {
        margin: 4px 0 0;
        font-size: 13px;
        color: #999;
      }

      button {
        min-width: 100px;
        border-radius: 8px;
        height: 36px;
        font-size: 14px;
        background: #1a1a1a;
        border-color: #1a1a1a;
        color: #fff;

        &:hover {
          background: #333;
          border-color: #333;
        }
      }
    }
  }

  .logout-section {
    margin-top: 20px;
    padding-top: 25px;
    border-top: 1px solid #f0f0f0;
    text-align: center;

    .logout-button {
      width: 50%;
      border-radius: 8px;
      height: 36px;
      font-weight: 500;
      font-size: 14px;

      .anticon {
        margin-right: 6px;
      }
    }
  }
}

.ai-profile-card {
  grid-column: 1 / -1;
  padding: 20px 24px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;

    h2 {
      margin: 0;
      border: none;
      padding-bottom: 0;
    }
  }

  .ai-profile-tip {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
    margin-bottom: 18px;
    border-radius: 10px;
    border: 1px solid #f0f0f0;
    background: #fafafa;

    p {
      margin: 0;
      color: #666;
      font-size: 13px;
      line-height: 1.7;
    }
  }

  .ai-profile-tip-badge {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 28px;
    padding: 0 10px;
    border-radius: 999px;
    background: #1a1a1a;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.2px;
  }

  .ai-profile-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;

    @media (max-width: 992px) {
      grid-template-columns: 1fr;
    }
  }

  .ai-profile-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-height: 104px;
    padding: 16px;
    border-radius: 10px;
    border: 1px solid #f0f0f0;
    background: #fff;
  }

  .ai-profile-label {
    color: #999;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.2px;
  }

  .ai-profile-value {
    color: #1a1a1a;
    font-size: 14px;
    line-height: 1.7;
    word-break: break-word;

    &.is-empty {
      color: #bbb;
    }
  }

  .ai-profile-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 220px;
    border: 1px dashed #e6e6e6;
    border-radius: 12px;
    background: #fafafa;
    text-align: center;
    padding: 24px;
  }

  .ai-profile-empty-title {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  .ai-profile-empty-desc {
    max-width: 520px;
    font-size: 13px;
    line-height: 1.8;
    color: #888;
  }
}

.gradient-button {
  background: #1a1a1a;
  border: none;
  color: #fff;
  border-radius: 8px;
  transition: all 0.2s ease;

  &:hover {
    background: #333;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  }
}

// 模态框样式优化
:deep(.custom-modal) {
  .ant-modal {
    border-radius: 12px;
    overflow: hidden;
  }

  .ant-modal-content {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid #f0f0f0;
  }

  .ant-modal-header {
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 24px;
    border-radius: 12px 12px 0 0;

    .ant-modal-title {
      color: #1a1a1a;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .ant-modal-body {
    padding: 20px 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid #f0f0f0;
    padding: 16px 24px;
    text-align: center;

    .ant-btn {
      border-radius: 8px;
      padding: 0 20px;
      height: 36px;
      font-weight: 500;
      font-size: 14px;
      transition: all 0.2s ease;

      &.ant-btn-default {
        border-color: #e5e5e5;
        color: #666;

        &:hover {
          border-color: #d0d0d0;
          background: #fafafa;
          color: #333;
        }
      }

      &.ant-btn-primary {
        background: #1a1a1a;
        border: none;
        color: #fff;

        &:hover {
          background: #333;
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
        }
      }
    }
  }

  .ant-form {
    .ant-form-item {
      margin-bottom: 16px;

      .ant-form-item-label {
        label {
          color: #1a1a1a;
          font-weight: 500;
          font-size: 14px;
        }
      }

      .ant-input, .ant-input-textarea {
        border-radius: 8px;
        border: 1px solid #f0f0f0;
        background: #fafafa;
        padding: 8px 12px;
        font-size: 14px;
        transition: all 0.2s ease;

        &:focus {
          border-color: #1a1a1a;
          background: #fff;
          box-shadow: 0 0 0 2px rgba(26, 26, 26, 0.1);
        }

        &::placeholder {
          color: #bbb;
        }
      }

      .ant-input-textarea {
        min-height: 100px;
        resize: vertical;
      }

      .ant-select {
        .ant-select-selector {
          border-radius: 8px !important;
          border: 1px solid #f0f0f0 !important;
          background: #fafafa !important;
          min-height: 40px;
          box-shadow: none !important;
          padding: 4px 12px !important;
          transition: all 0.2s ease;
        }

        &.ant-select-focused .ant-select-selector,
        &:hover .ant-select-selector {
          border-color: #1a1a1a !important;
          background: #fff !important;
        }
      }
    }
  }
}

.ai-profile-form-note {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  color: #666;
  font-size: 13px;
  line-height: 1.7;
}

/* 响应式设计 */
@media (max-width: 768px) {
  #userCenterPage {
    padding: 16px;
  }

  .page-header {
    h1 {
      font-size: 20px;
    }

    p {
      font-size: 13px;
    }
  }

  .info-card, .security-card, .ai-profile-card {
    padding: 16px;
  }

  .ai-profile-card {
    .card-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .ai-profile-tip {
      flex-direction: column;
    }
  }

  .security-card .logout-section .logout-button {
    width: 100%;
  }
}
</style>
