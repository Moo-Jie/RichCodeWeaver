// @ts-ignore
/* eslint-disable */
import request from '@/request'

/**
 * 素材管理 API
 */

// ===== 素材分类接口 =====

/** 获取所有已启用的分类列表 GET /material/category/list/enabled */
export async function listEnabledCategories(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMaterialCategoryVO>('/material/category/list/enabled', {
    method: 'GET',
    ...(options || {})
  })
}

/** 根据ID获取分类详情 GET /material/category/get/vo */
export async function getMaterialCategoryVOById(
  params: { id: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMaterialCategoryVO>('/material/category/get/vo', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 添加素材分类（管理员） POST /material/category/add */
export async function addMaterialCategory(
  body: API.MaterialCategoryAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/material/category/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 更新素材分类（管理员） POST /material/category/update */
export async function updateMaterialCategory(
  body: API.MaterialCategoryUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/material/category/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 删除素材分类（管理员） POST /material/category/delete */
export async function deleteMaterialCategory(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/material/category/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 分页查询素材分类（管理员） POST /material/category/list/page/vo */
export async function listMaterialCategoryByPage(
  body: API.MaterialCategoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMaterialCategoryVO>('/material/category/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

// ===== 素材接口 =====

/** 添加素材 POST /material/add */
export async function addMaterial(
  body: API.MaterialAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/material/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 更新素材 POST /material/update */
export async function updateMaterial(
  body: API.MaterialUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/material/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 删除素材 POST /material/delete */
export async function deleteMaterial(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/material/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 根据ID获取素材详情 GET /material/get/vo */
export async function getMaterialVOById(
  params: { id: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMaterialVO>('/material/get/vo', {
    method: 'GET',
    params: { ...params },
    ...(options || {})
  })
}

/** 分页查询我的素材 POST /material/my/list/page/vo */
export async function listMyMaterialByPage(
  body: API.MaterialQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMaterialVO>('/material/my/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 分页查询公开素材 POST /material/public/list/page/vo */
export async function listPublicMaterialByPage(
  body: API.MaterialQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMaterialVO>('/material/public/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 查询可选素材列表（用于主页素材选择弹窗） POST /material/selectable/list/page/vo */
export async function listSelectableMaterialByPage(
  body: API.MaterialQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMaterialVO>('/material/selectable/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 根据ID列表批量获取素材 POST /material/batch/get */
export async function batchGetMaterials(
  body: number[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMaterialVO>('/material/batch/get', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}

/** 管理员分页查询所有素材 POST /material/admin/list/page/vo */
export async function listAllMaterialByPage(
  body: API.MaterialQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMaterialVO>('/material/admin/list/page/vo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {})
  })
}
