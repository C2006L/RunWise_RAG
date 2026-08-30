// 日期 / 配速格式化工具（工程计划 2.2）
// formatPace 为纯函数：表单实时预览与 mock/api 层配速推导共用同一实现，保证口径一致

const pad = (n) => String(n).padStart(2, '0')

// Date → 'YYYY-MM-DD'
export function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// Date → 'YYYY年M月D日'
export function formatDateCn(date) {
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

// 周一为首列的星期标签（与周口径统计一致）
export const WEEKDAY_LABELS = ['一', '二', '三', '四', '五', '六', '日']

// 由距离（km）与时长（min）推导配速，如 6'09"/km；入参非法返回空串
export function formatPace(distanceKm, durationMin) {
  const d = Number(distanceKm)
  const t = Number(durationMin)
  if (!d || !t || d <= 0 || t <= 0) return ''
  const secPerKm = Math.round((t * 60) / d)
  return `${Math.floor(secPerKm / 60)}'${pad(secPerKm % 60)}"/km`
}

// 组合式入口：组件内按需解构使用
export function useFormatDate() {
  return { formatDate, formatDateCn, weekdayLabels: WEEKDAY_LABELS }
}
