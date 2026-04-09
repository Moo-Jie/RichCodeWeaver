// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 DELETE /generator/chatHistory/admin/delete/${param0} */
export async function deleteById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/generator/chatHistory/admin/delete/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {})
  })
}

/** 此处后端没有提供注释 POST /generator/chatHistory/admin/list/page/vo */
export async function listAppChatHistoryByPageAdmin(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatHistory>('/generator/chatHistory/admin/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 此处后端没有提供注释 GET /generator/chatHistory/app/${param0} */
export async function listAppChatHistoryByPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAppChatHistoryByPageParams,
  options?: { [key: string]: any }
) {
  const { appId: param0, ...queryParams } = params
  return request<API.BaseResponsePageChatHistory>(`/generator/chatHistory/app/${param0}`, {
    method: 'GET',
    params: {
      // pageSize has a default value: 10
      pageSize: '10',
      ...queryParams
    },
    ...(options || {})
  })
}
