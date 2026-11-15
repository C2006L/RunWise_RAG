import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import { useCheckinStore } from './checkin'
import { sessionGuard } from '../utils/session'
import { staleSessionError } from '../../../shared/session-guard.mjs'

// 登录态 store（工程计划 2.2 / 5.1 v1.3）：双 token + userInfo，持久化到 localStorage
const ACCESS_KEY = 'rw_access_token'
const REFRESH_KEY = 'rw_refresh_token'
const USER_KEY = 'rw_user_info'

// LoginResult（后端 LoginVO）→ 前端 UserInfo 的映射收口（工程计划 5.1：仅此一处做映射）
function toUserInfo(result, username = '') {
  return {
    id: String(result.userId ?? result.id),
    username, // 账密登录名，NavBar 一律显示 nickname
    nickname: result.nickname,
    avatar: result.avatarUrl || result.avatar || '',
    joinedDays: result.createTime ? Math.max(1,Math.floor((Date.now()-new Date(result.createTime).getTime())/86400000)+1) : 0,
  }
}

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    // 刷新页面时从 localStorage 恢复登录态（验收：刷新不丢）
    accessToken: localStorage.getItem(ACCESS_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    userInfo: readUser(),
    sessionVerified: false,
  }),
  getters: {
    // 守卫用：双 token 齐备即视为已登录；有效性由 api 层校验（401 → 静默刷新），守卫不深究
    isLoggedIn: (state) => Boolean(state.accessToken && state.refreshToken),
  },
  actions: {
    persist() {
      localStorage.setItem(ACCESS_KEY, this.accessToken)
      localStorage.setItem(REFRESH_KEY, this.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
    },
    // 写入会话：登录成功 / 静默刷新成功共用（request.js 回调）
    setSession(result, username, refreshed = false) {
      if (!refreshed) sessionGuard.invalidate()
      if(this.userInfo?.id!==String(result.userId ?? result.id)) useCheckinStore().$reset()
      this.accessToken = result.accessToken
      this.sessionVerified = true
      this.refreshToken = result.refreshToken
      this.userInfo = toUserInfo(result, username ?? this.userInfo?.username ?? '')
      this.persist()
    },
    async login(username, password) {
      const epoch = sessionGuard.invalidate()
      const result = await authApi.login(username, password)
      if (!sessionGuard.isCurrent(epoch)) throw staleSessionError()
      this.setSession(result, username)
      return result
    },
    async register(username,password,confirmPassword) {
      const epoch = sessionGuard.invalidate()
      const result=await authApi.register(username,password,confirmPassword)
      if (!sessionGuard.isCurrent(epoch)) throw staleSessionError()
      this.setSession(result,username)
      return result
    },
    async fetchProfile() {
      const epoch = sessionGuard.current()
      const result = await authApi.getProfile()
      if (!sessionGuard.isCurrent(epoch)) throw staleSessionError()
      this.userInfo = toUserInfo(result, this.userInfo?.username ?? '')
      this.persist()
      return this.userInfo
    },
    async restoreSession() {
      if (!this.isLoggedIn) return false
      if (this.sessionVerified) return true
      const epoch=sessionGuard.current()
      try {
        const profile=await authApi.getProfile({bootstrap:true})
        if (!sessionGuard.isCurrent(epoch)) return false
        this.userInfo=toUserInfo(profile,this.userInfo?.username??'')
        this.sessionVerified=true;this.persist();return true
      } catch(error) {
        if(error?.response?.status===401 || error?.code==='SESSION_CHANGED') return false
        throw error
      }
    },
    async logout() {
      const token = this.accessToken
      this.reset()
      try {
        await authApi.logout(token)
      } catch {
        // 登出接口失败不阻塞本地清理
      }
    },
    // 清空登录态（登出 / 双 token 失效踢回时调用）
    reset() {
      sessionGuard.invalidate()
      useCheckinStore().$reset()
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      this.sessionVerified = false
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
