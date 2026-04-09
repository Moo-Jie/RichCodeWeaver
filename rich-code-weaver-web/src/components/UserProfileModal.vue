<template>
  <a-modal
    v-model:open="visible"
    :footer="null"
    :width="720"
    centered
    class="user-profile-modal"
    title="用户名片"
  >
    <div class="profile-shell">
      <div class="profile-hero">
        <a-avatar :size="64" :src="userInfo?.userAvatar" class="profile-avatar">
          {{ userInfo?.userName?.charAt(0) || seedName.charAt(0) || 'U' }}
        </a-avatar>
        <div class="profile-main">
          <div class="profile-topline">
            <h3 class="profile-name">{{ userInfo?.userName || seedName || '未知用户' }}</h3>
            <span v-if="identityLabel" class="profile-pill profile-pill--identity">{{ identityLabel }}</span>
            <span v-if="isFriend" class="profile-pill">已是好友</span>
          </div>
          <div class="profile-meta">
            <span v-if="userInfo?.userIndustry">{{ userInfo.userIndustry }}</span>
            <span v-if="userInfo?.userIndustry && userInfo?.createTime">·</span>
            <span v-if="userInfo?.createTime">加入于 {{ formatDate(userInfo.createTime) }}</span>
          </div>
          <p class="profile-desc">{{ userInfo?.userProfile || '这个人很神秘，暂时还没有留下更多介绍。' }}</p>
        </div>
        <div v-if="showAddFriend" class="profile-actions">
          <a-button :loading="addingFriend" class="add-friend-btn" type="primary" @click="handleAddFriend">
            加好友
          </a-button>
        </div>
      </div>

      <div class="profile-section">
        <div class="section-header">
          <span class="section-title">相关产物</span>
          <span class="section-subtitle">开创过的产物与参与协作的产物</span>
        </div>

        <a-spin :spinning="loadingApps">
          <div v-if="relatedApps.length > 0" class="app-grid">
            <button
              v-for="app in relatedApps"
              :key="app.id"
              class="app-card"
              type="button"
              @click="openApp(app)"
            >
              <img v-if="app.cover" :src="app.cover" :alt="app.appName || '产物封面'" class="app-cover" />
              <div v-else class="app-cover app-cover--placeholder">RCW</div>
              <div class="app-body">
                <div class="app-headline">
                  <span class="app-name">{{ app.appName || '未命名数字产物' }}</span>
                  <span :class="['profile-pill', app.ownershipType === 'collaborator' ? 'profile-pill--muted' : 'profile-pill--mine']">
                    {{ app.ownershipType === 'collaborator' ? '协作' : '开创' }}
                  </span>
                </div>
                <div class="app-meta">
                  <span>{{ formatType(app.codeGenType) }}</span>
                  <span>·</span>
                  <span>{{ formatDate(app.updateTime || app.createTime) }}</span>
                </div>
              </div>
            </button>
          </div>
          <div v-else class="empty-state">暂无相关产物</div>
        </a-spin>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getUserVoById } from '@/api/userController'
import { listUserRelatedApps } from '@/api/appController'
import { sendFriendRequest } from '@/api/friendController'
import { useChatStore } from '@/stores/chatStore'
import { useLoginUserStore } from '@/stores/loginUser'

interface Props {
  open: boolean
  userId?: number
  seedName?: string
  seedAvatar?: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  userId: undefined,
  seedName: '',
  seedAvatar: ''
})

const emit = defineEmits<Emits>()
const router = useRouter()
const chatStore = useChatStore()
const loginUserStore = useLoginUserStore()

const userInfo = ref<API.UserVO>()
const relatedApps = ref<API.AppVO[]>([])
const loadingApps = ref(false)
const loadingUser = ref(false)
const addingFriend = ref(false)
const requestedFriendIds = ref<number[]>([])

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const seedName = computed(() => props.seedName || '')

const isCurrentUser = computed(() => {
  return !!props.userId && props.userId === loginUserStore.loginUser.id
})

const isFriend = computed(() => {
  if (!props.userId) return false
  return chatStore.friends.some((item) => item.friendId === props.userId)
})

const hasRequested = computed(() => {
  if (!props.userId) return false
  return requestedFriendIds.value.includes(props.userId)
})

const showAddFriend = computed(() => {
  return !!props.userId && !isCurrentUser.value && !isFriend.value && !hasRequested.value
})

const identityLabel = computed(() => {
  const value = userInfo.value?.userIdentity
  if (value === 'enterprise') return '企业'
  if (value === 'merchant') return '商户'
  if (value === 'individual') return '个体'
  return ''
})

const loadUserInfo = async () => {
  if (!props.userId) {
    userInfo.value = undefined
    return
  }
  loadingUser.value = true
  try {
    const res = await getUserVoById({ id: props.userId })
    userInfo.value = res.data.code === 0 ? res.data.data : undefined
  } catch (error) {
    userInfo.value = undefined
  } finally {
    loadingUser.value = false
  }
}

const loadRelatedApps = async () => {
  if (!props.userId) {
    relatedApps.value = []
    return
  }
  loadingApps.value = true
  try {
    const res = await listUserRelatedApps({ userId: props.userId, pageSize: 8 })
    relatedApps.value = res.data.code === 0 ? (res.data.data || []) : []
  } catch (error) {
    relatedApps.value = []
  } finally {
    loadingApps.value = false
  }
}

const ensureFriendList = async () => {
  if (!loginUserStore.loginUser.id) return
  if (chatStore.friends.length === 0) {
    await chatStore.fetchFriends()
  }
}

const handleAddFriend = async () => {
  if (!props.userId || !showAddFriend.value) return
  addingFriend.value = true
  try {
    const res = await sendFriendRequest({ friendId: props.userId, remark: '' })
    if (res.data.code === 0) {
      requestedFriendIds.value = [...requestedFriendIds.value, props.userId]
      message.success('好友申请已发送')
    } else {
      message.error(res.data.message || '发送好友申请失败')
    }
  } catch (error) {
    message.error('发送好友申请失败')
  } finally {
    addingFriend.value = false
  }
}

const openApp = (app: API.AppVO) => {
  if (!app.id) return
  visible.value = false
  router.push(`/app/chat/${app.id}`)
}

const formatDate = (value?: string) => {
  if (!value) return '--'
  return value.slice(0, 10)
}

const formatType = (type?: string) => {
  if (type === 'single_html') return '单文件结构'
  if (type === 'multi_file') return '多文件结构'
  if (type === 'vue_project') return 'VUE 项目工程'
  return type || '未知类型'
}

watch(
  () => [props.open, props.userId] as const,
  async ([open, userId]) => {
    if (!open || !userId) return
    await ensureFriendList()
    await Promise.all([loadUserInfo(), loadRelatedApps()])
  },
  { immediate: true }
)
</script>

<style scoped>
.user-profile-modal :deep(.ant-modal-content) {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #e8edf2;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
}

.user-profile-modal :deep(.ant-modal-header) {
  border-bottom: 1px solid #eef2f5;
  padding: 18px 24px;
}

.user-profile-modal :deep(.ant-modal-body) {
  padding: 20px 24px 24px;
}

.profile-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-hero {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e8edf2;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f7f9fb 100%);
}

.profile-avatar {
  flex-shrink: 0;
  background: #eaeef2;
  color: #57606a;
  font-weight: 700;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-topline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-name {
  margin: 0;
  font-size: 20px;
  line-height: 1.3;
  color: #24292f;
}

.profile-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #8c959f;
}

.profile-desc {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #57606a;
}

.profile-actions {
  flex-shrink: 0;
}

.add-friend-btn {
  height: 36px;
  border-radius: 10px;
  background: #1f2328;
  border-color: #1f2328;
}

.profile-section {
  border: 1px solid #e8edf2;
  border-radius: 18px;
  background: #ffffff;
  padding: 16px;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #24292f;
}

.section-subtitle {
  font-size: 12px;
  color: #8c959f;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.app-card {
  border: 1px solid #e8edf2;
  border-radius: 14px;
  background: #fff;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.app-card:hover {
  transform: translateY(-2px);
  border-color: #d7dfe7;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.app-cover {
  width: 100%;
  height: 108px;
  object-fit: cover;
  display: block;
  background: #f6f8fa;
}

.app-cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c959f;
  font-size: 20px;
  font-weight: 700;
}

.app-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.app-headline {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.app-name {
  min-width: 0;
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #24292f;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.app-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #8c959f;
  flex-wrap: wrap;
}

.profile-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
  color: #57606a;
  font-size: 11px;
  font-weight: 600;
}

.profile-pill--identity {
  background: #eef6ff;
  border-color: #c6ddff;
  color: #245ea6;
}

.profile-pill--mine {
  background: #eef6ff;
  border-color: #c6ddff;
  color: #245ea6;
}

.profile-pill--muted {
  background: #f6f8fa;
  border-color: #d0d7de;
  color: #57606a;
}

.empty-state {
  padding: 20px 0 8px;
  text-align: center;
  font-size: 13px;
  color: #8c959f;
}

@media (max-width: 768px) {
  .profile-hero {
    flex-direction: column;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
