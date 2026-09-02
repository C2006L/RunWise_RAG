// mock 数据统计（工程计划 5.1 结构 / 5.2 职责 / 5.4 口径，UI 精修 v2.0 修订）
// 全部数字从 mock/checkin 的同一记录集实时计算，不另造数据 ——
// 保证首页数据条、打卡页、统计页三处数字必然一致
// 口径（工程计划 5.4 + UI 精修 P1-6）：
// - streakDays：以今天为终点的连续打卡天数（由 checkin 断点算法决定，5~7 浮动）
// - weekDates / distances / checkinCount / totalKm：统一近 7 天滚动口径
//   （今天−6 至今天），图表与汇总卡共用同一数组，卡片数字 = 柱高之和
import { formatDate } from '../composables/useFormatDate'
import { getRawRecords } from './checkin'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function addDays(base, delta) {
  const d = new Date(base)
  d.setDate(d.getDate() + delta)
  return d
}

// 周报：近 7 天滚动窗口（今天−6 至今天）里程 + 打卡数 + 连续天数
// UI 精修 P1-6：图表与汇总卡共用同一数组计算（卡片数字 = 柱高之和），
// 任意一天访问 7 根柱齐，消除「自然周图表 × 滚动口径卡片」的逻辑打架
export async function getWeeklyStats(token) {
  const records = getRawRecords(token)
  await sleep(200)

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // 图表与汇总共用：近 7 天逐日里程（今天−6 至今天，时间正序）
  // 先逐日取整再求和，保证 totalKm 严格等于柱高之和
  const weekDates = []
  const distances = []
  let checkinCount = 0
  for (let i = 6; i >= 0; i--) {
    const key = formatDate(addDays(today, -i))
    const record = records.get(key)
    weekDates.push(key)
    distances.push(record ? Math.round(record.distanceKm * 10) / 10 : 0)
    if (record) checkinCount++
  }
  const totalKm = Math.round(distances.reduce((s, v) => s + v, 0) * 10) / 10

  // streakDays：以今天为终点向前数连续打卡天数
  let streakDays = 0
  for (let i = 0; ; i++) {
    if (records.has(formatDate(addDays(today, -i)))) streakDays++
    else break
  }

  return {
    weekDates,
    distances,
    totalKm,
    checkinCount,
    streakDays,
  }
}

// 月报：month 为 'YYYY-MM'（缺省当月），1 日至月末逐日里程
export async function getMonthlyStats(month, token) {
  const records = getRawRecords(token)
  await sleep(200)

  const now = new Date()
  let year = now.getFullYear()
  let monthIdx = now.getMonth()
  if (month) {
    const [y, m] = month.split('-').map(Number)
    if (Number.isFinite(y) && Number.isFinite(m) && m >= 1 && m <= 12) {
      year = y
      monthIdx = m - 1
    }
  }

  const days = new Date(year, monthIdx + 1, 0).getDate()
  const monthDates = []
  const distances = []
  let totalKm = 0
  let checkinCount = 0
  for (let d = 1; d <= days; d++) {
    const key = formatDate(new Date(year, monthIdx, d))
    const record = records.get(key)
    monthDates.push(key)
    const v = record ? record.distanceKm : 0
    distances.push(v ? Math.round(v * 10) / 10 : 0)
    totalKm += v
    if (record) checkinCount++
  }

  return {
    monthDates,
    distances,
    totalKm: Math.round(totalKm * 10) / 10,
    checkinCount,
  }
}
