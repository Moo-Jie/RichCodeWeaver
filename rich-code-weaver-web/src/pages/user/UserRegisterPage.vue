<template>
  <div id="userRegisterPage" :style="{ backgroundImage: `url('${backgroundImage}')` }">
    <div class="glass-container">
      <div class="header">
        <h1 class="title">织码睿奇 -注册</h1>
        <p class="desc">不写一行代码，生成完整应用</p>
      </div>
      <a-form :model="formState" autocomplete="off" name="basic" @finish="handleSubmit">
        <a-form-item :rules="[{ required: true, message: '请输入账号' }]" name="userAccount">
          <a-input v-model:value="formState.userAccount" class="input-field"
                   placeholder="请输入账号">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
          name="userPassword"
        >
          <a-input-password v-model:value="formState.userPassword" class="input-field"
                            placeholder="请输入密码">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
          name="checkPassword"
        >
          <a-input-password v-model:value="formState.checkPassword" class="input-field"
                            placeholder="请确认密码">
            <template #prefix>
              <SafetyOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <div class="tips">
          已有账号？
          <RouterLink class="link" to="/user/login">去登录</RouterLink>
        </div>

        <div class="tips">
          <RouterLink class="tips" to="/other/privacy">隐私政策</RouterLink>
          &nbsp;&nbsp;<span class="tips">|</span>&nbsp;&nbsp;
          <RouterLink class="tips" to="/other/terms">服务条款</RouterLink>
        </div>

        <a-form-item>
          <a-button :loading="submitting" class="submit-btn" html-type="submit" type="primary">
            注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const backgroundImage = 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'400\' height=\'400\' viewBox=\'0 0 100 100\'%3E%3Cpath fill=\'none\' stroke=\'rgba(204,230,255,0.1)\' stroke-width=\'1\' d=\'M80,80 Q20,60 20,20 M50,100 Q50,0 100,50\'/%3E%3C/svg%3E'
const submitting = ref(false)

const formState = reactive({
  userAccount: '',
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

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await userRegister({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword,
      checkPassword: formState.checkPassword
    })

    if (res.data.code === 0) {
      message.success('注册成功')
      router.replace({
        path: '/user/login',
        query: { username: formState.userAccount }
      })
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch (error) {
    message.error('注册过程中发生错误')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@300;400;700&family=Nunito:wght@300;400;600;700&display=swap');

#userRegisterPage {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, rgb(255, 248, 206) 0%, rgb(147, 203, 255) 100%);
  background-image: v-bind('backgroundImage'),
  radial-gradient(circle at 5% 90%, rgba(255, 230, 204, 0.3) 0%, transparent 25%),
  radial-gradient(circle at 95% 10%, rgba(204, 230, 255, 0.3) 0%, transparent 25%);
  background-size: 300px, auto;
  background-repeat: repeat;
  padding: 20px;
  position: relative;
  overflow: hidden;
  font-family: 'Nunito', 'Comic Neue', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

#userRegisterPage::before {
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

.glass-container {
  width: 100%;
  max-width: 520px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  z-index: 2;
  transition: all 0.3s ease;
}

.glass-container:hover {
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-family: 'Comic Neue', cursive;
  font-size: 2.8rem;
  font-weight: 700;
  margin: 0 0 12px;
  color: #2c3e50;
  text-shadow: 2px 2px 0px rgba(200, 180, 170, 0.2);
  letter-spacing: -0.5px;
}

.desc {
  font-family: 'Nunito', sans-serif;
  font-size: 1.1rem;
  margin: 0;
  color: #7f8c8d;
}

.input-field {
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 16px;
  border: 1px solid #e8e8e8;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.input-field:hover, .input-field:focus-within {
  border-color: #00c4ff;
  background: white;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.tips {
  text-align: right;
  margin: -10px 0 20px;
  font-family: 'Nunito', sans-serif;
  font-size: 14px;
  color: #7f8c8d;
}

.link {
  color: #00c4ff;
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
  background: #00c4ff;
  transition: width 0.3s ease;
}

.link:hover {
  color: #5adbc8;
}

.link:hover::after {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-family: 'Nunito', sans-serif;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #00c4ff 0%, #9face6 100%);
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 8px 16px rgba(116, 235, 213, 0.25);
  color: white;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #5adbc8 0%, #8b9de6 100%);
  transform: translateY(-3px);
  box-shadow: 0 12px 20px rgba(116, 235, 213, 0.35);
}

.submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 8px rgba(116, 235, 213, 0.3);
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
