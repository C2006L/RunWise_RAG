import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

// 登录 / 登出动作封装（工程计划 2.2 composables/useAuth）
export function useAuth() {
  const store = useUserStore()
  const route = useRoute()
  const router = useRouter()

  // 登录成功后优先回跳守卫写入的 redirect 目标，否则进首页
  async function login(username, password) {
    await store.login(username, password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    await router.replace(redirect)
  }

  async function logout() {
    await store.logout()
    if (route.path !== '/login') await router.replace('/login')
  }

  return { store, login, logout }
}
