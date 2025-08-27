<template>
  <div id="userLoginPage" :style="{ backgroundImage: `url('${backgroundImage}')` }">
    <div class="login-layout">
      <!-- 左侧特色展示区域 -->
      <div class="feature-panel">
        <div class="feature-content">
          <img src="@/assets/logo.png" alt="织码睿奇" class="logo">
          <h2 class="feature-title">赋能创意，无限可能</h2>

          <div class="feature-item">
            <div class="feature-icon">
              <svg width="32" height="32" viewBox="0 0 24 24">
                <path fill="currentColor" d="M7,2H17V13H7V2M7,15V21H17V15H7M4,11V13H2V11H4M22,11V13H20V11H22M5,4V6H6V4H5M5,7V9H6V7H5M5,10V12H6V10H5M18,4V6H19V4H18M18,7V9H19V7H18M18,10V12H19V10H18Z"/>
              </svg>
            </div>
            <div>
              <h3>无需编程经验</h3>
              <p>用自然语言描述您的应用想法，我们将自动生成完整解决方案</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon">
              <svg width="32" height="32" viewBox="0 0 24 24">
                <path fill="currentColor" d="M21,16V14H13V16H21M21,11V9H13V11H21M21,6V4H13V6H21M8.5,12A2.5,2.5 0 0,0 6,9.5A2.5,2.5 0 0,0 3.5,12A2.5,2.5 0 0,0 6,14.5A2.5,2.5 0 0,0 8.5,12M11,20V19.5A5.5,5.5 0 0,0 5.5,14A5.5,5.5 0 0,0 0,19.5V20H11Z"/>
              </svg>
            </div>
            <div>
              <h3>多样化应用模板</h3>
              <p>从数字艺术画廊到互动小说平台，丰富的创意模板供您选择</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon">
              <svg width="32" height="32" viewBox="0 0 24 24">
                <path fill="currentColor" d="M17,18V19H15V18H17M13,19V18H7V19H13M18,13V15H19V17H17V15H13V13H17V11H19V13H18M6,11V13H11V15H6V17H11V19H6V21H4V19H5V15H4V13H5V11H4V9H6V11M7,11V13H8V11H7M7,4V6H17V4H7M7,8V10H15V8H7Z"/>
              </svg>
            </div>
            <div>
              <h3>智能代码优化</h3>
              <p>自动生成高效、可维护的代码，持续迭代改进</p>
            </div>
          </div>

          <div class="feature-slider">
            <div class="slider-track"></div>
            <div class="slider-progress"></div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区域 -->
      <div class="glass-container">
        <div class="header">
          <h1 class="title">织码睿奇 -登录</h1>
          <p class="desc">不写一行代码，生成完整应用</p>
        </div>
        <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" class="input-field">
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" class="input-field">
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="tips">
            没有账号
            <RouterLink to="/user/register" class="link">去注册</RouterLink>
          </div>

          <div class="tips">
            <RouterLink to="/other/privacy" class="tips">隐私政策</RouterLink>
            &nbsp;&nbsp; <span class="tips">|</span>&nbsp;&nbsp;
            <RouterLink to="/other/terms" class="tips">服务条款</RouterLink>
          </div>

          <a-form-item>
            <a-button type="default" html-type="submit" class="submit-btn" :loading="submitting">
              登录
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const backgroundImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='400' viewBox='0 0 100 100'%3E%3Cpath fill='none' stroke='rgba(204,230,255,0.1)' stroke-width='1' d='M80,80 Q20,60 20,20 M50,100 Q50,0 100,50'/%3E%3C/svg%3E"
const submitting = ref(false)

const formState = reactive({
  userAccount: router.currentRoute.value.query.username || '',
  userPassword: '',
})

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await userLogin({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword
    })

    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.replace({ path: '/' })
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } catch (error) {
    message.error('登录过程中发生错误')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Source+Sans+Pro:wght@300;400;600&display=swap');

#userLoginPage {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(45deg, #fcf9f2 0%, #f0f6ff 100%);
  background-image: v-bind('backgroundImage'),
  radial-gradient(circle at 5% 90%, rgba(255, 230, 204, 0.3) 0%, transparent 25%),
  radial-gradient(circle at 95% 10%, rgba(204, 230, 255, 0.3) 0%, transparent 25%);
  background-size: 300px, auto;
  background-repeat: repeat;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

#userLoginPage::before {
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
}

.login-layout {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 30px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-panel {
  display: flex;
  align-items: center;
}

.logo {
  width: 80px;
  height: 80px;
  display: block;
  margin: 0 auto 20px;
}

.feature-content {
  width: 100%;
  padding: 40px;
  background: rgba(255, 251, 245, 0.75);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 12px 24px rgba(155, 140, 125, 0.1),
  0 6px 16px rgba(155, 140, 125, 0.05),
  inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.feature-content:hover {
  box-shadow: 0 16px 32px rgba(155, 140, 125, 0.15),
  inset 0 0 0 1px rgba(255, 255, 255, 0.8);
}

.feature-title {
  font-family: 'Playfair Display', serif;
  font-size: 2.5rem;
  font-weight: 700;
  color: #5c4a48;
  text-align: center;
  margin-bottom: 40px;
  position: relative;
}

.feature-title::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 35%;
  width: 30%;
  height: 2px;
  background: linear-gradient(to right, transparent, #c6a08a, transparent);
}

.feature-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 30px;
  padding: 15px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 16px rgba(155, 140, 125, 0.1);
}

.feature-icon {
  margin-right: 20px;
  color: #c6a08a;
}

.feature-item h3 {
  font-family: 'Source Sans Pro', sans-serif;
  font-weight: 600;
  font-size: 1.4rem;
  color: #5c4a48;
  margin: 0 0 8px 0;
}

.feature-item p {
  font-family: 'Source Sans Pro', sans-serif;
  color: #6d6b80;
  line-height: 1.5;
  margin: 0;
}

.feature-slider {
  margin-top: 30px;
  position: relative;
  height: 4px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.slider-track {
  position: absolute;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.15);
  width: 100%;
}

.slider-progress {
  position: absolute;
  top: 0;
  bottom: 0;
  background: #c6a08a;
  width: 40%;
  animation: slider-animation 3s infinite ease-in-out;
}

@keyframes slider-animation {
  0% { left: 0%; width: 40% }
  50% { left: 60%; width: 10% }
  100% { left: 0%; width: 40% }
}

.glass-container {
  width: 100%;
  max-width: 520px;
  background: rgba(255, 251, 245, 0.82);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 12px 24px rgba(155, 140, 125, 0.1),
  0 6px 16px rgba(155, 140, 125, 0.05),
  inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  z-index: 2;
  transition: all 0.3s ease;
}

.glass-container:hover {
  box-shadow: 0 16px 32px rgba(155, 140, 125, 0.15),
  inset 0 0 0 1px rgba(255, 255, 255, 0.8);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-family: 'Playfair Display', serif;
  font-size: 2.8rem;
  font-weight: 700;
  margin: 0 0 12px;
  color: #5c4a48;
  text-shadow: 2px 2px 0px rgba(200, 180, 170, 0.2);
  letter-spacing: -0.5px;
}

.desc {
  font-family: 'Source Sans Pro', sans-serif;
  font-size: 1.1rem;
  margin: 0;
  color: #6d6b80;
}

.input-field {
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 16px;
  border: 1px solid #e4d7c1;
  background: rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.input-field:hover, .input-field:focus-within {
  border-color: #c6a08a;
  background: white;
  box-shadow: 0 5px 15px rgba(155, 140, 125, 0.1);
}

.tips {
  text-align: right;
  margin: -10px 0 20px;
  font-family: 'Source Sans Pro', sans-serif;
  font-size: 14px;
  color: #8a7f6f;
}

.link {
  color: #c6a08a;
  font-weight: 600;
  transition: all 0.2s ease;
  position: relative;
  text-decoration: none;
}

.link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1px;
  background: #c6a08a;
  transition: width 0.3s ease;
}

.link:hover {
  color: #b38e77;
}

.link:hover::after {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-family: 'Source Sans Pro', sans-serif;
  font-size: 16px;
  font-weight: 600;
  background: #c6a08a;
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 8px 16px rgba(155, 110, 90, 0.25);
}

.submit-btn:hover {
  background: #b38e77;
  transform: translateY(-3px);
  box-shadow: 0 12px 20px rgba(155, 110, 90, 0.35);
}

.submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 8px rgba(155, 110, 90, 0.3);
}

@media (max-width: 992px) {
  .login-layout {
    grid-template-columns: 1fr;
    max-width: 600px;
  }

  .feature-panel {
    display: none;
  }
}

@media (max-width: 768px) {
  .glass-container {
    padding: 30px 20px;
  }

  .title {
    font-size: 2.4rem;
  }

  .input-field {
    padding: 10px 14px;
    font-size: 15px;
  }

  .submit-btn {
    height: 44px;
    font-size: 15px;
  }
}
</style>
