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

// ===== Phase B3：计划项增删改（组件经 planService 调用，禁止直连本层以外） =====
// payload { weekNo, weekdayIndex, type, targetKm, note }
export function addPlanItem(payload) {
  if (USE_MOCK) {
    return withAuthRetry(() => mockPlan.addPlanDay(payload, useUserStore().accessToken))
  }
  return instance.post('/plan/item', payload).then((res) => res.data.data)
}

export function updatePlanItem(id, payload) {
  if (USE_MOCK) {
    return withAuthRetry(() => mockPlan.updatePlanDay(id, payload, useUserStore().accessToken))
  }
  return instance.put(`/plan/item/${id}`, payload).then((res) => res.data.data)
}

export function removePlanItem(id) {
  if (USE_MOCK) {
    return withAuthRetry(() => mockPlan.removePlanDay(id, useUserStore().accessToken))
  }
  return instance.delete(`/plan/item/${id}`).then((res) => res.data.data)
}
