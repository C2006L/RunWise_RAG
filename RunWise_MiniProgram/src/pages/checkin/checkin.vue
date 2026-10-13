<template>
  <view class="ck-page">
    <!-- 背景网点纹理（P5 语言，纯 CSS 零体积） -->
    <view class="ck-page__dots"></view>

    <!-- 二级导航：日历 / 列表 / 统计（红切角选中块，对齐 tabbar 语言） -->
    <view class="sub-nav">
      <view
        v-for="(tab, idx) in tabs"
        :key="idx"
        class="sub-nav__item"
        :class="{ 'is-active': currentTab === idx }"
        hover-class="sub-nav__item--active"
        @click="switchTab(idx)"
      >
        <text class="sub-nav__text">{{ tab }}</text>
      </view>
    </view>

    <!-- ==================== 日历视图 ==================== -->
    <view v-if="currentTab === 0" class="view-pad">
      <!-- S3 竖排字：仅打卡页（左侧留白） -->
      <text class="side-caption">RECORD / 每一步都算数</text>

      <!-- 训练日历卡（S1 号码布：本月已打卡/当月天数 + 当月英文缩写，翻月联动） -->
      <view class="cal-zone">
        <!-- 号码布移出 rw-card slot，直接作为 cal-zone 定位子元素：
             跨组件 slot 内的绝对定位在 mp-weixin 下不稳定，会横向撑歪日历卡
             导致左侧日期列（含今天）被挤出可视区（20260921 修复） -->
        <view class="cal-bib">
          <rw-number-bib :value="monthStat" :unit="monthLabel" rotate="2deg" />
        </view>
        <rw-card
          tag="训练日历"
          tag-rotate="-5deg"
          tag-variant="red"
          frame="red"
          padding="0"
        >
          <view class="cal">
            <view class="cal__head">
              <view class="cal__nav" hover-class="cal__nav--active" @click="prevMonth">
                <text class="cal__nav-icon">‹</text>
              </view>
              <text class="cal__title">{{ year }} 年 {{ month }} 月</text>
              <view class="cal__nav" hover-class="cal__nav--active" @click="nextMonth">
                <text class="cal__nav-icon">›</text>
              </view>
            </view>

            <view class="cal__week">
              <text v-for="(w, i) in weekdays" :key="i" class="cal__week-item">{{ w }}</text>
            </view>

            <!-- B3 图例：◆ 已打卡 · ■ 今天 -->
            <view class="cal__legend">
              <text class="cal__legend-mark">◆</text>
              <text class="cal__legend-text">已打卡 · </text>
              <text class="cal__legend-mark">■</text>
              <text class="cal__legend-text"> 今天</text>
            </view>

            <view class="cal__grid">
              <view
                v-for="(cell, i) in calendarDays"
                :key="i"
                class="cal__cell"
                :class="
                  cell
                    ? {
                        'is-today': isToday(cell.dateStr),
                        'is-selected': isSelected(cell.dateStr),
                        'is-future': isDisabled(cell.dateStr),
                        'has-record': isChecked(cell.dateStr),
                      }
                    : 'is-empty'
                "
                @click="cell && selectDay(cell)"
              >
                <template v-if="cell">
                  <text class="cal__daynum">{{ cell.date }}</text>
                  <view
                    v-if="isChecked(cell.dateStr)"
                    class="cal__dot"
                    :class="{ 'cal__dot--stamp': cell.dateStr === stampedDate }"
                  ></view>
                </template>
              </view>
            </view>
          </view>
        </rw-card>
      </view>

      <!-- 当日卡（三态：只读记录 / 空态 / 内联打卡表单） -->
      <view class="detail-zone">
        <rw-card
          :tag="detailTag"
          tag-rotate="4deg"
          tag-variant="red"
          frame="red"
          padding="0"
        >
          <view class="ck">
            <!-- ① 只读：该日已有记录 -->
            <template v-if="selectedDayCheckin">
              <text class="ck__kicker">{{
                isTodaySelected ? "TODAY · 已完成打卡" : "RECORD · 当日记录"
              }}</text>
              <text class="ck__date">{{ selectedDateLabel }}</text>

              <view class="ck__grid">
                <view class="ck__item">
                  <text class="ck__label">距离</text>
                  <text class="ck__value"
                    >{{ selectedDayCheckin.distance }}<text class="ck__unit">km</text></text
                  >
                </view>
                <view class="ck__item">
                  <text class="ck__label">时长</text>
                  <text class="ck__value"
                    >{{ selectedDayCheckin.duration }}<text class="ck__unit">min</text></text
                  >
                </view>
                <view class="ck__item">
                  <text class="ck__label">配速</text>
                  <text class="ck__value"
                    >{{ formatPace(selectedDayCheckin.pace)
                    }}<text class="ck__unit">/km</text></text
                  >
                </view>
                <view class="ck__item">
                  <text class="ck__label">心情</text>
                  <text class="ck__value ck__value--text">{{ selectedDayCheckin.mood }}</text>
                </view>
              </view>

              <view class="ck__note">
                <text class="ck__label">备注</text>
                <text class="ck__note-text">{{ selectedDayCheckin.remark || "无" }}</text>
              </view>
            </template>

            <!-- ② 空态：过去日期无记录 -->
            <template v-else-if="!isTodaySelected">
              <text class="ck__kicker">EMPTY · 暂无记录</text>
              <text class="ck__date">{{ selectedDateLabel }}</text>
              <view class="ck__empty">
                <text class="ck__empty-text">这一天还没有记录 · </text>
                <text class="ck__empty-link" @click="goToday">去今天打卡 →</text>
              </view>
            </template>

            <!-- ③ 表单：今天无记录（打卡主链路） -->
            <template v-else>
              <text class="ck__kicker">TODAY · 待打卡</text>
              <text class="ck__date">今日打卡</text>

              <view class="field-row">
                <view class="field">
                  <text class="field__label">距离（km）</text>
                  <rw-input v-model="form.distance" type="digit" placeholder="如 5.2" />
                </view>
                <view class="field">
                  <text class="field__label">时长（分钟）</text>
                  <rw-input v-model="form.duration" type="number" placeholder="如 32" />
                </view>
              </view>

              <view class="field">
                <text class="field__label">心情</text>
                <view class="mood-chips">
                  <view
                    v-for="m in MOODS"
                    :key="m.value"
                    class="mood-chip"
                    :class="{ 'is-active': form.mood === m.value }"
                    @click="form.mood = m.value"
                  >
                    <text class="mood-chip__text">{{ m.label }}</text>
                  </view>
                </view>
              </view>

              <view class="field">
                <text class="field__label">备注（可选）</text>
                <rw-input v-model="form.note" placeholder="一句话记录今天的跑步" :maxlength="100" />
              </view>

              <view class="pace-preview">
                <text class="pace-preview__label">预计配速</text>
                <text class="pace-preview__val">{{ pacePreview || "--" }}</text>
              </view>

              <text v-if="errorMsg" class="ck__error">{{ errorMsg }}</text>

              <rw-button
                text="提交打卡"
                variant="primary"
                style="min-width: 340rpx"
                @tap="handleSubmit"
              />
            </template>
          </view>
        </rw-card>
      </view>
    </view>

    <!-- ==================== 列表视图 ==================== -->
    <view v-else-if="currentTab === 1" class="view-pad">
      <view class="list-head">
        <text class="list-summary"
          >本月 {{ stats.monthCount }} 次 · 累计 {{ stats.totalDistance }} km</text
        >
      </view>

      <view class="ck-list">
        <view
          v-for="item in checkinList"
          :key="item.id"
          class="ck-list__item"
          hover-class="ck-list__item--active"
          @click="onItemClick(item)"
        >
          <view class="ck-list__thumb">
            <image
              v-if="item.imageUrl"
              class="ck-list__img"
              :src="item.imageUrl"
              mode="aspectFill"
              lazy-load
            />
            <view v-else class="ck-list__ph">
              <text class="ck-list__ph-num">{{ dayOf(item.checkinDate) }}</text>
              <view class="ck-list__ph-dot"></view>
            </view>
          </view>

          <view class="ck-list__body">
            <text class="ck-list__date">{{ item.checkinDate }}</text>
            <text class="ck-list__data">{{ formatItemData(item) }}</text>
            <view class="ck-list__tags">
              <rw-tag :text="item.mood" :variant="moodVariant(item.mood)" />
              <text v-if="item.remark" class="ck-list__remark">{{ item.remark }}</text>
            </view>
          </view>

          <text class="ck-list__arrow"></text>
        </view>
      </view>
    </view>

    <!-- ==================== 统计视图 ==================== -->
    <view v-else class="view-pad">
      <view class="total-zone">
        <rw-card frame="red" padding="44rpx 32rpx">
          <view class="total">
            <text class="total__num">{{ stats.totalDistance }}</text>
            <text class="total__unit">km</text>
            <text class="total__label">累计总里程</text>
          </view>
        </rw-card>
      </view>

      <view class="chart-zone">
        <rw-card tag="月度里程" tag-variant="red" frame="red" padding="0">
          <view class="chart">
            <view class="chart__head">
              <text class="chart__title">近 8 个月</text>
              <text class="chart__sub">本月 {{ stats.monthDistance }} km</text>
            </view>
            <view class="chart__bars">
              <view v-for="(val, i) in stats.monthData" :key="i" class="chart__col">
                <view class="chart__track">
                  <view
                    class="chart__bar"
                    :class="{ 'is-current': i === stats.monthData.length - 1 }"
                    :style="{ height: barHeight(val) + 'rpx' }"
                  ></view>
                </view>
                <text class="chart__label">{{ monthLabels[i] }}</text>
              </view>
            </view>
          </view>
        </rw-card>
      </view>

      <view class="metrics">
        <rw-card v-for="m in metricList" :key="m.label" frame="red" padding="28rpx 16rpx">
          <view class="metric">
            <text class="metric__value">{{ m.value }}</text>
            <text class="metric__label">{{ m.label }}</text>
          </view>
        </rw-card>
      </view>
    </view>

    <!-- FAB：仅列表/统计 tab 显示（日历 tab 表单就在页面上，避免压住提交按钮） -->
    <view v-if="currentTab !== 0" class="fab" hover-class="fab--active" @click="goForm">
      <text class="fab__icon">+</text>
    </view>

    <rw-toast ref="toastRef" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { setTabBarIndex } from "@/utils/tabbar";

interface CheckinItem {
  id: number;
  checkinDate: string;
  distance: number;
  duration: number;
  pace: number; // 分钟/公里
  mood: string;
  remark: string;
  imageUrl: string;
}

interface DayCell {
  date: number;
  dateStr: string;
}

const tabs = ["日历", "列表", "统计"];
const weekdays = ["一", "二", "三", "四", "五", "六", "日"];

const MOODS = [
  { value: "easy", label: "轻松" },
  { value: "moderate", label: "适中" },
  { value: "hard", label: "吃力" },
];

const currentTab = ref(0);
const toastRef = ref<any>(null);

/* ========== 日期工具 ========== */
function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function formatDate(y: number, m: number, d: number): string {
  return `${y}-${pad(m)}-${pad(d)}`;
}

const today = new Date();
const year = ref(today.getFullYear());
const month = ref(today.getMonth() + 1);

const todayStr = formatDate(today.getFullYear(), today.getMonth() + 1, today.getDate());
const selectedDate = ref(todayStr);

/* ========== mock 数据（动态日期，随系统时间生成；P2 换接口） ========== */
const MOCK_DISTANCE = [5.2, 4.0, 6.3, 3.5, 7.1, 5.0, 4.6, 6.0];
const MOCK_DURATION = [32, 25, 40, 22, 46, 31, 28, 38];
const MOCK_MOOD = ["适中", "轻松", "适中", "轻松", "吃力", "适中", "轻松", "适中"];
const MOCK_REMARK = ["操场 5K", "晨跑恢复", "节奏跑", "慢跑放松", "长距离", "夜跑", "间歇训练", "轻松跑"];
// 距今天的天数（不含今天 → 默认展示「今日打卡」表单，主链路可直接体验）
const MOCK_OFFSETS = [1, 3, 5, 8, 11, 14, 17, 20];

function buildMockList(): CheckinItem[] {
  return MOCK_OFFSETS.map((off, i) => {
    const d = new Date(today);
    d.setDate(d.getDate() - off);
    const distance = MOCK_DISTANCE[i];
    const duration = MOCK_DURATION[i];
    return {
      id: i + 1,
      checkinDate: formatDate(d.getFullYear(), d.getMonth() + 1, d.getDate()),
      distance,
      duration,
      pace: Number((duration / distance).toFixed(2)),
      mood: MOCK_MOOD[i],
      remark: MOCK_REMARK[i],
      imageUrl: "",
    };
  });
}

const checkinList = ref<CheckinItem[]>(buildMockList());

/* ========== 日历逻辑 ========== */
const calendarDays = computed<(DayCell | null)[]>(() => {
  const firstDay = new Date(year.value, month.value - 1, 1);
  const daysInMonth = new Date(year.value, month.value, 0).getDate();
  // 周一起始：0=周一 ... 6=周日
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
  checkinList.value.forEach((c) => s.add(c.checkinDate));
  return s;
});

const selectedDayCheckin = computed<CheckinItem | null>(
  () => checkinList.value.find((i) => i.checkinDate === selectedDate.value) ?? null,
);

const isTodaySelected = computed(() => selectedDate.value === todayStr);

const selectedDateLabel = computed(() => {
  const parts = selectedDate.value.split("-");
  return `${Number(parts[1])}月${Number(parts[2])}日`;
});

// 胶带文案与卡片内容严格一致（有记录 / 今日打卡 / 暂无记录）
const detailTag = computed(() => {
  if (selectedDayCheckin.value) return "当日记录";
  return isTodaySelected.value ? "今日打卡" : "暂无记录";
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

function switchTab(idx: number) {
  currentTab.value = idx;
}

// S1 号码布：本月已打卡 / 当月天数（自动计算，禁止手写）
const monthStat = computed(() => {
  const prefix = `${year.value}-${pad(month.value)}`;
  const days = new Date(year.value, month.value, 0).getDate();
  const count = checkinList.value.filter((i) => i.checkinDate.startsWith(prefix)).length;
  return `${count}/${days}`;
});

// A-1 号码布单位 = 当月英文缩写（SEP 等，随翻月联动）
const MONTH_MAP = [
  "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
  "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
];
const monthLabel = computed(() => MONTH_MAP[month.value - 1] || "M");

// 新打卡「盖章」动画：提交后日历红菱形 scale 1.6→1 落章
const stampedDate = ref("");
let stampTimer: ReturnType<typeof setTimeout> | null = null;

/* ========== 表单 ========== */
const form = ref({ distance: "", duration: "", mood: "moderate", note: "" });
const errorMsg = ref("");

// 配速随输入实时预览
const pacePreview = computed(() => {
  const d = Number(form.value.distance);
  const t = Number(form.value.duration);
  if (!d || !t || d <= 0 || t <= 0) return "";
  return formatPace(t / d) + "/km";
});

function moodLabel(value: string): string {
  return MOODS.find((m) => m.value === value)?.label || "适中";
}

function moodVariant(mood: string): string {
  if (mood === "轻松") return "gray";
  if (mood === "吃力") return "red";
  return "white";
}

function handleSubmit() {
  const d = Number(form.value.distance);
  const t = Number(form.value.duration);
  if (!d || d <= 0) {
    errorMsg.value = "请填写有效的跑步距离";
    return;
  }
  if (!t || t <= 0) {
    errorMsg.value = "请填写有效的跑步时长";
    return;
  }
  errorMsg.value = "";

  const date = selectedDate.value;
  checkinList.value.unshift({
    id: Date.now(),
    checkinDate: date,
    distance: Number(d.toFixed(2)),
    duration: t,
    pace: Number((t / d).toFixed(2)),
    mood: moodLabel(form.value.mood),
    remark: form.value.note.trim(),
    imageUrl: "",
  });

  // 盖章动画（700ms 后清除标记）
  stampedDate.value = date;
  if (stampTimer) clearTimeout(stampTimer);
  stampTimer = setTimeout(() => {
    stampedDate.value = "";
  }, 700);

  form.value = { distance: "", duration: "", mood: "moderate", note: "" };
  toastRef.value?.show("打卡成功", "success");
}

/* ========== 格式化 ========== */
function formatPace(pace: number): string {
  const m = Math.floor(pace);
  const s = Math.round((pace - m) * 60);
  return `${m}'${pad(s)}"`;
}

function formatItemData(item: CheckinItem): string {
  return `${item.distance}km · ${item.duration}min · ${formatPace(item.pace)}/km`;
}

function dayOf(dateStr: string): string {
  return String(Number(dateStr.slice(8)));
}

/* ========== 统计（全部由记录实时计算，禁止手写） ========== */
const stats = computed(() => {
  const list = checkinList.value;
  const totalDistance = Number(list.reduce((s, i) => s + i.distance, 0).toFixed(1));
  const totalCount = list.length;

  const prefix = `${year.value}-${pad(month.value)}`;
  const monthList = list.filter((i) => i.checkinDate.startsWith(prefix));
  const monthCount = monthList.length;
  const monthDistance = Number(monthList.reduce((s, i) => s + i.distance, 0).toFixed(1));

  const avgPace = list.length
    ? formatPace(list.reduce((s, i) => s + i.pace, 0) / list.length)
    : "--";

  // 连续天数：今天无记录则从昨天起算
  let streak = 0;
  const cursor = new Date(today);
  if (!checkedDateSet.value.has(todayStr)) cursor.setDate(cursor.getDate() - 1);
  while (streak < 365) {
    const key = formatDate(cursor.getFullYear(), cursor.getMonth() + 1, cursor.getDate());
    if (!checkedDateSet.value.has(key)) break;
    streak++;
    cursor.setDate(cursor.getDate() - 1);
  }

  // 近 8 个月里程
  const monthData: number[] = [];
  const base = new Date(today.getFullYear(), today.getMonth(), 1);
  for (let i = 7; i >= 0; i--) {
    const d = new Date(base.getFullYear(), base.getMonth() - i, 1);
    const p = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
    monthData.push(
      Number(list.filter((x) => x.checkinDate.startsWith(p)).reduce((s, x) => s + x.distance, 0).toFixed(1)),
    );
  }

  return { totalDistance, totalCount, monthCount, monthDistance, avgPace, streak, monthData };
});

const monthLabels = computed(() => {
  const arr: string[] = [];
  const base = new Date(today.getFullYear(), today.getMonth(), 1);
  for (let i = 7; i >= 0; i--) {
    const d = new Date(base.getFullYear(), base.getMonth() - i, 1);
    arr.push(`${d.getMonth() + 1}月`);
  }
  return arr;
});

const metricList = computed(() => [
  { label: "连续天数", value: stats.value.streak },
  { label: "本月打卡", value: stats.value.monthCount },
  { label: "平均配速", value: stats.value.avgPace },
  { label: "总打卡次数", value: stats.value.totalCount },
]);

const maxMonthData = computed(() => Math.max(...stats.value.monthData, 1));

function barHeight(val: number): number {
  const maxH = 220;
  return Math.max(16, (val / maxMonthData.value) * maxH);
}

/* ========== 交互 ========== */
function scrollToForm() {
  nextTick(() => {
    uni.pageScrollTo({ selector: ".detail-zone", duration: 300, fail: () => {} });
  });
}

// 空态「去今天打卡 →」：切到今天（表单态）
function goToday() {
  selectedDate.value = todayStr;
  scrollToForm();
}

// FAB：回到日历 tab 的今日打卡表单
function goForm() {
  currentTab.value = 0;
  selectedDate.value = todayStr;
  scrollToForm();
}

// 列表项 → 切日历 tab 并选中该日期
function onItemClick(item: CheckinItem) {
  currentTab.value = 0;
  selectedDate.value = item.checkinDate;
  const [y, m] = item.checkinDate.split("-").map(Number);
  year.value = y;
  month.value = m;
  scrollToForm();
}

onShow(() => {
  setTabBarIndex(1);
  const app = getApp();
  if (app.globalData?.checkinTab !== undefined) {
    currentTab.value = app.globalData.checkinTab;
    app.globalData.checkinTab = undefined;
  }
});
</script>

<style lang="scss">
.ck-page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 240rpx;
  overflow-x: hidden;
  background: $rw-black;
}

/* 背景网点纹理（装饰层 z0，内容 z1） */
.ck-page__dots {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.07) 2rpx, transparent 2rpx);
  background-size: 28rpx 28rpx;
}

/* ========== 二级导航：红切角选中块 ========== */
.sub-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  gap: 16rpx;
  padding: 16rpx $rw-sp-4;
  background: $rw-black;
  border-bottom: 2rpx solid $rw-line;
}

.sub-nav__item {
  flex: 1;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(16rpx 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
  transition: $rw-transition;

  &.is-active {
    background: $rw-red;
  }
}

.sub-nav__item--active {
  background: rgba(255, 255, 255, 0.08);
}

.sub-nav__text {
  font-size: 28rpx;
  color: $rw-gray;
  letter-spacing: 2rpx;
}

.sub-nav__item.is-active .sub-nav__text {
  color: $rw-white;
  font-weight: 700;
}

/* ========== 页面内容层 ========== */
.view-pad {
  position: relative;
  z-index: 1;
  padding: 32rpx $rw-sp-4 0;
}

/* S3 竖排字（仅打卡页） */
.side-caption {
  position: absolute;
  left: 4rpx;
  top: 240rpx;
  writing-mode: vertical-rl;
  white-space: nowrap;
  font-size: 18rpx;
  letter-spacing: 8rpx;
  color: $rw-gray;
  opacity: 0.5;
  pointer-events: none;
}

/* ========== 训练日历卡 ========== */
.cal-zone {
  position: relative;
  /* 72rpx：为号码布上浮留出空间（原 48rpx 时号码布会顶到二级导航） */
  margin-top: 72rpx;
}

/* S1 号码布：骑日历卡右上缘。定位由本 wrapper 完成（rw-bib 组件自身已 relative）。
   实测约束（H5 751rpx 视口）：卡片顶部 = cal-zone.top+4，翻页按钮 › 顶部 = +40，
   号码布高 106.7 → top:-76rpx 时底边落在 +30.7，与 › 留 9rpx，与二级导航留 28rpx */
.cal-bib {
  position: absolute;
  top: -76rpx;
  right: 64rpx;
  z-index: 3;
  animation: bib-float 3.4s ease-in-out infinite;
}

/* 只向上浮，回位不越界（向下会把底边推回翻页按钮区域） */
@keyframes bib-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-12rpx);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cal-bib {
    animation: none;
  }
}

.cal {
  padding: 36rpx 32rpx 32rpx;
}

.cal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.cal__nav {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $rw-line;
  transition: $rw-transition;
}

.cal__nav--active {
  border-color: $rw-red;
}

.cal__nav-icon {
  font-size: 34rpx;
  line-height: 1;
  color: $rw-gray;
}

.cal__title {
  font-family: $rw-font-body;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  color: $rw-white;
}

.cal__week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 8rpx;
}

.cal__week-item {
  font-size: 22rpx;
  color: $rw-gray;
}

/* B3 图例：右对齐灰字，符号红点题 */
.cal__legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12rpx;
}

.cal__legend-mark {
  font-size: 20rpx;
  color: $rw-red;
}

.cal__legend-text {
  font-size: 20rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.cal__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.cal__cell {
  position: relative;
  height: 88rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: $rw-transition;

  &.is-empty {
    pointer-events: none;
  }
}

.cal__daynum {
  font-family: $rw-font-display;
  font-size: 26rpx;
  line-height: 1.1;
  color: $rw-white;
  font-feature-settings: "tnum";
}

/* 有打卡：数字加粗提亮 */
.cal__cell.has-record .cal__daynum {
  font-weight: 700;
}

/* 今日：红蒙层切角小标签（呼应全站切角语言） */
.cal__cell.is-today .cal__daynum {
  padding: 2rpx 8rpx;
  background: $rw-red-soft;
  clip-path: polygon(0 0, calc(100% - 8rpx) 0, 100% 8rpx, 100% 100%, 0 100%);
}

/* 选中：红底白字 */
.cal__cell.is-selected {
  background: $rw-red;

  .cal__daynum {
    color: $rw-white;
    font-weight: 700;
    background: none;
    clip-path: none;
  }
}

/* 未来日期：灰禁点 */
.cal__cell.is-future {
  .cal__daynum {
    color: #55555c;
    background: none;
    clip-path: none;
  }
}

/* 打卡标记：红色菱形（斜置方块，像「被盖章」） */
.cal__dot {
  width: 14rpx;
  height: 14rpx;
  margin-top: 4rpx;
  background: $rw-red;
  transform: rotate(45deg);
}

.cal__cell.is-selected .cal__dot {
  background: $rw-white;
}

/* 新打卡盖章动画：scale 1.6→1 落章 */
.cal__dot--stamp {
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

/* ========== 当日卡（三态） ========== */
.detail-zone {
  margin-top: 40rpx;
}

.ck {
  padding: 36rpx 32rpx 44rpx;
}

.ck__kicker {
  display: block;
  font-family: $rw-font-display;
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: $rw-red;
  margin-bottom: 12rpx;
}

.ck__date {
  display: block;
  font-family: $rw-font-body;
  font-size: 34rpx;
  font-weight: 900;
  letter-spacing: 1rpx;
  color: $rw-white;
  margin-bottom: 32rpx;
}

/* 只读记录：2×2 数据格（红左边框，对齐 Web 语言） */
.ck__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin-bottom: 32rpx;
}

.ck__item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 20rpx 24rpx;
  background: $rw-black;
  border: 2rpx solid $rw-line;
  border-left: 4rpx solid $rw-red;
}

.ck__label {
  font-size: 22rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.ck__value {
  font-family: $rw-font-display;
  font-size: 40rpx;
  line-height: 1.2;
  color: $rw-white;
  font-feature-settings: "tnum";
  letter-spacing: 1rpx;
}

.ck__value--text {
  font-family: $rw-font-body;
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.ck__unit {
  font-family: $rw-font-body;
  font-size: 22rpx;
  font-weight: 400;
  color: $rw-gray;
  margin-left: 6rpx;
}

.ck__note {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.ck__note-text {
  font-size: 26rpx;
  line-height: 1.7;
  color: $rw-gray;
}

/* 空态：过去日期无记录 */
.ck__empty {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 24rpx 0;
}

.ck__empty-text {
  font-size: 28rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.ck__empty-link {
  font-size: 28rpx;
  font-weight: 700;
  color: $rw-red;
}

/* ========== 内联打卡表单 ========== */
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.field__label {
  font-size: 24rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

/* 心情贴纸：切角，选中红底 */
.mood-chips {
  display: flex;
  gap: 20rpx;
}

.mood-chip {
  flex: 1;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $rw-line;
  clip-path: polygon(16rpx 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
  transition: $rw-transition;

  &.is-active {
    background: $rw-red;
    border-color: $rw-red;
  }
}

.mood-chip__text {
  font-size: 26rpx;
  color: $rw-gray;
}

.mood-chip.is-active .mood-chip__text {
  color: $rw-white;
  font-weight: 700;
}

.pace-preview {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.pace-preview__label {
  font-size: 26rpx;
  color: $rw-gray;
}

.pace-preview__val {
  font-family: $rw-font-display;
  font-size: 34rpx;
  letter-spacing: 2rpx;
  color: $rw-white;
  font-feature-settings: "tnum";
}

.ck__error {
  display: block;
  font-size: 24rpx;
  color: $rw-red;
  margin-bottom: 16rpx;
}

/* ========== 列表视图 ========== */
.list-head {
  padding: 8rpx 0 24rpx;
}

.list-summary {
  font-size: 26rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.ck-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.ck-list__item {
  position: relative;
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: $rw-panel;
  border: 2rpx solid $rw-line;
  clip-path: polygon(24rpx 0, 100% 0, 100% calc(100% - 24rpx), calc(100% - 24rpx) 100%, 0 100%, 0 24rpx);
  transition: $rw-transition;
}

.ck-list__item--active {
  background: rgba(230, 0, 18, 0.14);
  border-color: $rw-red;
}

.ck-list__thumb {
  width: 112rpx;
  height: 112rpx;
  flex-shrink: 0;
  background: $rw-black;
  border: 2rpx solid $rw-line;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ck-list__img {
  width: 100%;
  height: 100%;
}

.ck-list__ph {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.ck-list__ph-num {
  font-family: $rw-font-display;
  font-size: 40rpx;
  line-height: 1;
  color: $rw-white;
  font-feature-settings: "tnum";
}

.ck-list__ph-dot {
  position: absolute;
  right: 12rpx;
  bottom: 12rpx;
  width: 12rpx;
  height: 12rpx;
  background: $rw-red;
  transform: rotate(45deg);
}

.ck-list__body {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ck-list__date {
  font-family: $rw-font-display;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
  margin-bottom: 8rpx;
  font-feature-settings: "tnum";
}

.ck-list__data {
  font-size: 28rpx;
  color: $rw-white;
  margin-bottom: 12rpx;
}

.ck-list__tags {
  display: flex;
  align-items: center;
}

.ck-list__remark {
  margin-left: 16rpx;
  font-size: 24rpx;
  color: $rw-gray;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ck-list__arrow {
  margin-left: 12rpx;
  flex-shrink: 0;
  width: 16rpx;
  height: 16rpx;
  border-right: 2rpx solid $rw-gray;
  border-bottom: 2rpx solid $rw-gray;
  transform: rotate(-45deg);
}

/* ========== 统计视图 ========== */
.total-zone {
  margin-top: 16rpx;
}

.total {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.total__num {
  font-family: $rw-font-display;
  font-size: 96rpx;
  line-height: 1.05;
  color: $rw-white;
  font-feature-settings: "tnum";
  letter-spacing: 2rpx;
}

.total__unit {
  margin-top: 4rpx;
  font-family: $rw-font-display;
  font-size: 26rpx;
  letter-spacing: 4rpx;
  color: $rw-gray;
}

.total__label {
  margin-top: 16rpx;
  font-size: 24rpx;
  letter-spacing: 2rpx;
  color: $rw-gray;
}

.chart-zone {
  margin-top: 40rpx;
}

.chart {
  padding: 36rpx 32rpx 28rpx;
}

.chart__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.chart__title {
  font-size: 28rpx;
  letter-spacing: 1rpx;
  color: $rw-white;
}

.chart__sub {
  font-family: $rw-font-display;
  font-size: 24rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
  font-feature-settings: "tnum";
}

.chart__bars {
  display: flex;
  align-items: flex-end;
}

.chart__col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart__track {
  width: 100%;
  height: 240rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart__bar {
  width: 36rpx;
  background: rgba(230, 0, 18, 0.25);
  clip-path: polygon(8rpx 0, 100% 0, 100% 100%, 0 100%);
  transition: height 0.3s;

  &.is-current {
    background: $rw-red;
  }
}

.chart__label {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: $rw-gray;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32rpx;
  margin-top: 40rpx;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.metric__value {
  font-family: $rw-font-display;
  font-size: 52rpx;
  line-height: 1.2;
  color: $rw-white;
  font-feature-settings: "tnum";
  letter-spacing: 1rpx;
  margin-bottom: 8rpx;
}

.metric__label {
  font-size: 24rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

/* ========== FAB：红切角方块（避开 tabbar） ========== */
.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(200rpx + env(safe-area-inset-bottom));
  z-index: 30;
  width: 104rpx;
  height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $rw-red;
  clip-path: polygon(22rpx 0, 100% 0, calc(100% - 22rpx) 100%, 0 100%);
  transition: $rw-transition;
}

.fab--active {
  background: $rw-red-dark;
  transform: scale(0.94);
}

.fab__icon {
  font-size: 52rpx;
  font-weight: 300;
  line-height: 1;
  color: $rw-white;
}
</style>
