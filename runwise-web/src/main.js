import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 全局样式按「变量 → 基座 → 效果类」顺序加载
import './styles/variables.css'
import './styles/base.css'
import './styles/p5-effects.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
