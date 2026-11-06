import { createRouter, createWebHistory } from 'vue-router'
const GlobalLayout = () => import('@/layouts/GlobalLayout.vue')
const WorkspacePage = () => import('@/pages/WorkspacePage.vue')
const MessageCenterPage = () => import('@/pages/message/MessageCenterPage.vue')
const UserLoginPage = () => import('@/pages/user/UserLoginPage.vue')
const UserRegisterPage = () => import('@/pages/user/UserRegisterPage.vue')
const UserManagePage = () => import('@/pages/admin/UserManagePage.vue')
const AppManagePage = () => import('@/pages/admin/AppManagePage.vue')
const SystemManageDashboard = () => import('@/pages/admin/SystemManageDashboard.vue')
const AppEditPage = () => import('@/pages/app/AppEditPage.vue')
const MyAppsPage = () => import('@/pages/app/MyAppsPage.vue')
const AllAppsPage = () => import('@/pages/app/AllAppsPage.vue')
const MyFavoritesPage = () => import('@/pages/app/MyFavoritesPage.vue')
const UserCenterPage = () => import('@/pages/user/UserCenterPage.vue')
const ChatHistoryManagePage = () => import('@/pages/admin/ChatHistoryManagePage.vue')
const PromptTemplateManagePage = () => import('@/pages/admin/PromptTemplateManagePage.vue')
const SystemPromptManagePage = () => import('@/pages/admin/SystemPromptManagePage.vue')
const RagManagePage = () => import('@/pages/admin/RagManagePage.vue')
const MaterialManagePage = () => import('@/pages/admin/MaterialManagePage.vue')
const HigressManagePage = () => import('@/pages/admin/HigressManagePage.vue')
const GrafanaMonitorPage = () => import('@/pages/admin/GrafanaMonitorPage.vue')
const MyMaterialPage = () => import('@/pages/material/MyMaterialPage.vue')
const CommunityListPage = () => import('@/pages/community/CommunityListPage.vue')
const CommunityDetailPage = () => import('@/pages/community/CommunityDetailPage.vue')
const DocsPage = () => import('@/pages/other/DocsPage.vue')
const AboutPage = () => import('@/pages/other/AboutPage.vue')
const PrivacyPage = () => import('@/pages/other/PrivacyPage.vue')
const TermsPage = () => import('@/pages/other/TermsPage.vue')
const AiGeneratedContentPage = () => import('@/pages/other/AiGeneratedContentPage.vue')

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
          component: WorkspacePage,
        },
        {
          path: 'messages',
          name: '消息中心',
          component: MessageCenterPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'app/chat/:id',
          name: '数字产物对话',
          component: WorkspacePage,
        },
        {
          path: 'app/edit/:id',
          name: '编辑数字产物',
          component: AppEditPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'my/apps',
          name: '我的产物',
          component: MyAppsPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'all/apps',
          name: '全部产物',
          component: AllAppsPage,
        },
        {
          path: 'my/favorites',
          name: '我的收藏',
          component: MyFavoritesPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'my/materials',
          name: '我的素材',
          component: MyMaterialPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'community',
          name: '交流社区',
          component: CommunityListPage,
        },
        {
          path: 'community/post/:id',
          name: '社区帖子详情',
          component: CommunityDetailPage,
        },
        {
          path: 'user/userCenter',
          name: '个人中心',
          component: UserCenterPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'admin',
          name: '系统管理',
          component: SystemManageDashboard,
        },
        {
          path: 'admin/userManage',
          name: '用户管理',
          component: UserManagePage,
        },
        {
          path: 'admin/appManage',
          name: '产物管理',
          component: AppManagePage,
        },
        {
          path: 'admin/chatHistory',
          name: '对话历史管理',
          component: ChatHistoryManagePage,
        },
        {
          path: 'admin/promptTemplate',
          name: '提示词模板管理',
          component: PromptTemplateManagePage,
        },
        {
          path: 'admin/systemPrompt',
          name: '系统提示词管理',
          component: SystemPromptManagePage,
        },
        {
          path: 'admin/ragManage',
          name: 'RAG知识库管理',
          component: RagManagePage,
        },
        {
          path: 'admin/materialManage',
          name: '素材管理',
          component: MaterialManagePage,
        },
        {
          path: 'admin/higressManage',
          name: '网关管理',
          component: HigressManagePage,
        },
        {
          path: 'admin/grafanaMonitor',
          name: '系统监控',
          component: GrafanaMonitorPage,
        },
        {
          path: 'other/docs',
          name: '文档',
          component: DocsPage,
        },
        {
          path: 'other/about',
          name: '关于',
          component: AboutPage,
        },
        {
          path: 'other/privacy',
          name: '用户隐私协议',
          component: PrivacyPage,
        },
        {
          path: 'other/ai-generated-content',
          name: 'AI生成内容声明',
          component: AiGeneratedContentPage,
        },
        {
          path: 'other/terms',
          name: '服务条款',
          component: TermsPage,
        },
      ],
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
      path: '/:pathMatch(.*)*',
      name: '未找到页面',
      redirect: '/',
    },
  ],
})

export default router
