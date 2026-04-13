// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /generator/app/add */
export async function addApp(body: API.AppAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/generator/app/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/delete */
export async function deleteApp(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/generator/app/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/delete/admin */
export async function deleteAppByAdmin(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/generator/app/delete/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/deploy */
export async function deployApp(body: API.AppDeployRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/generator/app/deploy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/gen/code */
export async function chatToGenCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatToGenCodeParams,
  options?: { [key: string]: any }
) {
  return request<string>('/generator/app/gen/code', {
    method: 'GET',
    params: {
      ...params,
      appCodeGenRequest: undefined,
      ...params['appCodeGenRequest']
    },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/gen/code/stream */
export async function chatToGenCodeStream(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatToGenCodeStreamParams,
  options?: { [key: string]: any }
) {
  return request<API.ServerSentEventString[]>('/generator/app/gen/code/stream', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/get/vo */
export async function getAppVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppVO>('/generator/app/get/vo', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/get/vo/admin */
export async function getAppVoByIdByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppVOByIdByAdminParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppVO>('/generator/app/get/vo/admin', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/good/list/page/vo */
export async function listStarAppVoByPage(
  body: API.AppQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/generator/app/good/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/list/page/vo/admin */
export async function listAppVoByPageByAdmin(
  body: API.AppQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/generator/app/list/page/vo/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/my/list/page/vo */
export async function listMyAppVoByPage(
  body: API.AppQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/generator/app/my/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/user/related/list */
export async function listUserRelatedApps(
  params: { userId: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppVO>('/generator/app/user/related/list', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/update */
export async function updateApp(body: API.AppUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/generator/app/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/update/admin */
export async function updateAppByAdmin(
  body: API.AppAdminUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/generator/app/update/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/app/upload/cover */
export async function uploadAppCover(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadAppCoverParams,
  body: {},
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/generator/app/upload/cover', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    params: {
      ...params
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/app/view/${param0}/&#42;&#42; */
export async function viewApp(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.viewAppParams,
  options?: { [key: string]: any }
) {
  const { appId: param0, ...queryParams } = params
  return request<string>(`/generator/app/view/${param0}/**`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {})
  })
}

/** 优化用户提示词 POST /generator/app/optimize/prompt */
export async function optimizePrompt(userPrompt: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/generator/app/optimize/prompt', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: userPrompt,
    ...(options || {})
  })
}
