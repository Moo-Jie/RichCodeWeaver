import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 配置反向代理,交由 vite 做代理转发，解决预览子网页和父网页不同源问题的本地解决方案
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8101',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
