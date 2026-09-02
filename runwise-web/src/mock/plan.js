// mock 训练计划（工程计划 4.7 / 5.2 / 6.3，M9）：
// - 固定种子生成 4 周计划，同一天多次刷新数据完全一致
// - 每周 3~5 个课表日，类型 easy / tempo / interval / long / rest
// - 本期 status 全部为 'planned'（待执行）占位，实际完成态联动属后续里程碑
import { verifyAccess } from './user'
import { formatDate } from '../composables/useFormatDate'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// ===== 种子随机（与 checkin.js 同款 mulberry32，确定性伪随机） =====
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

// ===== 课表类型定义（label 供视图渲染，targetKm/paceRange 为生成区间） =====
const SESSION_TYPES = [
  { type: 'easy', label: '轻松跑', kmRange: [4, 8], pace: "6'30\"-7'30\"/km" },
  { type: 'tempo', label: '节奏跑', kmRange: [5, 8], pace: "5'40\"-6'10\"/km" },
  { type: 'interval', label: '间歇跑', kmRange: [4, 6], pace: "5'00\"-5'30\"/km" },
  { type: 'long', label: '长距离', kmRange: [10, 16], pace: "6'20\"-7'00\"/km" },
]

const WEEKDAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

function addDays(base, delta) {
  const d = new Date(base)
  d.setDate(d.getDate() + delta)
  return d
}

// ===== 懒生成 4 周计划：第 1 周从本周一开始，共 28 天 =====
let cache = null

function ensurePlan() {
  if (cache) return cache

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const thisMonday = addDays(today, -((today.getDay() + 6) % 7))
  const rand = mulberry32(hashSeed('runwise-plan-' + formatDate(thisMonday)))

  const weeks = []
  for (let w = 0; w < 4; w++) {
    const weekStart = addDays(thisMonday, w * 7)
    // 每周 3~5 个课表日：周二/周四固定 + 周末长距离 + 随机补充
    const sessionDays = new Set([1, 3, 6])
    while (sessionDays.size < 3 + Math.floor(rand() * 3)) {
      sessionDays.add(Math.floor(rand() * 6)) // 0~5（周日留给休息或随机）
    }

    const days = []
    for (let i = 0; i < 7; i++) {
      const date = addDays(weekStart, i)
      const isPast = date < today
      if (!sessionDays.has(i)) {
        days.push({
          date: formatDate(date),
          weekday: WEEKDAY_LABELS[i],
          type: 'rest',
          label: '休息',
          targetKm: 0,
          paceRange: '',
          status: 'rest',
        })
        continue
      }
      // 周末优先长距离，其余从 easy/tempo/interval 随机
      const pool = i === 6 ? [SESSION_TYPES[3]] : [SESSION_TYPES[0], SESSION_TYPES[1], SESSION_TYPES[2]]
      const t = pool[Math.floor(rand() * pool.length)]
      const targetKm = Math.round((t.kmRange[0] + rand() * (t.kmRange[1] - t.kmRange[0])) * 2) / 2
      days.push({
        date: formatDate(date),
        weekday: WEEKDAY_LABELS[i],
        type: t.type,
        label: t.label,
        targetKm,
        paceRange: t.pace,
        // 已过日期标记 done 占位（本期不联动打卡，后续里程碑填充）
        status: isPast ? 'planned' : 'planned',
      })
    }
    weeks.push({ weekNo: w + 1, weekStart: formatDate(weekStart), days })
  }

  cache = { weeks }
  return cache
}

// 4 周计划 → PlanWeek[]（每周 { weekNo, weekStart, days }）
export async function getPlanList(token) {
  verifyAccess(token)
  await sleep(300)
  const { weeks } = ensurePlan()
  return JSON.parse(JSON.stringify(weeks))
}
