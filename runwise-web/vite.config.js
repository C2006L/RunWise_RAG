import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// M1 骨架配置：env 变量（VITE_API_BASE / VITE_USE_MOCK）由 Vite 内置机制读取，见 docs/工程计划.md 第 6 章
export default defineConfig({
  plugins: [vue()],
  build: {
    minify: 'terser',
    terserOptions: {
      compress: { drop_console: true, drop_debugger: true },
    },
  },
  server: {
    // 5173/5174/5175 被其他项目占用（Vite 端口被占时会 +1 自动顺延，
    // 多项目并存会形成级联抢占）——本项目固定用 5273，远离默认端口级联区
    port: 5273,
    open: false,
    // 伤病预防等真实接口联调：/api 透传到本机 Spring Boot(8080)，避免跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
