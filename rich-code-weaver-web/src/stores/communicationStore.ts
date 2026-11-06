import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUnreadCount,
  listConversations,
  listMessages,
  markAsRead,
  sendMessage as sendMessageByHttp,
} from '@/api/chatController'
import { listFriends, listPendingRequests } from '@/api/friendController'
import { listPendingCollabInvitations } from '@/api/collaboratorController'
import { useLoginUserStore } from '@/stores/loginUser'
import { API_BASE_URL } from '@/config/env'

export type CommunicationTab = 'messages' | 'friends' | 'notifications'
export type ConnectionState = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'offline'
export type DeliveryState = 'sending' | 'sent' | 'failed'

export interface LocalChatMessage extends API.ChatMessageVO {
  clientMessageId?: string
  deliveryState?: DeliveryState
  deliveryError?: string
}

export interface CommunicationNotice {
  key: string
  kind: 'friend' | 'collab' | 'system'
  action: string
  title: string
  description: string
  time: string
  avatar?: string
  referenceId?: number
  appId?: number
  appName?: string
}

interface AckWaiter {
  resolve: (message: API.ChatMessageVO) => void
  reject: (error: Error) => void
  timer: ReturnType<typeof setTimeout>
  receiverId: number
  content: string
}

const ACK_TIMEOUT = 12000
const WS_MAX_DELAY = 5000

function sameId(a: unknown, b: unknown): boolean {
  if (a == null || b == null) return false
  return String(a) === String(b)
}

function createClientMessageId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `cm_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

function errorMessage(error: unknown, fallback = '操作失败，请稍后重试'): string {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'string' && error.trim()) return error
  return fallback
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function asText(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function asNumber(value: unknown): number | undefined {
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function messageTimeValue(value?: string): number {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

export const useCommunicationStore = defineStore('communication', () => {
  const loginUserStore = useLoginUserStore()

  const wsState = ref<ConnectionState>('idle')
  const wsLastError = ref('')
  const conversations = ref<API.ChatConversationVO[]>([])
  const friends = ref<API.UserFriendshipVO[]>([])
  const pendingRequests = ref<API.UserFriendshipVO[]>([])
  const pendingCollabInvitations = ref<API.AppCollaboratorVO[]>([])
  const recentNotices = ref<CommunicationNotice[]>([])

  const totalUnreadCount = ref(0)
  const pendingCount = ref(0)
  const pendingCollabCount = ref(0)

  const conversationsLoading = ref(false)
  const conversationsError = ref('')
  const friendsLoading = ref(false)
  const friendsError = ref('')
  const pendingLoading = ref(false)
  const pendingError = ref('')
  const collabLoading = ref(false)
  const collabError = ref('')
  const unreadLoading = ref(false)

  const activeConversationId = ref<number | null>(null)
  const activeConversationVisible = ref(false)
  const currentMessages = ref<LocalChatMessage[]>([])
  const messagesLoading = ref(false)
  const messagesError = ref('')
  const messagesPage = ref(1)
  const hasMoreMessages = ref(false)
  const pendingChatFriendId = ref<number | null>(null)
  const unseenRealtimeCount = ref(0)

  let socket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  let manuallyClosed = false
  let initializedUserId: number | null = null
  let initPromise: Promise<void> | null = null
  let activeMessageRequest = 0
  let readTimer: ReturnType<typeof setTimeout> | null = null
  const ackWaiters = new Map<string, AckWaiter>()
  const readRequests = new Set<number>()
  const readAgainRequests = new Set<number>()

  const currentUserId = computed(() => loginUserStore.loginUser?.id)
  const activeConversation = computed(
    () => conversations.value.find((item) => sameId(item.id, activeConversationId.value)) || null,
  )
  const pendingChatFriend = computed(
    () => friends.value.find((item) => sameId(item.friendId, pendingChatFriendId.value)) || null,
  )
  const activePeer = computed(() => {
    if (activeConversation.value) {
      return {
        id: activeConversation.value.targetUserId,
        name: activeConversation.value.targetUserName || '未命名用户',
        avatar: activeConversation.value.targetUserAvatar,
      }
    }
    const friend = pendingChatFriend.value
    if (friend) {
      return {
        id: friend.friendId,
        name: friend.friendName || '未命名用户',
        avatar: friend.friendAvatar,
      }
    }
    return null
  })
  const notificationCount = computed(() => pendingCount.value + pendingCollabCount.value)
  const notificationItems = computed<CommunicationNotice[]>(() => {
    const pending: CommunicationNotice[] = [
      ...pendingRequests.value.map((request) => ({
        key: `friend-request-${request.id}`,
        kind: 'friend' as const,
        action: 'pendingRequest',
        title: `${request.friendName || '新用户'} 请求添加好友`,
        description: request.remark?.trim() || '等待你处理好友申请',
        time: request.createTime || '',
        avatar: request.friendAvatar,
        referenceId: request.id,
      })),
      ...pendingCollabInvitations.value.map((invite) => ({
        key: `collab-invite-${invite.id}`,
        kind: 'collab' as const,
        action: 'invite',
        title: `${invite.inviterName || '协作者'} 邀请你协作`,
        description: invite.appName || '数字产物',
        time: invite.createTime || '',
        avatar: invite.inviterAvatar || invite.userAvatar,
        referenceId: invite.id,
        appId: invite.appId,
        appName: invite.appName,
      })),
    ]
    const notices = recentNotices.value.filter((notice) => {
      if (notice.kind === 'friend' && notice.action === 'newRequest') return false
      if (notice.kind === 'collab' && notice.action === 'invite') return false
      return true
    })
    return [...pending, ...notices].sort(
      (a, b) => messageTimeValue(b.time) - messageTimeValue(a.time),
    )
  })

  function buildWebSocketUrl(): string {
    if (API_BASE_URL && /^https?:\/\//i.test(API_BASE_URL)) {
      const apiUrl = new URL(API_BASE_URL)
      const protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${protocol}//${apiUrl.host}/api/user/ws/chat`
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/api/user/ws/chat`
  }

  function clearReconnectTimer() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function rejectPendingAcks(message: string) {
    ackWaiters.forEach((waiter) => {
      clearTimeout(waiter.timer)
      waiter.reject(new Error(message))
    })
    ackWaiters.clear()
  }

  function scheduleReconnect() {
    if (manuallyClosed || !currentUserId.value) return
    clearReconnectTimer()
    const delay = Math.min(1000 * 2 ** reconnectAttempts, WS_MAX_DELAY)
    reconnectAttempts += 1
    wsState.value = 'reconnecting'
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connectSocket()
    }, delay)
  }

  function connectSocket() {
    if (!currentUserId.value || manuallyClosed) return
    if (
      socket &&
      (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)
    ) {
      return
    }

    clearReconnectTimer()
    wsState.value = reconnectAttempts > 0 ? 'reconnecting' : 'connecting'
    wsLastError.value = ''

    try {
      const nextSocket = new WebSocket(buildWebSocketUrl())
      socket = nextSocket

      nextSocket.onopen = () => {
        if (socket !== nextSocket) return
        wsState.value = 'open'
        wsLastError.value = ''
        reconnectAttempts = 0
      }

      nextSocket.onmessage = (event) => {
        if (socket !== nextSocket) return
        handleSocketMessage(event.data)
      }

      nextSocket.onerror = () => {
        if (socket !== nextSocket) return
        wsLastError.value = '实时连接异常，已切换为自动重连'
      }

      nextSocket.onclose = () => {
        if (socket !== nextSocket) return
        socket = null
        wsState.value = 'offline'
        rejectPendingAcks('连接中断，发送结果未知，请重试')
        scheduleReconnect()
      }
    } catch (error) {
      wsLastError.value = errorMessage(error, '实时连接创建失败')
      wsState.value = 'offline'
      scheduleReconnect()
    }
  }

  function disconnectSocket() {
    manuallyClosed = true
    clearReconnectTimer()
    rejectPendingAcks('连接已关闭')
    if (socket) {
      const current = socket
      socket = null
      current.close()
    }
    wsState.value = 'idle'
    wsLastError.value = ''
  }

  function addNotice(notice: Omit<CommunicationNotice, 'key'> & { key?: string }) {
    const next: CommunicationNotice = {
      ...notice,
      key: notice.key || `${notice.kind}-${notice.action}-${Date.now()}-${Math.random()}`,
    }
    recentNotices.value = [next, ...recentNotices.value].slice(0, 20)
  }

  function recordNotice(notice: Omit<CommunicationNotice, 'key'> & { key?: string }) {
    addNotice(notice)
  }

  function handleFriendNotification(data: unknown) {
    const payload = isRecord(data) ? data : {}
    const action = asText(payload.action)
    const actor = asText(payload.fromUserName) || asText(payload.userName) || '一位用户'
    const title =
      action === 'accepted'
        ? `${actor} 已同意你的好友申请`
        : action === 'rejected'
          ? `${actor} 已拒绝你的好友申请`
          : `${actor} 发来好友申请`
    addNotice({
      kind: 'friend',
      action,
      title,
      description: action === 'newRequest' ? '前往通知中心处理好友申请' : '好友状态已更新',
      time: new Date().toISOString(),
      avatar: asText(payload.fromUserAvatar) || asText(payload.userAvatar) || undefined,
      referenceId: asNumber(payload.friendshipId) ?? asNumber(payload.requestId),
    })
    void fetchPendingRequests()
    if (action === 'accepted' || action === 'rejected') {
      void fetchFriends()
      void fetchConversations()
    }
  }

  function handleCollabNotification(data: unknown) {
    const payload = isRecord(data) ? data : {}
    const action = asText(payload.action)
    const actor =
      asText(payload.actorName) ||
      asText(payload.inviterName) ||
      asText(payload.userName) ||
      '协作者'
    const appName = asText(payload.appName) || '数字产物'
    const title =
      action === 'invite'
        ? `${actor} 邀请你协作「${appName}」`
        : action === 'accepted'
          ? `${actor} 已接受「${appName}」协作邀请`
          : action === 'rejected'
            ? `${actor} 已拒绝「${appName}」协作邀请`
            : `「${appName}」协作状态已更新`
    addNotice({
      kind: 'collab',
      action,
      title,
      description: action === 'invite' ? '等待你处理协作邀请' : '协作状态已更新',
      time: new Date().toISOString(),
      avatar:
        asText(payload.actorAvatar) ||
        asText(payload.userAvatar) ||
        asText(payload.inviterAvatar) ||
        undefined,
      referenceId: asNumber(payload.collabId) ?? asNumber(payload.id),
      appId: asNumber(payload.appId),
      appName,
    })
    void fetchPendingCollabInvitations()
    void fetchConversations()
  }

  function handleSocketMessage(raw: string) {
    try {
      const envelope = JSON.parse(raw) as {
        type?: string
        data?: unknown
        clientMessageId?: string
        message?: string
      }
      const envelopeData = isRecord(envelope.data) ? envelope.data : {}
      const clientMessageId =
        asText(envelope.clientMessageId) || asText(envelopeData.clientMessageId)
      switch (envelope?.type) {
        case 'newMessage':
          onNewMessage(envelope.data as API.ChatMessageVO)
          break
        case 'messageSent':
          onMessageConfirmed(envelope.data as API.ChatMessageVO, clientMessageId || undefined)
          break
        case 'friendNotification':
          handleFriendNotification(envelope.data)
          break
        case 'collabNotification':
          handleCollabNotification(envelope.data)
          break
        case 'unreadCount':
          if (typeof envelope.data === 'number') {
            totalUnreadCount.value = Math.max(0, envelope.data)
          }
          break
        case 'error': {
          const error = asText(envelope.message) || asText(envelopeData.message) || '消息处理失败'
          wsLastError.value = error
          if (clientMessageId) {
            const waiter = ackWaiters.get(clientMessageId)
            if (waiter) {
              clearTimeout(waiter.timer)
              ackWaiters.delete(clientMessageId)
              waiter.reject(new Error(error))
            }
            markMessageFailed(clientMessageId, error)
          }
          break
        }
      }
    } catch (error) {
      wsLastError.value = errorMessage(error, '实时消息解析失败')
    }
  }

  function normalizeMessage(message: API.ChatMessageVO): LocalChatMessage {
    const existing = currentMessages.value.find(
      (item) => item.id != null && sameId(item.id, message.id),
    )
    return {
      ...message,
      clientMessageId: (message as LocalChatMessage).clientMessageId || existing?.clientMessageId,
      deliveryState: 'sent',
    }
  }

  function updateConversationFromMessage(message: API.ChatMessageVO, incrementUnread = false) {
    if (!message.conversationId) return
    const peerId = sameId(message.senderId, currentUserId.value)
      ? message.receiverId
      : message.senderId
    const friend = friends.value.find((item) => sameId(item.friendId, peerId))
    let conversation = conversations.value.find((item) => sameId(item.id, message.conversationId))

    if (!conversation) {
      conversation = {
        id: message.conversationId,
        targetUserId: peerId,
        targetUserName:
          (sameId(message.senderId, currentUserId.value) ? undefined : message.senderName) ||
          friend?.friendName ||
          '未命名用户',
        targetUserAvatar:
          (sameId(message.senderId, currentUserId.value) ? undefined : message.senderAvatar) ||
          friend?.friendAvatar,
        unreadCount: 0,
        createTime: message.createTime,
      }
      conversations.value.unshift(conversation)
    }

    conversation.lastMessageType = message.messageType || 'text'
    conversation.lastMessageContent =
      message.messageType === 'collab_invite'
        ? '[协作邀请]'
        : message.messageType === 'app_forward'
          ? '[产物转发]'
          : (message.content || '').slice(0, 80)
    conversation.lastMessageTime = message.createTime

    if (incrementUnread) {
      conversation.unreadCount = (conversation.unreadCount || 0) + 1
      totalUnreadCount.value += 1
    }

    conversations.value = [...conversations.value].sort(
      (a, b) => messageTimeValue(b.lastMessageTime) - messageTimeValue(a.lastMessageTime),
    )
  }

  function onNewMessage(message?: API.ChatMessageVO) {
    if (!message) return
    const normalized = normalizeMessage(message)
    const isCurrent = sameId(message.conversationId, activeConversationId.value)
    const shouldMarkRead =
      isCurrent && activeConversationVisible.value && document.visibilityState === 'visible'

    if (isCurrent) {
      const exists = currentMessages.value.some(
        (item) =>
          (item.id != null && sameId(item.id, normalized.id)) ||
          (item.clientMessageId &&
            normalized.clientMessageId &&
            item.clientMessageId === normalized.clientMessageId),
      )
      if (!exists) {
        currentMessages.value.push(normalized)
        unseenRealtimeCount.value += 1
      }
    }

    updateConversationFromMessage(message, !shouldMarkRead)
    if (shouldMarkRead) {
      scheduleMarkRead()
    }
  }

  function onMessageConfirmed(message?: API.ChatMessageVO, explicitClientId?: string) {
    if (!message) return
    const clientId = explicitClientId || (message as LocalChatMessage).clientMessageId
    if (clientId) {
      const waiter = ackWaiters.get(clientId)
      if (waiter) {
        clearTimeout(waiter.timer)
        ackWaiters.delete(clientId)
        waiter.resolve(message)
      }
    }

    const pendingTarget = pendingChatFriendId.value
    if (
      !activeConversationId.value &&
      message.conversationId &&
      pendingTarget &&
      sameId(message.receiverId, pendingTarget)
    ) {
      activeConversationId.value = message.conversationId
      pendingChatFriendId.value = null
    }

    const optimisticIndex = currentMessages.value.findIndex(
      (item) =>
        (clientId && item.clientMessageId === clientId) ||
        (item.id == null &&
          item.content === message.content &&
          sameId(item.senderId, message.senderId)),
    )
    const confirmed: LocalChatMessage = {
      ...message,
      clientMessageId: clientId,
      deliveryState: 'sent',
    }
    if (optimisticIndex >= 0) {
      currentMessages.value.splice(optimisticIndex, 1, confirmed)
    } else if (
      sameId(message.conversationId, activeConversationId.value) &&
      !currentMessages.value.some((item) => item.id != null && sameId(item.id, message.id))
    ) {
      currentMessages.value.push(confirmed)
    }

    updateConversationFromMessage(message, false)
  }

  function markMessageFailed(clientMessageId: string, message: string) {
    const item = currentMessages.value.find((msg) => msg.clientMessageId === clientMessageId)
    if (item) {
      item.deliveryState = 'failed'
      item.deliveryError = message
    }
  }

  function waitForAck(clientMessageId: string, receiverId: number, content: string) {
    return new Promise<API.ChatMessageVO>((resolve, reject) => {
      const timer = setTimeout(() => {
        ackWaiters.delete(clientMessageId)
        reject(new Error('发送超时，请检查网络后重试'))
      }, ACK_TIMEOUT)
      ackWaiters.set(clientMessageId, { resolve, reject, timer, receiverId, content })
    })
  }

  async function sendViaWebSocket(
    receiverId: number,
    content: string,
    messageType: string,
    clientMessageId: string,
  ) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      throw new Error('实时连接不可用')
    }
    const ack = waitForAck(clientMessageId, receiverId, content)
    socket.send(
      JSON.stringify({
        type: 'chat',
        payload: { receiverId, content, messageType, clientMessageId },
      }),
    )
    return ack
  }

  async function sendViaHttp(
    receiverId: number,
    content: string,
    messageType: string,
  ): Promise<API.ChatMessageVO> {
    const response = await sendMessageByHttp({ receiverId, content, messageType })
    if (response.data.code !== 0 || !response.data.data) {
      throw new Error(response.data.message || '消息发送失败')
    }
    return response.data.data
  }

  function createOptimisticMessage(
    receiverId: number,
    content: string,
    messageType: string,
    clientMessageId: string,
  ): LocalChatMessage {
    const user = loginUserStore.loginUser
    return {
      senderId: user?.id,
      senderName: user?.userName,
      senderAvatar: user?.userAvatar,
      receiverId,
      content,
      messageType,
      createTime: new Date().toISOString(),
      clientMessageId,
      deliveryState: 'sending',
    }
  }

  async function sendChatMessage(receiverId: number, content: string, messageType = 'text') {
    const trimmed = content.trim()
    if (!trimmed) throw new Error('消息内容不能为空')
    if (trimmed.length > 5000) throw new Error('消息不能超过 5000 个字符')

    const clientMessageId = createClientMessageId()
    const optimistic = createOptimisticMessage(receiverId, trimmed, messageType, clientMessageId)
    currentMessages.value.push(optimistic)

    try {
      let confirmed: API.ChatMessageVO
      if (socket?.readyState === WebSocket.OPEN) {
        try {
          confirmed = await sendViaWebSocket(receiverId, trimmed, messageType, clientMessageId)
        } catch (error) {
          if (errorMessage(error).includes('实时连接不可用')) {
            confirmed = await sendViaHttp(receiverId, trimmed, messageType)
          } else {
            throw error
          }
        }
      } else {
        confirmed = await sendViaHttp(receiverId, trimmed, messageType)
      }
      onMessageConfirmed(confirmed, clientMessageId)
      return confirmed
    } catch (error) {
      markMessageFailed(clientMessageId, errorMessage(error))
      throw error
    }
  }

  async function retryMessage(message: LocalChatMessage) {
    if (!message.clientMessageId || !message.receiverId || !message.content) return
    message.deliveryState = 'sending'
    message.deliveryError = ''
    try {
      let confirmed: API.ChatMessageVO
      if (socket?.readyState === WebSocket.OPEN) {
        confirmed = await sendViaWebSocket(
          message.receiverId,
          message.content,
          message.messageType || 'text',
          message.clientMessageId,
        )
      } else {
        confirmed = await sendViaHttp(
          message.receiverId,
          message.content,
          message.messageType || 'text',
        )
      }
      onMessageConfirmed(confirmed, message.clientMessageId)
      return confirmed
    } catch (error) {
      message.deliveryState = 'failed'
      message.deliveryError = errorMessage(error)
      throw error
    }
  }

  async function markConversationRead(conversationId: number, lastReadMessageId?: number) {
    if (readRequests.has(conversationId)) {
      readAgainRequests.add(conversationId)
      return
    }
    readRequests.add(conversationId)
    const conversation = conversations.value.find((item) => sameId(item.id, conversationId))
    const previousUnread = conversation?.unreadCount || 0
    if (conversation) conversation.unreadCount = 0
    totalUnreadCount.value = Math.max(0, totalUnreadCount.value - previousUnread)
    try {
      const response = await markAsRead({ conversationId, lastReadMessageId })
      if (response.data.code !== 0) {
        throw new Error(response.data.message || '标记已读失败')
      }
    } catch {
      if (socket?.readyState === WebSocket.OPEN) {
        socket.send(
          JSON.stringify({
            type: 'read',
            payload: { conversationId, lastReadMessageId },
          }),
        )
        setTimeout(() => {
          void fetchUnreadTotal()
          void fetchConversations()
        }, 600)
      } else if (conversation && previousUnread > 0) {
        conversation.unreadCount = previousUnread
        totalUnreadCount.value += previousUnread
      }
    } finally {
      readRequests.delete(conversationId)
      if (readAgainRequests.delete(conversationId) && activeConversationVisible.value) {
        scheduleMarkRead()
      }
    }
  }

  function scheduleMarkRead() {
    if (readTimer) clearTimeout(readTimer)
    readTimer = setTimeout(() => {
      readTimer = null
      if (!activeConversationId.value || !activeConversationVisible.value) return
      const lastReadable = [...currentMessages.value].reverse().find((item) => item.id != null)?.id
      void markConversationRead(activeConversationId.value, lastReadable)
    }, 120)
  }

  async function selectConversation(conversationId: number) {
    activeConversationId.value = conversationId
    pendingChatFriendId.value = null
    currentMessages.value = []
    messagesPage.value = 1
    hasMoreMessages.value = false
    messagesError.value = ''
    unseenRealtimeCount.value = 0
  }

  function startChatWithFriend(friendId: number) {
    const existing = conversations.value.find((item) => sameId(item.targetUserId, friendId))
    if (existing?.id) {
      void selectConversation(existing.id)
      return existing
    }
    activeConversationId.value = null
    pendingChatFriendId.value = friendId
    currentMessages.value = []
    messagesPage.value = 1
    hasMoreMessages.value = false
    messagesError.value = ''
    unseenRealtimeCount.value = 0
    return null
  }

  function clearActiveConversation() {
    activeConversationId.value = null
    activeConversationVisible.value = false
    pendingChatFriendId.value = null
    currentMessages.value = []
    messagesError.value = ''
    unseenRealtimeCount.value = 0
    if (readTimer) {
      clearTimeout(readTimer)
      readTimer = null
    }
  }

  async function loadMessages(conversationId: number, page = 1) {
    const requestId = ++activeMessageRequest
    messagesLoading.value = true
    messagesError.value = ''
    try {
      const response = await listMessages({
        conversationId,
        pageNum: page,
        pageSize: 40,
      })
      if (
        requestId !== activeMessageRequest ||
        !sameId(conversationId, activeConversationId.value)
      ) {
        return false
      }
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '消息加载失败')
      }
      const result = response.data.data
      const records = (result.records || []).map(normalizeMessage)
      if (page === 1) {
        currentMessages.value = records
        messagesPage.value = 1
        unseenRealtimeCount.value = 0
      } else {
        const knownIds = new Set(
          currentMessages.value.filter((item) => item.id != null).map((item) => String(item.id)),
        )
        const older = records.filter((item) => !item.id || !knownIds.has(String(item.id)))
        currentMessages.value = [...older, ...currentMessages.value]
        messagesPage.value = page
      }
      hasMoreMessages.value = (result.pageNumber || page) < (result.totalPage || 1)
      scheduleMarkRead()
      return true
    } catch (error) {
      if (requestId === activeMessageRequest) {
        messagesError.value = errorMessage(error, '消息加载失败')
      }
      return false
    } finally {
      if (requestId === activeMessageRequest) {
        messagesLoading.value = false
      }
    }
  }

  async function loadOlderMessages() {
    if (!activeConversationId.value || !hasMoreMessages.value || messagesLoading.value) return false
    return loadMessages(activeConversationId.value, messagesPage.value + 1)
  }

  async function fetchConversations() {
    conversationsLoading.value = true
    conversationsError.value = ''
    try {
      const response = await listConversations()
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '会话加载失败')
      }
      conversations.value = [...response.data.data].sort(
        (a, b) => messageTimeValue(b.lastMessageTime) - messageTimeValue(a.lastMessageTime),
      )
    } catch (error) {
      conversationsError.value = errorMessage(error, '会话加载失败')
    } finally {
      conversationsLoading.value = false
    }
  }

  async function fetchFriends() {
    friendsLoading.value = true
    friendsError.value = ''
    try {
      const response = await listFriends()
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '好友加载失败')
      }
      friends.value = response.data.data
    } catch (error) {
      friendsError.value = errorMessage(error, '好友加载失败')
    } finally {
      friendsLoading.value = false
    }
  }

  async function fetchPendingRequests() {
    pendingLoading.value = true
    pendingError.value = ''
    try {
      const response = await listPendingRequests()
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '好友申请加载失败')
      }
      pendingRequests.value = response.data.data
      pendingCount.value = response.data.data.length
    } catch (error) {
      pendingError.value = errorMessage(error, '好友申请加载失败')
    } finally {
      pendingLoading.value = false
    }
  }

  async function fetchPendingCollabInvitations() {
    collabLoading.value = true
    collabError.value = ''
    try {
      const response = await listPendingCollabInvitations()
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '协作邀请加载失败')
      }
      pendingCollabInvitations.value = response.data.data
      pendingCollabCount.value = response.data.data.length
    } catch (error) {
      collabError.value = errorMessage(error, '协作邀请加载失败')
    } finally {
      collabLoading.value = false
    }
  }

  async function fetchUnreadTotal() {
    unreadLoading.value = true
    try {
      const response = await getUnreadCount()
      if (response.data.code === 0 && typeof response.data.data === 'number') {
        totalUnreadCount.value = response.data.data
      }
    } finally {
      unreadLoading.value = false
    }
  }

  async function refreshCommunication() {
    await Promise.all([
      fetchConversations(),
      fetchFriends(),
      fetchPendingRequests(),
      fetchPendingCollabInvitations(),
      fetchUnreadTotal(),
    ])
  }

  function resetUserState() {
    conversations.value = []
    friends.value = []
    pendingRequests.value = []
    pendingCollabInvitations.value = []
    recentNotices.value = []
    totalUnreadCount.value = 0
    pendingCount.value = 0
    pendingCollabCount.value = 0
    currentMessages.value = []
    activeConversationId.value = null
    pendingChatFriendId.value = null
    activeConversationVisible.value = false
    messagesError.value = ''
    conversationsError.value = ''
    friendsError.value = ''
    pendingError.value = ''
    collabError.value = ''
    unseenRealtimeCount.value = 0
    activeMessageRequest += 1
    if (readTimer) {
      clearTimeout(readTimer)
      readTimer = null
    }
    readRequests.clear()
    readAgainRequests.clear()
  }

  async function initCommunication(force = false) {
    const userId = currentUserId.value
    if (!userId) return
    if (initializedUserId !== null && initializedUserId !== userId) {
      resetUserState()
      manuallyClosed = false
    }
    if (force && initializedUserId === userId) {
      await refreshCommunication()
      return
    }
    if (initializedUserId === userId && initPromise) return initPromise
    if (initializedUserId === userId) {
      connectSocket()
      return
    }

    initializedUserId = userId
    manuallyClosed = false
    connectSocket()
    initPromise = refreshCommunication().finally(() => {
      initPromise = null
    })
    return initPromise
  }

  function resetCommunication() {
    resetUserState()
    initializedUserId = null
    initPromise = null
    rejectPendingAcks('登录状态已变化')
    disconnectSocket()
  }

  return {
    wsState,
    wsLastError,
    conversations,
    friends,
    pendingRequests,
    pendingCollabInvitations,
    recentNotices,
    totalUnreadCount,
    pendingCount,
    pendingCollabCount,
    conversationsLoading,
    conversationsError,
    friendsLoading,
    friendsError,
    pendingLoading,
    pendingError,
    collabLoading,
    collabError,
    unreadLoading,
    activeConversationId,
    activeConversationVisible,
    currentMessages,
    messagesLoading,
    messagesError,
    messagesPage,
    hasMoreMessages,
    pendingChatFriendId,
    unseenRealtimeCount,
    currentUserId,
    activeConversation,
    pendingChatFriend,
    activePeer,
    notificationCount,
    notificationItems,
    recordNotice,
    connectSocket,
    disconnectSocket,
    initCommunication,
    refreshCommunication,
    resetCommunication,
    fetchConversations,
    fetchFriends,
    fetchPendingRequests,
    fetchPendingCollabInvitations,
    fetchUnreadTotal,
    selectConversation,
    startChatWithFriend,
    clearActiveConversation,
    loadMessages,
    loadOlderMessages,
    sendChatMessage,
    retryMessage,
    markConversationRead,
    scheduleMarkRead,
  }
})
