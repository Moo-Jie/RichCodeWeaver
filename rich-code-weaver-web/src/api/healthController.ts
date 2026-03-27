// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 用户模块健康检查 GET /user/health/check */
export async function checkUserHealth(options?: { [key: string]: any }) {
  return request<any>('/user/health/check', {
    method: 'GET',
    ...(options || {})
  })
}

/** 产物生成模块健康检查 GET /app/health/check */
export async function checkGeneratorHealth(options?: { [key: string]: any }) {
  return request<any>('/app/health/check', {
    method: 'GET',
    ...(options || {})
  })
}

/** 文件模块健康检查 GET /file/health/check */
export async function checkFileHealth(options?: { [key: string]: any }) {
  return request<any>('/file/health/check', {
    method: 'GET',
    ...(options || {})
  })
}

/** 提示词模板模块健康检查 GET /systemPrompt/health/check */
export async function checkPromptHealth(options?: { [key: string]: any }) {
  return request<any>('/systemPrompt/health/check', {
    method: 'GET',
    ...(options || {})
  })
}

/** 社交模块健康检查 GET /social/health/check */
export async function checkSocialHealth(options?: { [key: string]: any }) {
  return request<any>('/social/health/check', {
    method: 'GET',
    ...(options || {})
  })
}

/**
 * 批量检查所有模块健康状态
 */
export async function checkAllModulesHealth(): Promise<Record<string, boolean>> {
  const healthChecks = [
    { key: 'user', fn: checkUserHealth },
    { key: 'generator', fn: checkGeneratorHealth },
    { key: 'ai', fn: checkGeneratorHealth }, // AI模块和产物生成模块共用
    { key: 'file', fn: checkFileHealth },
    { key: 'prompt', fn: checkPromptHealth },
    { key: 'social', fn: checkSocialHealth }
  ]

  const results = await Promise.all(
    healthChecks.map(async ({ key, fn }) => {
      try {
        const res = await fn()
        const isHealthy = res.data.code === 0 && res.data.data?.status === 'UP'
        return { key, isHealthy }
      } catch (error) {
        console.warn(`Module ${key} health check failed:`, error)
        return { key, isHealthy: false }
      }
    })
  )

  const healthStatus: Record<string, boolean> = {}
  results.forEach(({ key, isHealthy }) => {
    healthStatus[key] = isHealthy
  })

  return healthStatus
}
