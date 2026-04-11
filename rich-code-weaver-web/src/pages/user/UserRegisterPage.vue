<template>
  <div id="userRegisterPage">
    <RouterLink class="home-link" to="/">
      <HomeOutlined />
      <span>主页</span>
    </RouterLink>
    <div class="register-layout">
      <!-- 左侧品牌展示区域 -->
      <div class="brand-panel">
        <div class="brand-content">
          <img alt="织码睿奇" class="brand-logo" src="@/assets/logo.png" />
          <h2 class="brand-title">RichCodeWeaver</h2>
          <p class="brand-slogan">工作大幅提效，成果触手可及</p>

          <div class="feature-list">
            <div class="feature-item">
              <span class="feature-dot"></span>
              <div class="feature-text">
                <span class="feature-name">快速上手</span>
                <span class="feature-desc">注册即可免费使用，几分钟创建您的第一个数字产物</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <div class="feature-text">
                <span class="feature-name">一键部署</span>
                <span class="feature-desc">生成的产物可直接部署上线，即时分享给他人</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <div class="feature-text">
                <span class="feature-name">持续迭代</span>
                <span class="feature-desc">随时修改优化，通过对话式交互不断完善产物</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单区域 -->
      <div class="form-panel">
        <div class="form-card">
          <div class="form-header">
            <h1 class="form-title">注册</h1>
            <p class="form-desc">创建账号，开启您的数字产物之旅</p>
          </div>
          <a-form :model="formState" autocomplete="off" name="basic" @finish="handleSubmit">
            <!-- 邮箱 -->
            <a-form-item :rules="[{ required: true, message: '请输入邮箱' }, { validator: validateEmailField }]" name="email">
              <a-input v-model:value="formState.email" class="input-field" placeholder="请输入邮箱">
                <template #prefix>
                  <MailOutlined style="color: #bbb" />
                </template>
              </a-input>
            </a-form-item>

            <!-- 用户昵称 -->
            <a-form-item :rules="[{ required: true, message: '请输入昵称' }, { min: 1, max: 20, message: '昵称长度1-20位' }]" name="userName">
              <a-input v-model:value="formState.userName" class="input-field" placeholder="请输入昵称">
                <template #prefix>
                  <UserOutlined style="color: #bbb" />
                </template>
              </a-input>
            </a-form-item>

            <!-- 计算验证码 -->
            <a-form-item :rules="[{ required: true, message: '请输入计算结果' }]" name="captchaAnswer">
              <div class="captcha-row">
                <a-input v-model:value="formState.captchaAnswer" class="input-field captcha-input" placeholder="计算结果">
                  <template #prefix>
                    <SafetyOutlined style="color: #bbb" />
                  </template>
                </a-input>
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img" @click="refreshCaptcha" />
                <div v-else class="captcha-placeholder" @click="refreshCaptcha">点击获取</div>
              </div>
            </a-form-item>

            <!-- 邮箱验证码 -->
            <a-form-item :rules="[{ required: true, message: '请输入验证码' }, { len: 6, message: '验证码为6位' }]" name="emailCode">
              <div class="captcha-row">
                <a-input v-model:value="formState.emailCode" :maxlength="6" class="input-field captcha-input" placeholder="邮箱验证码">
                  <template #prefix>
                    <MailOutlined style="color: #bbb" />
                  </template>
                </a-input>
                <a-button :disabled="sendingCode || countdown > 0" :loading="sendingCode" class="send-code-btn" @click="handleSendCode">
                  {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
                </a-button>
              </div>
            </a-form-item>

            <!-- 密码 -->
            <a-form-item
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码不能小于 8 位' },
              ]"
              name="userPassword"
            >
              <a-input-password v-model:value="formState.userPassword" class="input-field" placeholder="请输入密码">
                <template #prefix>
                  <LockOutlined style="color: #bbb" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 确认密码 -->
            <a-form-item
              :rules="[
                { required: true, message: '请确认密码' },
                { min: 8, message: '密码不能小于 8 位' },
                { validator: validateCheckPassword },
              ]"
              name="checkPassword"
            >
              <a-input-password v-model:value="formState.checkPassword" class="input-field" placeholder="请确认密码">
                <template #prefix>
                  <LockOutlined style="color: #bbb" />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button :loading="submitting" class="submit-btn" html-type="submit" type="primary">
                注册
              </a-button>
            </a-form-item>

            <div class="form-footer">
              <span class="footer-text">已有账号？</span>
              <RouterLink class="footer-link" to="/user/login">立即登录</RouterLink>
            </div>

            <div class="form-legal">
              <RouterLink class="legal-link" to="/other/privacy">隐私政策</RouterLink>
              <span class="legal-divider">·</span>
              <RouterLink class="legal-link" to="/other/terms">服务条款</RouterLink>
            </div>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { HomeOutlined, LockOutlined, MailOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import { getMathCaptcha, sendEmailCode, userRegister } from '@/api/userController'
import { message } from 'ant-design-vue'
import { validateEmail } from '@/utils/emailUtil'

const router = useRouter()
const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const captchaId = ref('')
const captchaImage = ref('')
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formState = reactive({
  email: '',
  userName: '',
  captchaAnswer: '',
  emailCode: '',
  userPassword: '',
  checkPassword: ''
})

const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validateEmailField = async (_rule: unknown, value: string) => {
  const result = validateEmail(value)
  if (!result.valid) {
    throw new Error(result.message || '邮箱格式不正确')
  }
}

const refreshCaptcha = async () => {
  try {
    const res = await getMathCaptcha()
    if (res.data.code === 0 && res.data.data) {
      captchaId.value = res.data.data.captchaId || ''
      captchaImage.value = res.data.data.captchaImage || ''
    }
  } catch {
    message.error('获取验证码失败')
  }
}

const handleSendCode = async () => {
  const emailResult = validateEmail(formState.email)
  if (!emailResult.valid) {
    message.warning(emailResult.message || '邮箱格式不正确')
    return
  }
  formState.email = emailResult.normalizedEmail
  if (!formState.captchaAnswer) {
    message.warning('请先输入计算结果')
    return
  }
  if (!captchaId.value) {
    message.warning('请先获取图形验证码')
    return
  }

  sendingCode.value = true
  try {
    const res = await sendEmailCode({
      email: emailResult.normalizedEmail,
      captchaId: captchaId.value,
      captchaAnswer: formState.captchaAnswer
    })
    if (res.data.code === 0) {
      message.success('验证码已发送，请查看邮箱')
      startCountdown()
    } else {
      message.error(res.data.message || '发送失败')
      await refreshCaptcha()
    }
  } catch {
    message.error('发送验证码失败')
    await refreshCaptcha()
  } finally {
    sendingCode.value = false
  }
}

const startCountdown = () => {
  countdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 1000)
}

const handleSubmit = async () => {
  const emailResult = validateEmail(formState.email)
  if (!emailResult.valid) {
    message.warning(emailResult.message || '邮箱格式不正确')
    return
  }
  formState.email = emailResult.normalizedEmail
  submitting.value = true
  try {
    const res = await userRegister({
      email: emailResult.normalizedEmail,
      userName: formState.userName,
      emailCode: formState.emailCode,
      userPassword: formState.userPassword,
      checkPassword: formState.checkPassword
    })

    if (res.data.code === 0) {
      message.success('注册成功')
      router.replace({
        path: '/user/login',
        query: { email: emailResult.normalizedEmail }
      })
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch {
    message.error('注册过程中发生错误')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
#userRegisterPage {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fafafa;
  padding: 20px;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.register-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

/* === 左侧品牌区域 === */
.brand-panel {
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  width: 100%;
  padding: 48px 36px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-logo {
  width: 80px;
  height: 80px;
  border-radius: 14px;
  margin-bottom: 20px;
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.brand-slogan {
  font-size: 14px;
  color: #999;
  margin: 0 0 36px;
}

.feature-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.feature-item:hover {
  background: #f5f5f5;
  border-color: #e5e5e5;
}

.feature-dot {
  width: 8px;
  height: 8px;
  background: #1a1a1a;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.feature-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}

/* === 右侧表单区域 === */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 40px 36px;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.form-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.input-field {
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 15px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  transition: all 0.2s ease;
}

.input-field:hover,
.input-field:focus-within {
  border-color: #d0d0d0;
  background: #fff;
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  height: 40px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.captcha-img:hover {
  opacity: 0.8;
}

.captcha-placeholder {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-placeholder:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
}

.send-code-btn {
  height: 40px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 110px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: #1a1a1a;
  border: none;
  color: #fff;
  transition: all 0.2s ease;
}

.submit-btn:hover {
  background: #333;
}

.submit-btn:active {
  background: #111;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
}

.footer-text {
  font-size: 14px;
  color: #999;
}

.footer-link {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  text-decoration: none;
  margin-left: 4px;
  transition: color 0.15s;
}

.footer-link:hover {
  color: #666;
}

.form-legal {
  text-align: center;
  margin-top: 16px;
}

.legal-link {
  font-size: 12px;
  color: #bbb;
  text-decoration: none;
  transition: color 0.15s;
}

.legal-link:hover {
  color: #999;
}

.legal-divider {
  font-size: 12px;
  color: #ddd;
  margin: 0 8px;
}

@media (max-width: 768px) {
  .register-layout {
    grid-template-columns: 1fr;
    max-width: 440px;
  }

  .brand-panel {
    display: none;
  }

  .form-card {
    padding: 32px 24px;
  }
}

.home-link {
  position: absolute;
  top: 20px;
  left: 24px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #333;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  z-index: 10;
}

.home-link:hover {
  background: #fafafa;
  border-color: #d0d0d0;
  color: #1a1a1a;
}
</style>
