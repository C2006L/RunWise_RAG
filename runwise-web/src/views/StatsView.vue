<script setup>
// 数据统计页（工程计划 4.6 / M5 功能态，UI 精修 v2.0）：
// - 周 / 月分段切换：周 = 7 天滚动窗口里程柱状图（P1-6：与汇总卡同口径，
//   卡片数字 = 柱高之和）；月 = 当月逐日趋势折线图
// - Phase E1/E4：‹ › 时间翻页（只许向过去翻），图表与汇总卡随周期联动
// - Phase E2：周期标签随翻页联动（"3 周前 · 单位 km" / "2026年8月 · 逐日里程"）
// - Phase E3：年月一律 new Date() 实时计算，无硬编码年月字符串
import { computed, onMounted, ref } from "vue";
import P5Card from "../components/common/P5Card.vue";
import StatsChart from "../components/business/StatsChart.vue";
import * as statsApi from "../api/stats";
import { WEEKDAY_LABELS } from "../composables/useFormatDate";

const mode = ref("week"); // 'week' | 'month'
const loading = ref(false);
const errorMsg = ref("");

const weekly = ref(null); // WeeklyStats（当前周窗口）
const monthly = ref(null); // MonthlyStats（当前月）

// ===== Phase E1：时间翻页状态（0 = 本周/本月，只增向过去） =====
const weekOffset = ref(0);
const monthOffset = ref(0);
const canGoNext = computed(() =>
  mode.value === "week" ? weekOffset.value > 0 : monthOffset.value > 0,
);

function goPrev() {
  if (mode.value === "week") weekOffset.value += 1;
  else monthOffset.value += 1;
  loadAll();
}

function goNext() {
  if (!canGoNext.value) return; // 禁止翻向未来
  if (mode.value === "week") weekOffset.value -= 1;
  else monthOffset.value -= 1;
  loadAll();
}

// 日期串 → 周一为首的星期序号
function weekdayOf(dateStr) {
  const [y, m, d] = dateStr.split("-").map(Number);
  return WEEKDAY_LABELS[(new Date(y, m - 1, d).getDay() + 6) % 7];
}

// ===== Phase E2：月视图周期（YYYY-MM，由真实日期与偏移实时推导） =====
function monthKeyOf(offset) {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

// 当前口径下的图表与汇总数据
const chartLabels = computed(() => {
  if (mode.value === "week") {
    // 周视图 x 轴：窗口 7 天逐日「星期 + 日期」（滚动窗口，任意一天访问 7 根柱齐）
    return (weekly.value?.weekDates || []).map(
      (d) => `${weekdayOf(d)} ${Number(d.slice(8))}`,
    );
  }
  return (monthly.value?.monthDates || []).map((d) => Number(d.slice(8)));
});

const chartValues = computed(() =>
  mode.value === "week"
    ? weekly.value?.distances || []
    : monthly.value?.distances || [],
);

// ===== Phase E4：汇总随周期联动（label 与数字同周期） =====
const summary = computed(() => {
  if (mode.value === "week") {
    const w = weekly.value;
    const o = weekOffset.value;
    const tag = o === 0 ? "近7天" : `${o} 周前`;
    return w
      ? [
          { label: `${tag}总里程`, value: w.totalKm, unit: "km" },
          { label: `${tag}打卡`, value: w.checkinCount, unit: "次" },
          { label: `${tag}连续打卡`, value: w.streakDays, unit: "天" },
        ]
      : [];
  }
  const m = monthly.value;
  const [, mm] = monthKeyOf(monthOffset.value).split("-");
  const tag = monthOffset.value === 0 ? "本月" : `${Number(mm)}月`;
  return m
    ? [
        { label: `${tag}总里程`, value: m.totalKm, unit: "km" },
        { label: `${tag}打卡`, value: m.checkinCount, unit: "次" },
      ]
    : [];
});

// ===== Phase E2：周期标签（随翻页联动，年月来自 new Date() 实时推导） =====
const periodCaption = computed(() => {
  if (mode.value === "week") {
    const o = weekOffset.value;
    return o === 0 ? "本周滚动 · 单位 km" : `${o} 周前 · 单位 km`;
  }
  const [y, m] = monthKeyOf(monthOffset.value).split("-");
  return `${y}年${Number(m)}月 · 逐日里程`;
});

async function loadAll() {
  loading.value = true;
  errorMsg.value = "";
  try {
    if (mode.value === "week") {
      weekly.value = await statsApi.getWeeklyStats(weekOffset.value);
    } else {
      monthly.value = await statsApi.getMonthlyStats(
        monthKeyOf(monthOffset.value),
      );
    }
  } catch {
    errorMsg.value = "统计数据加载失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}

function switchMode(next) {
  if (mode.value === next) return;
  mode.value = next;
  loadAll(); // E4：切换口径即按该口径当前偏移取数
}

onMounted(loadAll);
</script>

<template>
  <div class="stats-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">数据统计</h1>
      <p class="page-desc">周里程与月度趋势一目了然，用图表见证你的成长。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <p v-if="errorMsg" class="error-bar">{{ errorMsg }}</p>

    <!-- 汇总数据条：与图表同源联动（v2.0 3.4.2 规则 2：大/中/小三档 + 微倾错位；E6 对齐 panel-frame 深红影） -->
    <div class="summary" :class="{ 'is-loading': loading }">
      <div
        v-for="(s, i) in summary"
        :key="s.label"
        class="sum-item panel-frame panel-frame--red"
        :class="`sum-item--${i + 1}`"
      >
        <span class="sum-value p5-num">{{ s.value }}</span>
        <span class="sum-unit">{{ s.unit }}</span>
        <span class="sum-label">{{ s.label }}</span>
      </div>
    </div>

    <P5Card
      :tag="mode === 'week' ? '本周里程' : '月度趋势'"
      tag-rotate="-5deg"
      frame="red"
    >
      <div class="chart-card">
        <div class="chart-head">
          <!-- 周 / 月分段切换 + E1 时间翻页（‹ › 只许向过去） -->
          <div class="head-left">
            <div class="seg">
              <button
                class="seg-btn"
                :class="{ 'is-active': mode === 'week' }"
                type="button"
                @click="switchMode('week')"
              >
                周视图
              </button>
              <button
                class="seg-btn"
                :class="{ 'is-active': mode === 'month' }"
                type="button"
                @click="switchMode('month')"
              >
                月视图
              </button>
            </div>
            <div class="period-nav">
              <button
                class="nav-btn"
                type="button"
                aria-label="上一周期"
                @click="goPrev"
              >
                ‹
              </button>
              <button
                class="nav-btn"
                type="button"
                aria-label="下一周期"
                :disabled="!canGoNext"
                @click="goNext"
              >
                ›
              </button>
            </div>
          </div>
          <span class="chart-caption">{{ periodCaption }}</span>
        </div>

        <div v-if="loading" class="chart-loading">统计中…</div>
        <StatsChart
          v-else
          :key="mode"
          :type="mode === 'week' ? 'bar' : 'line'"
          :labels="chartLabels"
          :values="chartValues"
          height="420px"
        />
      </div>
    </P5Card>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: 0; /* 间距由 p5-page-header 的 padding-bottom 提供 */
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

.error-bar {
  margin-bottom: var(--sp-4);
  padding: 10px 16px;
  font-size: var(--fs-sub);
  color: var(--p5-white);
  background: var(--p5-red-soft);
  border: 1px solid var(--p5-red);
}

/* ===== 汇总数据条（Anton 大数字 · v2.0 3.4.2 规则 2：大/中/小三档 + 微倾错位） ===== */
.summary {
  display: grid;
  /* 大 / 中 / 小三档列宽；月模式只有 2 项时自动占前两档 */
  grid-template-columns: 1.5fr 1.25fr 1fr;
  gap: var(--sp-5);
  margin-bottom: var(--sp-6);
  transition: opacity 0.2s;
}

.summary.is-loading {
  opacity: 0.45;
}

/* 档位：卡宽与数字字号同步递减，制造非均分节奏 */
.sum-item--1 .sum-value {
  font-size: 64px;
}

.sum-item--2 .sum-value {
  font-size: 56px;
}

.sum-item--3 .sum-value {
  font-size: 48px;
}

/* 各自微倾 + 垂直错位 8~12px（倾斜令牌，容器级） */
.sum-item--1 {
  transform: rotate(var(--tilt-2)) translateY(4px);
}

.sum-item--2 {
  transform: rotate(var(--tilt-3)) translateY(-8px);
}

.sum-item--3 {
  transform: rotate(var(--tilt-4)) translateY(6px);
}

@media (max-width: 900px) {
  /* 窄屏收敛：倾斜与错位归零，堆叠保可读 */
  .summary {
    grid-template-columns: 1fr 1fr;
    gap: var(--sp-4);
  }

  .sum-item--1,
  .sum-item--2,
  .sum-item--3 {
    transform: none;
  }

  .sum-item--1 .sum-value,
  .sum-item--2 .sum-value,
  .sum-item--3 .sum-value {
    font-size: 48px;
  }
}

.sum-item {
  /* E6：框体对齐 .panel-frame（白 2px 描边 + 12px 切角 + 红角标 + 深红 6px 实心影
     均由全局类提供），此处仅保留内边距；微倾错位 transform 保留（影随形动） */
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  padding: var(--sp-4) var(--sp-5);
}

.sum-value {
  font-family: var(--font-display);
  font-size: 56px;
  line-height: 1;
  color: var(--p5-white);
}

.sum-unit {
  /* A3：单位小字统一灰色（原红色违反全站单位规范） */
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-text-dim);
  margin-top: -4px;
}

.sum-label {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

/* ===== 图表卡片 ===== */
.chart-card {
  padding: var(--sp-5) var(--sp-6) var(--sp-6);
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--sp-3);
  margin-bottom: var(--sp-5);
}

.head-left {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
}

/* ===== E1：时间翻页 ‹ ›（白描边方块，hover 红底白字，末页禁用置灰） ===== */
.period-nav {
  display: inline-flex;
  gap: 6px;
}

.nav-btn {
  width: 34px;
  height: 34px;
  font-size: 20px;
  line-height: 1;
  color: var(--p5-white);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.7);
  transition:
    background 0.2s,
    color 0.2s,
    border-color 0.2s;
}

.nav-btn:hover:not(:disabled) {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
}

.nav-btn:disabled {
  color: var(--p5-text-dim);
  border-color: var(--p5-line);
  cursor: not-allowed;
  opacity: 0.45;
}

/* 周 / 月分段切换：红色激活态胶囊 */
.seg {
  display: inline-flex;
  border: 1px solid var(--p5-line);
}

.seg-btn {
  padding: 8px 26px;
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
  background: transparent;
  transition:
    color 0.2s,
    background 0.2s;
}

.seg-btn + .seg-btn {
  border-left: 1px solid var(--p5-line);
}

.seg-btn.is-active {
  color: var(--p5-white);
  background: var(--p5-red);
}

.chart-caption {
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
}

.chart-loading {
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-sub);
  letter-spacing: 0.2em;
  color: var(--p5-text-dim);
}
</style>
