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

            <!-- 状态图例：已打卡 / 今天 / 当前选中各自独立 -->
            <view class="cal__legend">
              <text class="cal__legend-mark">◆</text>
              <text class="cal__legend-text">已打卡 · </text>
              <text class="cal__legend-mark">■</text>
              <text class="cal__legend-text">今天 · </text>
              <text class="cal__legend-select">□</text>
              <text class="cal__legend-text">选中</text>
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
            <template v-if="selectedDayCheckin && formMode !== 'edit'">
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
                  <text class="ck__label">体感</text>
                  <text class="ck__value ck__value--text">{{ selectedDayCheckin.mood }}</text>
                </view>
              </view>

              <view class="ck__note">
                <text class="ck__label">备注</text>
                <text class="ck__note-text">{{ selectedDayCheckin.remark || "无" }}</text>
              </view>

              <view class="ck__detail-action">
                <rw-button text="编辑记录" variant="ghost" size="sm" @tap="startEdit" />
              </view>
            </template>

            <!-- ② 表单：今日新建 / 历史补录 / 已有记录编辑 -->
            <template v-else-if="showForm">
              <text class="ck__kicker">{{ formKicker }}</text>
              <text class="ck__date">{{ formTitle }}</text>

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
                <text class="field__label">体感</text>
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
                <rw-input v-model="form.note" placeholder="一句话记录这次跑步" :maxlength="100" />
              </view>

              <view class="pace-preview">
                <text class="pace-preview__label">预计配速</text>
                <text class="pace-preview__val">{{ pacePreview || "--" }}</text>
              </view>

              <text v-if="errorMsg" class="ck__error">{{ errorMsg }}</text>

              <rw-button
                :text="submitText"
                variant="primary"
                style="min-width: 340rpx"
                @tap="handleSubmit"
              />
              <text
                v-if="formMode"
                class="ck__cancel"
                hover-class="ck__cancel--pressed"
                @click="cancelForm"
              >取消</text>
            </template>

            <!-- ③ 空态：过去日期无记录，可补录训练 -->
            <template v-else>
              <text class="ck__kicker">EMPTY · 暂无记录</text>
              <text class="ck__date">{{ selectedDateLabel }}</text>
              <view class="ck__empty">
                <text class="ck__empty-text">这一天还没有训练记录。</text>
                <text class="ck__empty-rule">每个日期仅保存一条训练，可补录后继续编辑。</text>
                <rw-button text="补录训练" variant="primary" size="sm" @tap="startBackfill" />
              </view>
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
            <text class="total__eyebrow">TOTAL DISTANCE · 历史累计里程</text>
            <view class="total__number-row">
              <text class="total__num">{{ stats.totalDistance }}</text>
              <text class="total__unit">km</text>
            </view>
            <view class="total__foot">
              <text class="total__label">本月训练</text>
              <text class="total__month">{{ stats.monthDistance }} km · {{ stats.monthCount }} 次</text>
            </view>
          </view>
        </rw-card>
      </view>

      <view class="chart-zone">
        <rw-card tag="月度里程" tag-variant="red" frame="red" padding="0">
          <view class="chart">
            <view class="chart__head">
              <view>
                <text class="chart__title">最近跑得怎么样</text>
                <text class="chart__caption">近 8 个月 · 纵轴从 0 开始</text>
              </view>
              <text class="chart__sub">点击柱形读取</text>
            </view>
            <view class="chart__bars">
              <view
                v-for="(val, i) in stats.monthData"
                :key="i"
                class="chart__col"
                :class="{ 'is-selected': selectedMonthIndex === i }"
                hover-class="chart__col--pressed"
                @click="selectedMonthIndex = i"
              >
                <view class="chart__track">
                  <text class="chart__value">{{ val }}</text>
                  <view
                    class="chart__bar"
                    :class="{
                      'is-current': i === stats.monthData.length - 1,
                      'is-zero': val === 0,
                    }"
                    :style="{ height: barHeight(val) + 'rpx' }"
                  ></view>
                </view>
                <text class="chart__label">{{ monthLabels[i] }}</text>
              </view>
            </view>
            <view class="chart__readout">
              <view>
                <text class="chart__readout-month">{{ selectedMonthSummary.label }}</text>
                <text class="chart__readout-note">月度训练明细</text>
              </view>
              <text class="chart__readout-value">{{ selectedMonthSummary.distance }} km</text>
              <text class="chart__readout-count">{{ selectedMonthSummary.count }} 次</text>
            </view>
          </view>
        </rw-card>
      </view>

      <view class="metrics-head">
        <text class="metrics-head__title">本月成果与训练习惯</text>
        <text class="metrics-head__sub">跑了多少 → 是否稳定</text>
      </view>
      <view class="metrics">
        <view
          v-for="m in metricList"
          :key="m.label"
          class="metric-wrap"
          :class="{ 'is-primary': m.primary }"
        >
          <rw-card :frame="m.primary ? 'red' : ''" :accent="m.primary" padding="28rpx 20rpx">
            <view class="metric">
              <text class="metric__value">{{ m.value }}</text>
              <text class="metric__label">{{ m.label }}</text>
            </view>
          </rw-card>
        </view>
      </view>

      <view class="habit-strip">
        <text class="habit-strip__kicker">TRAINING HABIT</text>
        <text class="habit-strip__text">
          当前连续 {{ stats.streak }} 天 · 历史最长 {{ stats.longestStreak }} 天 · 累计 {{ stats.totalCount }} 次
        </text>
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
import {
  loadCheckinRecords,
  saveCheckinRecords,
  summarizeCheckins,
  type CheckinRecord as CheckinItem,
} from "@/utils/checkin-data";

interface DayCell {
  date: number;
  dateStr: string;
}

type FormMode = "create" | "edit" | null;

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

/* ========== 本地共享数据（P2 接口联调时替换此数据层） ========== */
const checkinList = ref<CheckinItem[]>(loadCheckinRecords());

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
const formMode = ref<FormMode>(null);

const selectedDateLabel = computed(() => {
  const parts = selectedDate.value.split("-");
  return `${Number(parts[1])}月${Number(parts[2])}日`;
});

const showForm = computed(
  () => formMode.value !== null || (!selectedDayCheckin.value && isTodaySelected.value),
);

const formKicker = computed(() => {
  if (formMode.value === "edit") return "EDIT · 编辑训练";
  if (!isTodaySelected.value) return "BACKFILL · 补录训练";
  return "TODAY · 待打卡";
});

const formTitle = computed(() => {
  if (formMode.value === "edit") return `${selectedDateLabel.value} · 编辑记录`;
  if (!isTodaySelected.value) return `${selectedDateLabel.value} · 补录训练`;
  return "今日打卡";
});

const submitText = computed(() => (formMode.value === "edit" ? "保存修改" : "保存训练"));

// 胶带文案与卡片内容严格一致（有记录 / 今日打卡 / 暂无记录）
const detailTag = computed(() => {
  if (formMode.value === "edit") return "编辑记录";
  if (formMode.value === "create" && !isTodaySelected.value) return "补录训练";
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
  formMode.value = null;
  resetForm();
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
  if (idx === 0) {
    formMode.value = null;
    resetForm();
  }
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

function resetForm() {
  form.value = { distance: "", duration: "", mood: "moderate", note: "" };
  errorMsg.value = "";
}

function moodValue(label: string): string {
  return MOODS.find((m) => m.label === label)?.value || "moderate";
}

function startBackfill() {
  if (isDisabled(selectedDate.value) || selectedDayCheckin.value) return;
  resetForm();
  formMode.value = "create";
}

function startEdit() {
  const item = selectedDayCheckin.value;
  if (!item) return;
  form.value = {
    distance: String(item.distance),
    duration: String(item.duration),
    mood: moodValue(item.mood),
    note: item.remark || "",
  };
  errorMsg.value = "";
  formMode.value = "edit";
}

function cancelForm() {
  formMode.value = null;
  resetForm();
}

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
  if (!Number.isFinite(d) || d <= 0) {
    errorMsg.value = "请填写有效的跑步距离";
    return;
  }
  if (!Number.isFinite(t) || t <= 0 || !Number.isInteger(t)) {
    errorMsg.value = "跑步时长必须是大于 0 的整数分钟";
    return;
  }
  errorMsg.value = "";

  const date = selectedDate.value;
  const existing = selectedDayCheckin.value;
  const payload = {
    checkinDate: date,
    distance: Number(d.toFixed(2)),
    duration: t,
    pace: Number((t / d).toFixed(2)),
    mood: moodLabel(form.value.mood),
    remark: form.value.note.trim(),
    imageUrl: existing?.imageUrl || "",
  };

  if (formMode.value === "edit" && existing) {
    const index = checkinList.value.findIndex((item) => item.id === existing.id);
    if (index >= 0) checkinList.value[index] = { ...existing, ...payload };
    saveCheckinRecords(checkinList.value);
    toastRef.value?.show("记录已更新", "success");
  } else {
    if (existing) {
      errorMsg.value = "该日期已有训练记录，请使用编辑功能修改";
      return;
    }
    checkinList.value.unshift({ id: Date.now(), ...payload });
    saveCheckinRecords(checkinList.value);

    // 盖章动画（700ms 后清除标记）
    stampedDate.value = date;
    if (stampTimer) clearTimeout(stampTimer);
    stampTimer = setTimeout(() => {
      stampedDate.value = "";
    }, 700);
    toastRef.value?.show(isTodaySelected.value ? "打卡成功" : "补录成功", "success");
  }

  formMode.value = null;
  resetForm();
}

/* ========== 格式化 ========== */
function formatPace(pace: number): string {
  const totalSeconds = Math.round(pace * 60);
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
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
  const summary = summarizeCheckins(list);

  // “本月”始终指自然月，而不是用户当前翻看的日历月份
  const prefix = `${today.getFullYear()}-${pad(today.getMonth() + 1)}`;
  const monthList = list.filter((i) => i.checkinDate.startsWith(prefix));
  const monthCount = monthList.length;
  const monthDistance = Number(monthList.reduce((s, i) => s + i.distance, 0).toFixed(1));

  const avgPace = list.length
    ? formatPace(list.reduce((s, i) => s + i.pace, 0) / list.length)
    : "--";

  // 近 8 个月里程
  const monthData: number[] = [];
  const monthCountData: number[] = [];
  const base = new Date(today.getFullYear(), today.getMonth(), 1);
  for (let i = 7; i >= 0; i--) {
    const d = new Date(base.getFullYear(), base.getMonth() - i, 1);
    const p = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
    const records = list.filter((x) => x.checkinDate.startsWith(p));
    monthData.push(Number(records.reduce((s, x) => s + x.distance, 0).toFixed(1)));
    monthCountData.push(records.length);
  }

  return {
    totalDistance: summary.totalDistance,
    totalCount: summary.totalCount,
    monthCount,
    monthDistance,
    avgPace,
    streak: summary.currentStreak,
    longestStreak: summary.longestStreak,
    monthData,
    monthCountData,
  };
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
  { label: "本月里程", value: `${stats.value.monthDistance} km`, primary: true },
  { label: "本月打卡", value: `${stats.value.monthCount} 次`, primary: true },
  { label: "平均配速", value: `${stats.value.avgPace}/km`, primary: false },
  { label: "当前连续打卡", value: `${stats.value.streak} 天`, primary: false },
]);

const selectedMonthIndex = ref(7);
const selectedMonthSummary = computed(() => ({
  label: monthLabels.value[selectedMonthIndex.value] || "--",
  distance: stats.value.monthData[selectedMonthIndex.value] ?? 0,
  count: stats.value.monthCountData[selectedMonthIndex.value] ?? 0,
}));

const maxMonthData = computed(() => Math.max(...stats.value.monthData, 1));

function barHeight(val: number): number {
  const maxH = 200;
  if (val <= 0) return 0;
  return Math.max(3, (val / maxMonthData.value) * maxH);
}

/* ========== 交互 ========== */
function scrollToForm() {
  nextTick(() => {
    uni.pageScrollTo({ selector: ".detail-zone", duration: 300, fail: () => {} });
  });
}

// FAB：回到日历 tab 的今日打卡表单
function goForm() {
  currentTab.value = 0;
  selectedDate.value = todayStr;
  formMode.value = null;
  resetForm();
  scrollToForm();
}

// 列表项 → 切日历 tab 并选中该日期
function onItemClick(item: CheckinItem) {
  currentTab.value = 0;
  selectedDate.value = item.checkinDate;
  formMode.value = null;
  resetForm();
  const [y, m] = item.checkinDate.split("-").map(Number);
  year.value = y;
  month.value = m;
  scrollToForm();
}

onShow(() => {
  setTabBarIndex(1);
  checkinList.value = loadCheckinRecords();
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

.cal__legend-select {
  font-size: 22rpx;
  color: $rw-white;
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
  color: $rw-gray;
  font-feature-settings: "tnum";
}

/* 有打卡：数字加粗提亮 */
.cal__cell.has-record .cal__daynum {
  font-weight: 700;
  color: $rw-white;
}

/* 今日：红色背景；它和选中日期是两个可叠加的状态 */
.cal__cell.is-today {
  background: $rw-red;

  .cal__daynum {
    color: $rw-white;
    font-weight: 700;
  }
}

/* 当前选中：白色内描边，不抢占「今天」的红色语义 */
.cal__cell.is-selected {
  box-shadow: inset 0 0 0 3rpx $rw-white;

  .cal__daynum {
    color: $rw-white;
    font-weight: 700;
  }
}

/* 未来日期：灰禁点 */
.cal__cell.is-future {
  opacity: 0.35;
  pointer-events: none;

  .cal__daynum {
    color: #55555c;
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

.cal__cell.is-today .cal__dot {
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

.ck__detail-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid $rw-line;
}

/* 空态：过去日期无记录 */
.ck__empty {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 18rpx;
  padding: 24rpx 0;
}

.ck__empty-text {
  font-size: 28rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.ck__empty-rule {
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(156, 156, 163, 0.75);
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

/* 体感选项：切角，选中红底 */
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

.ck__cancel {
  display: inline-block;
  margin-left: 28rpx;
  padding: 20rpx 28rpx;
  font-size: 26rpx;
  color: $rw-gray;
  vertical-align: middle;
}

.ck__cancel--pressed {
  color: $rw-white;
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
  align-items: stretch;
}

.total__eyebrow {
  font-family: $rw-font-display;
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: $rw-gray;
}

.total__number-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 14rpx;
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
  font-family: $rw-font-display;
  font-size: 32rpx;
  letter-spacing: 2rpx;
  color: $rw-gray;
}

.total__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid $rw-line;
}

.total__label {
  font-size: 26rpx;
  color: $rw-gray;
}

.total__month {
  font-family: $rw-font-display;
  font-size: 32rpx;
  letter-spacing: 1rpx;
  color: $rw-red;
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
  display: block;
  font-size: 28rpx;
  letter-spacing: 1rpx;
  color: $rw-white;
}

.chart__caption {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: $rw-gray;
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
  transition: opacity 0.15s ease, transform 0.15s ease;
  opacity: 0.62;
}

.chart__col.is-selected {
  opacity: 1;
}

.chart__col--pressed {
  transform: translateY(-4rpx);
}

.chart__track {
  position: relative;
  width: 100%;
  height: 240rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart__value {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  font-family: $rw-font-display;
  font-size: 18rpx;
  text-align: center;
  color: $rw-gray;
}

.chart__bar {
  width: 36rpx;
  background: rgba(230, 0, 18, 0.25);
  clip-path: polygon(8rpx 0, 100% 0, 100% 100%, 0 100%);
  transition: height 0.3s;

  &.is-current {
    background: $rw-red;
  }

  &.is-zero {
    height: 2rpx !important;
    background: $rw-line;
    clip-path: none;
  }
}

.chart__col.is-selected .chart__bar {
  background: $rw-red;
}

.chart__label {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: $rw-gray;
}

.chart__col.is-selected .chart__label,
.chart__col.is-selected .chart__value {
  color: $rw-white;
}

.chart__readout {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 24rpx;
  margin-top: 28rpx;
  padding: 22rpx 24rpx;
  background: $rw-black;
  border-left: 6rpx solid $rw-red;
}

.chart__readout-month,
.chart__readout-note {
  display: block;
}

.chart__readout-month {
  font-family: $rw-font-display;
  font-size: 26rpx;
  color: $rw-white;
}

.chart__readout-note {
  margin-top: 4rpx;
  font-size: 19rpx;
  color: $rw-gray;
}

.chart__readout-value,
.chart__readout-count {
  font-family: $rw-font-display;
  font-size: 30rpx;
  color: $rw-white;
}

.chart__readout-value {
  color: $rw-red;
}

.metrics-head {
  margin-top: 44rpx;
}

.metrics-head__title,
.metrics-head__sub {
  display: block;
}

.metrics-head__title {
  font-size: 30rpx;
  font-weight: 700;
  color: $rw-white;
}

.metrics-head__sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $rw-gray;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32rpx;
  margin-top: 20rpx;
}

.metric-wrap:not(.is-primary) .rw-card-body {
  border: 2rpx solid $rw-line;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.metric__value {
  font-family: $rw-font-display;
  font-size: 44rpx;
  line-height: 1.2;
  color: $rw-white;
  font-feature-settings: "tnum";
  letter-spacing: 1rpx;
  margin-bottom: 8rpx;
}

.metric-wrap.is-primary .metric__value {
  font-size: 52rpx;
}

.metric__label {
  font-size: 24rpx;
  letter-spacing: 1rpx;
  color: $rw-gray;
}

.habit-strip {
  margin-top: 28rpx;
  padding: 24rpx 28rpx;
  background: rgba(255, 255, 255, 0.035);
  border: 2rpx solid $rw-line;
  border-left: 6rpx solid $rw-red;
}

.habit-strip__kicker,
.habit-strip__text {
  display: block;
}

.habit-strip__kicker {
  font-family: $rw-font-display;
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: $rw-red;
}

.habit-strip__text {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.6;
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
