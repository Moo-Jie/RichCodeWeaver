<template>
  <div id="appHistoryPage" class="history-management">
    <div class="page-header">
      <h1>对话历史管理</h1>
      <p>管理数字产物历史对话记录</p>
    </div>

    <div class="main-content">
      <div class="app-section">
        <div class="section-header">
          <div class="decorative-line"></div>
          <h2 class="section-title">数字产物列表</h2>
          <div class="decorative-line"></div>
        </div>

        <a-card class="search-panel">
          <a-input-search
            v-model:value="appSearch"
            allow-clear
            class="search-input"
            placeholder="搜索数字产物..."
            @search="handleSearch"
            aria-label="搜索数字产物"
          />
        </a-card>

        <div v-if="loadingApps" class="loading-container">
          <a-spin size="large" tip="加载数字产物列表中..."/>
        </div>

        <a-empty
          v-else-if="!appList.length"
          class="empty-container"
          description="暂无数字产物数据"
        >
          <template #image>
            <div class="magic-pattern">
              <div class="pattern-element pattern-1"></div>
              <div class="pattern-element pattern-2"></div>
              <div class="pattern-element pattern-3"></div>
              <div class="pattern-element pattern-4"></div>
            </div>
          </template>
        </a-empty>

        <div v-else class="app-list">
          <div
            v-for="app in filteredAppList"
            :key="app.id"
            :class="{ active: selectedApp?.id === app.id }"
            class="app-item"
            tabindex="0"
            role="button"
            :aria-label="`选择数字产物 ${app.appName}`"
            @click="selectApp(app)"
            @keydown.enter="selectApp(app)"
            @keydown.space.prevent="selectApp(app)"
          >
            <div class="app-info">
              <a-avatar :src="app.cover" size="medium"/>
              <div class="app-meta">
                <div class="app-name">{{ app.appName }}</div>
                <div class="app-date">{{ formatTime(app.updateTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="conversation-section">
        <div v-if="selectedApp" class="detail-content">
          <div class="section-header">
            <div class="decorative-line"></div>
            <h2 class="section-title">数字产物 &lt;{{ selectedApp.appName }}&gt; 的对话记录</h2>
            <div class="decorative-line"></div>
          </div>

          <a-card :bordered="false" class="app-card">
            <div class="app-header">
              <a-avatar :src="selectedApp.cover" size="large"/>
              <div class="app-info">
                <h3>{{ selectedApp.appName }}</h3>
                <p class="app-desc">{{ selectedApp.appDescription }}</p>
                <div class="app-meta">
                  <span>创建时间: {{ formatTime(selectedApp.createTime) }}</span>
                  <span>更新时间: {{ formatTime(selectedApp.updateTime) }}</span>
                </div>
              </div>
              <div class="app-actions">
                <a-button
                  type="default"
                  @click="navigateToApp(selectedApp.id)"
                  aria-label="查看数字产物详情"
                >
                  <template #icon>
                    <ArrowRightOutlined/>
                  </template>
                  查看详情
                </a-button>
              </div>
            </div>
          </a-card>

          <div class="dialog-area">
            <div v-if="loadingHistory" class="loading-container">
              <a-spin size="large" tip="加载对话中..."/>
            </div>

            <a-empty
              v-else-if="!allAppHistory.length"
              class="empty-container"
              description="暂无对话记录"
            />

            <div v-else class="conversation-container">
              <!-- 分页提示 -->
              <div v-if="allAppHistory.length > 0" class="pagination-info">
                第 {{ currentPage }} 条 / 共 {{ totalPages }} 条
              </div>

              <!-- 对话内容区域 -->
              <div v-for="(history, index) in pagedHistory" :key="history.id"
                   class="conversation-item">
                <div v-if="history.messageType === 'ai'" class="ai-message">
                  <div class="message-avatar">
                    <a-avatar :src="aiAvatar" class="ai-avatar" size="default"/>
                    <span class="ai-label">AI</span>
                  </div>
                  <div class="message-content">
                    <div :class="{ 'collapsed': history.collapsed }" class="message-content-inner">
                      <markdown-renderer v-if="history.message" :content="history.message"/>
                    </div>
                    <a-button
                      v-if="history.message && history.message.length > 300"
                      class="toggle-content-btn"
                      type="link"
                      @click="toggleContent(history)"
                      :aria-label="history.collapsed ? '展开完整AI消息内容' : '收起AI消息内容'"
                    >
                      {{ history.collapsed ? '展开全部' : '收起' }}
                    </a-button>
                  </div>
                  <a-button class="delete-icon" @click="deleteMessage(history.id)" :aria-label="`删除AI消息 ${history.id}`">
                    <DeleteOutlined/>
                  </a-button>
                </div>

                <div v-if="history.messageType === 'user'" class="user-message">
                  <a-button class="delete-icon" @click="deleteMessage(history.id)" :aria-label="`删除用户消息 ${history.id}`">
                    <DeleteOutlined/>
                  </a-button>
                  <div class="message-content">
                    <div :class="{ 'collapsed': history.collapsed }" class="message-content-inner">
                      <div class="user-bubble">
                        {{ history.message }}
                      </div>
                    </div>
                    <a-button
                      v-if="history.message && history.message.length > 300"
                      class="toggle-content-btn"
                      type="link"
                      @click="toggleContent(history)"
                      :aria-label="history.collapsed ? '展开完整消息内容' : '收起消息内容'"
                    >
                      {{ history.collapsed ? '展开全部' : '收起' }}
                    </a-button>
                  </div>
                  <div class="message-avatar">
                    <a-avatar :src="loginUserStore.loginUser.userAvatar" size="default"/>
                    <span class="user-label">用户</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="allAppHistory.length" class="pagination-container">
              <a-pagination
                v-model:current="currentPage"
                :page-size="1"
                :total="allAppHistory.length"
                show-less-items
                @change="handlePageChange"
              />
            </div>
          </div>
        </div>

        <div v-else class="detail-empty">
          <a-empty class="empty-container" description="请从左侧选择一个数字产物">
            <template #image>
              <div class="magic-pattern">
                <div class="pattern-element pattern-1"></div>
                <div class="pattern-element pattern-2"></div>
                <div class="pattern-element pattern-3"></div>
                <div class="pattern-element pattern-4"></div>
              </div>
            </template>
          </a-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { ArrowRightOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { listAppVoByPageByAdmin as listAppsAdmin } from '@/api/appController'
import {
  deleteById,
  listAppChatHistoryByPageAdmin as listHistoryAdmin
} from '@/api/chatHistoryController'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { message } from 'ant-design-vue'

import aiAvatar from '@/assets/aiAvatar.png'
import { formatTime } from '@/utils/timeUtil.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 删除单条对话历史
const deleteMessage = async (messageId: number) => {
  if (!selectedApp.value) return

  try {
    const res = await deleteById({
      id: messageId
    })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 重新加载当前页数据
      await loadHistory()
    } else {
      message.error('删除失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败，请重试:' + res.data.message)
  }
}

// 数字产物列表相关状态
const appList = ref<any[]>([])
const appSearch = ref('')
const selectedApp = ref<any>(null)
const loadingApps = ref(true)

// 对话历史相关状态
const allAppHistory = ref<any[]>([]) // 存储所有历史记录
const loadingHistory = ref(true)
const currentPage = ref(1)
const contentLengthLimit = ref(5000) // 每页内容长度限制（字符数）

// 获取数字产物列表
const loadApps = async () => {
  try {
    loadingApps.value = true
    const params = {
      current: currentPage.value,
      pageSize: 1000,
      sortField: 'updateTime',
      sortOrder: 'descend'
    }

    const res = await listAppsAdmin(params)
    if (res.data.code === 0 && res.data.data) {
      appList.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('加载数字产物列表失败:', error)
    message.error('加载数字产物列表失败:' + res.data.message)
  } finally {
    loadingApps.value = false
  }
}

// 选择数字产物
const selectApp = async (app: any) => {
  selectedApp.value = app
  currentPage.value = 1 // 重置到第一页
  await loadHistory()
}

// 加载对话历史
const loadHistory = async () => {
  if (!selectedApp.value) return

  try {
    loadingHistory.value = true
    // 请求所有历史记录，不进行分页
    const params = {
      appId: selectedApp.value.id,
      current: 1,
      pageSize: 1000, // 设置较大的pageSize获取所有记录
      sortField: 'createTime',
      sortOrder: 'ascend'
    }

    const res = await listHistoryAdmin(params)
    if (res.data.code === 0 && res.data.data) {
      // 初始化折叠状态和分页数据
      allAppHistory.value = (res.data.data.records || []).map(item => ({
        ...item,
        collapsed: item.message && item.message.length > 300 // 长内容默认折叠
      }))
    }
  } catch (error) {
    console.error('加载对话失败:', error)
    message.error('加载对话失败:' + res.data.message)
  } finally {
    loadingHistory.value = false
  }
}

// 切换内容展开/折叠状态
const toggleContent = (history: any) => {
  history.collapsed = !history.collapsed
}

// 计算分页后的历史记录
const pagedHistory = computed(() => {
  if (allAppHistory.value.length === 0) return []

  let currentLength = 0
  const result = []
  let startIndex = 0

  // 计算当前页应该从哪个索引开始
  if (currentPage.value > 1) {
    let pageCount = 1
    let tempLength = 0

    for (let i = 0; i < allAppHistory.value.length; i++) {
      const history = allAppHistory.value[i]
      const messageLength = history.message ? history.message.length : 0

      if (tempLength + messageLength > contentLengthLimit.value) {
        pageCount++
        tempLength = messageLength
        if (pageCount === currentPage.value) {
          startIndex = i
          break
        }
      } else {
        tempLength += messageLength
      }
    }
  }

  // 从startIndex开始添加消息，直到达到内容长度限制
  for (let i = startIndex; i < allAppHistory.value.length; i++) {
    const history = allAppHistory.value[i]
    const messageLength = history.message ? history.message.length : 0

    // 如果添加当前消息会超过限制，且当前页已有内容，则停止添加
    if (currentLength + messageLength > contentLengthLimit.value && result.length > 0) {
      break
    }

    result.push(history)
    currentLength += messageLength
  }

  return result
})

// 计算总页数
const totalPages = computed(() => {
  if (allAppHistory.value.length === 0) return 0

  let totalLength = 0
  let pageCount = 1

  for (const history of allAppHistory.value) {
    const messageLength = history.message ? history.message.length : 0

    if (totalLength + messageLength > contentLengthLimit.value) {
      pageCount++
      totalLength = messageLength
    } else {
      totalLength += messageLength
    }
  }

  return pageCount
})

// 处理页面变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  // 滚动到顶部
  const container = document.querySelector('.conversation-container')
  if (container) {
    container.scrollTop = 0
  }
}

// 搜索处理
const handleSearch = (value: string) => {
  appSearch.value = value
}

// 导航到数字产物详情
const navigateToApp = (appId: string) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

// 数字产物列表过滤
const filteredAppList = computed(() => {
  if (!appSearch.value) return appList.value
  const searchLower = appSearch.value.toLowerCase()
  return appList.value.filter(app =>
    app.appName.toLowerCase().includes(searchLower)
  )
})

// 监听当前页变化，更新显示的数据
watch(currentPage, (newPage) => {
  // 这里不需要做额外处理，因为pagedHistory是计算属性，会自动更新
})

// 生命周期钩子
onMounted(() => {
  loadApps()
})
</script>

<style lang="less" scoped>
.history-management {
  padding: 32px;
  width: 100%;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.search-panel {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  margin-bottom: 20px;
  padding: 16px;
  transition: all 0.2s ease;

  &:hover {
    border-color: #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
}

.app-section, .conversation-section {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.2s ease;

  &:hover {
    border-color: #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
}

.main-content {
  flex: 1;
  display: flex;
  gap: 30px;
  overflow: hidden;
}

.app-section {
  width: 35%;
  display: flex;
  flex-direction: column;
}

.conversation-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px 0;
  gap: 12px;
}

.decorative-line {
  flex: 1;
  height: 1px;
  background: #f0f0f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  padding: 0 16px;
}

.loading-container,
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 40px 0;
}

.app-list {
  flex: 1;
  padding: 0 15px 10px;
  overflow-y: auto;
}

.app-item {
  padding: 12px;
  margin: 0 0 12px;
  background: white;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    border-color: #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  &.active {
    border-color: #1a1a1a;
    background-color: #fafafa;
  }
}

.app-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.app-date {
  font-size: 12px;
  color: #999;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow-y: auto;
}

.app-card {
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-radius: 20px;
}

.app-header {
  display: flex;
  align-items: center;
  padding: 10px;
}

.app-info {
  flex: 1;
  margin-left: 20px;
}

.app-desc {
  color: #666;
  margin: 10px 0;
}

.app-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #888;
}

.app-actions {
  display: flex;
  gap: 8px;
  align-self: flex-start;

  .ant-btn {
    height: 36px;
    padding: 0 16px;
    border-radius: 10px;
    font-weight: 500;
    font-size: 14px;
    transition: all 0.2s ease;
    border: 1px solid #e5e5e5;
    background: #1a1a1a;
    color: #fff;

    &:hover {
      background: #333;
    }
  }
}

:deep(.ant-btn) {
  border-radius: 10px;
  transition: all 0.2s ease;

  &:hover {
    transform: none;
  }
}

.dialog-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.conversation-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.conversation-item {
  margin-bottom: 30px;
  position: relative;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-message {
  display: flex;
  align-items: flex-start;
  margin-right: 50px;
  gap: 10px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  margin-left: 50px;
  gap: 10px;
}

.message-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.message-content {
  max-width: 70%;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  transition: all 0.2s ease;

  &:hover {
    border-color: #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
}

.ai-message .message-content {
  background: #fafafa;
  border-top-left-radius: 4px;
}

.user-message .message-content {
  background: #f0f0f0;
  border-top-right-radius: 4px;
}

.user-bubble {
  background: transparent;
  border-radius: 8px;
  padding: 0;
  line-height: 1.6;
  font-family: 'Nunito', sans-serif;
  color: #1a1a1a;
  font-size: 14px;
}

.ai-avatar {
  background-color: #fafafa;
}

.ai-label, .user-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  font-weight: 500;
}

.message-actions {
  position: absolute;
  top: 5px;
  right: 5px;
  opacity: 1;
  transition: opacity 0.3s, transform 0.2s;
  display: block;
}

.delete-icon {
  width: 30px;
  height: 30px;
  padding: 0;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #f0f0f0;
  margin: 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff6b6b;
  transition: all 0.2s ease;

  &:hover {
    border-color: #ff6b6b;
    background: rgba(255, 107, 107, 0.1);
  }
}

.pagination-container {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
  background: #fafafa;
  border-radius: 0 0 14px 14px;
  margin-top: 12px;
}

:deep(.ant-pagination) {
  display: flex;
  justify-content: center;
  align-items: center;

  .ant-pagination-item {
    border-radius: 8px;
    border: 1px solid #e5e5e5;
    transition: all 0.2s ease;

    &:hover {
      border-color: #1a1a1a;
    }

    &.ant-pagination-item-active {
      background: #1a1a1a;
      border-color: #1a1a1a;

      a {
        color: #fff;
      }
    }
  }

  .ant-pagination-prev,
  .ant-pagination-next {
    border-radius: 8px;
    transition: all 0.2s ease;
  }
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 40px 0;
}

.magic-pattern {
  position: relative;
  width: 150px;
  height: 150px;
}

.pattern-element {
  position: absolute;
  border-radius: 50%;
  opacity: 0.7;
  animation: pulse 5s infinite ease-in-out;
}

.pattern-1 {
  width: 80px;
  height: 80px;
  background: rgba(114, 46, 209, 0.15);
  top: 0;
  left: 35px;
  animation-delay: 0s;
}

.pattern-2 {
  width: 60px;
  height: 60px;
  background: rgba(24, 144, 255, 0.15);
  top: 20px;
  right: 30px;
  animation-delay: 0.5s;
}

.pattern-3 {
  width: 90px;
  height: 90px;
  background: rgba(146, 84, 222, 0.1);
  bottom: 15px;
  left: 25px;
  animation-delay: 1s;
}

.pattern-4 {
  width: 70px;
  height: 70px;
  background: rgba(82, 196, 26, 0.1);
  bottom: 30px;
  right: 25px;
  animation-delay: 1.5s;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.pagination-info {
  text-align: center;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #fafafa;
  border-radius: 10px;
  font-size: 14px;
  color: #666;
  font-weight: 500;
  border: 1px solid #f0f0f0;
}

.content-length-info {
  font-size: 12px;
  color: #999;
}

.message-content-inner {
  max-height: none;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.message-content-inner.collapsed {
  max-height: 200px;
  mask-image: linear-gradient(to bottom, black 50%, transparent 100%);
}

.toggle-content-btn {
  margin-top: 8px;
  font-size: 12px;
  padding: 4px 12px;
  height: auto;
  border-radius: 8px;
  background: #1a1a1a;
  color: white;
  border: none;
  transition: all 0.2s ease;

  &:hover {
    background: #333;
  }
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .app-section,
  .conversation-section {
    width: 100%;
    height: auto;
    min-height: 400px;
  }

  .app-section {
    max-height: 500px;
  }
}

/* Keyboard navigation focus indicators */
:deep(.ant-input-search:focus-within),
:deep(.ant-input:focus),
:deep(.ant-input-focused) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
  border-color: #4096ff;
}

:deep(.ant-btn:focus-visible) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

.app-item {
  &:focus-visible {
    outline: 3px solid #4096ff;
    outline-offset: 2px;
  }
}

:deep(.ant-pagination-item:focus-visible),
:deep(.ant-pagination-prev:focus-visible),
:deep(.ant-pagination-next:focus-visible) {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

.delete-icon:focus-visible {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

.toggle-content-btn:focus-visible {
  outline: 3px solid #4096ff;
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .history-management {
    padding: 15px;
  }

  .page-header h1 {
    font-size: 2rem;
  }

  .page-header p {
    font-size: 1rem;
  }

  .search-panel {
    padding: 15px;
  }

  .main-content {
    gap: 15px;
  }

  .section-title {
    font-size: 18px;
  }

  .app-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-info h3 {
    font-size: 1.1rem;
  }

  .app-desc {
    font-size: 0.9rem;
  }

  .app-meta {
    flex-direction: column;
    gap: 5px;
    font-size: 12px;
  }

  .app-actions {
    width: 100%;
    margin-top: 15px;
    justify-content: space-between;

    .ant-btn {
      font-size: 0.9rem;
      padding: 0 14px;
      height: 36px;
    }
  }

  .ai-message, .user-message {
    margin: 0 10px;
  }

  .message-content {
    max-width: 85%;
    padding: 14px;
    font-size: 0.9rem;
  }

  .app-name {
    font-size: 14px;
  }

  .app-date {
    font-size: 12px;
  }

  .pagination-info {
    font-size: 12px;
    padding: 10px 15px;
  }

  .ai-label, .user-label {
    font-size: 11px;
  }
}
</style>
