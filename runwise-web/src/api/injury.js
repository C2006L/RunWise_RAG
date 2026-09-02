import { USE_MOCK, injury as mockInjury } from '../mock'
import { useUserStore } from '../stores/user'
import { instance, withAuthRetry } from './request'

// 伤病预防接口层（工程计划 6.3 签名 / 6.2 分流，M9 新增）
// 后端暂无对应接口，axios 分支按前端契约预留路径，联调期与后端对齐

// 文章列表 → InjuryArticle[]（含 categoryLabel）
// category: 'knee' | 'ankle' | 'hip' | 'foot'，缺省全部
export function getArticles(category) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockInjury.getArticles(category, useUserStore().accessToken)
    )
  }
  return instance
    .get('/injury/articles', { params: category ? { category } : {} })
    .then((res) => res.data.data)
}

// 文章详情 → InjuryArticle（含 contentParagraphs）
export function getArticleDetail(id) {
  if (USE_MOCK) {
    return withAuthRetry(() =>
      mockInjury.getArticleDetail(id, useUserStore().accessToken)
    )
  }
  return instance.get(`/injury/articles/${id}`).then((res) => res.data.data)
}

// 分类列表 → { key, label }[]
export function getCategories() {
  if (USE_MOCK) {
    return withAuthRetry(() => mockInjury.getCategories(useUserStore().accessToken))
  }
  return instance.get('/injury/categories').then((res) => res.data.data)
}
