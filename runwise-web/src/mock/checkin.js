// mock 打卡数据（工程计划 5.1 字段 / 5.3 断点生成算法）
// 核心规则（v1.2 修订版）：
// 1. 随机断点：以固定种子从 {今天−5、−6、−7} 取一天为强制休息日 R
// 2. 连续段：R+1 至今天强制打卡 → streakDays 为 5~7 浮动值
// 3. 本周一至今天除 R 外强制打卡（自然周全勤）
// 4. 其余日期 55% 概率随机打卡
// 5. 种子 = hash('runwise-' + 今天日期) → 同一天多次刷新数据完全一致
// 下界保证：连续段（≥5 天）整体位于近 7 天滚动窗口内 → checkinCount ≥ 5 > 4 恒成立
import { formatDate, formatPace } from '../composables/useFormatDate'
import { verifyAccess } from './user'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// ===== 种子随机（mulberry32）：确定性伪随机，保证同日数据稳定 =====
function hashSeed(str) {
  let h = 2166136261
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

function mulberry32(seed) {
  return function () {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

// 备注文案池（8 条，30% 概率取用）
const NOTES = [
  '轻松完成，状态在线',
  '夜跑很舒服，配速稳',
  '有点顶，但坚持下来了',
  '恢复跑，刻意放慢',
  '节奏跑训练，目标配速达成',
  '天气不错，跑得很顺',
  '腿部略有疲劳，明天休整',
  '和跑友一起拉了个长距离',
]

// 心情加权随机：great 20% / good 55% / tired 25%
function pickMood(rand) {
  const x = rand()
  return x < 0.2 ? 'great' : x < 0.75 ? 'good' : 'tired'
}

function addDays(base, delta) {
  const d = new Date(base)
  d.setDate(d.getDate() + delta)
  return d
}

// ===== 懒生成的模块级数据集（会话内提交的记录也落在同一 Map） =====
let cache = null

function ensureData() {
  if (cache) return cache

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const todayKey = formatDate(today)
  const rand = mulberry32(hashSeed('runwise-' + todayKey))

  // 1. 随机断点 R ∈ {今天−5、−6、−7}
  const restOffset = 5 + Math.floor(rand() * 3)

  // 本周一（周为首列的自然周口径）
  const weekStart = addDays(today, -((today.getDay() + 6) % 7))

  const records = new Map()

  const genRecord = (key) => {
    const distanceKm = Math.round((3 + rand() * 7) * 10) / 10 // 3.0 ~ 10.0 一位小数
    const paceMin = 5.2 + rand() * 2.3 // 5.2 ~ 7.5
    const durationMin = Math.max(10, Math.round(distanceKm * paceMin))
    const mood = pickMood(rand)
    const note = rand() < 0.3 ? NOTES[Math.floor(rand() * NOTES.length)] : ''
    return {
      id: 'ck-' + key,
      date: key,
      distanceKm,
      durationMin,
      pace: formatPace(distanceKm, durationMin), // 配速由距离时长推导
      mood,
      note,
    }
  }

  // 生成 [今天−89, 今天] 共 90 天
  for (let i = 89; i >= 0; i--) {
    const d = addDays(today, -i)
    const key = formatDate(d)
    const isRest = i === restOffset // R 强制休息
    const inStreak = i < restOffset // R+1 .. 今天 强制打卡
    const inWeek = d >= weekStart && d <= today // 本周一 .. 今天
    const forced = inStreak || (inWeek && !isRest)
    if (isRest) continue
    if (!forced && rand() >= 0.55) continue
    records.set(key, genRecord(key))
  }

  cache = { records, restOffset, todayKey }
  return cache
}

// 打卡列表：'YYYY-MM-DD' 闭区间过滤，按日期升序
export async function getCheckinList(startDate, endDate, token) {
  verifyAccess(token)
  await sleep(200)
  const { records } = ensureData()
  const list = []
  records.forEach((record, key) => {
    if (key >= startDate && key <= endDate) list.push(record)
  })
  return list.sort((a, b) => a.date.localeCompare(b.date))
}

// 单日查询：无记录返回 null
export async function getCheckinByDate(date, token) {
  verifyAccess(token)
  await sleep(120)
  const { records } = ensureData()
  return records.get(date) || null
}

// 创建 / 补录打卡：pace 由距离时长推导写入，按日期 upsert（当天重复提交即覆盖）
export async function createCheckin(data, token) {
  verifyAccess(token)
  await sleep(300)
  const { records } = ensureData()
  const record = {
    id: 'ck-' + data.date,
    date: data.date,
    distanceKm: data.distanceKm,
    durationMin: data.durationMin,
    pace: formatPace(data.distanceKm, data.durationMin),
    mood: data.mood,
    note: (data.note || '').trim(),
  }
  records.set(record.date, record)
  return { ...record }
}
