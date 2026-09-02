<script setup>
// 伤病预防页（工程计划 4.8 / M9 骨架态）：四分类文章列表 + 内嵌详情
// - 路由 query ?article=id 定位文章详情，刷新可定位；返回列表保留数据
// - 文章底部「问 AI 助手」携带上下文跳 /qa（工程计划 4.8 联动预留，M9 简化为标题草稿）
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import P5Card from '../components/common/P5Card.vue'
import * as injuryApi from '../api/injury'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const currentCategory = ref('') // '' = 全部
const articles = ref([])
const loading = ref(false)
const loadError = ref(false)

// 详情态：当前展开的文章（由路由 query 驱动）
const detail = ref(null)
const detailLoading = ref(false)

onMounted(() => {
  injuryApi
    .getCategories()
    .then((list) => {
      categories.value = list || []
    })
    .catch(() => {})
  loadArticles('')

  // 初始 query 携带 article 时直接进详情
  if (route.query.article) {
    openDetail(String(route.query.article))
  }
})

async function loadArticles(category) {
  loading.value = true
  loadError.value = false
  try {
    const list = await injuryApi.getArticles(category || undefined)
    articles.value = list || []
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function selectCategory(key) {
  currentCategory.value = key
  loadArticles(key)
}

// 进详情：query 同步（可分享 / 刷新定位）
function openDetail(id) {
  router.replace({ query: { ...route.query, article: id } })
}

// 返回列表：清 query 保留分类
function backToList() {
  router.replace({ query: { ...route.query, article: undefined } })
}

// query 变化 → 详情态切换（前进 / 后退键同样生效）
watch(
  () => route.query.article,
  (id) => {
    if (!id) {
      detail.value = null
      return
    }
    // 优先从列表缓存取，避免闪烁；无缓存再拉详情
    const cached = articles.value.find((a) => a.id === id)
    if (cached) {
      detail.value = cached
      return
    }
    detailLoading.value = true
    injuryApi
      .getArticleDetail(String(id))
      .then((article) => {
        detail.value = article
      })
      .catch(() => {
        detail.value = null
        backToList()
      })
      .finally(() => {
        detailLoading.value = false
      })
  },
)

// 「问 AI 助手」：携带文章标题跳答疑页（草稿态由 QaView 读取 query 处理）
function askAi(article) {
  router.push({ path: '/qa', query: { draft: `关于「${article.title}」，我该注意什么？` } })
}

const categoryLabel = computed(
  () => (detail.value && detail.value.categoryLabel) || '',
)
</script>

<template>
  <div class="injury-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">伤病预防</h1>
      <p class="page-desc">膝 / 踝 / 髋 / 足 —— 跑者常见伤病知识库，科学防护，健康奔跑。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- 列表态 -->
    <template v-if="!route.query.article">
      <div class="cat-chips">
        <button
          class="cat-chip"
          type="button"
          :class="{ 'cat-chip--active': currentCategory === '' }"
          @click="selectCategory('')"
        >
          全部
        </button>
        <button
          v-for="c in categories"
          :key="c.key"
          class="cat-chip"
          type="button"
          :class="{ 'cat-chip--active': currentCategory === c.key }"
          @click="selectCategory(c.key)"
        >
          {{ c.label }}部
        </button>
      </div>

      <div v-if="loading" class="list-status">文章加载中…</div>
      <div v-else-if="loadError" class="list-status">文章加载失败，请刷新重试</div>
      <div v-else class="article-list">
        <P5Card
          v-for="(a, i) in articles"
          :key="a.id"
          :tag="a.categoryLabel + '部'"
          :tag-rotate="i % 2 === 0 ? '-4deg' : '3deg'"
          :tag-top="i % 2 === 0 ? '-15px' : '-18px'"
          :tag-right="i % 2 === 0 ? '48px' : ''"
          :tag-left="i % 2 === 0 ? '' : '42px'"
        >
          <button class="article-card" type="button" @click="openDetail(a.id)">
            <span class="article-title">{{ a.title }}</span>
            <span class="article-summary">{{ a.summary }}</span>
            <span class="article-meta">
              <span
                v-for="t in a.tags"
                :key="t"
                class="article-tag"
              >{{ t }}</span>
              <span class="article-cta">阅读全文 →</span>
            </span>
          </button>
        </P5Card>
        <p v-if="!articles.length" class="list-status">该分类下暂无文章</p>
      </div>
    </template>

    <!-- 详情态（内嵌展开） -->
    <P5Card v-else :tag="categoryLabel + '部'" tag-rotate="-4deg">
      <div class="detail-body">
        <div v-if="detailLoading" class="list-status">文章加载中…</div>
        <template v-else-if="detail">
          <button class="back-btn" type="button" @click="backToList">
            ← 返回列表
          </button>
          <h2 class="detail-title">{{ detail.title }}</h2>
          <div class="detail-tags">
            <span v-for="t in detail.tags" :key="t" class="article-tag">{{ t }}</span>
          </div>
          <div class="detail-content">
            <p v-for="(p, i) in detail.contentParagraphs" :key="i">{{ p }}</p>
          </div>
          <div class="detail-foot">
            <button class="ask-btn" type="button" @click="askAi(detail)">
              问 AI 助手 →
            </button>
          </div>
        </template>
      </div>
    </P5Card>
  </div>
</template>

<style scoped>
.page-kicker {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-2);
}

.page-title {
  font-size: 48px;
  line-height: 1.2;
  margin-bottom: var(--sp-3);
}

.page-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
}

/* ===== 分类 chips ===== */
.cat-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-3);
  margin: var(--sp-5) 0;
}

.cat-chip {
  padding: 6px 20px;
  /* 趣味点缀 2/3：ZCOOL KuaiLe 分类贴纸 */
  font-family: var(--font-fun);
  font-size: var(--fs-sub);
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
  background: transparent;
  border: 1px solid var(--p5-line);
  clip-path: polygon(7px 0, 100% 0, calc(100% - 7px) 100%, 0 100%);
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}

.cat-chip:hover {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.cat-chip--active {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
  font-weight: 700;
}

/* ===== 文章列表 ===== */
.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

.article-card {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  width: 100%;
  padding: var(--sp-5) var(--sp-6) var(--sp-5);
  text-align: left;
  transition: background 0.2s;
}

.article-card:hover {
  background: var(--p5-red-soft);
}

.article-title {
  font-size: var(--fs-h3);
  font-weight: 700;
  line-height: 1.4;
  color: var(--p5-white);
}

.article-summary {
  font-size: var(--fs-sub);
  line-height: 1.7;
  color: var(--p5-text-dim);
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sp-2);
  margin-top: var(--sp-2);
}

.article-tag {
  padding: 2px 10px;
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  border: 1px dashed var(--p5-line);
}

.article-cta {
  margin-left: auto;
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-red);
  white-space: nowrap;
}

.list-status {
  padding: var(--sp-6) 0;
  text-align: center;
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

/* ===== 详情 ===== */
.detail-body {
  padding: var(--sp-5) var(--sp-6) var(--sp-6);
}

.back-btn {
  padding: 0;
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-red);
  margin-bottom: var(--sp-4);
}

.back-btn:hover {
  text-decoration: underline;
}

.detail-title {
  font-size: var(--fs-h2);
  font-weight: 900;
  line-height: 1.4;
  color: var(--p5-white);
  margin-bottom: var(--sp-3);
}

.detail-tags {
  display: flex;
  gap: var(--sp-2);
  margin-bottom: var(--sp-5);
}

.detail-content p {
  font-size: var(--fs-body);
  line-height: 2;
  color: var(--p5-cream);
  margin-bottom: var(--sp-4);
}

.detail-foot {
  padding-top: var(--sp-5);
  border-top: 1px solid var(--p5-line);
}

.ask-btn {
  padding: 10px 28px;
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition: background 0.2s;
}

.ask-btn:hover {
  background: var(--p5-red-dark);
}
</style>
