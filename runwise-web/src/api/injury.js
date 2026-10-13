import { USE_MOCK, injury as mockInjury } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 伤病预防接口层（工程计划 6.3 签名 / 6.2 分流，M9 新增）
// 后端契约已对齐：GET /injury/articles、/injury/articles/{id}、/injury/categories
// 支持 VITE_INJURY_USE_MOCK 独立开关（便于单独联调本模块），缺省跟随全局 VITE_USE_MOCK
const useMock =
  import.meta.env.VITE_INJURY_USE_MOCK !== undefined
    ? import.meta.env.VITE_INJURY_USE_MOCK !== 'false'
    : USE_MOCK

// 文章列表 → InjuryArticle[]（含 categoryLabel）
// category: 'knee' | 'ankle' | 'shin'，缺省全部
export function getArticles(category) {
  if (useMock) {
    return withAuthRetry(() =>
      mockInjury.getArticles(category, useUserStore().accessToken)
    )
  }
  return instance
    .get('/injury/articles', { params: category ? { category } : {} })
    .then((res) => res.data.data)
}

// 文章详情 → InjuryArticle（含 sections[{ heading, body }]）
export function getArticleDetail(id) {
  if (useMock) {
    return withAuthRetry(() =>
      mockInjury.getArticleDetail(id, useUserStore().accessToken)
    )
  }
  return instance.get(`/injury/articles/${id}`).then((res) => res.data.data)
}

// 分类列表 → { key, label }[]
export function getCategories() {
  if (useMock) {
    return withAuthRetry(() => mockInjury.getCategories(useUserStore().accessToken))
  }
  return instance.get('/injury/categories').then((res) => res.data.data)
}
