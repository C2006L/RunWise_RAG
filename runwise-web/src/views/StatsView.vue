<script setup>
// 数据统计页（工程计划 4.6 / M5 功能态，UI 精修 v2.0）：
// - 周 / 月分段切换：周 = 近 7 天滚动窗口里程柱状图（P1-6：与汇总卡同口径，
//   卡片数字 = 柱高之和）；月 = 当月逐日趋势折线图
// - 顶部汇总随口径联动，与图表数据同源（mock 下均从 checkin 单一数据源实时计算）
import { computed, onMounted, ref } from "vue";
import P5Card from "../components/common/P5Card.vue";
import StatsChart from "../components/business/StatsChart.vue";
import * as statsApi from "../api/stats";
import { WEEKDAY_LABELS } from "../composables/useFormatDate";

const mode = ref("week"); // 'week' | 'month'
const loading = ref(false);
const errorMsg = ref("");

const weekly = ref(null); // WeeklyStats（近 7 天滚动口径，UI 精修 P1-6）
const monthly = ref(null); // MonthlyStats

// 日期串 → 周一为首的星期序号
function weekdayOf(dateStr) {
  const [y, m, d] = dateStr.split("-").map(Number);
  return WEEKDAY_LABELS[(new Date(y, m - 1, d).getDay() + 6) % 7];
}

// 当前口径下的图表与汇总数据
const chartLabels = computed(() => {
  if (mode.value === "week") {
    // 周视图 x 轴：近 7 天逐日「星期 + 日期」（滚动窗口，任意一天访问 7 根柱齐）
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

const summary = computed(() => {
  if (mode.value === "week") {
    const w = weekly.value;
    return w
      ? [
          { label: "近7天总里程", value: w.totalKm, unit: "km" },
          { label: "近7天打卡", value: w.checkinCount, unit: "次" },
          { label: "连续打卡", value: w.streakDays, unit: "天" },
        ]
      : [];
  }
  const m = monthly.value;
  return m
    ? [
        { label: "本月总里程", value: m.totalKm, unit: "km" },
        { label: "本月打卡", value: m.checkinCount, unit: "次" },
      ]
    : [];
});

const monthTitle = computed(() => {
  const d = new Date();
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`;
});

async function loadAll() {
  loading.value = true;
  errorMsg.value = "";
  try {
    const [w, m] = await Promise.all([
      statsApi.getWeeklyStats(),
      statsApi.getMonthlyStats(),
    ]);
    weekly.value = w;
    monthly.value = m;
  } catch {
    errorMsg.value = "统计数据加载失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}

function switchMode(next) {
  if (mode.value === next) return;
  mode.value = next;
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

    <!-- 汇总数据条：与图表同源联动（v2.0 3.4.2 规则 2：大/中/小三档 + 微倾错位） -->
    <div class="summary" :class="{ 'is-loading': loading }">
      <div
        v-for="(s, i) in summary"
        :key="s.label"
        class="sum-item"
        :class="`sum-item--${i + 1}`"
      >
        <span class="sum-value p5-num">{{ s.value }}</span>
        <span class="sum-unit">{{ s.unit }}</span>
        <span class="sum-label">{{ s.label }}</span>
      </div>
    </div>

    <P5Card :tag="mode === 'week' ? '本周里程' : '月度趋势'" tag-rotate="-5deg">
      <div class="chart-card">
        <div class="chart-head">
          <!-- 周 / 月分段切换 -->
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
          <span class="chart-caption">
            {{
              mode === "week"
                ? "近 7 天滚动 · 单位 km"
                : monthTitle + " · 逐日里程"
            }}
          </span>
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
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  padding: var(--sp-4) var(--sp-5);
  background: var(--p5-panel);
  clip-path: polygon(
    0 0,
    calc(100% - var(--cut)) 0,
    100% var(--cut),
    100% 100%,
    var(--cut) 100%,
    0 calc(100% - var(--cut))
  );
}

.sum-value {
  font-family: var(--font-display);
  font-size: 56px;
  line-height: 1;
  color: var(--p5-white);
}

.sum-unit {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-red);
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
