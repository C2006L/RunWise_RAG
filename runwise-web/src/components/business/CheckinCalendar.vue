<script setup>
import { computed, ref } from 'vue'
import { useCheckinStore } from '../../stores/checkin'
import { formatDate, WEEKDAY_LABELS } from '../../composables/useFormatDate'

// 打卡日历（工程计划 4.4）：月历网格 + 红点标记 + 选中高亮 + 前后翻月
// 翻月时向父组件抛 month-change（携带当月起止日期），由页面驱动 store.loadRange
const emit = defineEmits(['month-change'])
const store = useCheckinStore()

const now = new Date()
const todayKey = formatDate(now)
const view = ref({ year: now.getFullYear(), month: now.getMonth() }) // month 从 0 起

const monthLabel = computed(() => `${view.value.year} 年 ${view.value.month + 1} 月`)

const cells = computed(() => {
  const { year, month } = view.value
  const lead = (new Date(year, month, 1).getDay() + 6) % 7 // 周一为首列
  const days = new Date(year, month + 1, 0).getDate()
  const list = []
  for (let i = 0; i < lead; i++) list.push(null)
  for (let d = 1; d <= days; d++) {
    const key = formatDate(new Date(year, month, d))
    list.push({
      key,
      day: d,
      hasRecord: Boolean(store.records[key]),
      isToday: key === todayKey,
      isFuture: key > todayKey, // 'YYYY-MM-DD' 字符串比较即日期比较
    })
  }
  return list
})

function shiftMonth(delta) {
  const { year, month } = view.value
  const first = new Date(year, month + delta, 1)
  view.value = { year: first.getFullYear(), month: first.getMonth() }
  emit('month-change', {
    startDate: formatDate(first),
    endDate: formatDate(new Date(first.getFullYear(), first.getMonth() + 1, 0)),
  })
}

function handleClick(cell) {
  if (!cell || cell.isFuture) return // 不可选未来日期
  store.select(cell.key)
}
</script>

<template>
  <div class="cal">
    <div class="cal-head">
      <button class="cal-nav" type="button" aria-label="上一月" @click="shiftMonth(-1)">‹</button>
      <span class="cal-title">{{ monthLabel }}</span>
      <button class="cal-nav" type="button" aria-label="下一月" @click="shiftMonth(1)">›</button>
    </div>

    <div class="cal-week">
      <span v-for="label in WEEKDAY_LABELS" :key="label">{{ label }}</span>
    </div>

    <div class="cal-grid">
      <span
        v-for="(cell, idx) in cells"
        :key="idx"
        class="cal-cell"
        :class="{
          'cal-cell--empty': !cell,
          'is-future': cell && cell.isFuture,
          'is-today': cell && cell.isToday,
          'is-selected': cell && cell.key === store.selectedDate,
        }"
        @click="handleClick(cell)"
      >
        <template v-if="cell">
          <span class="cal-daynum">{{ cell.day }}</span>
          <span v-if="cell.hasRecord" class="cal-dot"></span>
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
  transition: color 0.2s, border-color 0.2s;
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
  transition: background-color 0.15s;
}

.cal-cell:hover:not(.cal-cell--empty):not(.is-future) {
  background: var(--p5-red-soft);
}

.cal-cell--empty {
  cursor: default;
}

.is-future {
  color: #55555c;
  cursor: default;
}

.is-today .cal-daynum {
  box-shadow: 0 0 0 1px var(--p5-red);
  padding: 0 4px;
}

.is-selected {
  background: var(--p5-red);
  font-weight: 700;
}

.is-selected:hover {
  background: var(--p5-red);
}

.cal-dot {
  width: 6px;
  height: 6px;
  margin-top: 2px;
  background: var(--p5-red);
  border-radius: 50%;
}

.is-selected .cal-dot {
  background: var(--p5-white);
}
</style>
