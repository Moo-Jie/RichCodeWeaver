import request from '@/request'

/** 邀请好友成为产物协作者 POST /collaborator/invite */
export async function inviteCollaborator(
  body: API.CollaboratorInviteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/collaborator/invite', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 处理协作邀请（接受/拒绝） POST /collaborator/handle */
export async function handleCollabInvitation(
  body: API.CollaboratorHandleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/collaborator/handle', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 移除协作者 POST /collaborator/remove */
export async function removeCollaborator(
  params: { appId: number; userId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/collaborator/remove', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取指定产物的协作者列表 GET /collaborator/list */
export async function listCollaborators(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppCollaboratorVO>('/collaborator/list', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取当前用户收到的待处理协作邀请列表 GET /collaborator/pending */
export async function listPendingCollabInvitations(
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppCollaboratorVO>('/collaborator/pending', {
    method: 'GET',
    ...(options || {})
  })
}

/** 检查当前用户是否为指定产物的协作者 GET /collaborator/check */
export async function checkCollaborator(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/collaborator/check', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}
