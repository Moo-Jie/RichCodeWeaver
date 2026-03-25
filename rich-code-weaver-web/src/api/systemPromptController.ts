// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 新增系统提示词（管理员） POST /systemPrompt/add */
export async function addSystemPrompt(
  body: API.SystemPromptAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/systemPrompt/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新系统提示词（管理员） POST /systemPrompt/update */
export async function updateSystemPrompt(
  body: API.SystemPromptUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/systemPrompt/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除系统提示词（管理员） POST /systemPrompt/delete */
export async function deleteSystemPrompt(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/systemPrompt/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据ID获取系统提示词详情（管理员） GET /systemPrompt/get/vo */
export async function getSystemPromptVOById(
  params: { id: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSystemPromptVO>('/systemPrompt/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页查询系统提示词（管理员） POST /systemPrompt/list/page/vo */
export async function listSystemPromptByPage(
  body: API.SystemPromptQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemPromptVO>('/systemPrompt/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取所有系统提示词列表（管理员） GET /systemPrompt/list/all */
export async function listAllSystemPrompts(options?: { [key: string]: any }) {
  return request<API.BaseResponseListSystemPromptVO>('/systemPrompt/list/all', {
    method: 'GET',
    ...(options || {}),
  })
}

