import { defineStore } from 'pinia'
import { ref } from 'vue'
import { API_BASE_URL } from '@/config/env'
import {
  getUnreadCount,
  listConversations,
  sendMessage as sendMessageByHttp,
} from '@/api/chatController'
import { listFriends, listPendingRequests } from '@/api/friendController'

/** 宽松ID比较，将两侧转为字符串后对比，解决string/number类型不一致问题 */
function sameId(a: unknown, b: unknown): boolean {
  if (a == null || b == null) return false
  return String(a) === String(b)
}

/**
 * 聊天与好友状态管理
 * 管理WebSocket连接、会话列表、好友列表、未读消息数等状态
 */
export const useChatStore = defineStore('chat', () => {
  // ===== 状态 =====

  /** WebSocket连接实例 */
  const ws = ref<WebSocket | null>(null)
  /** WebSocket连接状态 */
  const wsConnected = ref(false)
  /** 重连定时器 */
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  /** 重连次数 */
  let reconnectCount = 0
  let reconnectSuppressed = false
  let sessionGeneration = 0
  let conversationsRequestId = 0
  let friendsRequestId = 0
  let pendingRequestId = 0
  let unreadRequestId = 0
  let conversationRefreshTimer: ReturnType<typeof setTimeout> | null = null
  const WS_ACK_TIMEOUT = 12000
  interface PendingWsSend {
    receiverId: number
    content: string
    resolve: () => void
    reject: (error: Error) => void
    timer: ReturnType<typeof setTimeout>
  }
  const pendingWsSends = new Set<PendingWsSend>()
  /** 最大重连次数 */
  const MAX_RECONNECT = 5

  /** 会话列表 */
  const conversations = ref<API.ChatConversationVO[]>([])
  /** 当前选中的会话id */
  const activeConversationId = ref<number | null>(null)
  /** 当前会话的消息列表 */
  const currentMessages = ref<API.ChatMessageVO[]>([])

  /** 好友列表 */
  const friends = ref<API.UserFriendshipVO[]>([])
  /** 待处理的好友申请列表 */
  const pendingRequests = ref<API.UserFriendshipVO[]>([])

  /** 总未读消息数 */
  const totalUnreadCount = ref(0)
  /** 待处理好友申请数 */
  const pendingCount = ref(0)

  /** 好友抽屉可见状态 */
  const friendDrawerVisible = ref(false)
  /** 聊天抽屉可见状态 */
  const chatDrawerVisible = ref(false)

  /** 待发起聊天的好友id（无现有会话时使用） */
  const pendingChatFriendId = ref<number | null>(null)

  const buildWebSocketUrl = () => {
    if (API_BASE_URL && /^https?:\/\//i.test(API_BASE_URL)) {
      const apiUrl = new URL(API_BASE_URL)
      const protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${protocol}//${apiUrl.host}/api/user/ws/chat`
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/api/user/ws/chat`
  }

  // ===== WebSocket 管理 =====

  /**
   * 建立WebSocket连接
   * 使用当前页面域名和单体应用 WebSocket 入口
   */
  function connectWebSocket(isRetry = false) {
    if (!isRetry) {
      reconnectCount = 0
      reconnectSuppressed = false
    }
    if (reconnectSuppressed) return
    if (
      ws.value &&
      (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)
    ) {
      return
    }
    const wsUrl = buildWebSocketUrl()

    try {
      const socket = new WebSocket(wsUrl)
      ws.value = socket

      socket.onopen = () => {
        if (ws.value !== socket) return
        wsConnected.value = true
        reconnectCount = 0
        console.log('[WebSocket] 连接已建立')
      }

      socket.onmessage = (event: MessageEvent) => {
        if (ws.value !== socket) return
        handleWsMessage(event.data)
      }

      socket.onclose = () => {
        if (ws.value !== socket) return
        ws.value = null
        wsConnected.value = false
        rejectPendingWsSends('实时连接已断开，发送结果未知，请重试')
        console.log('[WebSocket] 连接已关闭')
        scheduleReconnect()
      }

      socket.onerror = (error) => {
        if (ws.value !== socket) return
        console.error('[WebSocket] 连接错误', error)
        wsConnected.value = false
      }
    } catch (e) {
      console.error('[WebSocket] 创建连接失败', e)
      scheduleReconnect()
    }
  }

  /**
   * 关闭WebSocket连接
   */
  function rejectPendingWsSends(messageText: string) {
    pendingWsSends.forEach((waiter) => {
      clearTimeout(waiter.timer)
      waiter.reject(new Error(messageText))
    })
    pendingWsSends.clear()
  }

  function settleWsSend(messageVO: API.ChatMessageVO) {
    for (const waiter of pendingWsSends) {
      const receiverMatches =
        !messageVO.receiverId || sameId(messageVO.receiverId, waiter.receiverId)
      if (!receiverMatches || waiter.content !== String(messageVO.content || '')) continue
      clearTimeout(waiter.timer)
      pendingWsSends.delete(waiter)
      waiter.resolve()
      break
    }
  }

  function waitForWsAck(receiverId: number, content: string) {
    let waiter: PendingWsSend
    const promise = new Promise<void>((resolve, reject) => {
      waiter = {
        receiverId,
        content,
        resolve,
        reject,
        timer: setTimeout(() => {
          pendingWsSends.delete(waiter)
          reject(new Error('消息发送确认超时，请稍后重试'))
        }, WS_ACK_TIMEOUT),
      }
      pendingWsSends.add(waiter)
    })
    return {
      promise,
      cancel: () => {
        clearTimeout(waiter.timer)
        pendingWsSends.delete(waiter)
      },
    }
  }

  function disconnectWebSocket() {
    reconnectSuppressed = true
    rejectPendingWsSends('连接已关闭，发送结果未知，请重试')
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (conversationRefreshTimer) {
      clearTimeout(conversationRefreshTimer)
      conversationRefreshTimer = null
    }
    const currentSocket = ws.value
    ws.value = null
    if (currentSocket) {
      currentSocket.onopen = null
      currentSocket.onmessage = null
      currentSocket.onerror = null
      currentSocket.onclose = null
      currentSocket.close()
    }
    wsConnected.value = false
  }

  /**
   * 定时重连逻辑，指数退避
   */
  function scheduleReconnect() {
    if (reconnectSuppressed || reconnectTimer) return
    if (reconnectCount >= MAX_RECONNECT) {
      console.warn('[WebSocket] 已达最大重连次数，停止重连')
      return
    }
    const delay = Math.min(1000 * Math.pow(2, reconnectCount), 30000)
    reconnectCount++
    console.log(`[WebSocket] ${delay}ms 后进行第 ${reconnectCount} 次重连`)
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connectWebSocket(true)
    }, delay)
  }

  /**
   * 处理WebSocket收到的消息
   * 消息类型: newMessage, messageSent, friendNotification, unreadCount, error
   */
  function handleWsMessage(data: string) {
    try {
      const msg = JSON.parse(data)
      switch (msg.type) {
        case 'newMessage':
          // 收到新消息：更新当前消息列表 & 刷新会话列表
          onNewMessage(msg.data)
          break
        case 'messageSent':
          settleWsSend(msg.data)
          onMessageSent(msg.data)
          break
        case 'friendNotification':
          // 好友申请通知：刷新待处理列表
          fetchPendingRequests()
          break
        case 'unreadCount':
          // 未读消息数更新
          totalUnreadCount.value = msg.data || 0
          break
        case 'error':
          console.error('[WebSocket] 服务端错误:', msg.message)
          rejectPendingWsSends(msg.message || '消息发送失败')
          break
      }
    } catch (e) {
      console.error('[WebSocket] 消息解析失败', e)
    }
  }

  /**
   * 处理收到的新消息
   */
  function onNewMessage(messageVO: API.ChatMessageVO) {
    // 如果消息属于当前打开的会话，追加到消息列表
    if (sameId(messageVO.conversationId, activeConversationId.value)) {
      const exists = currentMessages.value.some((m) => sameId(m.id, messageVO.id))
      if (!exists) {
        currentMessages.value.push(messageVO)
      }
    }
    // 更新未读数
    totalUnreadCount.value++
    scheduleConversationRefresh()
  }

  /**
   * 处理消息发送确认
   * 支持两种场景：1) 已有会话中发送 2) 待聊天模式首次发送（后端新建会话）
   */
  function onMessageSent(messageVO: API.ChatMessageVO) {
    if (pendingChatFriendId.value && !activeConversationId.value && messageVO.conversationId) {
      // 待聊天模式：后端已创建会话，更新activeConversationId
      activeConversationId.value = messageVO.conversationId
      // 用真实消息替换乐观插入的消息
      const optimistic = currentMessages.value.filter((m) => !m.id)
      if (optimistic.length > 0) {
        currentMessages.value = [messageVO]
      }
    } else if (sameId(messageVO.conversationId, activeConversationId.value)) {
      // 已有会话：用真实消息替换乐观消息（按内容匹配），或去重添加
      const optimisticIdx = currentMessages.value.findIndex(
        (m) => !m.id && m.content === messageVO.content,
      )
      if (optimisticIdx >= 0) {
        currentMessages.value.splice(optimisticIdx, 1, messageVO)
      } else {
        const exists = currentMessages.value.some((m) => sameId(m.id, messageVO.id))
        if (!exists) {
          currentMessages.value.push(messageVO)
        }
      }
    }
    scheduleConversationRefresh()
  }

  /**
   * 通过WebSocket发送聊天消息
   */
  async function sendWsMessage(receiverId: number, content: string, messageType: string = 'text') {
    const payload = {
      type: 'chat',
      payload: { receiverId, content, messageType },
    }
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      const ack = waitForWsAck(receiverId, content)
      try {
        ws.value.send(JSON.stringify(payload))
        return await ack.promise
      } catch (error) {
        ack.cancel()
        console.warn('[WebSocket] 发送失败，回退 HTTP 发送', error)
      }
    } else {
      console.warn('[WebSocket] 连接未建立，回退 HTTP 发送')
      connectWebSocket()
    }
    await sendHttpMessage(receiverId, content, messageType)
  }

  async function sendHttpMessage(
    receiverId: number,
    content: string,
    messageType: string = 'text',
  ) {
    try {
      const res = await sendMessageByHttp({ receiverId, content, messageType })
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '消息发送失败')
      }
      onMessageSent(res.data.data)
    } catch (error) {
      console.error('HTTP 发送消息异常', error)
      throw error
    }
  }

  /**
   * 通过WebSocket标记消息为已读
   */
  function sendWsRead(conversationId: number) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(
        JSON.stringify({
          type: 'read',
          payload: { conversationId },
        }),
      )
    }
  }

  // ===== 数据获取 =====

  function scheduleConversationRefresh(delay = 180) {
    if (conversationRefreshTimer) clearTimeout(conversationRefreshTimer)
    conversationRefreshTimer = setTimeout(() => {
      conversationRefreshTimer = null
      void fetchConversations()
    }, delay)
  }

  /** 加载会话列表 */
  async function fetchConversations() {
    const generation = sessionGeneration
    const requestId = ++conversationsRequestId
    try {
      const res = await listConversations()
      if (generation !== sessionGeneration || requestId !== conversationsRequestId) return
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '会话加载失败')
      }
      conversations.value = res.data.data
    } catch (e) {
      if (generation === sessionGeneration) console.error('获取会话列表失败', e)
    }
  }

  /** 加载好友列表 */
  async function fetchFriends() {
    const generation = sessionGeneration
    const requestId = ++friendsRequestId
    try {
      const res = await listFriends()
      if (generation !== sessionGeneration || requestId !== friendsRequestId) return
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '好友加载失败')
      }
      friends.value = res.data.data
    } catch (e) {
      if (generation === sessionGeneration) console.error('获取好友列表失败', e)
    }
  }

  /** 加载待处理好友申请列表 */
  async function fetchPendingRequests() {
    const generation = sessionGeneration
    const requestId = ++pendingRequestId
    try {
      const res = await listPendingRequests()
      if (generation !== sessionGeneration || requestId !== pendingRequestId) return
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '好友申请加载失败')
      }
      pendingRequests.value = res.data.data
      pendingCount.value = res.data.data.length
    } catch (e) {
      if (generation === sessionGeneration) console.error('获取待处理好友申请失败', e)
    }
  }

  /** 加载未读消息数 */
  async function fetchUnreadCount() {
    const generation = sessionGeneration
    const requestId = ++unreadRequestId
    try {
      const res = await getUnreadCount()
      if (generation !== sessionGeneration || requestId !== unreadRequestId) return
      if (res.data.code !== 0 || res.data.data === undefined) {
        throw new Error(res.data.message || '未读数加载失败')
      }
      totalUnreadCount.value = res.data.data
    } catch (e) {
      if (generation === sessionGeneration) console.error('获取未读消息数失败', e)
    }
  }

  // ===== UI 控制 =====

  /** 切换好友抽屉 */
  function toggleFriendDrawer() {
    friendDrawerVisible.value = !friendDrawerVisible.value
    if (friendDrawerVisible.value) {
      chatDrawerVisible.value = false
      fetchFriends()
      fetchPendingRequests()
    }
  }

  /** 切换聊天抽屉 */
  function toggleChatDrawer() {
    chatDrawerVisible.value = !chatDrawerVisible.value
    if (chatDrawerVisible.value) {
      friendDrawerVisible.value = false
      fetchConversations()
    }
  }

  /** 选中会话 */
  function selectConversation(conversationId: number) {
    activeConversationId.value = conversationId
    currentMessages.value = []
  }

  /**
   * 发起与指定好友的聊天
   * 从好友列表中点击「发消息」时调用：
   * 1. 关闭好友抽屉
   * 2. 设置待聊天好友id（先设置，避免抽屉打开时闪烁空列表）
   * 3. 打开聊天抽屉
   * ChatDrawer的watcher会自动检测pendingChatFriendId并查找/打开会话
   */
  function startChatWithFriend(friendId: number) {
    friendDrawerVisible.value = false
    activeConversationId.value = null
    currentMessages.value = []
    pendingChatFriendId.value = friendId
    chatDrawerVisible.value = true
  }

  /** 清除当前消息列表 */
  function clearCurrentMessages() {
    currentMessages.value = []
    activeConversationId.value = null
    pendingChatFriendId.value = null
  }

  /**
   * 初始化聊天模块
   * 建立WebSocket连接并加载初始数据
   */
  /** 清空旧账号的全部聊天缓存，避免 SPA 内切换账号时串数据。 */
  function resetChatState() {
    sessionGeneration += 1
    conversationsRequestId += 1
    friendsRequestId += 1
    pendingRequestId += 1
    unreadRequestId += 1
    disconnectWebSocket()
    conversations.value = []
    activeConversationId.value = null
    currentMessages.value = []
    friends.value = []
    pendingRequests.value = []
    totalUnreadCount.value = 0
    pendingCount.value = 0
    friendDrawerVisible.value = false
    chatDrawerVisible.value = false
    pendingChatFriendId.value = null
  }

  async function initChat() {
    connectWebSocket()
    await Promise.all([fetchUnreadCount(), fetchPendingRequests(), fetchConversations()])
  }

  return {
    // 状态
    wsConnected,
    conversations,
    activeConversationId,
    currentMessages,
    friends,
    pendingRequests,
    totalUnreadCount,
    pendingCount,
    friendDrawerVisible,
    chatDrawerVisible,
    pendingChatFriendId,
    // WebSocket
    connectWebSocket,
    disconnectWebSocket,
    sendWsMessage,
    sendWsRead,
    // 数据
    fetchConversations,
    fetchFriends,
    fetchPendingRequests,
    fetchUnreadCount,
    // UI
    toggleFriendDrawer,
    toggleChatDrawer,
    selectConversation,
    clearCurrentMessages,
    startChatWithFriend,
    resetChatState,
    initChat,
  }
})
