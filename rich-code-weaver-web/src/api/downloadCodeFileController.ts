// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /download/code/zip/${param0} */
export async function downloadCodeZipFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadCodeZipFileParams,
  options?: { [key: string]: any }
) {
  const {appId: param0, ...queryParams} = params
  return request<any>(`/download/code/zip/${param0}`, {
    method: 'GET',
    params: {...queryParams},
    ...(options || {}),
  })
}
