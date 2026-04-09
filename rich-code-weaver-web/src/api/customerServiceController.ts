// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function createCustomerServiceConversation(options?: { [key: string]: any }) {
  return request<API.BaseResponseCustomerServiceConversationVO>('/generator/customerService/conversation/create', {
    method: 'POST',
    ...(options || {})
  })
}

export async function listCustomerServiceConversations(options?: { [key: string]: any }) {
  return request<API.BaseResponseListCustomerServiceConversationVO>('/generator/customerService/conversation/list', {
    method: 'GET',
    ...(options || {})
  })
}

export async function listCustomerServiceMessages(
  params: API.listCustomerServiceMessagesParams,
  options?: { [key: string]: any }
) {
  const { conversationId: param0, ...queryParams } = params
  return request<API.BaseResponseListCustomerServiceMessageVO>(`/generator/customerService/message/list/${param0}`, {
    method: 'GET',
    params: {
      pageSize: '20',
      ...queryParams
    },
    ...(options || {})
  })
}
