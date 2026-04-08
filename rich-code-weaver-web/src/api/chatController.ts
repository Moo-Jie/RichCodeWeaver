import request from '@/request'

/** 获取会话列表 GET /chat/conversations */
export async function listConversations(options?: { [key: string]: any }) {
  return request<API.BaseResponseListChatConversationVO>('/chat/conversations', {
    method: 'GET',
    ...(options || {})
  })
}

/** 分页查询会话消息 POST /chat/messages */
export async function listMessages(
  body: API.ChatMessageQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatMessageVO>('/chat/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 通过HTTP发送消息（WebSocket备用） POST /chat/send */
export async function sendMessage(
  body: API.ChatMessageSendRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatMessageVO>('/chat/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 标记会话消息为已读 POST /chat/read */
export async function markAsRead(
  params: { conversationId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat/read', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取未读消息总数 GET /chat/unread/count */
export async function getUnreadCount(options?: { [key: string]: any }) {
  return request<API.BaseResponseInteger>('/chat/unread/count', {
    method: 'GET',
    ...(options || {})
  })
}
