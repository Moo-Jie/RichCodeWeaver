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
  let myAppsRequestId = 0
  let hotAppsRequestId = 0
  let selectedAppRequestId = 0

  const hasSelectedApp = computed(() => !!selectedApp.value?.id)
  const shouldOpenRightPanel = () =>
    typeof window !== 'undefined' && window.matchMedia('(min-width: 1201px)').matches

  function selectApp(app: API.AppVO) {
    selectedApp.value = app
    currentMode.value = 'chat'
    rightPanelVisible.value = shouldOpenRightPanel()
    hotPanelVisible.value = false
  }

  async function selectAppById(appId: number) {
    const requestId = ++selectedAppRequestId
    try {
      const res = await getAppVoById({ id: appId })
      if (requestId !== selectedAppRequestId) return
      if (res.data.code === 0 && res.data.data) {
        selectedApp.value = res.data.data
        currentMode.value = 'chat'
        rightPanelVisible.value = shouldOpenRightPanel()
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

  async function loadMyApps(
    page = 1,
    pageSize = 10,
    options?: { signal?: AbortSignal; append?: boolean },
  ) {
    const requestId = ++myAppsRequestId
    try {
      const res = await listMyAppVoByPage(
        {
          pageNum: page,
          pageSize,
          sortField: 'createTime',
          sortOrder: 'desc',
        },
        options,
      )
      if (requestId !== myAppsRequestId) return false
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '产物列表加载失败')
      }
      const records = res.data.data.records || []
      if (options?.append && page > 1) {
        const existingIds = new Set(myApps.value.map((item) => String(item.id)))
        myApps.value = [
          ...myApps.value,
          ...records.filter((item) => !existingIds.has(String(item.id))),
        ]
      } else {
        myApps.value = records
      }
      myAppsTotal.value = Number(res.data.data.totalRow) || 0
      return true
    } catch (e) {
      if (!(e instanceof DOMException && e.name === 'AbortError')) {
        console.error('Failed to load apps:', e)
      }
      return false
    }
  }

  async function loadHotApps(page = 1, pageSize = 20, options?: { signal?: AbortSignal }) {
    const requestId = ++hotAppsRequestId
    try {
      const res = await listStarAppVoByPage(
        {
          pageNum: page,
          pageSize,
          sortField: 'priority',
          sortOrder: 'asc',
        },
        options,
      )
      if (requestId !== hotAppsRequestId) return false
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '热门产物加载失败')
      }
      hotApps.value = res.data.data.records || []
      hotAppsTotal.value = Number(res.data.data.totalRow) || 0
      return true
    } catch (e) {
      if (!(e instanceof DOMException && e.name === 'AbortError')) {
        console.error('Failed to load hot apps:', e)
      }
      return false
    }
  }

  function resetAppState() {
    myAppsRequestId += 1
    hotAppsRequestId += 1
    selectedAppRequestId += 1
    selectedApp.value = null
    myApps.value = []
    myAppsTotal.value = 0
    hotApps.value = []
    hotAppsTotal.value = 0
    currentMode.value = 'chat'
    rightPanelVisible.value = false
    hotPanelVisible.value = true
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
    selectedApp,
    myApps,
    myAppsTotal,
    hotApps,
    hotAppsTotal,
    currentMode,
    sidebarCollapsed,
    rightPanelVisible,
    hotPanelVisible,
    hasSelectedApp,
    selectApp,
    selectAppById,
    refreshSelectedApp,
    clearSelectedApp,
    resetAppState,
    loadMyApps,
    loadHotApps,
    toggleSidebar,
    setMode,
    toggleRightPanel,
    toggleHotPanel,
  }
})
