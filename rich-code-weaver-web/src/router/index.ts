import { createRouter, createWebHistory } from 'vue-router'
import GlobalLayout from '@/layouts/GlobalLayout.vue'
import WorkspacePage from '@/pages/WorkspacePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import MyAppsPage from '@/pages/app/MyAppsPage.vue'
import AllAppsPage from '@/pages/app/AllAppsPage.vue'
import UserCenterPage from '@/pages/user/UserCenterPage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import DocsPage from '@/pages/other/DocsPage.vue'
import AboutPage from '@/pages/other/AboutPage.vue'
import PrivacyPage from '@/pages/other/PrivacyPage.vue'
import TermsPage from '@/pages/other/TermsPage.vue'

/**
 * 路由配置
 * - 主布局：左侧边栏 + 主内容区
 * - 登录/注册：独立全屏页面
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: GlobalLayout,
      children: [
        {
          path: '',
          name: '主页',
          component: WorkspacePage
        },
        {
          path: 'app/chat/:id',
          name: '应用对话',
          component: WorkspacePage
        },
        {
          path: 'app/edit/:id',
          name: '编辑应用',
          component: AppEditPage
        },
        {
          path: 'my/apps',
          name: '我的应用',
          component: MyAppsPage
        },
        {
          path: 'all/apps',
          name: '全部应用',
          component: AllAppsPage
        },
        {
          path: 'user/userCenter',
          name: '个人中心',
          component: UserCenterPage
        },
        {
          path: 'admin/userManage',
          name: '用户管理',
          component: UserManagePage
        },
        {
          path: 'admin/appManage',
          name: '应用管理',
          component: AppManagePage
        },
        {
          path: 'admin/chatHistory',
          name: '对话历史管理',
          component: ChatHistoryManagePage
        },
        {
          path: 'other/docs',
          name: '文档',
          component: DocsPage
        },
        {
          path: 'other/about',
          name: '关于',
          component: AboutPage
        },
        {
          path: 'other/privacy',
          name: '用户隐私协议',
          component: PrivacyPage
        },
        {
          path: 'other/terms',
          name: '服务条款',
          component: TermsPage
        }
      ]
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage
    }
  ]
})

export default router
