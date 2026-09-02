<script setup>
import { computed, onMounted } from "vue";
import P5Card from "../components/common/P5Card.vue";
import CheckinCalendar from "../components/business/CheckinCalendar.vue";
import CheckinForm from "../components/business/CheckinForm.vue";
import { useCheckinStore } from "../stores/checkin";
import { formatDate } from "../composables/useFormatDate";

// 打卡页（工程计划 4.4 / M3 功能态）：日历卡 + 表单卡，容器均为 P5Card 切角容器
// 页面职责：驱动数据加载（初始当月 + 翻月），选中日期状态收在 store
const store = useCheckinStore();

const todayKey = formatDate(new Date());
// 胶带文案与卡片内容严格一致（UI 精修 P0-3）：
// 有记录 →「当日记录」（只读卡）；今天无记录 →「今日打卡」（表单）；过去无记录 →「补录打卡」（补录表单）
const formTag = computed(() => {
  if (store.selectedRecord) return "当日记录";
  return store.selectedDate === todayKey ? "今日打卡" : "补录打卡";
});

function loadMonth({ startDate, endDate }) {
  store.loadRange(startDate, endDate);
}

onMounted(() => {
  const now = new Date();
  loadMonth({
    startDate: formatDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    endDate: formatDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
  });
});
</script>

<template>
  <div class="checkin-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">打卡记录</h1>
      <p class="page-desc">以日历回顾每一天的训练足迹，坚持从记录开始。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <div class="checkin-layout">
      <P5Card tag="训练日历" tag-rotate="-5deg">
        <CheckinCalendar @month-change="loadMonth" />
      </P5Card>
      <P5Card :tag="formTag" tag-rotate="4deg" tag-top="-15px" tag-right="36px">
        <CheckinForm />
      </P5Card>
    </div>
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

.checkin-layout {
  display: grid;
  /* v2.0 3.4.2 规则 1：7:5 不等分栏（告别均分栅格的 AI 味工整） */
  grid-template-columns: 7fr 5fr;
  gap: var(--sp-5);
  /* 拉伸对齐：右栏表单卡与日历卡等高，右栏下方不出空洞（空白压缩） */
  align-items: stretch;
}

@media (max-width: 960px) {
  .checkin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
