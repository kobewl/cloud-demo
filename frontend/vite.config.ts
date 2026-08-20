import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置。
 *
 * 两个关键点：
 * 1. resolve.alias：把 `@` 映射到 src 目录，代码里写 `@/api/xxx` 而不是 `../../api/xxx`。
 * 2. server.proxy：开发时把浏览器的 `/api` 请求转发到网关 8080。
 *    这样前端请求 `/api/product/list` 实际打到 `http://localhost:8080/api/product/list`，
 *    同源（都是 localhost:5173）就不会触发浏览器跨域限制，后端也无需开 CORS。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
