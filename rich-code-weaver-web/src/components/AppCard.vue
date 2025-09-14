<template>
  <div :class="{ 'app-card--featured': featured }" class="app-card">
    <div class="app-preview">
      <img v-if="app.cover" :alt="app.appName" :src="app.cover" />
      <div v-else class="app-placeholder">
        <img alt="应用封面"
             src="@/assets/logo.png"
             style="width: 100%; opacity: 0.8; margin: 0 auto; display: block;" />
      </div>
      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app.deployKey" target="_blank" type="default" @click="handleViewWork">
            查看作品
          </a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :size="40" :src="app.user?.userAvatar">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName?.substring(0, 10) + '...' || '未命名应用' }}</h3>
        <p class="app-author">
          {{ app.user?.userName || (featured ? '官方' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void

  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  window.open(`/app/chat/${props.app.id}`, '_blank')
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: transform 0.3s,
  box-shadow 0.3s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
}

.app-preview {
  height: 180px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-placeholder {
  font-size: 48px;
  color: #d9d9d9;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Comic Neue', cursive;
}

.app-author {
  font-size: 14px;
  color: #666;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Nunito', sans-serif;
}
</style>
