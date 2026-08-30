import { USE_MOCK, auth as mockAuth } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 认证接口层（工程计划 6.3 签名 / 6.2 分流）：USE_MOCK 时全部转发 mock，组件层无感知
// 注：与 request.js / stores/user.js 存在模块循环引用，但互调均发生在运行时（函数体内），ESM 安全

// 账密登录 → 联调期对应 POST /api/auth/login-password（附 device: 'web'，见第 9 章⑨）
export function login(username, password) {
  if (USE_MOCK) return mockAuth.login(username, password)
  return instance
    .post('/auth/login-password', { username, password, device: 'web' })
    .then((res) => res.data.data) // 联调期按后端 Result 包装核对取值路径
}

// 刷新 token：仅 request.js 的刷新队列内部调用，业务代码不直接使用
export function refreshToken() {
  const token = useUserStore().refreshToken
  if (USE_MOCK) return mockAuth.refreshToken(token)
  return instance
    .post('/auth/refresh', { refreshToken: token }, { __skipAuthRetry: true }) // 标记防拦截器死循环
    .then((res) => res.data.data)
}

// 获取用户信息：mock 通道同样经过 withAuthRetry（token 无效 → 静默刷新 → 重放，与 axios 拦截器同一队列）
export function getProfile() {
  if (USE_MOCK) {
    return withAuthRetry(() => mockAuth.getProfile(useUserStore().accessToken))
  }
  return instance.get('/user/profile').then((res) => res.data.data)
}

// 登出
export function logout() {
  if (USE_MOCK) return mockAuth.logout()
  return instance.post('/auth/logout').then(() => undefined)
}
