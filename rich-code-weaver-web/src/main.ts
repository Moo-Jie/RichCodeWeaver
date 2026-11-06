import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { message } from 'ant-design-vue'

import App from './App.vue'
import router from './router'

import { installAntdComponents } from '@/plugins/antd'
import 'ant-design-vue/dist/reset.css'
import '@/styles/library.css'

import '@/access'

const app = createApp(App)
let lastGlobalErrorAt = 0

const reportGlobalError = (error: unknown, context: string) => {
  console.error(context, error)
  if (error instanceof DOMException && error.name === 'AbortError') return
  const now = Date.now()
  if (now - lastGlobalErrorAt < 3000) return
  lastGlobalErrorAt = now
  message.error('页面运行异常，请刷新后重试')
}

app.config.errorHandler = (error, _instance, info) => {
  reportGlobalError(error, `[Vue] ${info}`)
}

window.addEventListener('unhandledrejection', (event) => {
  reportGlobalError(event.reason, '[Unhandled Promise]')
})

app.use(createPinia())
app.use(router)
installAntdComponents(app)

app.mount('#app')
