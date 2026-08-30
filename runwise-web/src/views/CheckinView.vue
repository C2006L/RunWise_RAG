<script setup>
import { computed, onMounted } from 'vue'
import P5Card from '../components/common/P5Card.vue'
import CheckinCalendar from '../components/business/CheckinCalendar.vue'
import CheckinForm from '../components/business/CheckinForm.vue'
import { useCheckinStore } from '../stores/checkin'
import { formatDate } from '../composables/useFormatDate'

// 打卡页（工程计划 4.4 / M3 功能态）：日历卡 + 表单卡，容器均为 P5Card 切角容器
// 页面职责：驱动数据加载（初始当月 + 翻月），选中日期状态收在 store
const store = useCheckinStore()

const todayKey = formatDate(new Date())
const formTag = computed(() => (store.selectedDate === todayKey ? '今日打卡' : '补录打卡'))

function loadMonth({ startDate, endDate }) {
  store.loadRange(startDate, endDate)
}

onMounted(() => {
  const now = new Date()
  loadMonth({
    startDate: formatDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    endDate: formatDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
  })
})
</script>

<template>
  <div class="checkin-page">
    <header class="page-head">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title">打卡记录</h1>
      <p class="page-desc">以日历回顾每一天的训练足迹，坚持从记录开始。</p>
    </header>

    <div class="checkin-layout">
      <P5Card tag="训练日历" tag-rotate="-5deg">
        <CheckinCalendar @month-change="loadMonth" />
      </P5Card>
      <P5Card :tag="formTag" tag-rotate="4deg">
        <CheckinForm />
      </P5Card>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: var(--sp-6);
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
  font-weight: 900;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--p5-white);
  margin-bottom: var(--sp-3);
}

.page-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
}

.checkin-layout {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: var(--sp-5);
  align-items: start;
}

@media (max-width: 960px) {
  .checkin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
