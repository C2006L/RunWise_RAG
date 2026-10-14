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

// 个人档案统计使用的完整记录集。
// Mock 继续复用同一份打卡数据；真实环境分页读取现有 /checkin/list，
// 只在前端做字段归一化，不新增或修改后端接口。
export async function getAllCheckins() {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockCheckin.getCheckinList(
        '2000-01-01',
        '2099-12-31',
        useUserStore().accessToken,
      ),
    )
  }

  const pageSize = 200
  const firstResponse = await instance.get('/checkin/list', {
    params: { pageNum: 1, pageSize },
  })
  const firstPage = firstResponse.data.data || {}
  const total = Number(firstPage.total || 0)
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const remainingRequests = []

  for (let pageNum = 2; pageNum <= pageCount; pageNum += 1) {
    remainingRequests.push(
      instance.get('/checkin/list', { params: { pageNum, pageSize } }),
    )
  }

  const remainingResponses = await Promise.all(remainingRequests)
  const rawRecords = [
    ...(firstPage.list || []),
    ...remainingResponses.flatMap((response) => response.data.data?.list || []),
  ]

  return rawRecords
    .map((record) => ({
      id: record.id,
      date: record.checkinDate,
      distanceKm: Number(record.distance || 0),
      durationMin: Number(record.duration || 0),
      pace: record.pace,
      mood: record.mood,
      note: record.remark || '',
      createTime: record.createTime,
    }))
    .sort((a, b) => a.date.localeCompare(b.date))
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
