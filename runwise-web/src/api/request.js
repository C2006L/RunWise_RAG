import axios from 'axios'
import { useUserStore } from '../stores/user'
import * as authApi from './auth'
import router from '../router'

// axios 实例（工程计划 6.1）：baseURL 读 env，超时 10s
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
})

// 请求拦截器：自动附加 Bearer accessToken
instance.interceptors.request.use((config) => {
  const token = useUserStore().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：业务码约定 code === 200 为成功（联调期与后端核对）
instance.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && body.code !== undefined && body.code !== 200) {
      showToast(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return res
  },
  async (err) => {
    const status = err.response?.status
    const config = err.config || {}
    // 401 且非刷新接口自身、未重放过 → 走统一静默刷新后重放（标记防死循环）
    if (status === 401 && !config.__viaAuthRetry && !config.__skipAuthRetry) {
      config.__viaAuthRetry = true
      await refreshSession()
      return instance.request(config) // 重放（请求拦截器会附加新 token）
    }
    if (status >= 500) showToast('服务异常，请稍后重试')
    else if (err.message === 'Network Error') showToast('网络异常，请检查连接')
    return Promise.reject(err)
  },
)

// ===== 静默刷新队列（工程计划 6.1 v1.3：先刷新、失败才踢回）=====
// 并发 401 共享同一次刷新：refreshing 非空时后续请求挂起等待，成功后各自重放，不重复刷新
let refreshing = null

function isUnauthorized(err) {
  return err?.response?.status === 401
}

async function refreshSession() {
  refreshing =
    refreshing ||
    (async () => {
      try {
        // 调用刷新接口（api/auth.js 内部完成 mock / axios 分流）
        const result = await authApi.refreshToken()
        useUserStore().setSession(result)
        return result
      } catch (err) {
        // 刷新失败（refreshToken 过期 / 被拒）→ 清双 token、踢回登录页、统一提示
        useUserStore().reset()
        showToast('登录已过期')
        const current = router.currentRoute.value
        if (current.path !== '/login') {
          router.push({ path: '/login', query: { redirect: current.fullPath } })
        }
        throw err
      } finally {
        refreshing = null
      }
    })()
  return refreshing
}

// 带 401 自动刷新重试的请求包装：mock 通道复用（与 axios 拦截器同一套刷新队列）
// requestFn 内部现取最新 token，重放时天然携带刷新后的新 token
export async function withAuthRetry(requestFn) {
  try {
    return await requestFn()
  } catch (err) {
    if (!isUnauthorized(err)) throw err
    await refreshSession() // 失败会向上抛（此时已 reset + 跳登录）
    return requestFn()
  }
}

// 极简全局提示：P5 红黑切角样式（M2 内嵌实现，后续如需多处复用再抽独立模块）
let toastTimer = null
function showToast(message) {
  let el = document.getElementById('rw-toast')
  if (!el) {
    el = document.createElement('div')
    el.id = 'rw-toast'
    el.style.cssText = [
      'position:fixed',
      'top:24px',
      'left:50%',
      'transform:translateX(-50%) translateY(-12px)',
      'z-index:9999',
      'padding:10px 28px',
      'background:#e60012',
      'color:#f5f0e6',
      'font-size:14px',
      'letter-spacing:.08em',
      'clip-path:polygon(10px 0,100% 0,calc(100% - 10px) 100%,0 100%)',
      'opacity:0',
      'pointer-events:none',
      'transition:opacity .25s, transform .25s',
    ].join(';')
    document.body.appendChild(el)
  }
  el.textContent = message
  requestAnimationFrame(() => {
    el.style.opacity = '1'
    el.style.transform = 'translateX(-50%) translateY(0)'
  })
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    el.style.opacity = '0'
    el.style.transform = 'translateX(-50%) translateY(-12px)'
  }, 2400)
}

export { instance }
