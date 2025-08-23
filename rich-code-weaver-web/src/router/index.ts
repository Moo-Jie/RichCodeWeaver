import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import UserCenterPage from '@/pages/user/UserCenterPage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import DocsPage from '@/pages/other/DocsPage.vue'
import AboutPage from '@/pages/other/AboutPage.vue'
import PrivacyPage from '@/pages/other/PrivacyPage.vue'
import TermsPage from '@/pages/other/TermsPage.vue'

/**
 * 路由配置
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/userCenter',
      name: '个人中心',
      component: UserCenterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
    },
    {
      path: '/admin/chatHistory',
      name: '对话历史管理',
      component: ChatHistoryManagePage,
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
    },
    {
      path: '/app/edit/:id',
      name: '编辑应用',
      component: AppEditPage,
    },
    {
      path: '/other/docs',
      name: '文档',
      component: DocsPage,
    },
    {
      path: '/other/about',
      name: '关于',
      component: AboutPage,
    },
    {
      path: '/other/privacy',
      name: '用户隐私协议',
      component: PrivacyPage,
    },
    {
      path: '/other/terms',
      name: '服务条款',
      component: TermsPage,
    },
  ],
})

export default router
