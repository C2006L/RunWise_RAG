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
    // 伤病预防 / AI 真实接口联调：RunWise 后端固定使用 8081，
    // 避开本机另一个项目占用的 8080。
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
