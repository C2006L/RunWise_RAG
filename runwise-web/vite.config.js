import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// M1 骨架配置：env 变量（VITE_API_BASE / VITE_USE_MOCK）由 Vite 内置机制读取，见 docs/工程计划.md 第 6 章
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
  },
})
