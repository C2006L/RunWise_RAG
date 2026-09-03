// mock 训练计划（工程计划 4.7 / 5.2 / 6.3，M9；Phase B3 会话内增删改）：
// - 固定种子生成 4 周计划，同一天多次刷新数据完全一致
// - 每周 3~5 个课表日，类型 easy / tempo / interval / long / rest
// - 本期 status 全部为 'planned'（待执行）占位，实际完成态联动属后续里程碑
// - Phase B3：addPlanDay / updatePlanDay / removePlanDay 直接改 cache，
//   会话内持久（刷新后重置为种子数据，mock 语义）
import { verifyAccess } from './user'
import { formatDate } from '../composables/useFormatDate'
import { resolveTrainingType } from '../constants/trainingTypes'

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
          id: `w${w}-${i}`,
          date: formatDate(date),
          weekday: WEEKDAY_LABELS[i],
          type: 'rest',
          label: '休息',
          targetKm: 0,
          paceRange: '',
          note: '',
          status: 'rest',
        })
        continue
      }
      // 周末优先长距离，其余从 easy/tempo/interval 随机
      const pool = i === 6 ? [SESSION_TYPES[3]] : [SESSION_TYPES[0], SESSION_TYPES[1], SESSION_TYPES[2]]
      const t = pool[Math.floor(rand() * pool.length)]
      const targetKm = Math.round((t.kmRange[0] + rand() * (t.kmRange[1] - t.kmRange[0])) * 2) / 2
      days.push({
        id: `w${w}-${i}`,
        date: formatDate(date),
        weekday: WEEKDAY_LABELS[i],
        type: t.type,
        label: t.label,
        targetKm,
        paceRange: t.pace,
        note: '',
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

// ===== Phase B3：计划项增删改（会话内改 cache；label 由类型映射表统一出） =====
// 同一(周, 星期)已有非休息课表时，新增覆盖原项（一期简化：每星期至多一条课表）
function findWeek(weekNo) {
  const { weeks } = ensurePlan()
  return weeks.find((w) => w.weekNo === weekNo)
}

function findDayById(id) {
  const { weeks } = ensurePlan()
  for (const w of weeks) {
    const day = w.days.find((d) => d.id === id)
    if (day) return { week: w, day }
  }
  return null
}

function buildDay(weekNo, weekdayIndex, type, targetKm, note) {
  const week = findWeek(weekNo)
  const t = resolveTrainingType(type)
  const paceTable = SESSION_TYPES.find((s) => s.type === type)
  const km = type === 'rest' ? 0 : Math.max(0, Math.round(Number(targetKm) * 10) / 10)
  return {
    id: `w${weekNo}-${weekdayIndex}-u${Date.now() % 100000}`,
    date: formatDate(addDays(new Date(week.weekStart), weekdayIndex)),
    weekday: WEEKDAY_LABELS[weekdayIndex],
    type,
    label: t.label,
    targetKm: km,
    paceRange: paceTable ? paceTable.pace : '',
    note: (note || '').trim(),
    status: type === 'rest' ? 'rest' : 'planned',
  }
}

// 新增：payload { weekNo, weekdayIndex(0=周一), type, targetKm, note }
export async function addPlanDay(payload, token) {
  verifyAccess(token)
  await sleep(150)
  const week = findWeek(payload.weekNo)
  if (!week) throw new Error('week not found')
  const day = buildDay(payload.weekNo, payload.weekdayIndex, payload.type, payload.targetKm, payload.note)
  const idx = week.days.findIndex((d) => d.weekday === day.weekday)
  if (idx >= 0) week.days.splice(idx, 1, day)
  else week.days.push(day)
  week.days.sort((a, b) => a.weekday.localeCompare(b.weekday, 'zh'))
  return JSON.parse(JSON.stringify(day))
}

// 编辑：按 id 全量更新可变字段
export async function updatePlanDay(id, payload, token) {
  verifyAccess(token)
  await sleep(150)
  const hit = findDayById(id)
  if (!hit) throw new Error('plan day not found')
  const weekdayIndex = WEEKDAY_LABELS.indexOf(payload.weekday)
  const day = buildDay(hit.week.weekNo, weekdayIndex, payload.type, payload.targetKm, payload.note)
  day.id = id // 保持 id 稳定
  const idx = hit.week.days.findIndex((d) => d.id === id)
  hit.week.days.splice(idx, 1, day)
  hit.week.days.sort((a, b) => a.weekday.localeCompare(b.weekday, 'zh'))
  return JSON.parse(JSON.stringify(day))
}

// 删除：恢复为休息日（课表每天必须有行，删除 = 清空该日训练）
export async function removePlanDay(id, token) {
  verifyAccess(token)
  await sleep(150)
  const hit = findDayById(id)
  if (!hit) throw new Error('plan day not found')
  const idx = hit.week.days.findIndex((d) => d.id === id)
  const weekdayIndex = WEEKDAY_LABELS.indexOf(hit.day.weekday)
  const rest = buildDay(hit.week.weekNo, weekdayIndex, 'rest', 0, '')
  rest.id = id
  hit.week.days.splice(idx, 1, rest)
  return JSON.parse(JSON.stringify(rest))
}
