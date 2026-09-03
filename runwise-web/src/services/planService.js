// 训练计划 service 层（Phase B3.4）：
// 组件只允许经此层读写计划数据，禁止直接操作 mock / api —— 将来换 axios 只改本层
// - getWeeks(): PlanWeek[]（含 4 周）
// - add(payload) / update(id, payload) / remove(id)
import * as planApi from '../api/plan'

export function getWeeks() {
  return planApi.getPlanList()
}

export function add(payload) {
  return planApi.addPlanItem(payload)
}

export function update(id, payload) {
  return planApi.updatePlanItem(id, payload)
}

export function remove(id) {
  return planApi.removePlanItem(id)
}
