import { USE_MOCK, qa as mockQa } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 答疑接口层（工程计划 6.3 签名 / 6.2 分流）：USE_MOCK 时转发 mock，组件无感知
// axios 路径与后端 QaController 逐一对齐：
// - POST /api/qa/ask（请求体 AskDTO { question }）
// - GET  /api/qa/history?pageNum=&pageSize=
// - GET  /api/qa/categories
// - GET  /api/qa/hot（后端无 limit 参数，前端截取）
// - POST /api/qa/feedback?id=&feedback=（后端 @RequestParam，参数走 query string）

// 提问 → QaAskResult { answer, sources, safetyTip? }
export function askQuestion(question) {
  if (USE_MOCK) {
    return withAuthRetry(() => mockQa.ask(question, useUserStore().accessToken))
  }
  return instance.post('/qa/ask', { question }).then((res) => res.data.data)
}

// 历史分页 → { list: QaHistoryRecord[], total, pageNum, pageSize }
export function getHistory(pageNum = 1, pageSize = 10) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockQa.getHistory(pageNum, pageSize, useUserStore().accessToken)
    )
  }
  return instance
    .get('/qa/history', { params: { pageNum, pageSize } })
    .then((res) => res.data.data)
}

// 问题分类 → QaCategory[]
export function getCategories() {
  if (USE_MOCK) {
    return withAuthRetry(() => mockQa.getCategories(useUserStore().accessToken))
  }
  return instance.get('/qa/categories').then((res) => res.data.data)
}

// 热门问题 → HotQuestion[]（后端返回全量，此处按 limit 截取）
export function getHotQuestions(limit = 5) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockQa.getHotQuestions(limit, useUserStore().accessToken)
    )
  }
  return instance
    .get('/qa/hot')
    .then((res) => (res.data.data || []).slice(0, limit))
}

// 回答反馈：feedback 1 赞 / -1 踩 / 0 取消
export function feedback(id, feedback) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockQa.feedback(id, feedback, useUserStore().accessToken)
    )
  }
  return instance
    .post('/qa/feedback', null, { params: { id, feedback } })
    .then((res) => res.data.data)
}
