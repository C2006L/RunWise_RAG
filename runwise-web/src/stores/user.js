import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

// 登录态 store（工程计划 2.2 / 5.1 v1.3）：双 token + userInfo，持久化到 localStorage
const ACCESS_KEY = 'rw_access_token'
const REFRESH_KEY = 'rw_refresh_token'
const USER_KEY = 'rw_user_info'

// LoginResult（后端 LoginVO）→ 前端 UserInfo 的映射收口（工程计划 5.1：仅此一处做映射）
function toUserInfo(result, username = '') {
  return {
    id: String(result.userId),
    username, // 账密登录名，NavBar 一律显示 nickname
    nickname: result.nickname,
    avatar: result.avatarUrl || '',
    joinedDays: 128, // mock 固定值；联调后改为按用户创建时间计算
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
    setSession(result, username) {
      this.accessToken = result.accessToken
      this.refreshToken = result.refreshToken
      this.userInfo = toUserInfo(result, username ?? this.userInfo?.username ?? '')
      this.persist()
    },
    async login(username, password) {
      const result = await authApi.login(username, password)
      this.setSession(result, username)
      return result
    },
    async fetchProfile() {
      const result = await authApi.getProfile()
      this.userInfo = toUserInfo(result, this.userInfo?.username ?? '')
      this.persist()
      return this.userInfo
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 登出接口失败不阻塞本地清理
      }
      this.reset()
    },
    // 清空登录态（登出 / 双 token 失效踢回时调用）
    reset() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
