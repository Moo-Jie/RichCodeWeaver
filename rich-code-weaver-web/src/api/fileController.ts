// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /file/upload */
export async function upload(body: {}, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/file/upload', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** 上传文件到OSS（multipart/form-data，同步版本） POST /file/upload */
export async function uploadFile(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseString>('/file/upload', {
    method: 'POST',
    data: formData,
    ...(options || {})
  })
}

/** 上传文件到OSS（multipart/form-data，异步版本，推荐使用） POST /file/upload/async */
export async function uploadFileAsync(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseString>('/file/upload/async', {
    method: 'POST',
    data: formData,
    ...(options || {})
  })
}
