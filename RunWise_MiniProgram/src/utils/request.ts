import {createSessionGuard,staleSessionError} from '../../../shared/session-guard.mjs'
const sessionGuard=createSessionGuard()
export const beginLogin=()=>sessionGuard.invalidate()
export const isCurrentLogin=(epoch:number)=>sessionGuard.isCurrent(epoch)
/**
 * 网络请求封装
 * 统一处理 token、错误提示、刷新 token
 */

/**
 * 后端 API 基础地址（全项目唯一来源，其他页面禁止再各自硬编码）
 * 注意：微信开发者工具中小程序沙箱的 localhost 不等于主机 localhost，
 *       必须使用实际的局域网 IP 地址，否则会请求超时。
 *       换网络后 IP 会变，改这里一处即可。
 * 生产环境需替换为正式域名（必须 HTTPS）。
 */
export const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8081').replace(/\/$/, '')

/** 获取本地存储的 access_token */
function getToken(): string {
  return uni.getStorageSync('accessToken') || ''
}

/** 获取本地存储的 refresh_token */
function getRefreshToken(): string {
  return uni.getStorageSync('refreshToken') || ''
}

/** 保存 token */
export function saveTokens(accessToken: string, refreshToken: string, refreshed = false) {
  if(!refreshed)sessionGuard.invalidate()
  uni.setStorageSync('accessToken', accessToken)
  uni.setStorageSync('refreshToken', refreshToken)
}

/** 清除 token */
export function clearTokens() {
  sessionGuard.invalidate()
  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('refreshToken')
  uni.removeStorageSync('userId')
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

/** Only authentication rejection expires an identity. Network/business errors preserve it. */
function authError(message:string){return Object.assign(new Error(message),{code:'AUTH_EXPIRED'})}
function expire(epoch:number):never {
  if(!sessionGuard.isCurrent(epoch))throw staleSessionError()
  clearTokens();uni.navigateTo({url:'/pages/account/login'});throw authError('登录已过期')
}
async function doRefresh(epoch:number):Promise<void>{
  return sessionGuard.refresh(async()=>{
    if(!sessionGuard.isCurrent(epoch))throw staleSessionError()
    const token=getRefreshToken()
    if(!token)throw authError('登录已过期')
    const res=await uni.request({url:`${BASE_URL}/api/auth/refresh`,method:'POST',header:{Authorization:`Bearer ${token}`},timeout:15000})
    if(!sessionGuard.isCurrent(epoch)||token!==getRefreshToken())throw staleSessionError()
    const body=res.data as ApiResponse<{accessToken:string;refreshToken:string}>
    if(res.statusCode===401||body?.code===401)throw authError('登录已过期')
    if(res.statusCode!==200||body?.code!==200)throw new Error(body?.message||'登录凭证刷新暂不可用，请重试')
    saveTokens(body.data.accessToken,body.data.refreshToken,true)
  })
}

export async function request<T=any>(url:string,method:'GET'|'POST'|'PUT'|'DELETE'='GET',data?:any):Promise<T>{
  const epoch=sessionGuard.current(),sentToken=getToken()
  const send=()=>uni.request({url:`${BASE_URL}${url}`,method,data,
    header:{'Content-Type':'application/json',...(getToken()?{Authorization:`Bearer ${getToken()}`}:{})},
    timeout:(url.endsWith('/qa/ask')||url.endsWith('/qa/plan-drafts'))?100000:15000})
  try {
    let res=await send()
    if(!sessionGuard.isCurrent(epoch))throw staleSessionError()
    let body=res.data as ApiResponse<T>
    if(res.statusCode===401||body?.code===401){
      try{if(sentToken===getToken())await doRefresh(epoch)}
      catch(error:any){if(error?.code==='AUTH_EXPIRED')expire(epoch);throw error}
      if(!sessionGuard.isCurrent(epoch))throw staleSessionError()
      res=await send()
      if(!sessionGuard.isCurrent(epoch))throw staleSessionError()
      body=res.data as ApiResponse<T>
      if(res.statusCode===401||body?.code===401)expire(epoch)
    }
    if(res.statusCode===200&&body?.code===200)return body.data as T
    const message=body?.message||'请求失败'
    uni.showToast({title:message,icon:'none'});throw new Error(message)
  }catch(error:any){
    if(!sessionGuard.isCurrent(epoch)&&error?.code!=='AUTH_EXPIRED')throw staleSessionError()
    if(error?.errMsg?.includes('request:fail'))uni.showToast({title:'网络连接失败，请稍后重试',icon:'none'})
    throw error
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
  const epoch=beginLogin()
  // 1. 获取微信 code
  const { code } = await uni.login({ provider: 'weixin' })
  if (!code || !isCurrentLogin(epoch)) return false

  // 2. 调用后端登录接口
  try {
    const res = await uni.request({
      url: `${BASE_URL}/api/auth/login`,
      method: 'POST',
      data: { code }
    })
    if(!isCurrentLogin(epoch))return false
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
