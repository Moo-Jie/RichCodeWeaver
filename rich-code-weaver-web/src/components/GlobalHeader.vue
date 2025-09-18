<template>
  <a-layout-header class="header">
    <a-row :gutter="[8, 8]" :wrap="true">
      <!-- 左侧：Logo和标题 -->
      <a-col :lg="4" :md="6" :sm="10" :xl="4" :xs="8">
        <RouterLink to="/">
          <div class="header-left">
            <img alt="Logo" class="logo" src="@/assets/logo.png" />
            <h1 class="site-title">织码睿奇</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto" style="justify-content: center;">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          :items="menuItems"
          mode="horizontal"
          style="font-size: 16px;"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="goToProfile">
                    <UserOutlined />
                    个人中心
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-dropdown>
              <a-space>
                <UserOutlined />
                <span>登录</span>
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="goToLogin">
                    <UserOutlined />
                    登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  AuditOutlined,
  BorderlessTableOutlined,
  HomeOutlined,
  LogoutOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 个人中心跳转
const goToProfile = () => {
  router.push('/user/userCenter')
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/user/login')
}

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页'
  },
  {
    key: '/admin',
    icon: () => h(AuditOutlined),
    label: '管理',
    title: '管理',
    children: [
      {
        key: '/admin/userManage',
        icon: () => h(BorderlessTableOutlined),
        label: '用户管理',
        title: '用户管理'
      },
      {
        key: '/admin/appManage',
        icon: () => h(BorderlessTableOutlined),
        label: '应用管理',
        title: '应用管理'
      },
      {
        key: '/admin/chatHistory',
        icon: () => h(BorderlessTableOutlined),
        label: '对话历史管理',
        title: '对话历史管理'
      }
    ]
  },
  {
    key: '/other',
    icon: () => h(AuditOutlined),
    label: '其他',
    title: '其他',
    children: [
      {
        key: '/other/about',
        icon: () => h(BorderlessTableOutlined),
        label: '关于',
        title: '关于'
      },
      {
        key: '/other/docs',
        icon: () => h(BorderlessTableOutlined),
        label: '文档',
        title: '文档'
      },
      {
        key: '/other/privacy',
        icon: () => h(BorderlessTableOutlined),
        label: '隐私政策',
        title: '隐私政策'
      },
      {
        key: '/other/terms',
        icon: () => h(BorderlessTableOutlined),
        label: '服务条款',
        title: '服务条款'
      }
    ]
  }
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
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
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Nunito:wght@300;400;600;700&family=Comic+Neue:wght@700&display=swap');

/* 全局圆角增强 */
.header {
  background: transparent;
  padding: 0 30px;
  height: 80px; /* 高度容纳新布局 */
  box-shadow: 0 4px 20px rgba(140, 150, 160, 0.12);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(220, 220, 230, 0.3); /* 轻微边框 */
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  width: 80%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(200, 200, 210, 0.5), transparent);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
  height: 100%;
  transition: transform 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}

.header-left:hover {
  transform: translateX(5px);
}

.logo {
  height: 46px;
  width: 46px;
  border-radius: 16px !important; /* 增强圆角效果 */
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  box-shadow: 0 4px 10px rgba(150, 160, 180, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.5); /* 可爱边框 */
}

.logo:hover {
  transform: translateY(-5px) rotate(8deg);
  box-shadow: 0 8px 15px rgba(150, 160, 180, 0.3);
}

/* 可爱字体风格 */
.site-title {
  margin: 0;
  font-size: 2rem; /* 增大字体 */
  font-weight: 700;
  font-family: 'Comic Neue', 'YouYuan', 'Microsoft YaHei UI', cursive, sans-serif; /* 可爱字体组合 */
  color: #6a7b8d;
  letter-spacing: 1px;
  text-shadow: 2px 2px 3px rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  border-radius: 12px; /* 文字容器圆角 */
  padding: 0 12px;
}

.site-title:hover {
  color: #8d9eb0;
  text-shadow: 3px 3px 5px rgba(180, 190, 200, 0.2);
  background: rgba(255, 255, 255, 0.3); /* 悬停背景效果 */
}

.ant-menu-horizontal {
  border-bottom: none !important;
  height: 100%;
  padding: 0 20px;
  background: transparent !important;
}

.ant-menu-item {
  font-size: 18px; /* 增大菜单字体 */
  font-family: 'Comic Neue', 'YouYuan', 'Microsoft YaHei UI', cursive, sans-serif; /* 统一可爱字体 */
  font-weight: 700;
  color: #7a8b9c;
  border-radius: 25px !important; /* 增强菜单项圆角 */
  height: 42px; /* 高度 */
  margin: 0 10px; /* 增大间距 */
  padding: 0 22px;
  line-height: 42px;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  border: 1px solid transparent; /* 为悬停效果准备 */
}

.ant-menu-item:hover {
  color: #5c6c7d;
  transform: translateY(-3px); /* 增强悬停效果 */
  border-color: rgba(200, 200, 210, 0.4); /* 悬停边框 */
}

.ant-menu-item:active {
  transform: translateY(1px);
}

.ant-menu-item-selected {
  color: #fff;
  background: linear-gradient(45deg, #a3b1c6, #c3cfda);
  box-shadow: 0 6px 14px rgba(150, 160, 180, 0.35); /* 增强选中阴影 */
  border-radius: 25px !important; /* 圆角一致 */
}

.user-login-status {
  display: flex;
  align-items: flex-end;
  height: 100%;
  padding-left: 15px;
  padding-bottom: 6px;
}

.ant-space {
  transition: all 0.3s ease;
  border-radius: 18px; /* 头像区域圆角 */
  padding: 4px 8px;
}

.ant-space:hover {
  transform: scale(1.05);
  background: rgba(240, 240, 245, 0.6); /* 悬停背景 */
}

.ant-avatar {
  box-shadow: 0 4px 10px rgba(140, 150, 160, 0.25);
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  border-radius: 16px !important; /* 增强头像圆角 */
}

.ant-avatar:hover {
  transform: rotate(10deg);
  box-shadow: 0 6px 15px rgba(140, 150, 160, 0.4);
}

/* 响应式调整 */
@media (max-width: 992px) {
  .header {
    height: 70px;
    padding: 0 22px;
    border-radius: 0 0 30px 30px !important;
  }

  .site-title {
    font-size: 1.7rem;
  }

  .logo {
    height: 40px;
    width: 40px;
  }

  .ant-menu-item {
    font-size: 16px;
    padding: 0 14px;
    height: 36px;
    line-height: 36px;
    margin: 0 6px;
  }
}

@media (max-width: 768px) {
  .site-title {
    display: none;
  }

  .header-left {
    gap: 8px;
  }

  .header {
    padding: 0 12px;
    border-radius: 0 0 25px 25px !important;
    height: 64px;
  }

  .user-login-status {
    padding-bottom: 4px;
  }
}
</style>
