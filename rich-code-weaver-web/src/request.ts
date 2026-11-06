import axios from 'axios'
import { message } from 'ant-design-vue'
import { API_BASE_URL } from '@/config/env'

let sessionExpiryHandled = false

function handleSessionExpired(responseUrl?: string) {
  if (sessionExpiryHandled) return
  if (responseUrl?.includes('user/get/login')) return
  if (window.location.pathname.includes('/user/login')) return

  sessionExpiryHandled = true
  const redirect = encodeURIComponent(
    window.location.pathname + window.location.search + window.location.hash,
  )
  message.warning('登录状态已失效，请重新登录')
  window.location.replace('/user/login?redirect=' + redirect)
}

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      handleSessionExpired(response.request?.responseURL)
    }
    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

export default myAxios
