import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // 字符串简写写法：'/foo' -> 'http://localhost:4567/foo'
      // '/api' 是一个标识，所有以/api开头的请求都会被代理
      '/api': {
        target: 'http://localhost:8080', // 目标是我们的 Spring Boot 后端服务
        changeOrigin: true, // 需要改变源，这是必须的
        // rewrite: (path) => path.replace(/^\/api/, '') // 如果后端接口没有 /api 前缀，需要重写路径
        // 在我们的例子中，后端的URL就是 /api/auth/...，所以不需要rewrite
      }
    }
  }
})
