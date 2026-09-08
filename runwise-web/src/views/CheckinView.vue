<script setup>
// 打卡记录页（工程计划 4.4 / M3 功能态）：日历卡 + 表单卡，容器均为 P5Card 切角容器
// 页面职责：驱动数据加载（初始当月 + 翻月），选中日期状态收在 store
// - 内页装饰系统 2.0：星 6 + 网点 1 + 斜杠 1 + 水印 1
import { computed, onMounted, ref } from "vue";
import P5Card from "../components/common/P5Card.vue";
import NumberBib from "../components/common/NumberBib.vue";
import RansomNote from "../components/common/RansomNote.vue";
import CheckinDecor from "../components/decor/CheckinDecor.vue";
import CheckinCalendar from "../components/business/CheckinCalendar.vue";
import CheckinForm from "../components/business/CheckinForm.vue";
import { useCheckinStore } from "../stores/checkin";
import { formatDate } from "../composables/useFormatDate";

// 打卡页（工程计划 4.4 / M3 功能态）：日历卡 + 表单卡，容器均为 P5Card 切角容器
// 页面职责：驱动数据加载（初始当月 + 翻月），选中日期状态收在 store
const store = useCheckinStore();

const todayKey = formatDate(new Date());
// 胶带文案与卡片内容严格一致（UI 精修 P0-3 / B2 空态）：
// 有记录 →「当日记录」（只读卡）；今天无记录 →「今日打卡」（表单）；
// 过去无记录 →「暂无记录」（空态卡 + 去首页打卡链接）
const formTag = computed(() => {
  if (store.selectedRecord) return "当日记录";
  return store.selectedDate === todayKey ? "今日打卡" : "暂无记录";
});

function loadMonth({ startDate, endDate }) {
  // S1 号码布：随日历翻月联动（默认即本月），统计可视月已打卡数
  viewRange.value = { startDate, endDate };
  store.loadRange(startDate, endDate);
}

// ===== S1 号码布数据：本月已打卡 / 当月天数（自动计算，禁止手写） =====
const now = new Date();
const initialRange = {
  startDate: formatDate(new Date(now.getFullYear(), now.getMonth(), 1)),
  endDate: formatDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
};
const viewRange = ref({ ...initialRange });

const monthStat = computed(() => {
  const { startDate, endDate } = viewRange.value;
  const days = Number(endDate.slice(8));
  const count = Object.keys(store.records).filter(
    (k) => k >= startDate && k <= endDate,
  ).length;
  return `${count}/${days}`;
});

// A-1 号码布单位 = 当月英文缩写（SEP 等，随翻月联动），与值【4/30】同源
const MONTH_MAP = [
  'JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN',
  'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC',
];
const monthLabel = computed(
  () => MONTH_MAP[Number(viewRange.value.startDate.slice(5, 7)) - 1] || 'M',
);

onMounted(() => {
  loadMonth(initialRange);
});
</script>

<template>
  <div class="checkin-page">
    <!-- 内页装饰系统 2.0：页面级装饰层（z-index:0，卡片 z-index:1） -->
    <CheckinDecor />

    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title" aria-label="打卡记录">
        <RansomNote text="打卡记录" :seed="11" />
      </h1>
      <p class="page-desc">以日历回顾每一天的训练足迹，坚持从记录开始。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- S3 竖排字：左侧留白（app-main 左内边距 gutter），仅此一页 -->
    <span class="side-caption" aria-hidden="true">RECORD / 每一步都算数</span>

    <div class="checkin-layout">
      <div class="cal-card-zone">
        <!-- S1/A-1 号码布：本月已打卡/当月天数 + 当月英文缩写单位（随翻月联动） -->
        <NumberBib
          :unit="monthLabel"
          :value="monthStat"
          rotate="2deg"
          style="top: -28px; right: 22px"
        />
        <!-- B1：贴纸红底白字（全站贴纸已定红），框体保持默认切角工艺不变 -->
        <P5Card tag="训练日历" tag-rotate="-5deg" tag-variant="red">
          <CheckinCalendar @month-change="loadMonth" />
        </P5Card>
      </div>
      <P5Card
        :tag="formTag"
        tag-rotate="4deg"
        tag-top="-15px"
        tag-right="36px"
        tag-variant="red"
      >
        <CheckinForm />
      </P5Card>
    </div>
  </div>
</template>

<style scoped>
.checkin-page {
  position: relative; /* 承载 CheckinDecor 装饰层 */
}

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
  position: relative;
  z-index: 1; /* 0-2 装饰层级：卡片永远在装饰层（z-index:0）之上 */
  display: grid;
  /* v2.0 3.4.2 规则 1：7:5 不等分栏（告别均分栅格的 AI 味工整） */
  grid-template-columns: 7fr 5fr;
  gap: var(--sp-5);
  /* 拉伸对齐：右栏表单卡与日历卡等高，右栏下方不出空洞（空白压缩） */
  align-items: stretch;
}

/* S1 号码布锚定区：网格拉伸由 zone 承接（原 wrap 为直接网格项） */
.cal-card-zone {
  position: relative;
  display: flex;
  flex-direction: column;
}

.cal-card-zone :deep(.p5-card-wrap) {
  flex: 1;
}

/* S3 竖排字：左侧留白（落进 app-main 左 24px 内边距，任意屏宽均可见） */
.side-caption {
  position: absolute;
  left: calc(-1 * var(--sp-5));
  top: 230px; /* 页头/分隔带之下，沿日历卡左侧纵向排布 */
  writing-mode: vertical-rl;
  white-space: nowrap;
  font-size: 10px;
  letter-spacing: 0.6em;
  color: var(--p5-gray);
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}

@media (max-width: 960px) {
  .checkin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
