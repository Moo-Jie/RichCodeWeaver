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
              :custom-request="({ file }) => handleAvatarUpload(file as File)"
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
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  CalendarOutlined,
  EditOutlined,
  LockOutlined,
  LogoutOutlined,
  UploadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import {
  bindEmail,
  getLoginUser,
  updateUser,
  updateUserAvatar,
  updateUserPassword,
  userLogout
} from '@/api/userController'
import { getMathCaptcha, sendEmailCode } from '@/api/userController'
import { identityLabelMap, identityOptions, industryOptions } from '@/constants/identityOptions'
import { validateEmail } from '@/utils/emailUtil'

const router = useRouter()
const loginUserStore = useLoginUserStore()

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
const emailCaptchaId = ref('')
const emailCaptchaImage = ref('')
const emailCountdown = ref(0)
const emailSendingCode = ref(false)
let emailCountdownTimer: ReturnType<typeof setInterval> | null = null

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

const industryAutoOptions = industryOptions.map(item => ({
  value: item
}))

const filterIndustry = (input: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

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

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser(loginUserStore.loginUser.id)
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

// 显示编辑模态框
const showEditModal = () => {
  editVisible.value = true
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

.info-card, .security-card {
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
    }
  }
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

  .info-card, .security-card {
    padding: 16px;
  }

  .security-card .logout-section .logout-button {
    width: 100%;
  }
}
</style>
