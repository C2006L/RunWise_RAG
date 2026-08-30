import { USE_MOCK, checkin as mockCheckin } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 打卡接口层（工程计划 6.3 签名 / 6.2 分流）：USE_MOCK 时转发 mock，组件与 store 无感知
// mock 通道同样经过 withAuthRetry：token 过期 → 401 → 静默刷新 → 重放，与 axios 拦截器同一队列

// 'YYYY-MM-DD' 闭区间列表
export function getCheckinList(startDate, endDate) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockCheckin.getCheckinList(startDate, endDate, useUserStore().accessToken)
    )
  }
  return instance
    .get('/checkin/list', { params: { startDate, endDate } })
    .then((res) => res.data.data)
}

// 单日记录，无则 null
export function getCheckinByDate(date) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockCheckin.getCheckinByDate(date, useUserStore().accessToken)
    )
  }
  return instance.get(`/checkin/date/${date}`).then((res) => res.data.data)
}

// 创建 / 补录打卡：pace 由 api 层推导写入（组件不传 pace）
export function createCheckin(payload) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockCheckin.createCheckin(payload, useUserStore().accessToken)
    )
  }
  return instance.post('/checkin', payload).then((res) => res.data.data)
}
