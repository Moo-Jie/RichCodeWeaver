<template>
  <div class="message-page">
    <header class="page-header">
      <div class="page-heading">
        <div class="heading-line">
          <h1>消息中心</h1>
          <span :class="['connection-chip', communicationStore.wsState]">
            <span class="connection-dot"></span>
            {{ connectionLabel }}
          </span>
        </div>
        <p>集中处理会话、好友关系与协作通知，实时消息会自动同步。</p>
      </div>
      <a-button class="refresh-button" :loading="refreshing" @click="refreshCurrent">
        <ReloadOutlined />
        刷新
      </a-button>
    </header>

    <nav class="section-tabs" role="tablist" aria-label="消息中心分类">
      <button
        v-for="tabItem in tabs"
        :key="tabItem.key"
        type="button"
        role="tab"
        :aria-selected="activeTab === tabItem.key"
        :class="['section-tab', { active: activeTab === tabItem.key }]"
        @click="changeTab(tabItem.key)"
      >
        <component :is="tabItem.icon" />
        <span>{{ tabItem.label }}</span>
        <span v-if="tabItem.count" class="tab-count">{{
          tabItem.count > 99 ? '99+' : tabItem.count
        }}</span>
      </button>
    </nav>

    <section :class="['hub-body', { 'has-detail': hasDetail, 'is-empty-mode': isEmptyMode }]">
      <aside class="hub-list">
        <template v-if="activeTab === 'messages'">
          <div class="list-top">
            <div class="list-title-row">
              <div>
                <h2>全部会话</h2>
                <span>{{ communicationStore.conversations.length }} 个联系人</span>
              </div>
            </div>
            <a-input v-model:value="conversationKeyword" allow-clear placeholder="搜索会话">
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>

          <div class="list-scroll">
            <div
              v-if="
                communicationStore.conversationsLoading &&
                communicationStore.conversations.length === 0
              "
              class="list-loading"
            >
              <a-skeleton v-for="index in 4" :key="index" avatar :paragraph="{ rows: 1 }" />
            </div>
            <div v-else-if="communicationStore.conversationsError" class="compact-state">
              <ExclamationCircleOutlined />
              <strong>会话加载失败</strong>
              <span>{{ communicationStore.conversationsError }}</span>
              <a-button size="small" @click="communicationStore.fetchConversations()"
                >重试</a-button
              >
            </div>
            <div v-else-if="filteredConversations.length === 0" class="compact-state">
              <MessageOutlined />
              <strong>{{ conversationKeyword ? '没有匹配的会话' : '还没有会话' }}</strong>
              <span>{{ conversationKeyword ? '换个关键词试试' : '从好友列表发起第一段对话' }}</span>
              <a-button v-if="!conversationKeyword" size="small" @click="changeTab('friends')"
                >查看好友</a-button
              >
            </div>
            <button
              v-for="conversation in filteredConversations"
              v-else
              :key="conversation.id"
              type="button"
              :class="[
                'conversation-row',
                {
                  active:
                    String(conversation.id) === String(communicationStore.activeConversationId),
                },
              ]"
              @click="openConversation(conversation)"
            >
              <UserAvatar
                :src="conversation.targetUserAvatar"
                :name="conversation.targetUserName"
                :size="38"
              />
              <span class="conversation-copy">
                <span class="conversation-title-line">
                  <strong>{{ conversation.targetUserName || '未命名用户' }}</strong>
                  <time>{{ formatConversationTime(conversation.lastMessageTime) }}</time>
                </span>
                <span class="conversation-preview-line">
                  <span>{{ getConversationPreview(conversation) }}</span>
                  <span v-if="conversation.unreadCount" class="unread-dot">
                    {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
                  </span>
                </span>
              </span>
            </button>
          </div>
        </template>

        <template v-else-if="activeTab === 'friends'">
          <div class="list-top">
            <div class="list-title-row">
              <div>
                <h2>好友</h2>
                <span>{{ communicationStore.friends.length }} 位联系人</span>
              </div>
              <a-button size="small" type="primary" @click="openAddFriend">
                <UserAddOutlined />
                添加
              </a-button>
            </div>
            <a-input v-model:value="friendKeyword" allow-clear placeholder="搜索好友">
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>

          <div class="list-scroll">
            <div
              v-if="communicationStore.friendsLoading && communicationStore.friends.length === 0"
              class="list-loading"
            >
              <a-skeleton v-for="index in 4" :key="index" avatar :paragraph="{ rows: 1 }" />
            </div>
            <div v-else-if="communicationStore.friendsError" class="compact-state">
              <ExclamationCircleOutlined />
              <strong>好友加载失败</strong>
              <span>{{ communicationStore.friendsError }}</span>
              <a-button size="small" @click="communicationStore.fetchFriends()">重试</a-button>
            </div>
            <div v-else-if="filteredFriends.length === 0" class="compact-state">
              <TeamOutlined />
              <strong>{{ friendKeyword ? '没有匹配的好友' : '好友列表为空' }}</strong>
              <span>{{ friendKeyword ? '换个名字试试' : '搜索并添加你认识的用户' }}</span>
              <a-button v-if="!friendKeyword" size="small" type="primary" @click="openAddFriend"
                >添加好友</a-button
              >
            </div>
            <button
              v-for="friend in filteredFriends"
              v-else
              :key="friend.id"
              type="button"
              :class="[
                'friend-row',
                { active: String(friend.friendId) === String(activeFriendId) },
              ]"
              @click="selectFriend(friend)"
            >
              <UserAvatar :src="friend.friendAvatar" :name="friend.friendName" :size="38" />
              <span class="friend-copy">
                <strong>{{ friend.friendName || '未命名用户' }}</strong>
                <span>{{ friend.remark || '好友' }}</span>
              </span>
              <MessageOutlined class="row-arrow" />
            </button>
          </div>
        </template>

        <template v-else>
          <div class="list-top">
            <div class="list-title-row">
              <div>
                <h2>通知</h2>
                <span>{{ communicationStore.notificationItems.length }} 条消息</span>
              </div>
            </div>
          </div>

          <div class="list-scroll">
            <div
              v-if="communicationStore.pendingLoading || communicationStore.collabLoading"
              class="list-loading"
            >
              <a-skeleton v-for="index in 3" :key="index" avatar :paragraph="{ rows: 1 }" />
            </div>
            <div
              v-else-if="communicationStore.pendingError || communicationStore.collabError"
              class="compact-state"
            >
              <ExclamationCircleOutlined />
              <strong>通知加载失败</strong>
              <span>{{ communicationStore.pendingError || communicationStore.collabError }}</span>
              <a-button size="small" @click="refreshNotifications">重试</a-button>
            </div>
            <div
              v-else-if="communicationStore.notificationItems.length === 0"
              class="compact-state"
            >
              <BellOutlined />
              <strong>暂无通知</strong>
              <span>好友申请和协作邀请会出现在这里</span>
            </div>
            <button
              v-for="notice in communicationStore.notificationItems"
              v-else
              :key="notice.key"
              type="button"
              :class="['notice-row', { active: notice.key === activeNotificationKey }]"
              @click="selectNotice(notice)"
            >
              <UserAvatar :src="notice.avatar" :name="notice.title" :size="38" />
              <span class="notice-copy">
                <strong>{{ notice.title }}</strong>
                <span>{{ notice.description }}</span>
                <time>{{ formatConversationTime(notice.time) }}</time>
              </span>
              <span v-if="isPendingNotice(notice)" class="pending-mark">待处理</span>
            </button>
          </div>
        </template>
      </aside>

      <main class="hub-main">
        <template v-if="activeTab === 'messages'">
          <div v-if="communicationStore.activePeer" class="chat-shell">
            <header class="chat-header">
              <button type="button" class="mobile-back" aria-label="返回列表" @click="backToList">
                <ArrowLeftOutlined />
              </button>
              <UserAvatar
                :src="communicationStore.activePeer.avatar"
                :name="communicationStore.activePeer.name"
                :size="36"
              />
              <div class="chat-heading">
                <strong>{{ communicationStore.activePeer.name }}</strong>
                <span>{{
                  communicationStore.wsState === 'open' ? '实时连接已就绪' : '消息将通过服务端同步'
                }}</span>
              </div>
              <div class="chat-header-actions">
                <span v-if="!canSendMessage" class="relationship-hint"
                  >当前不是好友，仅可查看历史</span
                >
              </div>
            </header>

            <div
              ref="messagesContainer"
              class="message-scroll"
              @scroll.passive="handleMessageScroll"
            >
              <div class="message-scroll-inner">
                <div v-if="communicationStore.hasMoreMessages" class="load-more-row">
                  <a-button
                    size="small"
                    :loading="communicationStore.messagesLoading"
                    @click="loadOlderMessages"
                  >
                    加载更早消息
                  </a-button>
                </div>

                <div
                  v-if="
                    communicationStore.messagesLoading && !communicationStore.currentMessages.length
                  "
                  class="message-loading"
                >
                  <a-spin size="small" />
                  <span>正在加载消息</span>
                </div>

                <div
                  v-else-if="
                    communicationStore.messagesError && !communicationStore.currentMessages.length
                  "
                  class="main-state"
                >
                  <ExclamationCircleOutlined />
                  <strong>消息加载失败</strong>
                  <span>{{ communicationStore.messagesError }}</span>
                  <a-button size="small" @click="reloadActiveMessages">重新加载</a-button>
                </div>

                <div
                  v-else-if="!communicationStore.currentMessages.length"
                  class="main-state chat-empty"
                >
                  <MessageOutlined />
                  <strong>{{
                    communicationStore.pendingChatFriendId ? '开始一段新对话' : '暂无消息'
                  }}</strong>
                  <span>输入一段消息，内容只对会话双方可见。</span>
                </div>

                <template v-for="row in messageRows" :key="row.key">
                  <div v-if="row.type === 'date'" class="date-divider">
                    <span>{{ row.label }}</span>
                  </div>
                  <ChatMessageItem
                    v-else
                    :msg="row.message"
                    :current-user-id="currentUser?.id"
                    :current-user-name="currentUser?.userName"
                    :current-user-avatar="currentUser?.userAvatar"
                    :peer-name="communicationStore.activePeer?.name"
                    :peer-avatar="communicationStore.activePeer?.avatar"
                    @retry="retryFailedMessage"
                    @collab-action="handleCollabAction"
                    @open-app="openForwardedApp"
                  />
                </template>
              </div>
            </div>

            <button
              v-if="communicationStore.unseenRealtimeCount && !isNearBottom"
              type="button"
              class="new-message-pill"
              @click="scrollToLatest(true)"
            >
              {{ communicationStore.unseenRealtimeCount }} 条新消息
              <ArrowDownOutlined />
            </button>

            <footer class="composer-wrap">
              <div v-if="!canSendMessage" class="composer-disabled">
                好友关系已解除，历史消息仍可查看，但无法继续发送。
              </div>
              <div v-else class="composer">
                <a-textarea
                  v-model:value="composerText"
                  :maxlength="5000"
                  :auto-size="{ minRows: 1, maxRows: 5 }"
                  placeholder="输入消息，Enter 发送，Shift + Enter 换行"
                  @keydown.enter="handleComposerEnter"
                />
                <button
                  type="button"
                  class="send-button"
                  :disabled="!composerText.trim() || sending"
                  aria-label="发送消息"
                  @click="sendCurrentMessage"
                >
                  <LoadingOutlined v-if="sending" spin />
                  <SendOutlined v-else />
                </button>
              </div>
              <div v-if="composerText.length > 3500" class="composer-count">
                {{ composerText.length }} / 5000
              </div>
            </footer>
          </div>

          <div v-else class="main-state full-main-state">
            <div class="empty-visual"><MessageOutlined /></div>
            <strong>{{
              communicationStore.conversations.length ? '选择一个会话' : '还没有会话'
            }}</strong>
            <span>{{
              communicationStore.conversations.length
                ? '左侧会展示最近的对话和未读消息，选择后即可继续沟通。'
                : '从好友列表发起第一段对话，消息会集中显示在这里。'
            }}</span>
            <a-button
              v-if="!communicationStore.conversations.length"
              size="small"
              @click="changeTab('friends')"
              >查看好友</a-button
            >
          </div>
        </template>

        <template v-else-if="activeTab === 'friends'">
          <div v-if="addFriendOpen" class="detail-panel add-friend-panel">
            <button type="button" class="mobile-back" aria-label="返回列表" @click="closeAddFriend">
              <ArrowLeftOutlined />
            </button>
            <div class="detail-heading">
              <div>
                <span class="detail-kicker">联系人</span>
                <h2>添加好友</h2>
                <p>搜索用户名，发送申请后可在这里查看处理结果。</p>
              </div>
            </div>
            <form class="friend-search-form" @submit.prevent="searchFriendUsers">
              <a-input
                v-model:value="userSearchKeyword"
                allow-clear
                size="large"
                placeholder="输入用户名"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
              <a-button
                type="primary"
                size="large"
                :loading="userSearching"
                @click="searchFriendUsers"
                >搜索</a-button
              >
            </form>

            <div v-if="userSearching" class="search-state">
              <a-spin size="small" />
              <span>正在查找用户</span>
            </div>
            <div v-else-if="userSearchDone && userSearchResults.length === 0" class="search-state">
              <SearchOutlined />
              <strong>没有找到匹配用户</strong>
              <span>请检查用户名后重试。</span>
            </div>
            <div v-else class="search-results">
              <div v-for="user in userSearchResults" :key="user.id" class="search-result-row">
                <UserAvatar :src="user.userAvatar" :name="user.userName" :size="42" />
                <div class="search-result-copy">
                  <strong>{{ user.userName || '未命名用户' }}</strong>
                  <span>{{ user.userProfile || 'RichCodeWeaver 用户' }}</span>
                </div>
                <a-button
                  size="small"
                  :type="isFriendUser(user.id) ? 'default' : 'primary'"
                  :disabled="isFriendUser(user.id) || actionBusyKey === `add-${user.id}`"
                  :loading="actionBusyKey === `add-${user.id}`"
                  @click="addFriend(user)"
                >
                  {{ isFriendUser(user.id) ? '已是好友' : '添加好友' }}
                </a-button>
              </div>
            </div>
          </div>

          <div v-else-if="selectedFriend" class="detail-panel friend-detail-panel">
            <button
              type="button"
              class="mobile-back"
              aria-label="返回列表"
              @click="activeFriendId = null"
            >
              <ArrowLeftOutlined />
            </button>
            <div class="friend-profile">
              <UserAvatar
                :src="selectedFriend.friendAvatar"
                :name="selectedFriend.friendName"
                :size="72"
              />
              <div>
                <span class="detail-kicker">好友</span>
                <h2>{{ selectedFriend.friendName || '未命名用户' }}</h2>
                <p>{{ selectedFriend.remark || '你们已经是好友，可以开始实时对话。' }}</p>
              </div>
            </div>
            <div class="profile-actions">
              <a-button type="primary" @click="startChatWithSelectedFriend">
                <MessageOutlined />
                发消息
              </a-button>
              <a-popconfirm
                title="删除后将解除双向好友关系，确定继续吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="removeSelectedFriend"
              >
                <a-button :loading="actionBusyKey === 'remove-friend'">
                  <DeleteOutlined />
                  删除好友
                </a-button>
              </a-popconfirm>
            </div>

            <div class="friend-overview-grid">
              <section class="overview-card">
                <div class="overview-card-head">
                  <UserOutlined />
                  <span>关系信息</span>
                </div>
                <dl>
                  <div>
                    <dt>好友状态</dt>
                    <dd>已建立双向好友关系</dd>
                  </div>
                  <div>
                    <dt>备注</dt>
                    <dd>{{ selectedFriend.remark || '暂无备注' }}</dd>
                  </div>
                  <div>
                    <dt>建立时间</dt>
                    <dd>{{ formatFullDateTime(selectedFriend.createTime) }}</dd>
                  </div>
                </dl>
              </section>
              <section class="overview-card recent-chat-card">
                <div class="overview-card-head">
                  <MessageOutlined />
                  <span>最近会话</span>
                </div>
                <template v-if="selectedFriendConversation">
                  <p class="recent-chat-copy">
                    {{ getConversationPreview(selectedFriendConversation) }}
                  </p>
                  <div class="recent-chat-meta">
                    <span>{{
                      formatFullDateTime(selectedFriendConversation.lastMessageTime)
                    }}</span>
                    <span v-if="selectedFriendConversation.unreadCount"
                      >{{ selectedFriendConversation.unreadCount }} 条未读</span
                    >
                  </div>
                  <a-button size="small" @click="startChatWithSelectedFriend">继续对话</a-button>
                </template>
                <template v-else>
                  <p class="recent-chat-copy muted">还没有聊天记录，发送第一条消息开始沟通。</p>
                  <a-button size="small" type="primary" @click="startChatWithSelectedFriend"
                    >开始对话</a-button
                  >
                </template>
              </section>
            </div>
            <div class="detail-note">
              <MessageOutlined />
              <div>
                <strong>消息可追溯</strong>
                <span>删除好友不会删除已有聊天记录，但双方将无法继续发送新消息。</span>
              </div>
            </div>
          </div>

          <div v-else class="main-state full-main-state">
            <div class="empty-visual"><TeamOutlined /></div>
            <strong>{{
              communicationStore.friends.length ? '选择一位好友' : '添加你的第一位好友'
            }}</strong>
            <span>在选择好友后查看资料、发起会话或管理关系。</span>
            <a-button size="small" type="primary" @click="openAddFriend">添加好友</a-button>
          </div>
        </template>

        <template v-else>
          <div v-if="activeNotice" class="detail-panel notification-detail">
            <button
              type="button"
              class="mobile-back"
              aria-label="返回列表"
              @click="activeNotificationKey = ''"
            >
              <ArrowLeftOutlined />
            </button>
            <div class="notification-title-block">
              <span class="detail-kicker">通知详情</span>
              <h2>{{ activeNotice.title }}</h2>
              <p>{{ activeNotice.description }}</p>
            </div>

            <div class="notification-meta-card">
              <div>
                <span>类型</span>
                <strong>{{ noticeTypeLabel(activeNotice) }}</strong>
              </div>
              <div>
                <span>时间</span>
                <strong>{{ formatFullDateTime(activeNotice.time) }}</strong>
              </div>
              <div v-if="activeNotice.appName">
                <span>关联产物</span>
                <strong>{{ activeNotice.appName }}</strong>
              </div>
            </div>

            <div v-if="isPendingNotice(activeNotice)" class="notification-actions">
              <a-button
                :loading="actionBusyKey === `notice-reject-${activeNotice.key}`"
                @click="handleNoticeAction(activeNotice, 2)"
                >拒绝</a-button
              >
              <a-button
                type="primary"
                :loading="actionBusyKey === `notice-accept-${activeNotice.key}`"
                @click="handleNoticeAction(activeNotice, 1)"
                >接受</a-button
              >
            </div>
            <div v-else class="resolved-note"><CheckOutlined /> 此通知已完成处理</div>
          </div>

          <div v-else class="main-state full-main-state notification-empty-state">
            <div class="empty-visual"><BellOutlined /></div>
            <strong>{{
              communicationStore.notificationItems.length ? '选择一条通知' : '通知中心很安静'
            }}</strong>
            <span>好友申请和协作邀请会在这里集中处理，并实时同步处理状态。</span>
            <div v-if="!communicationStore.notificationItems.length" class="empty-hint-grid">
              <span><TeamOutlined />好友申请</span>
              <span><AppstoreOutlined />协作邀请</span>
              <span><CheckOutlined />状态回执</span>
            </div>
          </div>
        </template>
      </main>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  ArrowDownOutlined,
  ArrowLeftOutlined,
  BellOutlined,
  CheckOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined,
  MessageOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import ChatMessageItem from '@/components/chat/ChatMessageItem.vue'
import UserAvatar from '@/components/chat/UserAvatar.vue'
import {
  useCommunicationStore,
  type CommunicationNotice,
  type CommunicationTab,
  type LocalChatMessage,
} from '@/stores/communicationStore'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  handleFriendRequest,
  removeFriend,
  searchUsers,
  sendFriendRequest,
} from '@/api/friendController'
import { handleCollabInvitation } from '@/api/collaboratorController'

type MessageRow =
  | { type: 'date'; key: string; label: string }
  | { type: 'message'; key: string; message: LocalChatMessage }

const communicationStore = useCommunicationStore()
const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()

const activeTab = ref<CommunicationTab>('messages')
const activeFriendId = ref<number | null>(null)
const activeNotificationKey = ref('')
const addFriendOpen = ref(false)
const conversationKeyword = ref('')
const friendKeyword = ref('')
const userSearchKeyword = ref('')
const userSearchResults = ref<API.UserVO[]>([])
const userSearching = ref(false)
const userSearchDone = ref(false)
const actionBusyKey = ref('')
const composerText = ref('')
const sending = ref(false)
const refreshing = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const isNearBottom = ref(true)
let searchRequestId = 0
let conversationOpenSequence = 0
let adjustingHistory = false

const currentUser = computed(() => loginUserStore.loginUser)
const tabs = computed(() => [
  {
    key: 'messages' as const,
    label: '消息',
    icon: MessageOutlined,
    count: communicationStore.totalUnreadCount,
  },
  { key: 'friends' as const, label: '好友', icon: TeamOutlined, count: 0 },
  {
    key: 'notifications' as const,
    label: '通知',
    icon: BellOutlined,
    count: communicationStore.notificationCount,
  },
])
const connectionLabel = computed(() => {
  if (communicationStore.wsState === 'open') return '实时在线'
  if (communicationStore.wsState === 'reconnecting') return '正在重连'
  if (communicationStore.wsState === 'connecting') return '正在连接'
  return '离线可用'
})
const filteredConversations = computed(() => {
  const keyword = conversationKeyword.value.trim().toLowerCase()
  if (!keyword) return communicationStore.conversations
  return communicationStore.conversations.filter((conversation) =>
    `${conversation.targetUserName || ''} ${conversation.lastMessageContent || ''}`
      .toLowerCase()
      .includes(keyword),
  )
})
const filteredFriends = computed(() => {
  const keyword = friendKeyword.value.trim().toLowerCase()
  if (!keyword) return communicationStore.friends
  return communicationStore.friends.filter((friend) =>
    `${friend.friendName || ''} ${friend.remark || ''}`.toLowerCase().includes(keyword),
  )
})
const selectedFriend = computed(
  () =>
    communicationStore.friends.find(
      (friend) => String(friend.friendId) === String(activeFriendId.value),
    ) || null,
)
const selectedFriendConversation = computed(
  () =>
    communicationStore.conversations.find(
      (conversation) => String(conversation.targetUserId) === String(activeFriendId.value),
    ) || null,
)
const isEmptyMode = computed(
  () =>
    (activeTab.value === 'messages' &&
      !communicationStore.conversationsLoading &&
      !communicationStore.conversations.length) ||
    (activeTab.value === 'notifications' &&
      !communicationStore.pendingLoading &&
      !communicationStore.collabLoading &&
      !communicationStore.notificationItems.length),
)
const activeNotice = computed(
  () =>
    communicationStore.notificationItems.find((item) => item.key === activeNotificationKey.value) ||
    null,
)
const canSendMessage = computed(() => {
  const peerId = communicationStore.activePeer?.id
  if (!peerId) return false
  return communicationStore.friends.some((friend) => String(friend.friendId) === String(peerId))
})
const hasDetail = computed(() => {
  if (activeTab.value === 'messages') return Boolean(communicationStore.activePeer)
  if (activeTab.value === 'friends') return addFriendOpen.value || Boolean(selectedFriend.value)
  return Boolean(activeNotice.value)
})
const messageRows = computed<MessageRow[]>(() => {
  const rows: MessageRow[] = []
  let previousDate = ''
  for (const item of communicationStore.currentMessages) {
    const date = item.createTime ? new Date(item.createTime) : null
    const dateKey = date && Number.isFinite(date.getTime()) ? date.toDateString() : 'unknown'
    if (dateKey !== previousDate) {
      rows.push({
        type: 'date',
        key: `date-${dateKey}-${item.id || item.clientMessageId || rows.length}`,
        label: formatDateDivider(item.createTime),
      })
      previousDate = dateKey
    }
    rows.push({
      type: 'message',
      key: item.id ? `message-${item.id}` : item.clientMessageId || `local-${rows.length}`,
      message: item,
    })
  }
  return rows
})

function applyRouteState() {
  const routeTab = String(route.query.tab || '')
  activeTab.value =
    routeTab === 'friends' ? 'friends' : routeTab === 'notifications' ? 'notifications' : 'messages'
  const conversationId = Number(route.query.conversationId)
  const friendId = Number(route.query.friendId)
  const notificationKey = String(route.query.notification || '')
  if (
    activeTab.value === 'messages' &&
    !(Number.isFinite(conversationId) && conversationId > 0) &&
    !(Number.isFinite(friendId) && friendId > 0)
  ) {
    conversationOpenSequence += 1
    communicationStore.clearActiveConversation()
  }
  const addFriendQuery = route.query.add === '1'
  if (activeTab.value === 'messages' && Number.isFinite(conversationId) && conversationId > 0) {
    const conversation = communicationStore.conversations.find(
      (item) => String(item.id) === String(conversationId),
    )
    if (
      conversation &&
      String(communicationStore.activeConversationId) !== String(conversationId)
    ) {
      void openConversation(conversation, false)
    }
  } else if (activeTab.value === 'messages' && Number.isFinite(friendId) && friendId > 0) {
    const existing = communicationStore.startChatWithFriend(friendId)
    if (existing?.id) void openConversation(existing, false)
  }
  if (activeTab.value === 'friends' && addFriendQuery) {
    activeFriendId.value = null
    addFriendOpen.value = true
  } else if (activeTab.value === 'friends' && Number.isFinite(friendId) && friendId > 0) {
    activeFriendId.value = friendId
    addFriendOpen.value = false
  }
  if (activeTab.value === 'notifications' && notificationKey) {
    activeNotificationKey.value = notificationKey
  }
}

async function changeTab(tab: CommunicationTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  addFriendOpen.value = false
  activeNotificationKey.value = ''
  if (tab !== 'friends') activeFriendId.value = null
  if (tab !== 'messages') communicationStore.activeConversationVisible = false
  await router.push({ path: '/messages', query: tab === 'messages' ? {} : { tab } })
  if (tab === 'messages' && !communicationStore.activeConversationId) {
    const firstConversation = communicationStore.conversations[0]
    if (firstConversation) {
      await openConversation(firstConversation, false)
    }
  }
}

function routeQueryWith(extra: Record<string, string | undefined>) {
  return Object.fromEntries(
    Object.entries(extra).filter(([, value]) => value !== undefined && value !== ''),
  )
}

async function openConversation(conversation: API.ChatConversationVO, updateRoute = true) {
  if (!conversation.id) return
  const sequence = ++conversationOpenSequence
  activeNotificationKey.value = ''
  await communicationStore.selectConversation(conversation.id)
  if (sequence !== conversationOpenSequence) return
  communicationStore.activeConversationVisible = true
  const loaded = await communicationStore.loadMessages(conversation.id, 1)
  if (sequence !== conversationOpenSequence) return
  if (loaded) {
    await nextTick()
    if (sequence !== conversationOpenSequence) return
    await scrollToLatest(false)
  }
  if (updateRoute && sequence === conversationOpenSequence) {
    await router.push({
      path: '/messages',
      query: routeQueryWith({ conversationId: String(conversation.id) }),
    })
  }
}

async function reloadActiveMessages() {
  if (!communicationStore.activeConversationId) return
  const loaded = await communicationStore.loadMessages(communicationStore.activeConversationId, 1)
  if (loaded) {
    await nextTick()
    scrollToLatest(false)
  }
}

async function loadOlderMessages() {
  const container = messagesContainer.value
  if (!container) return
  adjustingHistory = true
  const previousHeight = container.scrollHeight
  const previousTop = container.scrollTop
  try {
    const loaded = await communicationStore.loadOlderMessages()
    if (!loaded) return
    await nextTick()
    container.scrollTop = container.scrollHeight - previousHeight + previousTop
  } finally {
    adjustingHistory = false
  }
}

function handleMessageScroll() {
  const container = messagesContainer.value
  if (!container) return
  isNearBottom.value = container.scrollHeight - container.scrollTop - container.clientHeight < 120
  communicationStore.activeConversationAtBottom = isNearBottom.value
  if (isNearBottom.value) {
    communicationStore.unseenRealtimeCount = 0
    communicationStore.scheduleMarkRead()
  }
}

async function scrollToLatest(clearUnseen = true) {
  await nextTick()
  const container = messagesContainer.value
  if (!container) return
  container.scrollTop = container.scrollHeight
  isNearBottom.value = true
  if (clearUnseen) communicationStore.unseenRealtimeCount = 0
}

async function handleComposerEnter(event: KeyboardEvent) {
  if (event.isComposing || event.keyCode === 229) return
  if (event.shiftKey) return
  event.preventDefault()
  await sendCurrentMessage()
}

async function sendCurrentMessage() {
  const peerId = communicationStore.activePeer?.id
  const content = composerText.value.trim()
  if (!peerId || !content || sending.value || !canSendMessage.value) return
  sending.value = true
  composerText.value = ''
  try {
    const confirmed = await communicationStore.sendChatMessage(peerId, content)
    if (confirmed.conversationId && communicationStore.activeConversationId) {
      await router.replace({
        path: '/messages',
        query: { conversationId: String(communicationStore.activeConversationId) },
      })
    }
    await scrollToLatest(true)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '消息发送失败')
  } finally {
    sending.value = false
  }
}

async function retryFailedMessage(item: LocalChatMessage) {
  try {
    await communicationStore.retryMessage(item)
    await scrollToLatest(true)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '重试失败')
  }
}

async function handleCollabAction(item: LocalChatMessage, action: number) {
  if (!item.collabId || actionBusyKey.value) return
  actionBusyKey.value = `collab-${item.collabId}`
  try {
    const response = await handleCollabInvitation({ id: item.collabId, action })
    if (response.data.code !== 0) throw new Error(response.data.message || '处理协作邀请失败')
    item.collabStatus = action === 1 ? 1 : 2
    await Promise.all([
      communicationStore.fetchPendingCollabInvitations(),
      communicationStore.fetchConversations(),
    ])
    message.success(action === 1 ? '已接受协作邀请' : '已拒绝协作邀请')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '处理协作邀请失败')
  } finally {
    actionBusyKey.value = ''
  }
}

function openForwardedApp(item: LocalChatMessage) {
  if (!item.appId) {
    message.warning('该分享缺少产物信息')
    return
  }
  void router.push(`/app/chat/${item.appId}`)
}

function selectFriend(friend: API.UserFriendshipVO) {
  activeFriendId.value = friend.friendId || null
  activeNotificationKey.value = ''
  addFriendOpen.value = false
  void router.push({
    path: '/messages',
    query: routeQueryWith({
      tab: 'friends',
      friendId: friend.friendId ? String(friend.friendId) : undefined,
    }),
  })
}

async function startChatWithSelectedFriend() {
  if (!selectedFriend.value?.friendId) return
  const existing = communicationStore.startChatWithFriend(selectedFriend.value.friendId)
  activeTab.value = 'messages'
  if (existing?.id) {
    await openConversation(existing)
    return
  }
  await router.push({
    path: '/messages',
    query: routeQueryWith({ friendId: String(selectedFriend.value.friendId) }),
  })
}

function openAddFriend() {
  addFriendOpen.value = true
  activeFriendId.value = null
  userSearchKeyword.value = ''
  userSearchResults.value = []
  userSearchDone.value = false
  void router.push({ path: '/messages', query: { tab: 'friends', add: '1' } })
}

function closeAddFriend() {
  addFriendOpen.value = false
  void router.push({ path: '/messages', query: { tab: 'friends' } })
}

async function searchFriendUsers() {
  if (userSearching.value) return
  const keyword = userSearchKeyword.value.trim()
  if (!keyword) {
    message.warning('请输入用户名')
    return
  }
  const requestId = ++searchRequestId
  userSearching.value = true
  userSearchDone.value = false
  userSearchResults.value = []
  try {
    const response = await searchUsers({ keyword })
    if (requestId !== searchRequestId) return
    if (response.data.code !== 0) throw new Error(response.data.message || '搜索失败')
    userSearchResults.value = response.data.data || []
  } catch (error) {
    if (requestId === searchRequestId) {
      userSearchResults.value = []
      message.error(error instanceof Error ? error.message : '搜索用户失败')
    }
  } finally {
    if (requestId === searchRequestId) {
      userSearching.value = false
      userSearchDone.value = true
    }
  }
}

function isFriendUser(userId?: number) {
  if (!userId) return false
  return communicationStore.friends.some((friend) => String(friend.friendId) === String(userId))
}

async function addFriend(user: API.UserVO) {
  if (!user.id || actionBusyKey.value) return
  actionBusyKey.value = `add-${user.id}`
  try {
    const response = await sendFriendRequest({ friendId: user.id, remark: '' })
    if (response.data.code !== 0) throw new Error(response.data.message || '好友申请发送失败')
    message.success('好友申请已发送')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '好友申请发送失败')
  } finally {
    actionBusyKey.value = ''
  }
}

async function removeSelectedFriend() {
  const friendId = selectedFriend.value?.friendId
  if (!friendId) return
  actionBusyKey.value = 'remove-friend'
  try {
    const response = await removeFriend({ friendId })
    if (response.data.code !== 0) throw new Error(response.data.message || '删除好友失败')
    activeFriendId.value = null
    await Promise.all([communicationStore.fetchFriends(), communicationStore.fetchConversations()])
    message.success('已删除好友')
    await router.replace({ path: '/messages', query: { tab: 'friends' } })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除好友失败')
  } finally {
    actionBusyKey.value = ''
  }
}

function selectNotice(notice: CommunicationNotice) {
  activeNotificationKey.value = notice.key
  void router.push({
    path: '/messages',
    query: { tab: 'notifications', notification: notice.key },
  })
}

function isPendingNotice(notice: CommunicationNotice) {
  return notice.action === 'pendingRequest' || notice.action === 'invite'
}

function noticeTypeLabel(notice: CommunicationNotice) {
  return notice.kind === 'collab' ? '协作邀请' : '好友申请'
}

async function handleNoticeAction(notice: CommunicationNotice, action: number) {
  if (!notice.referenceId) return
  const actionName = action === 1 ? 'accept' : 'reject'
  actionBusyKey.value = `notice-${actionName}-${notice.key}`
  try {
    const response =
      notice.kind === 'collab'
        ? await handleCollabInvitation({ id: notice.referenceId, action })
        : await handleFriendRequest({ id: notice.referenceId, action })
    if (response.data.code !== 0) throw new Error(response.data.message || '通知处理失败')
    await Promise.all([
      communicationStore.fetchPendingRequests(),
      communicationStore.fetchPendingCollabInvitations(),
      communicationStore.fetchFriends(),
      communicationStore.fetchConversations(),
    ])
    communicationStore.recordNotice({
      kind: notice.kind,
      action: action === 1 ? 'accepted' : 'rejected',
      title:
        notice.kind === 'collab'
          ? `已${action === 1 ? '接受' : '拒绝'}「${notice.appName || '数字产物'}」协作邀请`
          : `已${action === 1 ? '接受' : '拒绝'}${notice.title.replace(' 请求添加好友', '')}的好友申请`,
      description:
        action === 1 ? '状态已同步，相关关系已更新。' : '该申请已处理，可在后续通知中继续跟进。',
      time: new Date().toISOString(),
      avatar: notice.avatar,
      referenceId: notice.referenceId,
      appId: notice.appId,
      appName: notice.appName,
    })
    activeNotificationKey.value = ''
    message.success(action === 1 ? '已接受' : '已拒绝')
    await router.replace({ path: '/messages', query: { tab: 'notifications' } })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '通知处理失败')
  } finally {
    actionBusyKey.value = ''
  }
}

async function refreshNotifications() {
  await Promise.all([
    communicationStore.fetchPendingRequests(),
    communicationStore.fetchPendingCollabInvitations(),
  ])
}

async function refreshCurrent() {
  refreshing.value = true
  try {
    if (activeTab.value === 'messages') await communicationStore.fetchConversations()
    if (activeTab.value === 'friends') await communicationStore.fetchFriends()
    if (activeTab.value === 'notifications') await refreshNotifications()
    await communicationStore.fetchUnreadTotal()
  } finally {
    refreshing.value = false
  }
}

function backToList() {
  if (activeTab.value === 'messages') {
    communicationStore.clearActiveConversation()
    void router.push({ path: '/messages' })
    return
  }
  if (activeTab.value === 'friends') {
    activeFriendId.value = null
    addFriendOpen.value = false
    void router.push({ path: '/messages', query: { tab: 'friends' } })
    return
  }
  activeNotificationKey.value = ''
  void router.push({ path: '/messages', query: { tab: 'notifications' } })
}

function getConversationPreview(conversation: API.ChatConversationVO) {
  if (conversation.lastMessageType === 'collab_invite') return '[协作邀请]'
  if (conversation.lastMessageType === 'app_forward') return '[产物分享]'
  return conversation.lastMessageContent?.trim() || '暂无消息'
}

function formatConversationTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return ''
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function formatDateDivider(value?: string) {
  if (!value) return '较早消息'
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return '较早消息'
  const now = new Date()
  if (date.toDateString() === now.toDateString()) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function formatFullDateTime(value?: string) {
  if (!value) return '未知时间'
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return '未知时间'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

watch(
  () => communicationStore.currentMessages.length,
  async (length, oldLength) => {
    if (length <= oldLength || adjustingHistory) return
    if (isNearBottom.value || communicationStore.unseenRealtimeCount === 0) {
      await scrollToLatest(false)
    }
  },
)

watch(
  () => route.query,
  () => applyRouteState(),
  { deep: true },
)

onMounted(async () => {
  applyRouteState()
  await communicationStore.initCommunication()
  applyRouteState()
  if (
    activeTab.value === 'messages' &&
    !route.query.conversationId &&
    !route.query.friendId &&
    !communicationStore.activeConversationId &&
    communicationStore.conversations.length > 0
  ) {
    await openConversation(communicationStore.conversations[0], false)
  }
})

onBeforeUnmount(() => {
  communicationStore.activeConversationVisible = false
})
</script>

<style scoped>
:deep(.ant-spin-dot-item) {
  background-color: #1a1a1a !important;
}

:deep(.ant-btn-primary:not(:disabled)) {
  border-color: #1a1a1a !important;
  background: #1a1a1a !important;
  box-shadow: none !important;
}

:deep(.ant-btn-primary:not(:disabled):hover) {
  border-color: #333 !important;
  background: #333 !important;
}

.message-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 22px clamp(18px, 2.4vw, 34px) 20px;
  background: #f7f7f8;
  color: #1a1a1a;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  flex: 0 0 auto;
}

.page-heading h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.page-heading p {
  margin: 5px 0 0;
  color: #8b8b8b;
  font-size: 13px;
}

.heading-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.connection-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 7px;
  border: 1px solid #e8e8e8;
  border-radius: 999px;
  background: #fff;
  color: #777;
  font-size: 11px;
  line-height: 1.2;
}

.connection-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b8b8b8;
}

.connection-chip.open .connection-dot {
  background: #1a1a1a;
}

.connection-chip.connecting .connection-dot,
.connection-chip.reconnecting .connection-dot {
  background: #a58b54;
  animation: connection-pulse 1.2s ease-in-out infinite;
}

@keyframes connection-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

.refresh-button {
  min-width: 82px;
  border-color: #dedede;
  box-shadow: none;
}

.section-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  width: fit-content;
  padding: 4px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: #fff;
}

.section-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 13px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #777;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease;
}

.section-tab:hover {
  background: #f5f5f5;
  color: #1a1a1a;
}

.section-tab.active {
  background: #1a1a1a;
  color: #fff;
}

.section-tab:focus-visible,
.conversation-row:focus-visible,
.friend-row:focus-visible,
.notice-row:focus-visible,
.send-button:focus-visible,
.mobile-back:focus-visible {
  outline: 2px solid #4096ff;
  outline-offset: 2px;
}

.tab-count {
  min-width: 17px;
  height: 17px;
  padding: 1px 5px;
  border-radius: 999px;
  background: #ececec;
  color: #444;
  font-size: 10px;
  line-height: 15px;
  text-align: center;
}

.section-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.hub-body {
  display: grid;
  grid-template-columns: minmax(270px, 318px) minmax(0, 1fr);
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e7e7e7;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.035);
}

.hub-list {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  border-right: 1px solid #eeeeee;
  background: #fcfcfc;
}

.list-top {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 14px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.list-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.list-title-row h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
}

.list-title-row > div > span {
  display: block;
  margin-top: 3px;
  color: #999;
  font-size: 11px;
}

.list-top :deep(.ant-input-affix-wrapper) {
  min-height: 34px;
  border-color: #e5e5e5;
  border-radius: 8px;
  background: #fff;
  box-shadow: none;
}

.list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 7px;
  scrollbar-width: thin;
  scrollbar-color: #d8d8d8 transparent;
}

.list-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 10px;
}

.list-loading :deep(.ant-skeleton) {
  margin: 0;
}

.conversation-row,
.friend-row,
.notice-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.15s ease,
    box-shadow 0.15s ease;
}

.conversation-row:hover,
.friend-row:hover,
.notice-row:hover {
  background: #f4f4f4;
}

.conversation-row.active,
.friend-row.active,
.notice-row.active {
  background: #f0f0f0;
  box-shadow: inset 0 0 0 1px #e5e5e5;
}

.conversation-copy,
.friend-copy,
.notice-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.conversation-title-line,
.conversation-preview-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.conversation-title-line strong,
.friend-copy strong,
.notice-copy strong {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #222;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-title-line time {
  flex: 0 0 auto;
  color: #aaa;
  font-size: 10.5px;
}

.conversation-preview-line {
  margin-top: 4px;
}

.conversation-preview-line > span:first-child {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #909090;
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  min-width: 17px;
  height: 17px;
  padding: 1px 5px;
  border-radius: 999px;
  background: #1a1a1a;
  color: #fff;
  font-size: 9.5px;
  line-height: 15px;
  text-align: center;
}

.friend-copy span,
.notice-copy span {
  margin-top: 3px;
  overflow: hidden;
  color: #929292;
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-copy time {
  margin-top: 3px;
  color: #b2b2b2;
  font-size: 10px;
}

.row-arrow {
  color: #b7b7b7;
  font-size: 12px;
}

.pending-mark {
  align-self: flex-start;
  margin-top: 3px;
  padding: 2px 5px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  font-size: 9.5px;
}

.compact-state,
.search-state,
.main-state {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  color: #9a9a9a;
  text-align: center;
}

.compact-state {
  gap: 6px;
  min-height: 260px;
  padding: 28px 18px;
}

.compact-state > :first-child,
.search-state > :first-child {
  margin-bottom: 3px;
  color: #c4c4c4;
  font-size: 25px;
}

.compact-state strong,
.search-state strong,
.main-state strong {
  color: #4a4a4a;
  font-size: 13px;
  font-weight: 600;
}

.compact-state span,
.search-state span,
.main-state span {
  max-width: 320px;
  color: #999;
  font-size: 11.5px;
  line-height: 1.6;
}

.compact-state :deep(.ant-btn) {
  margin-top: 4px;
}

.hub-main {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: #fff;
}

.chat-shell {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  padding: 11px 18px;
  border-bottom: 1px solid #eeeeee;
  background: rgba(255, 255, 255, 0.96);
}

.chat-heading {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.chat-heading strong {
  overflow: hidden;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-heading span,
.relationship-hint {
  overflow: hidden;
  color: #9a9a9a;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relationship-hint {
  max-width: 210px;
  padding: 3px 7px;
  border-radius: 5px;
  background: #f5f5f5;
}

.message-scroll {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: #f8f8f8;
  scrollbar-width: thin;
  scrollbar-color: #d6d6d6 transparent;
}

.message-scroll-inner {
  display: flex;
  flex-direction: column;
  gap: 11px;
  min-height: 100%;
  padding: 18px clamp(16px, 3vw, 34px) 20px;
}

.message-loading,
.main-state {
  flex: 1;
  min-height: 260px;
}

.message-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 12px;
}

.main-state {
  gap: 8px;
  padding: 34px 20px;
}

.main-state > :first-child {
  color: #bdbdbd;
  font-size: 34px;
}

.full-main-state {
  flex: 1;
  min-height: 0;
}

.empty-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  margin-bottom: 3px;
  border: 1px solid #ececec;
  border-radius: 16px;
  background: #fafafa;
}

.date-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0;
  color: #aaa;
  font-size: 10px;
}

.date-divider::before,
.date-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e9e9e9;
}

.load-more-row {
  display: flex;
  justify-content: center;
  padding: 0 0 8px;
}

.new-message-pill {
  position: absolute;
  right: 22px;
  bottom: 91px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 10px;
  border: 1px solid #1a1a1a;
  border-radius: 999px;
  background: #1a1a1a;
  color: #fff;
  font: inherit;
  font-size: 11px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.composer-wrap {
  flex: 0 0 auto;
  padding: 12px clamp(14px, 2.5vw, 28px) 14px;
  border-top: 1px solid #eeeeee;
  background: #fff;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 9px;
}

.composer :deep(.ant-input) {
  min-height: 40px;
  padding: 9px 12px;
  border-color: #dfdfdf;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.55;
  box-shadow: none;
  resize: none;
}

.composer :deep(.ant-input:focus) {
  border-color: #9a9a9a;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04);
}

.send-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border: 1px solid #1a1a1a;
  border-radius: 10px;
  background: #1a1a1a;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.send-button:disabled {
  border-color: #e5e5e5;
  background: #f1f1f1;
  color: #b8b8b8;
  cursor: not-allowed;
}

.composer-disabled {
  padding: 10px 12px;
  border: 1px solid #eeeeee;
  border-radius: 9px;
  background: #fafafa;
  color: #8e8e8e;
  font-size: 12px;
}

.composer-count {
  margin-top: 5px;
  color: #999;
  font-size: 10px;
  text-align: right;
}
.detail-panel {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: 22px;
  overflow-y: auto;
  padding: 30px clamp(22px, 4vw, 48px);
}

.detail-heading h2,
.friend-profile h2,
.notification-title-block h2 {
  margin: 4px 0 0;
  color: #1a1a1a;
  font-size: 21px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.detail-heading p,
.friend-profile p,
.notification-title-block p {
  max-width: 560px;
  margin: 7px 0 0;
  color: #8f8f8f;
  font-size: 12.5px;
  line-height: 1.65;
}

.detail-kicker {
  color: #9b9b9b;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.friend-search-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  max-width: 720px;
}

.friend-search-form :deep(.ant-input-affix-wrapper) {
  border-radius: 9px;
  box-shadow: none;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 720px;
}

.search-result-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #eeeeee;
  border-radius: 10px;
  background: #fff;
}

.search-result-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.search-result-copy strong,
.search-result-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-copy strong {
  color: #222;
  font-size: 13px;
}

.search-result-copy span {
  color: #999;
  font-size: 11.5px;
}

.search-state {
  gap: 7px;
  min-height: 280px;
}

.friend-profile {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid #eeeeee;
}

.profile-actions,
.notification-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
}

.detail-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 620px;
  padding: 13px;
  border: 1px solid #eeeeee;
  border-radius: 10px;
  background: #fafafa;
  color: #888;
}

.detail-note > :first-child {
  margin-top: 2px;
  color: #777;
}

.detail-note div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.detail-note strong {
  color: #555;
  font-size: 12px;
}

.detail-note span {
  font-size: 11.5px;
  line-height: 1.55;
}

.hub-body.is-empty-mode {
  grid-template-columns: minmax(0, 1fr);
}

.hub-body.is-empty-mode .hub-list {
  display: none;
}

.notification-empty-state {
  min-height: 100%;
  background: radial-gradient(circle at 50% 45%, rgba(0, 0, 0, 0.018), transparent 28%), #fff;
}

.empty-hint-grid {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 7px;
}

.empty-hint-grid span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 9px;
  border: 1px solid #eeeeee;
  border-radius: 7px;
  background: #fafafa;
  color: #777;
  font-size: 10.5px;
}

.friend-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: min(100%, 760px);
}

.overview-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid #eeeeee;
  border-radius: 10px;
  background: #fff;
}

.overview-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 11.5px;
  font-weight: 600;
}

.overview-card dl {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 13px 0 0;
}

.overview-card dl > div {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 10px;
  font-size: 11.5px;
}

.overview-card dt {
  color: #999;
}

.overview-card dd {
  margin: 0;
  overflow: hidden;
  color: #555;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-chat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.recent-chat-copy {
  display: -webkit-box;
  margin: 14px 0 8px;
  overflow: hidden;
  color: #555;
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.recent-chat-copy.muted {
  color: #999;
}

.recent-chat-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  color: #aaa;
  font-size: 10.5px;
}

.notification-title-block {
  padding-bottom: 20px;
  border-bottom: 1px solid #eeeeee;
}

.notification-meta-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 10px;
  background: #eeeeee;
}

.notification-meta-card > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  padding: 13px;
  background: #fff;
}

.notification-meta-card span {
  color: #9b9b9b;
  font-size: 10px;
}

.notification-meta-card strong {
  overflow: hidden;
  color: #454545;
  font-size: 12px;
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resolved-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 8px 10px;
  border-radius: 7px;
  background: #f5f5f5;
  color: #777;
  font-size: 11.5px;
}

.mobile-back {
  display: none;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

@media (max-width: 1080px) {
  .hub-body {
    grid-template-columns: minmax(240px, 282px) minmax(0, 1fr);
  }

  .page-heading p {
    max-width: 520px;
  }
}

@media (max-width: 820px) {
  .message-page {
    gap: 12px;
    padding: 14px 12px 12px;
  }

  .page-header {
    align-items: center;
  }

  .page-heading p {
    display: none;
  }

  .hub-body {
    display: block;
  }

  .hub-list,
  .hub-main {
    width: 100%;
    height: 100%;
    border-right: 0;
  }

  .hub-body.has-detail .hub-list {
    display: none;
  }

  .hub-body:not(.has-detail):not(.is-empty-mode) .hub-main {
    display: none;
  }

  .mobile-back {
    display: inline-flex;
  }

  .detail-panel {
    padding: 20px 18px;
  }

  .notification-meta-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .message-page {
    padding: 10px 8px 8px;
  }

  .page-heading h1 {
    font-size: 19px;
  }

  .refresh-button {
    min-width: 34px;
    padding-inline: 9px;
    font-size: 0;
  }

  .refresh-button :deep(.anticon) {
    margin-inline-end: 0;
    font-size: 14px;
  }

  .section-tabs {
    width: 100%;
  }

  .section-tab {
    flex: 1;
    justify-content: center;
    padding: 0 8px;
  }

  .section-tab > :deep(.anticon) {
    font-size: 13px;
  }

  .hub-body {
    border-radius: 11px;
  }

  .chat-header {
    padding-inline: 11px;
  }

  .chat-header-actions {
    display: none;
  }

  .message-scroll-inner {
    padding: 13px 10px 16px;
  }

  .composer-wrap {
    padding: 9px 10px 10px;
  }

  .composer :deep(.ant-input) {
    font-size: 12.5px;
  }

  .friend-search-form {
    grid-template-columns: 1fr;
  }

  .friend-profile {
    align-items: flex-start;
  }

  .profile-actions :deep(.ant-btn) {
    flex: 1;
  }
}
@media (max-width: 820px) {
  .friend-overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
