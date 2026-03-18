<template>
  <aside :class="['sidebar', { collapsed: appStore.sidebarCollapsed }]">
    <!-- Logo -->
    <div class="sidebar-logo" @click="handleLogoClick">
      <img alt="Logo" class="logo-img" src="@/assets/logo.png" />
      <transition name="fade-text">
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">织码睿奇</span>
      </transition>
    </div>

    <!-- New Chat -->
    <div class="sidebar-action">
      <button class="new-chat-btn" @click="handleNewChat">
        <PlusOutlined />
        <transition name="fade-text">
          <span v-show="!appStore.sidebarCollapsed">新建对话</span>
        </transition>
      </button>
    </div>

    <!-- Navigation -->
    <nav class="sidebar-nav">
      <div
        v-for="item in baseNavItems"
        :key="item.path"
        :class="['nav-item', { active: isActive(item.path) }]"
        @click="navigateTo(item.path)"
      >
        <component :is="item.icon" class="nav-icon" />
        <transition name="fade-text">
          <span v-show="!appStore.sidebarCollapsed" class="nav-label">{{ item.label }}</span>
        </transition>
      </div>

      <!-- Admin Menu -->
      <template v-if="isAdmin">
        <div
          :class="['nav-item', 'nav-item-expandable', { active: route.path.startsWith('/admin') }]"
          @click="toggleAdminMenu"
        >
          <SettingOutlined class="nav-icon" />
          <transition name="fade-text">
            <span v-show="!appStore.sidebarCollapsed" class="nav-label">系统管理</span>
          </transition>
          <transition name="fade-text">
            <DownOutlined v-show="!appStore.sidebarCollapsed" :class="['nav-arrow', { expanded: adminMenuExpanded }]" />
          </transition>
        </div>
        <transition name="submenu-slide">
          <div v-show="adminMenuExpanded && !appStore.sidebarCollapsed" class="nav-submenu">
            <div
              v-for="subItem in adminNavItems"
              :key="subItem.path"
              :class="['nav-subitem', { active: isActive(subItem.path) }]"
              @click.stop="navigateTo(subItem.path)"
            >
              <component :is="subItem.icon" class="nav-icon" />
              <span class="nav-label">{{ subItem.label }}</span>
            </div>
          </div>
        </transition>
      </template>
    </nav>

    <!-- Other Menu -->
    <div
      v-for="item in otherNavItems"
      :key="item.path"
      :class="['nav-item', { active: isActive(item.path) }]"
      @click="navigateTo(item.path)"
    >
      <component :is="item.icon" class="nav-icon" />
      <transition name="fade-text">
        <span v-show="!appStore.sidebarCollapsed" class="nav-label">{{ item.label }}</span>
      </transition>
    </div>

    <!-- Divider -->
    <div class="sidebar-divider"></div>

    <!-- App List -->
    <div class="sidebar-apps">
      <transition name="fade-text">
        <div v-show="!appStore.sidebarCollapsed" class="apps-header">我的应用</div>
      </transition>
      <SidebarAppList />
    </div>

    <!-- User Profile -->
    <div class="sidebar-user">
      <a-dropdown :trigger="['click']" placement="topRight">
        <div class="user-trigger">
          <a-avatar
            :size="appStore.sidebarCollapsed ? 30 : 34"
            :src="loginUserStore.loginUser.userAvatar"
          >
            {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
          </a-avatar>
          <transition name="fade-text">
            <span v-show="!appStore.sidebarCollapsed" class="user-name">
              {{ loginUserStore.loginUser.userName || '未登录' }}
            </span>
          </transition>
        </div>
        <template #overlay>
          <a-menu>
            <a-menu-item v-if="loginUserStore.loginUser.id" @click="navigateTo('/user/userCenter')">
              <UserOutlined />
              <span style="margin-left: 8px;">个人中心</span>
            </a-menu-item>
            <a-menu-item v-if="loginUserStore.loginUser.id" @click="doLogout">
              <LogoutOutlined />
              <span style="margin-left: 8px;">退出登录</span>
            </a-menu-item>
            <a-menu-item v-if="!loginUserStore.loginUser.id" @click="navigateTo('/user/login')">
              <LoginOutlined />
              <span style="margin-left: 8px;">登录</span>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>

    <!-- Collapse Toggle -->
    <button class="collapse-toggle" @click="appStore.toggleSidebar">
      <MenuFoldOutlined v-if="!appStore.sidebarCollapsed" />
      <MenuUnfoldOutlined v-else />
    </button>
  </aside>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAppStore } from '@/stores/appStore'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import SidebarAppList from './SidebarAppList.vue'
import {
  AppstoreOutlined,
  AuditOutlined,
  DownOutlined,
  FileTextOutlined,
  GlobalOutlined,
  HomeOutlined,
  LoginOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlusOutlined,
  SettingOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const appStore = useAppStore()
const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()
const adminMenuExpanded = ref(false)

const baseNavItems = [
  {path: '/', label: '主页', icon: HomeOutlined},
  {path: '/my/apps', label: '我的应用', icon: AppstoreOutlined},
  {path: '/all/apps', label: '全部应用', icon: GlobalOutlined},
]

const otherNavItems = [
  {path: '/other/about', label: '关于', icon: AuditOutlined}
]

const adminNavItems = [
  {path: '/admin/userManage', label: '用户管理', icon: UserOutlined},
  {path: '/admin/appManage', label: '应用管理', icon: AppstoreOutlined},
  {path: '/admin/chatHistory', label: '对话历史', icon: FileTextOutlined}
]

const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

const toggleAdminMenu = () => {
  adminMenuExpanded.value = !adminMenuExpanded.value
}

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const navigateTo = (path: string) => {
  if (path === '/') {
    appStore.clearSelectedApp()
  }
  router.push(path)
}

const handleLogoClick = () => {
  appStore.clearSelectedApp()
  router.push('/')
}

const handleNewChat = () => {
  appStore.clearSelectedApp()
  router.push('/')
}

// Load apps when sidebar mounts and user is logged in
onMounted(() => {
  if (loginUserStore.loginUser?.id) {
    appStore.loadMyApps()
  }
})

// Reload apps when login state changes (watch entire loginUser object to catch async updates)
watch(
  () => loginUserStore.loginUser,
  (newUser) => {
    if (newUser?.id) {
      appStore.loadMyApps()
    }
  },
  { deep: true, immediate: true }
)

const doLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({userName: '未登录'})
      message.success('退出登录成功')
      appStore.clearSelectedApp()
      await router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch (e) {
    message.error('退出登录失败')
  }
}
</script>

<style scoped>
.sidebar {
  width: 240px;
  height: 100vh;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  z-index: 50;
}

.sidebar.collapsed {
  width: 64px;
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.sidebar-logo:hover .logo-img {
  transform: scale(1.05);
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  white-space: nowrap;
  letter-spacing: -0.3px;
}

/* New Chat Button */
.sidebar-action {
  padding: 4px 12px 8px;
  flex-shrink: 0;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #fff;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  background: #fafafa;
  border-color: #d0d0d0;
}

.collapsed .new-chat-btn {
  padding: 10px;
}

/* Navigation */
.sidebar-nav {
  padding: 4px 0;
  flex-shrink: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  margin: 1px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #666;
  font-size: 14px;
}

.nav-item:hover {
  background: #f5f5f5;
  color: #333;
}

.nav-item.active {
  background: #f0f0f0;
  color: #1a1a1a;
  font-weight: 500;
}

.nav-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Divider */
.sidebar-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 8px 16px;
  flex-shrink: 0;
}

/* App List */
.sidebar-apps {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.apps-header {
  padding: 8px 20px 4px;
  font-size: 12px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

/* User Profile */
.sidebar-user {
  flex-shrink: 0;
  border-top: 1px solid #f0f0f0;
  padding: 10px 12px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-trigger:hover {
  background: #f5f5f5;
}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Collapse Toggle */
.collapse-toggle {
  position: absolute;
  top: 18px;
  right: -13px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid #e5e5e5;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  color: #999;
  transition: all 0.2s ease;
  z-index: 10;
  opacity: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.sidebar:hover .collapse-toggle {
  opacity: 1;
}

.collapse-toggle:hover {
  color: #333;
  border-color: #d0d0d0;
  background: #fafafa;
}

/* Transition */
.fade-text-enter-active,
.fade-text-leave-active {
  transition: opacity 0.15s ease;
}

.fade-text-enter-from,
.fade-text-leave-to {
  opacity: 0;
}

/* Admin Submenu */
.nav-item-expandable {
  position: relative;
}

.nav-arrow {
  position: absolute;
  right: 16px;
  font-size: 10px;
  transition: transform 0.2s ease;
}

.nav-arrow.expanded {
  transform: rotate(180deg);
}

.nav-submenu {
  overflow: hidden;
}

.nav-subitem {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px 8px 36px;
  margin: 1px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #666;
  font-size: 13px;
}

.nav-subitem:hover {
  background: #f5f5f5;
  color: #333;
}

.nav-subitem.active {
  background: #f0f0f0;
  color: #1a1a1a;
  font-weight: 500;
}

.submenu-slide-enter-active,
.submenu-slide-leave-active {
  transition: all 0.2s ease;
}

.submenu-slide-enter-from,
.submenu-slide-leave-to {
  max-height: 0;
  opacity: 0;
}

.submenu-slide-enter-to,
.submenu-slide-leave-from {
  max-height: 200px;
  opacity: 1;
}
</style>
