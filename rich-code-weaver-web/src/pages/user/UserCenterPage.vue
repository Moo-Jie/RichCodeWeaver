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
          <a-button type="primary" class="gradient-button" @click="showEditModal">
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
              name="avatar"
              :show-upload-list="false"
              class="avatar-uploader"
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
              <h3>手机绑定</h3>
              <p>{{ userInfo.phone ? `已绑定手机: ${maskPhone(userInfo.phone)}` : '未绑定手机号'
                }}</p>
            </div>
            <a-button class="gradient-button" @click="handleMobile" type="primary">
              {{ userInfo.phone ? '更换手机' : '绑定手机' }}
            </a-button>
          </div>

          <div class="setting-item">
            <div>
              <h3>邮箱验证</h3>
              <p>{{ userInfo.email ? `已绑定邮箱: ${maskEmail(userInfo.email)}` : '未绑定邮箱'
                }}</p>
            </div>
            <a-button class="gradient-button" @click="handleEmail" type="primary">
              {{ userInfo.email ? '更换邮箱' : '绑定邮箱' }}
            </a-button>
          </div>
        </div>

        <div class="logout-section">
          <a-button danger class="logout-button" @click="handleLogout">
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
      title="编辑个人信息"
      @ok="handleEditSubmit"
      :ok-text="'提交'"
      :cancel-text="'取消'"
      :keyboard="false"
      :mask-closable="false"
      class="custom-modal"
    >
      <br>
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户昵称">
          <a-input v-model:value="editForm.userName" placeholder="请输入新昵称" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            placeholder="介绍一下你自己..."
            :rows="4"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码模态框 -->
    <a-modal
      v-model:visible="passwordVisible"
      title="修改登录密码"
      @ok="handlePasswordSubmit"
      class="custom-modal"
    >
      <br>
      <a-form :model="passwordForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
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
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  UserOutlined,
  LockOutlined,
  EditOutlined,
  CalendarOutlined,
  UploadOutlined,
  MailOutlined,
  PhoneOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { getLoginUser, updateUser, updateUserPassword, userLogout } from '@/api/userController'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户信息
const userInfo = reactive({
  userAccount: '',
  userName: '未命名',
  userRole: 'user',
  userAvatar: '',
  userProfile: '',
  email: '',
  phone: '',
  createTime: new Date().toISOString()
})

// 模态框状态
const editVisible = ref(false)
const passwordVisible = ref(false)

// 编辑表单
const editForm = reactive({
  userName: '',
  userProfile: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser(loginUserStore.loginUser.id)
    if (res.data.code === 0 && res.data.data) {
      Object.assign(userInfo, res.data.data)
      Object.assign(editForm, {
        userName: res.data.data.userName,
        userProfile: res.data.data.userProfile
      })
    } else {
      message.error('获取用户信息失败')
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
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新用户信息失败：', error)
    message.error('更新失败')
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
    message.error('修改失败')
  }
}

// TODO 处理手机操作
const handleMobile = () => {
  message.info('手机绑定功能即将上线')
}

// TODO 处理邮箱操作
const handleEmail = () => {
  message.info('邮箱绑定功能即将上线')
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

// 手机号脱敏处理
const maskPhone = (phone: string) => {
  if (!phone || phone.length < 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
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

<style scoped lang="less">
@import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@300;400;700&family=Nunito:wght@300;400;600;700&display=swap');

#userCenterPage {
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  padding: 24px;
  min-height: calc(100vh - 48px);
  position: relative;
  overflow: hidden;
  color: #333333;
}

/* 美团风格背景 */
#userCenterPage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  pointer-events: none;
  z-index: 0;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 10px 0;
  position: relative;
  z-index: 2;

  h1 {
    font-family: 'Comic Neue', cursive;
    font-size: 2.8rem;
    font-weight: 700;
    color: #2c3e50;
    margin-bottom: 12px;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  }

  p {
    font-size: 1.3rem;
    color: #7f8c8d;
    max-width: 600px;
    margin: 0 auto;
    font-family: 'Comic Neue', cursive;
  }
}

.content-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
  position: relative;
  z-index: 2;

  @media (max-width: 992px) {
    grid-template-columns: 1fr;
  }
}

.info-card, .security-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
    transform: translateY(-3px);
  }

  h2 {
    font-family: 'Comic Neue', cursive;
    font-size: 1.6rem;
    color: #2c3e50;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 2px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 10px;

    .anticon {
      color: #1890ff;
    }
  }
}

.info-card {
  padding: 25px;

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
      border: 4px solid #e6f7ff;
      background: #f9f9f9;
      box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);

      .ant-avatar-icon {
        color: #1890ff;
      }
    }

    .upload-btn {
      border-radius: 12px;
      font-size: 0.9rem;
      padding: 0 15px;
      height: 36px;
      display: flex;
      align-items: center;
      gap: 8px;
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
        color: #7f8c8d;
        font-weight: 600;
        text-align: right;
        padding-right: 15px;
        font-family: 'Nunito', sans-serif;
      }

      span {
        flex: 1;
        color: #2c3e50;
        font-family: 'Nunito', sans-serif;

        .anticon {
          margin-right: 8px;
          color: #1890ff;
        }
      }
    }
  }
}

.security-card {
  padding: 25px;

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
        font-size: 1.1rem;
        color: #2c3e50;
        font-weight: 600;
        font-family: 'Nunito', sans-serif;
      }

      p {
        margin: 6px 0 0;
        font-size: 0.95rem;
        color: #7f8c8d;
        font-family: 'Nunito', sans-serif;
      }

      button {
        min-width: 120px;
        border-radius: 12px;
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
      border-radius: 12px;
      height: 42px;
      font-weight: 500;
      font-family: 'Nunito', sans-serif;

      .anticon {
        margin-right: 8px;
      }
    }
  }
}

.gradient-button {
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  color: white;
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(116, 235, 213, 0.4);
  }
}

// 模态框样式优化
:deep(.custom-modal) {
  .ant-modal {
    border-radius: 20px;
    overflow: hidden;
  }

  .ant-modal-content {
    background: #ffffff;
    border-radius: 20px;
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
  }

  .ant-modal-header {
    background: linear-gradient(135deg, #f8f9fa 0%, #e6f7ff 100%);
    border-bottom: 1px solid #f0f0f0;
    padding: 22px 24px;
    border-radius: 20px 20px 0 0;

    .ant-modal-title {
      color: #2c3e50;
      font-size: 1.4rem;
      font-weight: 600;
      text-align: center;
      font-family: 'Comic Neue', cursive;
    }
  }

  .ant-modal-body {
    padding: 28px 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid #f0f0f0;
    padding: 18px 24px;
    text-align: center;

    .ant-btn {
      border-radius: 12px;
      padding: 0 22px;
      height: 38px;
      font-weight: 500;
      transition: all 0.2s ease;
      font-family: 'Nunito', sans-serif;

      &.ant-btn-default {
        border-color: #d9d9d9;
        color: #2c3e50;

        &:hover {
          border-color: #00c4ff;
          color: #00c4ff;
        }
      }

      &.ant-btn-primary {
        background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
        border: none;
        color: white;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(116, 235, 213, 0.3);
        }
      }
    }
  }

  .ant-form {
    .ant-form-item {
      margin-bottom: 22px;

      .ant-form-item-label {
        label {
          color: #2c3e50;
          font-weight: 600;
          font-size: 0.95rem;
          font-family: 'Nunito', sans-serif;
        }
      }

      .ant-input, .ant-input-textarea {
        border-radius: 12px;
        border: 1px solid #d9d9d9;
        padding: 10px 14px;
        font-size: 0.95rem;
        transition: all 0.2s ease;
        font-family: 'Nunito', sans-serif;

        &:focus {
          border-color: #00c4ff;
          box-shadow: 0 0 0 2px rgba(116, 235, 213, 0.2);
        }

        &::placeholder {
          color: #a8a8a8;
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
  .page-header {
    h1 {
      font-size: 2.2rem;
    }

    p {
      font-size: 1.1rem;
    }
  }

  .info-card, .security-card {
    padding: 20px;
  }

  .security-card .logout-section .logout-button {
    width: 100%;
  }
}
</style>
