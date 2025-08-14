<template>
  <div id="userRegisterPage" :style="{ backgroundImage: `url('${backgroundImage}')` }">
    <div class="glass-container">
      <div class="header">
        <h1 class="title">织码睿奇 -注册</h1>
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
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" class="input-field">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" class="input-field">
            <template #prefix>
              <SafetyOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <div class="tips">
          已有账号？
          <RouterLink to="/user/login" class="link">去登录</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" class="submit-btn" :loading="submitting">
            注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const backgroundImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='400' viewBox='0 0 100 100'%3E%3Cpath fill='none' stroke='rgba(198,160,138,0.1)' stroke-width='1' d='M20,20 Q40,5 60,20 T100,20 M20,40 Q30,30 40,40 T80,40 M10,70 Q35,55 60,70 T90,70'/%3E%3C/svg%3E"
const submitting = ref(false)

const formState = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
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
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Source+Sans+Pro:wght@300;400;600&display=swap');

#userRegisterPage {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #fcf9f2 0%, #f5eee4 100%);
  background-image: v-bind('backgroundImage'),
  radial-gradient(circle at 5% 10%, rgba(255, 230, 204, 0.3) 0%, transparent 25%),
  radial-gradient(circle at 95% 90%, rgba(204, 230, 255, 0.3) 0%, transparent 25%);
  background-size: 300px, auto;
  background-repeat: repeat;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

#userRegisterPage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"><path fill="none" stroke="rgba(198,160,138,0.05)" stroke-width="1" d="M0,0 Q200,100 400,0 M0,400 Q400,300 400,400"/></svg>');
  background-size: 400px;
  opacity: 0.3;
  pointer-events: none;
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
