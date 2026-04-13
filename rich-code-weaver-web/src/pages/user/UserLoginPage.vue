<template>
  <div id="userLoginPage">
    <RouterLink class="home-link" to="/">
      <HomeOutlined />
      <span>主页</span>
    </RouterLink>
    <div class="login-layout">
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
                <span class="feature-name">无需编程经验</span>
                <span class="feature-desc">用自然语言描述想法，自动生成完整解决方案</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <div class="feature-text">
                <span class="feature-name">多样化模板</span>
                <span class="feature-desc">从数字画廊到互动平台，丰富模板供您选择</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <div class="feature-text">
                <span class="feature-name">智能代码优化</span>
                <span class="feature-desc">自动生成高效可维护代码，持续迭代改进</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区域 -->
      <div class="form-panel">
        <div class="form-card">
          <div class="form-header">
            <h1 class="form-title">登录</h1>
            <p class="form-desc">登录您的账号，开始创建数字产物</p>
          </div>
          <a-form :model="formState" autocomplete="off" name="basic" @finish="handleSubmit">
            <a-form-item :rules="[{ required: true, message: '请输入邮箱' }, { validator: validateEmailField }]" name="email">
              <a-input v-model:value="formState.email" class="input-field"
                       placeholder="请输入邮箱">
                <template #prefix>
                  <MailOutlined style="color: #bbb" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码长度不能小于 8 位' },
              ]"
              name="userPassword"
            >
              <a-input-password v-model:value="formState.userPassword" class="input-field"
                                placeholder="请输入密码">
                <template #prefix>
                  <LockOutlined style="color: #bbb" />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button :loading="submitting" class="submit-btn" html-type="submit" type="primary">
                登录
              </a-button>
            </a-form-item>

            <div class="form-footer">
              <span class="footer-text">没有账号？</span>
              <RouterLink class="footer-link" to="/user/register">立即注册</RouterLink>
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
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {HomeOutlined, LockOutlined, MailOutlined} from '@ant-design/icons-vue'
import {userLogin} from '@/api/userController'
import {useLoginUserStore} from '@/stores/loginUser'
import {message} from 'ant-design-vue'
import {validateEmail} from '@/utils/emailUtil'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const submitting = ref(false)

const formState = reactive({
  email: (router.currentRoute.value.query.email as string) || '',
  userPassword: ''
})

const validateEmailField = async (_rule: unknown, value: string) => {
  const result = validateEmail(value)
  if (!result.valid) {
    throw new Error(result.message || '邮箱格式不正确')
  }
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
    const res = await userLogin({
      email: emailResult.normalizedEmail,
      userPassword: formState.userPassword
    })

    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.replace({ path: '/' })
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } catch {
    message.error('登录过程中发生错误')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#userLoginPage {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fafafa;
  padding: 20px;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.login-layout {
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
  .login-layout {
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
