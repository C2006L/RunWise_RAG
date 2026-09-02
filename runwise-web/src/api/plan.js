import { USE_MOCK, plan as mockPlan } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 训练计划接口层（工程计划 6.3 签名 / 6.2 分流，M9 新增）
// 后端暂无对应接口，axios 分支按前端契约预留路径，联调期与后端对齐

// 4 周计划 → PlanWeek[] { weekNo, weekStart, days: PlanDay[] }
// PlanDay { date, weekday, type: 'easy'|'tempo'|'interval'|'long'|'rest', label, targetKm, paceRange, status }
export function getPlanList() {
  if (USE_MOCK) {
    return withAuthRetry(() => mockPlan.getPlanList(useUserStore().accessToken))
  }
  return instance.get('/plan/list').then((res) => res.data.data)
}
