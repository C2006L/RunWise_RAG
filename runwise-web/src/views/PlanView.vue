<script setup>
// 训练计划页（工程计划 4.7 / M9 骨架态）：4 周课表 + 周切换
// - 数据来自 api/plan.getPlanList（mock 固定种子，与打卡数据无联动，后续里程碑填充）
// - 构图沿用内容页规范（页头红条斜纹），课表表格保持零倾斜（3.4.3 硬规则）
import { onMounted, ref } from 'vue'
import P5Card from '../components/common/P5Card.vue'
import * as planApi from '../api/plan'

const weeks = ref([])
const currentWeek = ref(1)
const loading = ref(true)
const loadError = ref(false)

onMounted(async () => {
  try {
    const list = await planApi.getPlanList()
    weeks.value = list || []
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

function selectWeek(weekNo) {
  currentWeek.value = weekNo
}

const currentWeekData = () => weeks.value.find((w) => w.weekNo === currentWeek.value)

// 课表类型 → 行内类型标签配色
const TYPE_STYLES = {
  easy: 'type--easy',
  tempo: 'type--tempo',
  interval: 'type--interval',
  long: 'type--long',
  rest: 'type--rest',
}
</script>

<template>
  <div class="plan-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">训练计划</h1>
      <p class="page-desc">4 周课表安排 —— 轻松跑、节奏跑、间歇与长距离的科学配比。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <P5Card tag="WEEKLY PLAN" tag-rotate="-4deg" tag-top="-16px" tag-left="28px">
      <div class="plan-body">
        <!-- 周切换 -->
        <div class="week-tabs">
          <button
            v-for="w in weeks"
            :key="w.weekNo"
            class="week-tab"
            type="button"
            :class="{ 'week-tab--active': w.weekNo === currentWeek }"
            @click="selectWeek(w.weekNo)"
          >
            第 {{ w.weekNo }} 周
          </button>
        </div>

        <!-- 课表表格 -->
        <div v-if="loading" class="plan-status">课表加载中…</div>
        <div v-else-if="loadError" class="plan-status">
          课表加载失败，请刷新重试
        </div>
        <table v-else-if="currentWeekData()" class="plan-table">
          <thead>
            <tr>
              <th>星期</th>
              <th>类型</th>
              <th class="col-num">目标距离</th>
              <th>目标配速</th>
              <th class="col-status">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="d in currentWeekData().days"
              :key="d.date"
              :class="{ 'row--rest': d.type === 'rest' }"
            >
              <td class="cell-weekday">{{ d.weekday }}</td>
              <td>
                <span class="type-tag" :class="TYPE_STYLES[d.type]">{{ d.label }}</span>
              </td>
              <td class="cell-num p5-num">
                {{ d.targetKm > 0 ? d.targetKm.toFixed(1) + ' km' : '—' }}
              </td>
              <td class="cell-pace">{{ d.paceRange || '—' }}</td>
              <td class="cell-status">
                <span class="status-dot"></span>
                <span>{{ d.type === 'rest' ? '休息日' : '待执行' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </P5Card>
  </div>
</template>

<style scoped>
.plan-page {
  display: flex;
  flex-direction: column;
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

.plan-body {
  padding: var(--sp-5) var(--sp-6) var(--sp-6);
}

/* ===== 周切换 ===== */
.week-tabs {
  display: flex;
  gap: var(--sp-3);
  margin-bottom: var(--sp-5);
}

.week-tab {
  padding: 8px 24px;
  font-size: var(--fs-sub);
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
  background: transparent;
  border: 1px solid var(--p5-line);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}

.week-tab:hover {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.week-tab--active {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
  font-weight: 700;
}

/* ===== 课表表格（零倾斜，3.4.3 硬规则） ===== */
.plan-table {
  width: 100%;
  border-collapse: collapse;
}

.plan-table th {
  padding: var(--sp-3) var(--sp-3);
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-align: left;
  color: var(--p5-text-dim);
  border-bottom: 1px solid var(--p5-line);
}

.plan-table td {
  padding: var(--sp-4) var(--sp-3);
  font-size: var(--fs-sub);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.plan-table tr:last-child td {
  border-bottom: none;
}

.row--rest td {
  color: var(--p5-text-dim);
  opacity: 0.65;
}

.cell-weekday {
  font-weight: 700;
  color: var(--p5-white);
  white-space: nowrap;
}

.col-num,
.cell-num {
  text-align: right;
  white-space: nowrap;
}

.cell-num {
  font-size: var(--fs-h3);
  color: var(--p5-white);
}

.row--rest .cell-num {
  font-size: var(--fs-sub);
}

.col-status,
.cell-status {
  text-align: right;
  white-space: nowrap;
}

.cell-pace {
  color: var(--p5-text-dim);
  white-space: nowrap;
}

/* 类型标签：色块前缀区分课表类型（红黑米三色纪律内） */
.type-tag {
  display: inline-block;
  padding: 3px 12px;
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  border: 1px solid var(--p5-line);
}

.type--easy {
  color: var(--p5-text-dim);
}

.type--tempo {
  color: var(--p5-white);
  border-color: var(--p5-text-dim);
}

.type--interval {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
  font-weight: 700;
}

.type--long {
  color: var(--p5-red);
  border-color: var(--p5-red);
}

.type--rest {
  color: var(--p5-text-dim);
  border-style: dashed;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 8px;
  background: var(--p5-red);
  transform: rotate(45deg);
}

.row--rest .status-dot {
  background: var(--p5-text-dim);
  transform: none;
  border-radius: 50%;
}

.plan-status {
  padding: var(--sp-6) 0;
  text-align: center;
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

@media (max-width: 760px) {
  .cell-pace,
  .plan-table th:nth-child(4) {
    display: none;
  }
}
</style>
