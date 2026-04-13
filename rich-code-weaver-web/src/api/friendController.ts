import request from '@/request'

/** 发送好友申请 POST /user/friend/add */
export async function sendFriendRequest(
  body: API.FriendAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/user/friend/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 处理好友申请（同意/拒绝） POST /user/friend/handle */
export async function handleFriendRequest(
  body: API.FriendHandleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/friend/handle', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 删除好友 POST /user/friend/remove */
export async function removeFriend(
  params: { friendId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/friend/remove', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取好友列表 GET /user/friend/list */
export async function listFriends(options?: { [key: string]: any }) {
  return request<API.BaseResponseListUserFriendshipVO>('/user/friend/list', {
    method: 'GET',
    ...(options || {})
  })
}

/** 获取待处理好友申请列表 GET /user/friend/pending */
export async function listPendingRequests(options?: { [key: string]: any }) {
  return request<API.BaseResponseListUserFriendshipVO>('/user/friend/pending', {
    method: 'GET',
    ...(options || {})
  })
}

/** 搜索用户（用于添加好友） GET /user/friend/search */
export async function searchUsers(
  params: { keyword: string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListUserVO>('/user/friend/search', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}
