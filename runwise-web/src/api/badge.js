import { instance } from './request'

// 徽章接口层：解锁判定与进度全部由后端计算，前端只做展示映射
export function getBadges() {
  return instance.get('/badges').then((res) => {
    const list = res.data.data
    return Array.isArray(list) ? list : []
  })
}
