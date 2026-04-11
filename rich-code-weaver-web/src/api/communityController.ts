// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function addCommunityPost(
  body: API.CommunityPostAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/social/community/post/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

export async function listCommunityPostByPage(
  body: API.CommunityPostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/social/community/post/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

export async function getCommunityPostVOById(
  params: { id: number | string; increaseView?: boolean },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCommunityPostVO>('/social/community/post/get/vo', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

export async function toggleCommunityPostLike(
  params: { postId: number | string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/community/post/like', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

export async function updateCommunityPostTopStatus(
  params: { postId: number | string; isTop: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/community/post/top', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

export async function deleteCommunityPost(
  params: { postId: number | string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/community/post/delete', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}

export async function addCommunityReply(
  body: API.CommunityReplyAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/social/community/reply/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

export async function listCommunityReplyByPage(
  body: API.CommunityReplyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityReplyVO>('/social/community/reply/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

export async function toggleCommunityReplyLike(
  params: { replyId: number | string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/community/reply/like', {
    method: 'POST',
    params: { ...params },
    ...(options || {})
  })
}
