<template>
  <view class="page">
    <!-- 背景图片层 -->
    <image class="page-bg" src="/static/blurry-gradient-haikei.png" mode="aspectFill"></image>

    <!-- 二级导航 -->
    <view class="sub-nav">
      <view
        v-for="(tab, idx) in tabs"
        :key="idx"
        class="nav-item"
        :class="{ active: currentTab === idx }"
        @click="switchTab(idx)"
      >
        <text class="nav-text">{{ tab }}</text>
      </view>
    </view>

    <!-- 日历视图 -->
    <view v-if="currentTab === 0" class="view-pad">
      <view class="card calendar-card">
        <view class="month-switcher">
          <view class="arrow" @click="prevMonth">
            <uni-icons type="arrow-left" size="18" color="#6b7280"></uni-icons>
          </view>
          <text class="month-title">{{ year }}年{{ month }}月</text>
          <view class="arrow" @click="nextMonth">
            <uni-icons type="arrow-right" size="18" color="#6b7280"></uni-icons>
          </view>
        </view>

        <view class="weekdays">
          <text v-for="(w, i) in weekdays" :key="i" class="weekday">{{
            w
          }}</text>
        </view>

        <view class="calendar-grid">
          <view
            v-for="(cell, i) in calendarDays"
            :key="i"
            class="day-cell"
            :class="
              cell
                ? {
                    'is-today': isToday(cell.dateStr),
                    'is-selected': isSelected(cell.dateStr),
                    'is-disabled': isDisabled(cell.dateStr),
                  }
                : 'is-empty'
            "
            @click="cell && selectDay(cell)"
          >
            <template v-if="cell">
              <view class="day-inner">
                <text class="day-text">{{ cell.date }}</text>
              </view>
              <view v-if="isChecked(cell.dateStr)" class="check-dot"></view>
            </template>
          </view>
        </view>
      </view>

      <!-- 当日打卡卡片 -->
      <view v-if="selectedDayCheckin" class="card day-detail-card">
        <view class="detail-top">
          <text class="detail-date">{{ selectedDateLabel }}</text>
          <view class="mood-tag" :class="moodClass(selectedDayCheckin.mood)">
            <text class="mood-tag-text">{{ selectedDayCheckin.mood }}</text>
          </view>
        </view>
        <view class="detail-stats">
          <view class="d-stat">
            <view class="d-value-row">
              <text class="d-value">{{ selectedDayCheckin.distance }}</text>
              <text class="d-unit">km</text>
            </view>
            <text class="d-label">距离</text>
          </view>
          <view class="d-stat">
            <view class="d-value-row">
              <text class="d-value">{{ selectedDayCheckin.duration }}</text>
              <text class="d-unit">min</text>
            </view>
            <text class="d-label">时长</text>
          </view>
          <view class="d-stat">
            <view class="d-value-row">
              <text class="d-value">{{
                selectedDayCheckin.pace
                  ? formatPace(selectedDayCheckin.pace)
                  : "—"
              }}</text>
            </view>
            <text class="d-label">配速</text>
          </view>
        </view>
        <view v-if="selectedDayCheckin.remark" class="detail-remark">
          <text class="remark-text">{{ selectedDayCheckin.remark }}</text>
        </view>
      </view>
      <view v-else class="card empty-state-card">
        <view class="empty-runner-icon">🏃</view>
        <text class="empty-date-label">{{ selectedDateLabel }}</text>
        <text class="empty-main-text">暂无打卡记录</text>
        <view class="empty-go-btn" @click="onFabClick">
          <text class="empty-go-text">去打卡 →</text>
        </view>
      </view>
    </view>

    <!-- 列表视图 -->
    <view v-else-if="currentTab === 1" class="view-pad">
      <view class="list-header">
        <text class="list-summary"
          >本月{{ stats.monthCount }}次 · 累计{{ stats.totalDistance }}km</text
        >
      </view>
      <view class="checkin-list">
        <view
          v-for="item in checkinList"
          :key="item.id"
          class="card checkin-item"
          @click="onItemClick(item)"
        >
          <view class="thumb">
            <image
              v-if="item.imageUrl"
              class="thumb-img"
              :src="item.imageUrl"
              mode="aspectFill"
              lazy-load
            />
            <view v-else class="thumb-placeholder">
              <uni-icons type="person-filled" size="24" color="#FF6B35"></uni-icons>
            </view>
          </view>
          <view class="item-body">
            <text class="item-date">{{ item.checkinDate }}</text>
            <text class="item-data">{{ formatItemData(item) }}</text>
            <view class="item-tags">
              <view class="mood-tag" :class="moodClass(item.mood)">
                <text class="mood-tag-text">{{ item.mood }}</text>
              </view>
              <text v-if="item.remark" class="item-remark">{{
                item.remark
              }}</text>
            </view>
          </view>
          <text class="item-arrow"></text>
        </view>
      </view>
    </view>

    <!-- 统计视图 -->
    <view v-else-if="currentTab === 2" class="view-pad">
      <view class="card total-card">
        <text class="total-number">{{ stats.totalDistance }}</text>
        <text class="total-unit">km</text>
        <text class="total-label">累计总里程</text>
      </view>

      <view class="card chart-card">
        <view class="card-head">
          <text class="card-title">月度里程</text>
          <text class="card-sub">本月 {{ currentMonthValue }}km</text>
        </view>
        <view class="bar-chart">
          <view v-for="(val, i) in stats.monthData" :key="i" class="bar-col">
            <view class="bar-track">
              <view
                class="bar"
                :class="{ 'bar-current': isCurrentMonthBar(i) }"
                :style="{ height: barHeight(val) + 'rpx' }"
              ></view>
            </view>
            <text class="bar-label">{{ i + 1 }}月</text>
          </view>
        </view>
      </view>

      <view class="metrics-grid">
        <view class="card metric-card">
          <text class="metric-value orange">{{ stats.streak }}</text>
          <text class="metric-label">连续天数</text>
        </view>
        <view class="card metric-card">
          <text class="metric-value blue">{{ stats.monthCount }}</text>
          <text class="metric-label">本月打卡</text>
        </view>
        <view class="card metric-card">
          <text class="metric-value orange">{{ stats.avgPace }}</text>
          <text class="metric-label">平均配速</text>
        </view>
        <view class="card metric-card">
          <text class="metric-value blue">{{ stats.totalCount }}</text>
          <text class="metric-label">总打卡次数</text>
        </view>
      </view>
    </view>

    <!-- FAB -->
    <view class="fab" @click="onFabClick">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onShow } from "@dcloudio/uni-app";

interface CalendarItem {
  date: string;
  distance: number;
  duration: number;
  mood: string;
}

interface CheckinItem {
  id: number;
  checkinDate: string;
  distance: number;
  duration: number;
  pace: number;
  mood: string;
  remark: string;
  imageUrl: string;
}

interface Stats {
  totalDistance: number;
  streak: number;
  monthCount: number;
  avgPace: string;
  totalCount: number;
  monthData: number[];
}

interface DayCell {
  date: number;
  dateStr: string;
}

interface DayDetail {
  date: string;
  distance: number;
  duration: number;
  mood: string;
  pace?: number;
  remark?: string;
  imageUrl?: string;
}

const tabs = ["日历", "列表", "统计"];
const weekdays = ["一", "二", "三", "四", "五", "六", "日"];

const currentTab = ref(0);

const calendarData = ref<CalendarItem[]>([
  { date: "2025-08-01", distance: 3.5, duration: 25, mood: "轻松" },
  { date: "2025-08-03", distance: 5.2, duration: 32, mood: "适中" },
  { date: "2025-08-05", distance: 4.0, duration: 28, mood: "轻松" },
]);

const checkinList = ref<CheckinItem[]>([
  {
    id: 1,
    checkinDate: "2025-08-05",
    distance: 4.0,
    duration: 28,
    pace: 7.0,
    mood: "轻松",
    remark: "晨跑恢复",
    imageUrl: "",
  },
  {
    id: 2,
    checkinDate: "2025-08-03",
    distance: 5.2,
    duration: 32,
    pace: 6.15,
    mood: "适中",
    remark: "操场5K",
    imageUrl: "",
  },
  {
    id: 3,
    checkinDate: "2025-08-01",
    distance: 3.5,
    duration: 25,
    pace: 7.14,
    mood: "轻松",
    remark: "",
    imageUrl: "",
  },
]);

const stats = ref<Stats>({
  totalDistance: 125.75,
  streak: 12,
  monthCount: 8,
  avgPace: "6'30\"",
  totalCount: 42,
  monthData: [5.2, 3.8, 4.0, 6.1, 3.5, 2.8, 4.2, 5.0],
});

/* ========== 日历逻辑 ========== */
const today = new Date();
const year = ref(today.getFullYear());
const month = ref(today.getMonth() + 1);

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function formatDate(y: number, m: number, d: number): string {
  return `${y}-${pad(m)}-${pad(d)}`;
}

const todayStr = formatDate(
  today.getFullYear(),
  today.getMonth() + 1,
  today.getDate(),
);
const selectedDate = ref(todayStr);

// 计算当前月日期数组（周一起始）
const calendarDays = computed<(DayCell | null)[]>(() => {
  const firstDay = new Date(year.value, month.value - 1, 1);
  const daysInMonth = new Date(year.value, month.value, 0).getDate();
  // 转换为周一起始：0=周一 ... 6=周日
  let firstWeekday = firstDay.getDay() - 1;
  if (firstWeekday < 0) firstWeekday = 6;

  const cells: (DayCell | null)[] = [];
  for (let i = 0; i < firstWeekday; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({ date: d, dateStr: formatDate(year.value, month.value, d) });
  }
  return cells;
});

const checkedDateSet = computed(() => {
  const s = new Set<string>();
  calendarData.value.forEach((c) => s.add(c.date));
  checkinList.value.forEach((c) => s.add(c.checkinDate));
  return s;
});

// 合并日历数据与列表数据，列表数据字段更全（含 pace/remark）
const checkinByDate = computed(() => {
  const map = new Map<string, DayDetail>();
  checkinList.value.forEach((c) =>
    map.set(c.checkinDate, {
      date: c.checkinDate,
      distance: c.distance,
      duration: c.duration,
      mood: c.mood,
      pace: c.pace,
      remark: c.remark,
      imageUrl: c.imageUrl,
    }),
  );
  calendarData.value.forEach((c) => {
    if (!map.has(c.date)) map.set(c.date, { ...c });
  });
  return map;
});

const selectedDayCheckin = computed<DayDetail | null>(
  () => checkinByDate.value.get(selectedDate.value) ?? null,
);

const selectedDateLabel = computed(() => {
  const parts = selectedDate.value.split("-");
  return `${Number(parts[1])}月${Number(parts[2])}日`;
});

function isToday(dateStr: string): boolean {
  return dateStr === todayStr;
}

function isSelected(dateStr: string): boolean {
  return dateStr === selectedDate.value;
}

function isChecked(dateStr: string): boolean {
  return checkedDateSet.value.has(dateStr);
}

function isDisabled(dateStr: string): boolean {
  return dateStr > todayStr;
}

function switchTab(idx: number) {
  currentTab.value = idx;
}

// 页面显示时读取首页传递的 Tab 参数，自动切换到日历视图
onShow(() => {
  const app = getApp();
  if (app.globalData?.checkinTab !== undefined) {
    currentTab.value = app.globalData.checkinTab;
    app.globalData.checkinTab = undefined;
  }
});

function selectDay(cell: DayCell) {
  if (isDisabled(cell.dateStr)) return;
  selectedDate.value = cell.dateStr;
}

function prevMonth() {
  if (month.value === 1) {
    year.value--;
    month.value = 12;
  } else {
    month.value--;
  }
}

function nextMonth() {
  if (month.value === 12) {
    year.value++;
    month.value = 1;
  } else {
    month.value++;
  }
}

/* ========== 格式化 ========== */
function moodClass(mood: string): string {
  if (mood === "轻松") return "mood-easy";
  if (mood === "适中") return "mood-moderate";
  if (mood === "吃力") return "mood-hard";
  return "mood-moderate";
}

function formatPace(pace: number): string {
  const m = Math.floor(pace);
  const s = Math.round((pace - m) * 60);
  return `${m}'${pad(s)}"`;
}

function formatItemData(item: CheckinItem): string {
  return `${item.distance}km · ${item.duration}min · ${formatPace(item.pace)}/km`;
}

/* ========== 统计柱状图 ========== */
const maxMonthData = computed(() => Math.max(...stats.value.monthData));

function barHeight(val: number): number {
  const maxH = 220;
  return Math.max(16, (val / maxMonthData.value) * maxH);
}

function isCurrentMonthBar(i: number): boolean {
  return i === stats.value.monthData.length - 1;
}

const currentMonthValue = computed(
  () => stats.value.monthData[stats.value.monthData.length - 1] ?? 0,
);

/* ========== 交互 ========== */
function onFabClick() {
  uni.navigateTo({ url: "/pages/checkin/create" });
}

function onItemClick(item: CheckinItem) {
  uni.navigateTo({ url: `/pages/checkin/create` });
}
</script>

<style lang="scss">
.page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 240rpx;
  overflow: hidden;
}

.page-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
  filter: brightness(1.15) saturate(1.3);
}

/* ========== 二级导航 ========== */
.sub-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  height: 80rpx;
  background-color: $rw-bg-card;
  border-bottom: 2rpx solid $rw-divider;
}

.nav-item {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-text {
  font-size: 28rpx;
  font-weight: 400;
  color: $rw-text-placeholder;
  transition: all 0.2s;
}

.nav-item.active .nav-text {
  color: $rw-primary;
  font-weight: 500;
}

.nav-item.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background-color: $rw-primary;
  border-radius: 2rpx;
  transition: all 0.2s;
}

/* ========== 通用卡片 ========== */
.view-pad {
  padding: 24rpx;
}

.card {
  @include rw-glass-card;
  padding: 32rpx;
  transition: $rw-transition-bounce;

  &:active {
    transform: scale(0.98);
    box-shadow: $rw-shadow-card-hover;
  }
}

/* ========== 日历视图 ========== */
.calendar-card {
  margin-bottom: 24rpx;
}

.month-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.arrow {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.month-title {
  font-size: 32rpx;
  font-weight: 500;
  color: $rw-text-primary;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8rpx;
}

.weekday {
  text-align: center;
  font-size: 24rpx;
  color: $rw-text-placeholder;
  line-height: 48rpx;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4rpx;
}

.day-cell {
  height: 80rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.day-cell.is-empty {
  pointer-events: none;
}

.day-inner {
  width: 72rpx;
  height: 64rpx;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: $rw-transition-bounce;
}

.day-text {
  font-size: 26rpx;
  color: $rw-text-primary;
}

.day-cell.is-today .day-inner {
  border: 2rpx solid $rw-primary;
}

.day-cell.is-today .day-text {
  color: $rw-primary;
}

.day-cell.is-selected .day-inner {
  background-color: $rw-primary-selected;
  border-radius: 32rpx;
}

.day-cell.is-selected .day-text {
  color: $rw-primary;
  font-weight: 500;
}

.day-cell.is-disabled {
  opacity: 0.4;
}

.day-cell.is-disabled .day-text {
  color: $rw-text-placeholder;
}

.check-dot {
  position: absolute;
  bottom: 6rpx;
  right: 12rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: $rw-primary;
}

/* 当日打卡卡片 */
.day-detail-card {
  margin-bottom: 24rpx;
}

.detail-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.detail-date {
  font-size: 30rpx;
  font-weight: 500;
  color: $rw-text-primary;
}

.detail-stats {
  display: flex;
  justify-content: space-between;
}

.d-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.d-value-row {
  display: flex;
  align-items: baseline;
}

.d-value {
  font-size: 36rpx;
  font-weight: 600;
  color: $rw-text-primary;
}

.d-unit {
  font-size: 22rpx;
  color: $rw-text-secondary;
  margin-left: 4rpx;
}

.d-label {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $rw-text-secondary;
}

.detail-remark {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid $rw-divider;
}

.remark-text {
  font-size: 26rpx;
  color: $rw-text-secondary;
}

/* ========== 空状态（卡片化） ========== */
.empty-state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48rpx 32rpx;
  margin-bottom: 24rpx;
}

.empty-runner-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.empty-date-label {
  font-size: 24rpx;
  color: $rw-text-secondary;
  margin-bottom: 8rpx;
}

.empty-main-text {
  font-size: 28rpx;
  color: #666666;
  font-weight: 500;
  margin-bottom: 24rpx;
}

.empty-go-btn {
  background: $rw-primary;
  border-radius: 9999rpx;
  padding: 16rpx 48rpx;
  box-shadow: $rw-shadow-fab;

  &:active {
    transform: scale(0.95);
    opacity: 0.9;
  }
}

.empty-go-text {
  font-size: 28rpx;
  color: #ffffff;
  font-weight: 600;
}

/* ========== 心情标签 ========== */
.mood-tag {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.mood-tag-text {
  font-size: 22rpx;
  line-height: 32rpx;
}

.mood-tag.mood-easy {
  background-color: rgba(0, 200, 83, 0.1);
}

.mood-tag.mood-easy .mood-tag-text {
  color: $rw-mood-easy;
}

.mood-tag.mood-moderate {
  background-color: rgba(24, 144, 255, 0.1);
}

.mood-tag.mood-moderate .mood-tag-text {
  color: $rw-mood-moderate;
}

.mood-tag.mood-hard {
  background-color: rgba(245, 158, 11, 0.1);
}

.mood-tag.mood-hard .mood-tag-text {
  color: $rw-mood-hard;
}

/* ========== 列表视图 ========== */
.list-header {
  padding: 8rpx 8rpx 20rpx;
}

.list-summary {
  font-size: 28rpx;
  color: $rw-text-secondary;
}

.checkin-list {
  display: flex;
  flex-direction: column;
}

.checkin-item {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 24rpx;
}

.checkin-item:last-child {
  margin-bottom: 0;
}

.thumb {
  width: 128rpx;
  height: 128rpx;
  border-radius: 24rpx;
  background-color: $rw-bg-hover;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-body {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.item-date {
  font-size: 28rpx;
  color: $rw-text-secondary;
  margin-bottom: 8rpx;
}

.item-data {
  font-size: 28rpx;
  color: $rw-text-primary;
  margin-bottom: 12rpx;
}

.item-tags {
  display: flex;
  align-items: center;
}

.item-remark {
  margin-left: 12rpx;
  font-size: 24rpx;
  color: $rw-text-secondary;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item-arrow {
  margin-left: 12rpx;
  flex-shrink: 0;
  width: 16rpx;
  height: 16rpx;
  border-right: 2rpx solid #c0c4cc;
  border-bottom: 2rpx solid #c0c4cc;
  transform: rotate(-45deg);
}

/* ========== 统计视图 ========== */
.total-card {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 32rpx;
}

.total-number {
  font-size: 64rpx;
  font-weight: 700;
  color: $rw-primary;
  line-height: 1.1;
}

.total-unit {
  margin-top: 8rpx;
  font-size: 28rpx;
  color: $rw-text-secondary;
}

.total-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.chart-card {
  margin-bottom: 24rpx;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 500;
  color: $rw-text-primary;
}

.card-sub {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-track {
  width: 100%;
  height: 240rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar {
  width: 36rpx;
  border-radius: 8rpx 8rpx 0 0;
  background-color: $rw-chart-history;
  transition: height 0.3s;
}

.bar.bar-current {
  background-color: $rw-chart-current;
}

.bar-label {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: $rw-text-placeholder;
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}

.metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 24rpx;
}

.metric-value {
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8rpx;
}

.metric-value.orange {
  color: $rw-primary;
}

.metric-value.blue {
  color: $rw-secondary;
}

.metric-label {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

/* ========== FAB ========== */
.fab {
  @include rw-fab;
  right: 32rpx;
  bottom: 144rpx;
  width: 112rpx;
  height: 112rpx;
}

.fab-icon {
  font-size: 56rpx;
  font-weight: 300;
  color: #ffffff;
  line-height: 1;
}
</style>