import axios from 'axios'

/**
 * 获取所有 RAG 参数列表（管理员）
 */
export const listRagParams = () => {
  return axios.get<API.BaseResponseListRagParamVO>('/api/rag/param/list')
}

/**
 * 更新 RAG 参数值（管理员）
 */
export const updateRagParam = (data: API.RagParamUpdateRequest) => {
  return axios.post<API.BaseResponseBoolean>('/api/rag/param/update', data)
}
