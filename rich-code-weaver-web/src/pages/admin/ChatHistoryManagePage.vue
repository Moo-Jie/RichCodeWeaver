<template>
  <div id="appHistoryPage" class="history-management">
    <div class="page-header">
      <h1>对话历史管理</h1>
      <p>管理应用历史对话记录</p>
    </div>

    <div class="main-content">
      <div class="app-section">
        <div class="section-header">
          <div class="decorative-line"></div>
          <h2 class="section-title">应用列表</h2>
          <div class="decorative-line"></div>
        </div>

        <a-card class="search-panel">
          <a-input-search
            v-model:value="appSearch"
            placeholder="搜索应用..."
            allow-clear
            @search="handleSearch"
            class="search-input"
          />
        </a-card>

        <div v-if="loadingApps" class="loading-container">
          <a-spin size="large" tip="加载应用列表中..." />
        </div>

        <a-empty
          v-else-if="!appList.length"
          description="暂无应用数据"
          class="empty-container"
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
            class="app-item"
            :class="{ active: selectedApp?.id === app.id }"
            @click="selectApp(app)"
          >
            <div class="app-info">
              <a-avatar :src="app.cover" size="medium" />
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
            <h2 class="section-title">应用 &lt;{{ selectedApp.appName }}&gt; 的对话记录</h2>
            <div class="decorative-line"></div>
          </div>

          <a-card class="app-card" :bordered="false">
            <div class="app-header">
              <a-avatar :src="selectedApp.cover" size="large" />
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
                >
                  <template #icon>
                    <ArrowRightOutlined />
                  </template>
                  查看详情
                </a-button>
              </div>
            </div>
          </a-card>

          <div class="dialog-area">
            <div v-if="loadingHistory" class="loading-container">
              <a-spin size="large" tip="加载对话中..." />
            </div>

            <a-empty
              v-else-if="!appHistory.length"
              description="暂无对话记录"
              class="empty-container"
            />

            <div v-else class="conversation-container">
              <div v-for="(history, index) in appHistory" :key="history.id"
                   class="conversation-item">
                <div v-if="history.messageType === 'ai'" class="ai-message">
                  <div class="message-avatar">
                    <a-avatar :src="aiAvatar" class="ai-avatar" size="default" />
                    <span class="ai-label">AI</span>
                  </div>
                  <div class="message-content">
                    <markdown-renderer v-if="history.message" :content="history.message" />
                  </div>
                  <a-button class="delete-icon" @click="deleteMessage(history.id)">
                    <DeleteOutlined />
                  </a-button>
                </div>

                <div v-if="history.messageType === 'user'" class="user-message">
                  <a-button class="delete-icon" @click="deleteMessage(history.id)">
                    <DeleteOutlined />
                  </a-button>
                  <div class="message-content">
                    <div class="user-bubble">
                      {{ history.message }}
                    </div>
                  </div>
                  <div class="message-avatar">
                    <a-avatar :src="loginUserStore.loginUser.userAvatar" size="default" />
                    <span class="user-label">用户</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="appHistory.length" class="pagination-container">
              <a-pagination
                v-model:current="currentPage"
                :total="historyTotal"
                :page-size="pageSize"
                show-less-items
                @change="loadHistory"
              />
            </div>
          </div>
        </div>

        <div v-else class="detail-empty">
          <a-empty description="请从左侧选择一个应用" class="empty-container">
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

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  DeleteOutlined
} from '@ant-design/icons-vue'
import {
  listAppVoByPageByAdmin as listAppsAdmin
} from '@/api/appController'
import {
  listAppChatHistoryByPageAdmin as listHistoryAdmin,
  deleteById
} from '@/api/chatHistoryController'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {
  ArrowRightOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import aiAvatar from '@/assets/aiAvatar.png'
import { formatTime } from '@/utils/time'

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
      await loadHistory()
    } else {
      message.error('删除失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败，请重试')
  }
}

// 应用列表相关状态
const appList = ref<any[]>([])
const appSearch = ref('')
const selectedApp = ref<any>(null)
const loadingApps = ref(true)

// 对话历史相关状态
const appHistory = ref<any[]>([])
const loadingHistory = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const historyTotal = ref(0)

// 获取应用列表
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
    console.error('加载应用列表失败:', error)
    message.error('加载应用列表失败')
  } finally {
    loadingApps.value = false
  }
}

// 选择应用
const selectApp = async (app: any) => {
  selectedApp.value = app
  await loadHistory()
}

// 加载对话历史
const loadHistory = async () => {
  if (!selectedApp.value) return

  try {
    loadingHistory.value = true
    const params = {
      appId: selectedApp.value.id,
      current: currentPage.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'ascend'
    }

    const res = await listHistoryAdmin(params)
    if (res.data.code === 0 && res.data.data) {
      appHistory.value = res.data.data.records || []
      historyTotal.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('加载对话失败:', error)
    message.error('加载对话失败')
  } finally {
    loadingHistory.value = false
  }
}

// 搜索处理
const handleSearch = (value: string) => {
  appSearch.value = value
}

// 导航到应用详情
const navigateToApp = (appId: string) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

// 应用列表过滤
const filteredAppList = computed(() => {
  if (!appSearch.value) return appList.value
  const searchLower = appSearch.value.toLowerCase()
  return appList.value.filter(app =>
    app.appName.toLowerCase().includes(searchLower)
  )
})

// 生命周期钩子
onMounted(() => {
  loadApps()
})
</script>

<style scoped>
.history-management {
  padding: 24px;
  background: linear-gradient(135deg, #fdfcf9 0%, #f7f5f2 100%);
  min-height: calc(100vh - 48px);
  position: relative;
  font-family: 'Source Sans Pro', sans-serif;
  overflow: hidden;
}

.history-management::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" opacity="0.05"><rect x="40" y="10" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="10" y="40" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="70" y="40" width="20" height="20" fill="%23666" rx="4" ry="4"/><rect x="40" y="70" width="20" height="20" fill="%23666" rx="4" ry="4"/></svg>');
  background-size: 200px;
  pointer-events: none;
  z-index: 0;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 10px 0;
}

.page-header h1 {
  font-size: 2.8rem;
  font-weight: 600;
  color: #5c4a48;
  margin-bottom: 8px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.page-header p {
  font-size: 1.2rem;
  color: #7a787c;
  max-width: 600px;
  margin: 0 auto;
}

.search-panel {
  background: rgba(255, 253, 248, 0.92);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(155, 140, 125, 0.1);
  border: 1px solid rgba(198, 180, 165, 0.15);
  margin-bottom: 25px;
  padding: 20px;
}

.search-panel .ant-input-search {
  width: 100%;
}

.app-section,
.conversation-section {
  background: rgba(255, 253, 248, 0.92);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(155, 140, 125, 0.1);
  border: 1px solid rgba(198, 180, 165, 0.15);
  overflow: hidden;
  position: relative;
  z-index: 1;
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
  margin: 20px 0;
  gap: 15px;
}

.decorative-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(to right, transparent, rgba(198, 160, 138, 0.4), transparent);
}

.section-title {
  font-family: 'Playfair Display', serif;
  font-size: 22px;
  font-weight: 700;
  color: #5c4a48;
  text-align: center;
  padding: 0 20px;
  letter-spacing: -0.5px;
  position: relative;
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
  padding: 15px;
  margin: 0 0 15px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
  border: 1px solid rgba(220, 220, 220, 0.3);
}

.app-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.08);
  border-color: rgba(114, 46, 209, 0.2);
}

.app-item.active {
  border-color: #722ed1;
  background-color: rgba(114, 46, 209, 0.05);
}

.app-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.app-meta {
  flex: 1;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #5c4a48;
  margin-bottom: 5px;
}

.app-date {
  font-size: 13px;
  color: #8c8c8c;
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
  gap: 10px;
  align-self: flex-start;
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
  margin-bottom: 25px;
  position: relative;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  margin-right: 40px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  margin-left: 40px;
}

.message-avatar {
  margin: 0 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.message-content {
  max-width: 70%;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.08);
  position: relative;
}

.ai-message .message-content {
  background: #f5f7fe;
  border-top-left-radius: 0;
}

.user-message .message-content {
  background: #e9f5ff;
  border-top-right-radius: 0;
}

.user-bubble {
  background: #e9f5ff;
  border-radius: 12px;
  padding: 12px 16px;
  line-height: 1.6;
}

.ai-avatar {
  background-color: #f0f5ff;
}

.ai-label, .user-label {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
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
  width: 24px;
  height: 24px;
  padding: 0;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.8);
  border: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.delete-icon:hover {
  transform: scale(1.1);
  background-color: #fff;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

.pagination-container {
  padding: 15px;
  border-top: 1px solid rgba(235, 235, 235, 0.8);
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

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .app-section,
  .conversation-section {
    width: 100%;
    height: 50%;
  }
}

@media (max-width: 768px) {
  .history-management {
    padding: 15px;
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

  .app-actions {
    width: 100%;
    margin-top: 15px;
    justify-content: space-between;
  }
}
</style>
