// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function getMyAiUserProfile(options?: { [key: string]: any }) {
  return request<API.BaseResponseAiUserProfile>('/generator/userProfile/my', {
    method: 'GET',
    ...(options || {})
  })
}

export async function updateMyAiUserProfile(body: API.AiUserProfileUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseAiUserProfile>('/generator/userProfile/update/my', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}
