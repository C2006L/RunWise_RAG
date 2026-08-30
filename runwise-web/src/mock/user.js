// mock 用户与认证（工程计划 5.2 / 6.2，v1.3 契约对齐 LoginVO）
// 模拟后端行为：token 校验失败抛出 axios 错误形状（{ response: { status: 401 } }），
// 供 request.js 的 401 判定统一识别 —— 这是「篡改 token → 静默刷新」链路在 mock 下可验证的关键。
//
// token 规则：
// - accessToken：'mock-access-' + 签发时间戳，60 秒过期（模拟后端短效凭证，联调时后端为 2h）
// - refreshToken：'mock-refresh-' + 签发时间戳，7 天过期

const ACCESS_PREFIX = 'mock-access-'
const REFRESH_PREFIX = 'mock-refresh-'
const ACCESS_TTL = 60 * 1000
const REFRESH_TTL = 7 * 24 * 60 * 60 * 1000

// 预置统一用户「跑者小王」（工程计划 5.2）
const BASE_USER = {
  userId: '10086',
  nickname: '跑者小王',
  avatarUrl: '',
  isNewUser: false,
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// 构造 axios 错误形状的 401，模拟后端鉴权失败
const unauthorized = (message) => {
  const err = new Error(message)
  err.response = { status: 401 }
  return err
}

// 解析 token 中的签发时间戳；非法形状返回 null
function extractTs(token, prefix) {
  if (typeof token !== 'string' || !token.startsWith(prefix)) return null
  const ts = Number(token.slice(prefix.length))
  return Number.isFinite(ts) ? ts : null
}

// 签发新双 token
function genPair() {
  const now = Date.now()
  return { accessToken: ACCESS_PREFIX + now, refreshToken: REFRESH_PREFIX + now }
}

// 登录：任意非空账密成功，延迟 400ms（工程计划 5.2）
export async function login(username, password) {
  await sleep(400)
  if (!username || !password) {
    const err = new Error('账号或密码不能为空')
    err.response = { status: 400, data: { message: '账号或密码不能为空' } }
    throw err
  }
  return { ...genPair(), ...BASE_USER }
}

// 刷新 token：校验 refreshToken，有效则签发新双 token，无效抛 401
export async function refreshToken(refreshToken) {
  await sleep(200)
  const ts = extractTs(refreshToken, REFRESH_PREFIX)
  if (ts === null || Date.now() - ts >= REFRESH_TTL) {
    throw unauthorized('登录已过期')
  }
  return { ...genPair(), ...BASE_USER }
}

// 供其它 mock 模块复用：校验 accessToken，无效 / 过期抛 axios 形状 401
export function verifyAccess(token) {
  const ts = extractTs(token, ACCESS_PREFIX)
  if (ts === null || Date.now() - ts >= ACCESS_TTL) {
    throw unauthorized('访问凭证已过期')
  }
}

// 获取用户信息：校验 accessToken（模拟后端拦截器鉴权），无效/过期抛 401
export async function getProfile(accessToken) {
  await sleep(150)
  verifyAccess(accessToken)
  return { ...BASE_USER }
}

// 登出：假实现
export async function logout() {
  await sleep(100)
  return null
}
