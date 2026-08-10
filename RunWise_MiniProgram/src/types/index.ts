/**
 * 全局类型定义
 */

/** 打卡记录 */
export interface Checkin {
  id: number
  userId: number
  checkinDate: string
  distance: number
  duration: number
  pace: number
  mood: string
  remark: string
  imageUrl: string
  imageAudit: number
  createTime: string
  updateTime: string
}

/** 打卡统计 */
export interface CheckinStats {
  totalCount: number
  weekCount: number
  streak: number
  totalDistance: number
}

/** 日历打卡项 */
export interface CalendarItem {
  date: string
  distance: number
  duration: number
  mood: string
}

/** 问答记录 */
export interface QaRecord {
  id: number
  userId: number
  question: string
  answer: string
  sources: string
  feedback: number
  createTime: string
}

/** 热门问题 */
export interface HotQuestion {
  question: string
  category: string
}

/** 用户信息 */
export interface UserInfo {
  id: number
  openid: string
  nickname: string
  avatarUrl: string
  gender: number
  status: number
  createTime: string
}

/** 分页结果 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 统一响应 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}
