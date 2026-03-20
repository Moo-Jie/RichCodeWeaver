import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAppVoById, listMyAppVoByPage, listStarAppVoByPage } from '@/api/appController'

/**
 * 数字产物全局状态管理
 */
export const useAppStore = defineStore('app', () => {
  const selectedApp = ref<API.AppVO | null>(null)
  const myApps = ref<API.AppVO[]>([])
  const myAppsTotal = ref(0)
  const hotApps = ref<API.AppVO[]>([])
  const hotAppsTotal = ref(0)
  const currentMode = ref<'chat' | 'app'>('chat')
  const sidebarCollapsed = ref(false)
  const rightPanelVisible = ref(false)
  const hotPanelVisible = ref(true)

  const hasSelectedApp = computed(() => !!selectedApp.value?.id)

  function selectApp(app: API.AppVO) {
    selectedApp.value = app
    currentMode.value = 'chat'
    rightPanelVisible.value = true
    hotPanelVisible.value = false
  }

  async function selectAppById(appId: number) {
    try {
      const res = await getAppVoById({id: appId})
      if (res.data.code === 0 && res.data.data) {
        selectedApp.value = res.data.data
        currentMode.value = 'chat'
        rightPanelVisible.value = true
        hotPanelVisible.value = false
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
    hotPanelVisible.value = true
  }

  async function loadMyApps(page = 1, pageSize = 10) {
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

  async function loadHotApps(page = 1, pageSize = 20) {
    try {
      const res = await listStarAppVoByPage({
        pageNum: page,
        pageSize,
        sortField: 'priority',
        sortOrder: 'asc'
      })
      if (res.data.code === 0 && res.data.data) {
        hotApps.value = res.data.data.records || []
        hotAppsTotal.value = res.data.data.totalRow || 0
      }
    } catch (e) {
      console.error('Failed to load hot apps:', e)
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

  function toggleHotPanel() {
    hotPanelVisible.value = !hotPanelVisible.value
  }

  return {
    selectedApp, myApps, myAppsTotal, hotApps, hotAppsTotal,
    currentMode, sidebarCollapsed, rightPanelVisible, hotPanelVisible,
    hasSelectedApp,
    selectApp, selectAppById, refreshSelectedApp, clearSelectedApp,
    loadMyApps, loadHotApps,
    toggleSidebar, setMode, toggleRightPanel, toggleHotPanel
  }
})
