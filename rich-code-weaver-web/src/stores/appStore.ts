import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {getAppVoById, listMyAppVoByPage} from '@/api/appController'

/**
 * 应用全局状态管理
 */
export const useAppStore = defineStore('app', () => {
  const selectedApp = ref<API.AppVO | null>(null)
  const myApps = ref<API.AppVO[]>([])
  const myAppsTotal = ref(0)
  const currentMode = ref<'chat' | 'app'>('chat')
  const sidebarCollapsed = ref(false)
  const rightPanelVisible = ref(false)

  const hasSelectedApp = computed(() => !!selectedApp.value?.id)

  function selectApp(app: API.AppVO) {
    selectedApp.value = app
    currentMode.value = 'chat'
    rightPanelVisible.value = true
  }

  async function selectAppById(appId: number) {
    try {
      const res = await getAppVoById({id: appId})
      if (res.data.code === 0 && res.data.data) {
        selectedApp.value = res.data.data
        currentMode.value = 'chat'
        rightPanelVisible.value = true
      }
    } catch (e) {
      console.error('Failed to load app:', e)
    }
  }

  function refreshSelectedApp(app: API.AppVO) {
    selectedApp.value = app
  }

  function clearSelectedApp() {
    selectedApp.value = null
    currentMode.value = 'chat'
    rightPanelVisible.value = false
  }

  async function loadMyApps(page = 1, pageSize = 50) {
    try {
      const res = await listMyAppVoByPage({
        pageNum: page,
        pageSize,
        sortField: 'createTime',
        sortOrder: 'desc'
      })
      if (res.data.code === 0 && res.data.data) {
        myApps.value = res.data.data.records || []
        myAppsTotal.value = res.data.data.totalRow || 0
      }
    } catch (e) {
      console.error('Failed to load apps:', e)
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setMode(mode: 'chat' | 'app') {
    currentMode.value = mode
  }

  function toggleRightPanel() {
    rightPanelVisible.value = !rightPanelVisible.value
  }

  return {
    selectedApp, myApps, myAppsTotal, currentMode, sidebarCollapsed, rightPanelVisible,
    hasSelectedApp,
    selectApp, selectAppById, refreshSelectedApp, clearSelectedApp, loadMyApps,
    toggleSidebar, setMode, toggleRightPanel
  }
})
