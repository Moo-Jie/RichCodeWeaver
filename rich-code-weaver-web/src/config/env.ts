/**
 * 环境 URL 变量配置
 */

// 应用部署域名
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://rubyyan.cn/userWeb'

// API 基础地址
// 默认用相对路径，方便本地 dev 走 Vite proxy，也方便同域部署
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 静态资源地址
export const STATIC_BASE_URL = `${API_BASE_URL}/app/view`

// 获取部署应用的完整URL
export const getDeployUrl = (deployKey: string) => {
  return `${DEPLOY_DOMAIN}/${deployKey}`
}

// 获取静态资源预览 URL
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  return `${STATIC_BASE_URL}/${appId}/index.html`
}

// 获取 WEB 工程项目静态资源预览 URL
export const getWebProjectStaticPreviewUrl = (codeGenType: string, appId: string) => {
  return `${STATIC_BASE_URL}/${appId}/dist/index.html`
}
