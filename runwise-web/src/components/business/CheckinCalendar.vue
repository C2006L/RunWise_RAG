<script setup>
import { computed, ref, watch } from "vue";
import { useCheckinStore } from "../../stores/checkin";
import { formatDate, WEEKDAY_LABELS } from "../../composables/useFormatDate";

// 打卡日历（工程计划 4.4）：月历网格 + 打卡标记 + 选中高亮 + 前后翻月
// 翻月时向父组件抛 month-change（携带当月起止日期），由页面驱动 store.loadRange
const emit = defineEmits(["month-change"]);
const store = useCheckinStore();

const now = new Date();
const todayKey = formatDate(now);
const view = ref({ year: now.getFullYear(), month: now.getMonth() }); // month 从 0 起

// 新打卡「盖章」动画（UI 精修 P3-2）：store.records 出现新日期时短暂点亮该格
// 初始加载（空 → 全月记录）不触发，仅提交打卡时盖章
const stampedDate = ref("");
watch(
  () => store.records,
  (nv, ov) => {
    if (!ov || Object.keys(ov).length === 0) return;
    const added = Object.keys(nv).find((k) => !ov[k]);
    if (!added) return;
    stampedDate.value = added;
    setTimeout(() => {
      if (stampedDate.value === added) stampedDate.value = "";
    }, 700);
  },
);

const monthLabel = computed(
  () => `${view.value.year} 年 ${view.value.month + 1} 月`,
);

const cells = computed(() => {
  const { year, month } = view.value;
  const lead = (new Date(year, month, 1).getDay() + 6) % 7; // 周一为首列
  const days = new Date(year, month + 1, 0).getDate();
  const list = [];
  for (let i = 0; i < lead; i++) list.push(null);
  for (let d = 1; d <= days; d++) {
    const key = formatDate(new Date(year, month, d));
    list.push({
      key,
      day: d,
      hasRecord: Boolean(store.records[key]),
      isToday: key === todayKey,
      isFuture: key > todayKey, // 'YYYY-MM-DD' 字符串比较即日期比较
    });
  }
  return list;
});

function shiftMonth(delta) {
  const { year, month } = view.value;
  const first = new Date(year, month + delta, 1);
  view.value = { year: first.getFullYear(), month: first.getMonth() };
  emit("month-change", {
    startDate: formatDate(first),
    endDate: formatDate(new Date(first.getFullYear(), first.getMonth() + 1, 0)),
  });
}

function handleClick(cell) {
  if (!cell || cell.isFuture) return; // 不可选未来日期
  store.select(cell.key);
}
</script>

<template>
  <div class="cal">
    <div class="cal-head">
      <button
        class="cal-nav"
        type="button"
        aria-label="上一月"
        @click="shiftMonth(-1)"
      >
        ‹
      </button>
      <span class="cal-title">{{ monthLabel }}</span>
      <button
        class="cal-nav"
        type="button"
        aria-label="下一月"
        @click="shiftMonth(1)"
      >
        ›
      </button>
    </div>

    <div class="cal-week">
      <span v-for="label in WEEKDAY_LABELS" :key="label">{{ label }}</span>
    </div>

    <!-- B3：图例（日历卡右上区域，右对齐 11px 灰字） -->
    <p class="cal-legend" aria-hidden="true"><i>◆</i> 已打卡 · <i>■</i> 今天</p>

    <div class="cal-grid">
      <span
        v-for="(cell, idx) in cells"
        :key="idx"
        class="cal-cell"
        :class="{
          'cal-cell--empty': !cell,
          'is-future': cell && cell.isFuture,
          'is-today': cell && cell.isToday,
          'has-record': cell && cell.hasRecord,
          'is-selected': cell && cell.key === store.selectedDate,
        }"
        @click="handleClick(cell)"
      >
        <template v-if="cell">
          <span class="cal-daynum">{{ cell.day }}</span>
          <span
            v-if="cell.hasRecord"
            class="cal-dot"
            :class="{ 'cal-dot--stamp': cell.key === stampedDate }"
          ></span>
        </template>
      </span>
    </div>
  </div>
</template>

<style scoped>
.cal {
  padding: var(--sp-6);
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-4);
}

.cal-title {
  font-size: var(--fs-h3);
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--p5-white);
}

.cal-nav {
  width: 32px;
  height: 32px;
  font-size: 18px;
  line-height: 1;
  color: var(--p5-text-dim);
  border: 1px solid var(--p5-line);
  transition:
    color 0.2s,
    border-color 0.2s;
}

.cal-nav:hover {
  color: var(--p5-red);
  border-color: var(--p5-red);
}

.cal-week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  margin-bottom: var(--sp-2);
}

/* B3：图例——11px 灰字，标记符号红色点题 */
.cal-legend {
  margin: 0 0 var(--sp-2);
  text-align: right;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
}

.cal-legend i {
  font-style: normal;
  color: var(--p5-red);
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--sp-1);
}

.cal-cell {
  position: relative;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--p5-white);
  font-size: var(--fs-sub);
  transition:
    background-color 0.15s,
    box-shadow 0.2s;
}

/* B2：hover 白色描边浮现（0.2s），暗示可点 */
.cal-cell:hover:not(.cal-cell--empty):not(.is-future) {
  background: var(--p5-red-soft);
  box-shadow: inset 0 0 0 1px var(--p5-white);
}

/* B2：有打卡的日期数字提亮（加粗白） */
.has-record .cal-daynum {
  font-weight: 700;
  color: var(--p5-white);
}

.cal-cell--empty {
  cursor: default;
}

.is-future {
  color: #55555c;
  cursor: default;
}

/* 今日：红蒙层切角小标签（UI 精修 P1-4，呼应全站切角语言） */
.is-today .cal-daynum {
  padding: 1px 5px;
  background: var(--p5-red-soft);
  clip-path: polygon(0 0, calc(100% - 5px) 0, 100% 5px, 100% 100%, 0 100%);
}

.is-selected {
  background: var(--p5-red);
  font-weight: 700;
}

.is-selected:hover {
  background: var(--p5-red);
}

/* 打卡标记：红色菱形（斜置方块，呼应切角语言，像「被盖章」） */
.cal-dot {
  width: 8px;
  height: 8px;
  margin-top: 4px;
  background: var(--p5-red);
  transform: rotate(45deg);
}

.is-selected .cal-dot {
  background: var(--p5-white);
}

/* 新打卡盖章动画（P3-2）：scale 1.6→1 落章 */
.cal-dot--stamp {
  animation: cal-stamp 0.45s cubic-bezier(0.2, 1.4, 0.4, 1) both;
}

@keyframes cal-stamp {
  0% {
    transform: rotate(45deg) scale(1.6);
    opacity: 0;
  }
  60% {
    opacity: 1;
  }
  100% {
    transform: rotate(45deg) scale(1);
    opacity: 1;
  }
}
</style>
