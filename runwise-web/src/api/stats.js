import { USE_MOCK, stats as mockStats } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 统计接口层（工程计划 6.3 签名 / 6.2 分流）：USE_MOCK 时转发 mock，组件无感知
// 注：后端暂无独立周 / 月统计接口（现有 /api/checkin/stats 与 /api/checkin/calendar
// 字段口径不同），axios 分支按前端契约（工程计划 5.1）预留路径，联调期与后端对齐

// 周报 → WeeklyStats { weekDates, distances, totalKm, checkinCount, streakDays }
// offsetWeeks: Phase E1 时间翻页（0=本周 7 天窗口，1=上个窗口……只许向过去翻）
export function getWeeklyStats(offsetWeeks = 0) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockStats.getWeeklyStats(useUserStore().accessToken, offsetWeeks)
    )
  }
  return instance
    .get('/stats/weekly', { params: offsetWeeks ? { offset: offsetWeeks } : {} })
    .then((res) => res.data.data)
}

// 月报 → MonthlyStats { monthDates, distances, totalKm, checkinCount }
// month: 'YYYY-MM'，缺省当月
export function getMonthlyStats(month) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockStats.getMonthlyStats(month, useUserStore().accessToken)
    )
  }
  return instance
    .get('/stats/monthly', { params: month ? { month } : {} })
    .then((res) => res.data.data)
}
