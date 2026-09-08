<script setup>
// 伤病预防页（工程计划 4.8 / M9 骨架态）：四分类文章列表 + 内嵌详情
// - 路由 query ?article=id 定位文章详情，刷新可定位；返回列表保留数据
// - 文章底部「问 AI 助手」携带上下文跳 /qa（工程计划 4.8 联动预留，M9 简化为标题草稿）
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import P5Card from '../components/common/P5Card.vue'
import Stars from '../components/common/Stars.vue'
import RansomNote from '../components/common/RansomNote.vue'
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
      <h1 class="page-title p5-page-title" aria-label="伤病预防">
        <RansomNote text="伤病预防" :seed="44" />
      </h1>
      <p class="page-desc">膝 / 踝 / 髋 / 足 —— 跑者常见伤病知识库，科学防护，健康奔跑。</p>
    </header>

    <!-- F5：页头右侧空白区撒星（白3，X-4 黄仅限左上部位标签，星不再用黄） -->
    <div class="page-stars" aria-hidden="true">
      <Stars
        :count="3"
        accent-color="var(--accent-yellow)"
        :positions="[
          { left: '88%', top: '38px', size: 10, color: 'rgba(255,255,255,0.5)', bright: true, dur: 5 },
          { left: '75%', top: '118px', size: 6, color: 'rgba(255,255,255,0.42)', bright: true, dur: 6.2 },
          { left: '92%', top: '156px', size: 9, color: 'rgba(255,255,255,0.55)', bright: true, dur: 4.4 },
        ]"
      />
    </div>

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
          class="article-card-wrap"
          :tag="a.categoryLabel + '部'"
          :tag-rotate="i % 2 === 0 ? '-4deg' : '3deg'"
          :tag-top="i % 2 === 0 ? '-15px' : '-18px'"
          :tag-left="i % 2 === 0 ? '48px' : ''"
          :tag-right="i % 2 === 0 ? '' : '42px'"
          frame="red"
          tag-variant="yellow"
        >
          <!-- S4 第三卡强调态：不做整卡红底反白，与其他卡同款黑底白描边，
               仅标题前红菱形 + 标签行红底，保持列表节奏一致 -->
          <button
            class="article-card"
            :class="{ 'article-card--hot': i === 2 }"
            type="button"
            @click="openDetail(a.id)"
          >
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
/* F5：页面为星层定位父级；星层只覆盖页头右侧空白带（180px 高），远离卡片区 */
.injury-page {
  position: relative;
}

.page-stars {
  position: absolute;
  inset: 0 0 auto 0;
  height: 180px;
  pointer-events: none;
  z-index: 0;
}

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
  /* X-1a：列表容器不设 overflow 裁切（本页本就没有），改用 padding 为
     整卡旋转留出空间——旋转出来的角不许被裁切 */
  padding: var(--sp-3);
}

/* ===== X-1b / X-2：卡单位 = P5Card wrap（框体/切角/贴纸/内容一体） =====
   旋转与提层挂 wrap 而非内容层，根因修复：
   - 卡体 .p5-card 带 clip-path，内容层旋转戳出的直角会被父框裁掉（"角被吞"）；
     改旋 wrap 后整卡（含切角形状）一起转，父容器 .article-list 无裁切，
     角永远完整
   - z-index 基础 1，hover/选中 10 → 旋转角盖住邻卡；红三角旗在 wrap 上，
     随卡同步提升、任何状态不被裁切 */
.article-card-wrap {
  position: relative;
  z-index: 1;
  transition: transform 0.12s;
}

.article-card-wrap:hover,
.article-card-wrap.selected {
  transform: rotate(-2deg); /* X-2 整卡倾斜 2°（0.12s 干脆，漫画式） */
  z-index: 10;
}

/* 框体描边过渡（hover 外圈红描边用，仅伤病页列表卡启用） */
.article-card-wrap :deep(.p5-card) {
  transition: border-color 0.12s;
}

/* X-2 外圈 2px 红描边：卡体白描边 hover 变红，卡内文字保持原色（无整卡重涂） */
.article-card-wrap:hover :deep(.p5-card),
.article-card-wrap.selected :deep(.p5-card) {
  border-color: var(--p5-red);
}

/* A-4 左缘红色小三角旗（主页 CHECK-IN 旗样式）：挂 wrap 上，
   不被框体 clip-path 裁切（任何状态下完整显示），随卡旋转同步提升 */
.article-card-wrap::before {
  content: "";
  position: absolute;
  left: -2px;
  top: 14px;
  width: 0;
  height: 0;
  z-index: 1;
  border-style: solid;
  border-width: 0 0 16px 16px;
  border-color: transparent transparent var(--p5-red) transparent;
  pointer-events: none;
}

/* X-2 左缘红条：hover/选中时沿左缘竖条（顶部斜切与卡体 12px 切角同向，
   不超出切角内侧；贴纸/角标层之上） */
.article-card-wrap::after {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  z-index: 1;
  background: var(--p5-red);
  clip-path: polygon(0 12px, 4px 8px, 4px 100%, 0 100%);
  opacity: 0;
  transition: opacity 0.12s;
  pointer-events: none;
}

.article-card-wrap:hover::after,
.article-card-wrap.selected::after {
  opacity: 1;
}

/* 内容点击层：切角与父框联动（直角不戳出），自身不再承担任何 hover 变换 */
.article-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  width: 100%;
  padding: var(--sp-5) var(--sp-6) var(--sp-5);
  text-align: left;
  clip-path: polygon(
    12px 0,
    100% 0,
    100% calc(100% - 12px),
    calc(100% - 12px) 100%,
    0 100%,
    0 12px
  );
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

/* X-3 阅读全文 →：红色切角小按钮（红底白字 12px）；
   卡内 padding 右/下 24px+，按钮距卡边 ≥16px（黑底红小字方案废除） */
.article-cta {
  margin-left: auto;
  padding: 5px 14px;
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: 0.08em;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(6px 0, 100% 0, calc(100% - 6px) 100%, 0 100%);
  white-space: nowrap;
}

/* ===== S4 第三卡强调态（过审决议，整卡红底反白方案的替代） =====
   卡体与其他卡同款黑底白描边（frame="red" 不变），仅两处红色强调：
   标题前红色菱形 + 标签行红底反白，保持列表节奏一致 */
.article-card--hot .article-title::before {
  content: "";
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 10px;
  background: var(--p5-red);
  transform: rotate(45deg);
  vertical-align: 2px;
}

/* 标签行红底：红条内标签白字描白边、CTA 反白（红底上不再用红按钮，
   改白描边透明钮，保证可读） */
.article-card--hot .article-meta {
  margin-top: var(--sp-3);
  padding: 7px 12px;
  background: var(--p5-red);
}

.article-card--hot .article-tag {
  color: var(--p5-white);
  border-color: rgba(255, 255, 255, 0.55);
}

.article-card--hot .article-cta {
  color: var(--p5-white);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.65);
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
