<template>
  <a-modal
    v-model:open="visible"
    title="邀请协作者"
    :width="420"
    :footer="null"
    :bodyStyle="{ padding: '16px' }"
    @cancel="handleClose"
  >
    <!-- 身份检查提示：个人用户无法邀请 -->
    <div v-if="isIndividual" class="identity-hint">
      <InfoCircleOutlined class="hint-icon" />
      <div class="hint-content">
        <p class="hint-title">当前身份为「个体」，无法邀请协作者</p>
        <p class="hint-desc">请前往个人中心将身份升级为「商户」或「企业」后再邀请</p>
        <a-button type="primary" size="small" @click="goToProfile">
          <UserOutlined />
          前往个人中心
        </a-button>
      </div>
    </div>

    <!-- 好友列表选择 -->
    <template v-else>
      <!-- 已有协作者展示 -->
      <div v-if="collaborators.length > 0" class="collab-section">
        <div class="section-label">当前协作者</div>
        <div class="collab-list">
          <div v-for="c in collaborators" :key="c.id" class="collab-item">
            <a-avatar :size="28" :src="c.userAvatar">
              {{ c.userName?.charAt(0) || 'U' }}
            </a-avatar>
            <span class="collab-name">{{ c.userName }}</span>
            <span class="collab-role">{{ c.role === 'viewer' ? '查看者' : '编辑者' }}</span>
            <a-popconfirm title="确定移除该协作者？" @confirm="doRemoveCollab(c)">
              <a-button type="text" size="small" danger>
                <CloseOutlined />
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>

      <!-- 好友选择列表 -->
      <div class="collab-section">
        <div class="section-label">从好友中邀请</div>
        <div v-if="availableFriends.length === 0" class="empty-hint">
          <TeamOutlined class="empty-icon" />
          <p>暂无可邀请的好友</p>
        </div>
        <div v-else class="friend-select-list">
          <div
            v-for="friend in availableFriends"
            :key="friend.friendId"
            class="friend-select-item"
          >
            <a-avatar :size="34" :src="friend.friendAvatar">
              {{ friend.friendName?.charAt(0) || 'U' }}
            </a-avatar>
            <div class="friend-select-info">
              <span class="friend-select-name">{{ friend.friendName }}</span>
            </div>
            <a-button
              type="primary"
              size="small"
              :loading="invitingId === friend.friendId"
              @click="doInvite(friend)"
            >
              <PlusOutlined />
              邀请
            </a-button>
          </div>
        </div>
      </div>
    </template>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CloseOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  TeamOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useChatStore } from '@/stores/chatStore'
import { inviteCollaborator, listCollaborators, removeCollaborator } from '@/api/collaboratorController'

interface Props {
  open: boolean
  appId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:open'])
const router = useRouter()
const loginUserStore = useLoginUserStore()
const chatStore = useChatStore()

/** 控制弹窗可见性 */
const visible = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val)
})

/** 当前产物的协作者列表 */
const collaborators = ref<API.AppCollaboratorVO[]>([])
/** 正在邀请的好友id（loading状态） */
const invitingId = ref<number | null>(null)

/** 当前用户是否为个人身份 */
const isIndividual = computed(() => {
  const identity = loginUserStore.loginUser.userIdentity
  return !identity || identity === 'individual'
})

/**
 * 可邀请的好友列表：排除已是协作者和待确认的好友
 */
const availableFriends = computed(() => {
  const collabUserIds = new Set(collaborators.value.map((c) => c.userId))
  return chatStore.friends.filter((f) => f.friendId && !collabUserIds.has(f.friendId))
})

/** 弹窗打开时加载数据 */
watch(
  () => props.open,
  async (val) => {
    if (val && props.appId) {
      await loadCollaborators()
      await chatStore.fetchFriends()
    }
  }
)

/** 加载当前产物的协作者列表 */
async function loadCollaborators() {
  if (!props.appId) return
  try {
    const res = await listCollaborators({ appId: props.appId })
    if (res.data.code === 0 && res.data.data) {
      collaborators.value = res.data.data
    }
  } catch (e) {
    console.error('获取协作者列表失败', e)
  }
}

/** 邀请好友为协作者 */
async function doInvite(friend: API.UserFriendshipVO) {
  if (!props.appId || !friend.friendId) return
  invitingId.value = friend.friendId
  try {
    const res = await inviteCollaborator({
      appId: props.appId,
      userId: friend.friendId,
      role: 'editor'
    })
    if (res.data.code === 0) {
      message.success('邀请已发送')
      visible.value = false
    } else {
      message.error(res.data.message || '邀请失败')
    }
  } catch (e) {
    message.error('邀请协作者失败')
  } finally {
    invitingId.value = null
  }
}

/** 移除协作者 */
async function doRemoveCollab(collab: API.AppCollaboratorVO) {
  if (!props.appId || !collab.userId) return
  try {
    const res = await removeCollaborator({ appId: props.appId, userId: collab.userId })
    if (res.data.code === 0) {
      message.success('已移除协作者')
      await loadCollaborators()
    } else {
      message.error(res.data.message || '移除失败')
    }
  } catch (e) {
    message.error('移除协作者失败')
  }
}

/** 跳转到个人中心修改身份 */
function goToProfile() {
  visible.value = false
  router.push('/user/userCenter')
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
/* 身份提示 */
.identity-hint {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
}

.hint-icon {
  font-size: 20px;
  color: #faad14;
  flex-shrink: 0;
  margin-top: 2px;
}

.hint-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hint-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin: 0;
}

.hint-desc {
  font-size: 12px;
  color: #999;
  margin: 0 0 4px 0;
}

/* 协作者区块 */
.collab-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 11px;
  color: #bbb;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 0 0 8px;
  font-weight: 500;
}

/* 已有协作者列表 */
.collab-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.collab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #fafafa;
  border-radius: 8px;
}

.collab-name {
  font-size: 13px;
  color: #1a1a1a;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collab-role {
  font-size: 11px;
  color: #999;
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 4px;
}

/* 好友选择列表 */
.friend-select-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}

.friend-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.15s;
}

.friend-select-item:hover {
  background: #fafafa;
}

.friend-select-info {
  flex: 1;
  min-width: 0;
}

.friend-select-name {
  font-size: 13px;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空提示 */
.empty-hint {
  text-align: center;
  padding: 24px 0;
  color: #999;
}

.empty-hint .empty-icon {
  font-size: 28px;
  color: #ddd;
  display: block;
  margin-bottom: 8px;
}

.empty-hint p {
  margin: 0;
  font-size: 13px;
}
</style>
