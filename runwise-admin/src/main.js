import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import Login from './views/Login.vue'
import Articles from './views/Articles.vue'
import Editor from './views/Editor.vue'
import Logs from './views/Logs.vue'
import api, { session } from './api'
import './style.css'

const router = createRouter({ history: createWebHashHistory(), routes: [
  { path: '/', redirect: '/articles' },
  { path: '/login', component: Login },
  { path: '/articles', component: Articles },
  { path: '/articles/new', component: Editor },
  { path: '/articles/:id', component: Editor },
  { path: '/logs', component: Logs },
] })
router.beforeEach(async to => {
  if (to.path === '/login') return true
  if (!session.token) return '/login'
  if (!session.account) {
    try { session.account = await api.get('/auth/me') } catch { return '/login' }
  }
  if (to.path === '/logs' && session.account.role !== 'SUPER_ADMIN') return '/articles'
})
createApp(App).use(router).mount('#app')
