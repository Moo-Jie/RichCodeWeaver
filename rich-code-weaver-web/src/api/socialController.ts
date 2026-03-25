// @ts-ignore
/* eslint-disable */
import request from '@/request'

// ===== 热点统计 =====

/** 获取产物热点统计数据 GET /social/hotStat/get */
export async function getAppHotStat(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppHotStatVO>('/social/hotStat/get', {
    method: 'GET',
    params: { ...params },
    ...(options || {}),
  })
}

/** 分页查询热门产物列表 POST /social/hotStat/list/page */
export async function listHotApps(
  body: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppHotStat>('/social/hotStat/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

// ===== 点赞 =====

/** 切换产物点赞状态 POST /social/like */
export async function toggleAppLike(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/like', {
    method: 'POST',
    params: { ...params },
    ...(options || {}),
  })
}

// ===== 收藏 =====

/** 切换产物收藏状态 POST /social/favorite */
export async function toggleAppFavorite(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/favorite', {
    method: 'POST',
    params: { ...params },
    ...(options || {}),
  })
}

/** 分页查询我的收藏列表 POST /social/favorite/my/page */
export async function listMyFavorites(
  body: API.AppFavoriteQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppFavoriteRecord>('/social/favorite/my/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

// ===== 转发 =====

/** 记录产物转发行为 POST /social/share */
export async function doAppShare(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/share', {
    method: 'POST',
    params: { ...params },
    ...(options || {}),
  })
}

// ===== 评论 =====

/** 新增评论 POST /social/comment/add */
export async function addAppComment(
  body: API.AppCommentAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/social/comment/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 删除评论 POST /social/comment/delete */
export async function deleteAppComment(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/comment/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 分页查询评论列表 POST /social/comment/list/page */
export async function listAppCommentByPage(
  body: API.AppCommentQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppCommentVO>('/social/comment/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 切换评论点赞状态 POST /social/comment/like */
export async function toggleCommentLike(
  params: { commentId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/social/comment/like', {
    method: 'POST',
    params: { ...params },
    ...(options || {}),
  })
}
