// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 新增知识库文档（管理员） POST /generator/rag/add */
export async function addRagDocument(
  body: API.RagDocumentAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/generator/rag/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 更新知识库文档（管理员） POST /generator/rag/update */
export async function updateRagDocument(
  body: API.RagDocumentUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/generator/rag/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 删除知识库文档（管理员） POST /generator/rag/delete */
export async function deleteRagDocument(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/generator/rag/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 根据id获取文档详情（管理员） GET /generator/rag/get/vo */
export async function getRagDocumentVOById(
  params: { id: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagDocumentVO>('/generator/rag/get/vo', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 分页查询知识库文档（管理员） POST /generator/rag/list/page/vo */
export async function listRagDocumentByPage(
  body: API.RagDocumentQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageRagDocumentVO>('/generator/rag/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 手动触发向量库重新索引（管理员） POST /generator/rag/reindex */
export async function reindexRagDocuments(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/generator/rag/reindex', {
    method: 'POST',
    ...(options || {})
  })
}
