/**
 * 网络请求封装
 * 统一处理 token、错误提示、刷新 token
 */

const BASE_URL = 'http://localhost:8080'

/** 获取本地存储的 access_token */
function getToken(): string {
  return uni.getStorageSync('accessToken') || ''
}

/** 获取本地存储的 refresh_token */
function getRefreshToken(): string {
  return uni.getStorageSync('refreshToken') || ''
}

/** 保存 token */
export function saveTokens(accessToken: string, refreshToken: string) {
  uni.setStorageSync('accessToken', accessToken)
  uni.setStorageSync('refreshToken', refreshToken)
}

/** 清除 token */
export function clearTokens() {
  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('refreshToken')
}

/** 是否已登录 */
export function isLoggedIn(): boolean {
  return !!getToken()
}

/** 通用 API 响应结构 */
interface ApiResponse<T = any> {
  code: number
  data: T
  message?: string
}

/** 刷新 token */
async function doRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const res = await uni.request({
      url: `${BASE_URL}/api/auth/refresh`,
      method: 'POST',
      header: { Authorization: `Bearer ${refreshToken}` }
    })
    const body = res.data as ApiResponse<{ accessToken: string; refreshToken: string }>
    if (res.statusCode === 200 && body?.code === 200) {
      saveTokens(body.data.accessToken, body.data.refreshToken)
      return true
    }
    return false
  } catch {
    return false
  }
}

/** 统一请求方法 */
export async function request<T = any>(
  url: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET',
  data?: any
): Promise<T> {
  const header: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  const token = getToken()
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  try {
    const res = await uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header,
      timeout: 180000  // 超时180秒=3分钟（Ollama CPU推理慢，复杂问题需要更长时间）
    })

    const body = res.data as ApiResponse<T>

    // 401 尝试刷新 token
    if (res.statusCode === 401) {
      const refreshed = await doRefresh()
      if (refreshed) {
        // 重试请求
        header['Authorization'] = `Bearer ${getToken()}`
        const retryRes = await uni.request({
          url: `${BASE_URL}${url}`,
          method,
          data,
          header
        })
        const retryBody = retryRes.data as ApiResponse<T>
        if (retryRes.statusCode === 200 && retryBody?.code === 200) {
          return retryBody.data as T
        }
      }
      // 刷新失败，跳转登录
      clearTokens()
      uni.reLaunch({ url: '/pages/index/index' })
      throw new Error('登录已过期')
    }

    if (res.statusCode === 200 && body?.code === 200) {
      return body.data as T
    }

    // 业务错误
    const msg = body?.message || '请求失败'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  } catch (err: any) {
    // 网络错误
    if (err?.errMsg?.includes('request:fail')) {
      uni.showToast({ title: '网络连接失败', icon: 'none' })
    }
    throw err
  }
}

/** GET 请求 */
export function get<T = any>(url: string): Promise<T> {
  return request<T>(url, 'GET')
}

/** POST 请求 */
export function post<T = any>(url: string, data?: any): Promise<T> {
  return request<T>(url, 'POST', data)
}

/** PUT 请求 */
export function put<T = any>(url: string, data?: any): Promise<T> {
  return request<T>(url, 'PUT', data)
}

/** DELETE 请求 */
export function del<T = any>(url: string): Promise<T> {
  return request<T>(url, 'DELETE')
}

/** 微信登录 */
export async function wxLogin(): Promise<boolean> {
  // 1. 获取微信 code
  const { code } = await uni.login({ provider: 'weixin' })
  if (!code) return false

  // 2. 调用后端登录接口
  try {
    const res = await uni.request({
      url: `${BASE_URL}/api/auth/login`,
      method: 'POST',
      data: { code }
    })
    const body = res.data as ApiResponse<{ accessToken: string; refreshToken: string; userId: number }>
    if (res.statusCode === 200 && body?.code === 200) {
      const { accessToken, refreshToken, userId } = body.data
      saveTokens(accessToken, refreshToken)
      uni.setStorageSync('userId', userId)
      return true
    }
    return false
  } catch {
    return false
  }
}