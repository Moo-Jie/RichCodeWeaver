<template>
  <div class="app-list-wrapper">
    <div class="app-list">
      <div
        v-for="app in appStore.myApps"
        :key="app.id"
        :class="['app-item', { active: appStore.selectedApp?.id === app.id }]"
        @click="handleSelect(app)"
      >
        <div class="app-cover">
          <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
          <img v-else alt="默认封面" class="default-cover" src="@/assets/logo.png" />
        </div>
        <transition name="fade-text">
          <div v-show="!appStore.sidebarCollapsed" class="app-info">
            <span class="app-name">{{ app.appName || '未命名数字产物' }}</span>
            <a-tag :color="app.ownershipType === 'collaborator' ? 'default' : 'blue'" class="app-tag">
              {{ app.ownershipType === 'collaborator' ? '协作' : '我的' }}
            </a-tag>
          </div>
        </transition>
      </div>
      <div v-if="appStore.myApps.length === 0 && !appStore.sidebarCollapsed" class="empty-tip">
        暂无数字产物
      </div>
    </div>
    <transition name="fade-text">
      <button
        v-if="appStore.myApps.length > 0 && !appStore.sidebarCollapsed"
        class="view-more-btn"
        @click="router.push('/my/apps')"
      >
        查看更多
        <RightOutlined />
      </button>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import { useAppStore } from '@/stores/appStore'
import { useRouter } from 'vue-router'
import { RightOutlined } from '@ant-design/icons-vue'

const appStore = useAppStore()
const router = useRouter()

const handleSelect = (app: API.AppVO) => {
  appStore.selectApp(app)
  router.push(`/app/chat/${app.id}`)
}
</script>

<style scoped>
.app-list-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.app-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
}

.app-list::-webkit-scrollbar {
  width: 3px;
}

.app-list::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 3px;
}

.view-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  margin: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.view-more-btn:hover {
  background: #f5f5f5;
  border-color: #e5e5e5;
  color: #333;
}

.app-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 2px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 44px;
}

.app-item:hover {
  background: #f5f5f5;
}

.app-item.active {
  background: #f0f0f0;
}

.app-cover {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #fafafa;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-cover .default-cover {
  opacity: 0.6;
}

.app-name {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.app-tag {
  flex-shrink: 0;
  margin-inline-end: 0;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  padding-inline: 7px;
}

.empty-tip {
  text-align: center;
  color: #bbb;
  font-size: 13px;
  padding: 20px 0;
}

.fade-text-enter-active,
.fade-text-leave-active {
  transition: opacity 0.15s ease;
}

.fade-text-enter-from,
.fade-text-leave-to {
  opacity: 0;
}
</style>
