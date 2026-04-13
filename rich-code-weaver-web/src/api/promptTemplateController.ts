// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取匹配的模板列表 GET /prompt/promptTemplate/list/matched */
export async function listMatchedTemplates(
  params: { userIdentity?: string; userIndustry?: string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPromptTemplateVO>('/prompt/promptTemplate/list/matched', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** 根据id获取模板详情 GET /prompt/promptTemplate/get/vo */
export async function getPromptTemplateVOById(
  params: { id: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePromptTemplateVO>('/prompt/promptTemplate/get/vo', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** 新增模板（管理员） POST /prompt/promptTemplate/add */
export async function addPromptTemplate(
  body: API.PromptTemplateAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/prompt/promptTemplate/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 更新模板（管理员） POST /prompt/promptTemplate/update */
export async function updatePromptTemplate(
  body: API.PromptTemplateUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/prompt/promptTemplate/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 删除模板（管理员） POST /prompt/promptTemplate/delete */
export async function deletePromptTemplate(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/prompt/promptTemplate/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 分页查询模板（管理员） POST /prompt/promptTemplate/list/page/vo */
export async function listPromptTemplateByPage(
  body: API.PromptTemplateQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePromptTemplateVO>('/prompt/promptTemplate/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}
