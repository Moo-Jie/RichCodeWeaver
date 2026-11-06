import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

let loginUserInitialized = false
let loginUserRequest: Promise<void> | null = null

function safeRedirectPath(fullPath: string): string {
  if (!fullPath.startsWith('/') || fullPath.startsWith('//')) return '/'
  return fullPath
}

async function ensureLoginUser() {
  if (loginUserInitialized) return
  if (!loginUserRequest) {
    const loginUserStore = useLoginUserStore()
    loginUserRequest = loginUserStore
      .fetchLoginUser()
      .catch((error) => {
        console.warn('[Auth] 登录态初始化失败，按未登录处理', error)
      })
      .finally(() => {
        loginUserInitialized = true
        loginUserRequest = null
      })
  }
  await loginUserRequest
}

router.beforeEach(async (to) => {
  await ensureLoginUser()
  const loginUserStore = useLoginUserStore()
  const loginUser = loginUserStore.loginUser
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(
    (record) => record.path === '/admin' || record.path.startsWith('/admin/'),
  )

  if (requiresAuth && !loginUser?.id) {
    return {
      path: '/user/login',
      query: { redirect: safeRedirectPath(to.fullPath) },
      replace: true,
    }
  }

  if (requiresAdmin && loginUser?.userRole !== 'admin') {
    message.error('没有权限访问管理员页面')
    return {
      path: '/user/login',
      query: { redirect: safeRedirectPath(to.fullPath) },
      replace: true,
    }
  }

  if ((to.path === '/user/login' || to.path === '/user/register') && loginUser?.id) {
    return safeRedirectPath(typeof to.query.redirect === 'string' ? to.query.redirect : '/')
  }

  return true
})

let lastRouterErrorAt = 0

router.onError((error) => {
  console.error('[Router] 路由切换失败', error)
  const now = Date.now()
  if (now - lastRouterErrorAt < 3000) return
  lastRouterErrorAt = now
  message.error('页面加载失败，请检查网络后重试')
})

export function resetAuthInitialization() {
  loginUserInitialized = false
  loginUserRequest = null
}

export { safeRedirectPath }
