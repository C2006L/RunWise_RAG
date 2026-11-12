import axios from 'axios'
import { reactive } from 'vue'

// 与跑者端隔离；仅当前浏览器会话保存管理员凭证。
export const session = reactive({ token: sessionStorage.getItem('runwise.admin.token') || '', account: null })
export function clearSession() {
  session.token = ''; session.account = null
  sessionStorage.removeItem('runwise.admin.token')
}
export function setSession(data) {
  session.token = data.token; session.account = data.account
  sessionStorage.setItem('runwise.admin.token', data.token)
}
const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api/admin', timeout: 15000 })
client.interceptors.request.use(config => {
  if (session.token) config.headers.Authorization = `Bearer ${session.token}`
  return config
})
client.interceptors.response.use(response => {
  if (response.data.code !== 200) throw new Error(response.data.message || '操作失败')
  return response.data.data
}, error => {
  if (error.response?.status === 401 && !error.config?.url?.endsWith('/auth/login')) {
    clearSession()
    window.location.hash = '#/login'
  }
  return Promise.reject(new Error(error.response?.data?.message || '连接失败，请检查后端和数据库迁移'))
})
export default client
export const statusLabel = value => ({ DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下架' }[value] || value)
export const dateLabel = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
