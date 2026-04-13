import request from '@/request'

/** 邀请好友成为产物协作者 POST /user/collaborator/invite */
export async function inviteCollaborator(
  body: API.CollaboratorInviteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/user/collaborator/invite', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 处理协作邀请（接受/拒绝） POST /user/collaborator/handle */
export async function handleCollabInvitation(
  body: API.CollaboratorHandleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/collaborator/handle', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 移除协作者 POST /user/collaborator/remove */
export async function removeCollaborator(
  params: { appId: number; userId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/collaborator/remove', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取指定产物的协作者列表 GET /user/collaborator/list */
export async function listCollaborators(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppCollaboratorVO>('/user/collaborator/list', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 获取当前用户收到的待处理协作邀请列表 GET /user/collaborator/pending */
export async function listPendingCollabInvitations(
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppCollaboratorVO>('/user/collaborator/pending', {
    method: 'GET',
    ...(options || {})
  })
}

/** 检查当前用户是否为指定产物的协作者 GET /user/collaborator/check */
export async function checkCollaborator(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/collaborator/check', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}
