<template>
  <view class="page">
    <!-- ===== ① 品牌区：轻量栏目眉题 + 原拼贴 Logo（品牌视觉核心） ===== -->
    <view class="brand rw-enter rw-enter-1">
      <view class="brand__meta">
        <text class="brand__name">RunWise</text>
        <text class="brand__section">TRAINING CENTER</text>
      </view>
      <view class="brand__float">
        <text
          v-for="(l, i) in brandLetters"
          :key="i"
          class="brand__lt"
          :class="l.gap ? 'brand__lt--gap' : 'brand__lt--' + l.kind"
          :style="l.gap ? {} : { transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
          >{{ l.ch }}</text
        >
      </view>
    </view>

    <!-- ===== ② Hero 区：红色斜切色块背景 + 主标题拼贴 + 灰副题 ===== -->
    <view class="hero rw-enter rw-enter-2">
      <view class="hero__band" aria-hidden="true"></view>
      <!-- Z-1：-3° 作用于父容器，主标题+副题整体联动无断层 -->
      <view class="hero__kicker">
        <view class="hero__title">
          <text
            v-for="(c, i) in titleChars"
            :key="i"
            class="hero__char"
            :class="{ 'hero__char--block': c.block }"
            :style="{ transform: `rotate(${c.rot}deg)`, fontSize: c.size + 'em' }"
            >{{ c.ch }}</text
          >
        </view>
        <text class="hero__desc">记录每一步，挑战每一次突破。</text>
      </view>
    </view>

    <!-- ===== ③ 主视觉舞台：打卡卡 + 跑步女孩（前景主视觉） + 散落拼贴大字入口 =====
         打卡卡与角色展示区上下分区，避免卡片遮挡原创角色 -->
    <view class="stage rw-enter rw-enter-3">
      <!-- 今日打卡卡 -->
      <view class="ck-section">
        <rw-card frame="red" tag="CHECK-IN" :tag-rotate="'-6deg'" padding="28rpx 28rpx 24rpx">
          <view class="ck__head">
            <text class="ck__date">{{ todayDate }}</text>
            <text class="ck__status">{{ todayCheckin.checkedIn ? "已完成" : "待出发" }}</text>
          </view>
          <text class="ck__title">{{ todayCheckin.checkedIn ? "今日训练完成！" : "今天，准备跑多远？" }}</text>

          <view v-if="todayCheckin.checkedIn" class="ck__metrics">
            <view class="ck__metric">
              <text class="ck__metric-value">{{ todayCheckin.distance }}</text>
              <text class="ck__metric-unit">公里</text>
            </view>
            <view class="ck__metric">
              <text class="ck__metric-value">{{ todayCheckin.duration }}</text>
              <text class="ck__metric-unit">分钟</text>
            </view>
            <view class="ck__metric">
              <text class="ck__metric-value">{{ todayCheckin.pace }}</text>
              <text class="ck__metric-unit">平均配速/km</text>
            </view>
          </view>
          <text v-else class="ck__data ck__data--empty">穿好跑鞋，完成今天的第一次出发。</text>

          <view class="ck__progress">
            <view class="ck__progress-meta">
              <text class="ck__progress-label">本周目标</text>
              <text class="ck__progress-num">{{ weekStats.distance }} / {{ WEEK_TARGET_KM }} km</text>
            </view>
            <view class="ck__progress-track">
              <view class="ck__progress-fill" :style="{ width: weekPercent + '%' }"></view>
            </view>
          </view>

          <!-- 查看详情/去打卡按钮（缩小版）：深红实心影 + 左侧白菱形引出线 + 右上炸星 + 右下红方块 -->
          <view class="ck__btn-wrap">
            <view class="ck__btn-shadow" aria-hidden="true"></view>
            <view class="ck__btn-lead" aria-hidden="true">
              <view class="ck__btn-lead-line"></view>
              <view class="ck__btn-lead-diamond"></view>
            </view>
            <view class="ck__btn" hover-class="ck__btn--hover" :hover-stay-time="80" @click="goCheckin">
              <text class="ck__btn-text">{{ todayCheckin.checkedIn ? "查看训练详情 →" : "开始今日打卡 →" }}</text>
            </view>
            <view class="ck__btn-dot" aria-hidden="true"></view>
            <rw-star-field tier="S" color="red" :seed="1" :duration="4.6" right="-18rpx" top="-22rpx" />
          </view>

          <template #extra>
            <rw-star-field tier="M" color="red" :seed="2" :duration="5.4" right="-8rpx" bottom="-14rpx" />
          </template>
        </rw-card>
      </view>

      <!-- 角色专属展示区：人物完整露出，入口位于两侧且不遮挡主体 -->
      <view class="stage__visual">
        <view class="stage__heartbeat" aria-hidden="true">
          <view class="stage__heartbeat-line"></view>
        </view>

        <!-- 跑步女孩主视觉（PNG：webp 在部分安卓真机 alpha 解码失败，20260919 换回） -->
        <image class="stage__runner" src="/static/images/hero-runner.png" mode="aspectFit" />

        <!-- 快捷入口：无框拼贴大字（编号+拼贴字+中文小注） -->
        <view class="menu menu--record" hover-class="menu--pressed" :hover-stay-time="150" @click="goCreate">
        <text class="menu__no">01</text>
        <view class="menu__word">
          <text
            v-for="(l, i) in recordLetters"
            :key="i"
            class="menu__lt"
            :class="l.gap ? 'menu__lt--gap' : 'menu__lt--' + l.kind"
            :style="l.gap ? {} : { transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
            >{{ l.ch }}</text
          >
        </view>
        <text class="menu__note">记一笔</text>
        <view class="menu__underline"></view>
        </view>

        <view class="menu menu--ask" hover-class="menu--pressed" :hover-stay-time="150" @click="goQa">
        <text class="menu__no">02</text>
        <view class="menu__word">
          <text
            v-for="(l, i) in askLetters"
            :key="i"
            class="menu__lt"
            :class="l.gap ? 'menu__lt--gap' : 'menu__lt--' + l.kind"
            :style="l.gap ? {} : { transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
            >{{ l.ch }}</text
          >
        </view>
        <text class="menu__note">问问题</text>
        <view class="menu__underline"></view>
        </view>

        <view class="menu menu--calendar" hover-class="menu--pressed" :hover-stay-time="150" @click="goCalendar">
        <text class="menu__no">03</text>
        <view class="menu__word">
          <text
            v-for="(l, i) in calendarLetters"
            :key="i"
            class="menu__lt"
            :class="l.gap ? 'menu__lt--gap' : 'menu__lt--' + l.kind"
            :style="l.gap ? {} : { transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
            >{{ l.ch }}</text
          >
        </view>
        <text class="menu__note">看日历</text>
        <view class="menu__underline"></view>
        </view>

        <!-- RUN! 装饰字：女孩脚下左侧（Web 同款位置） -->
        <text class="stage__run" aria-hidden="true">RUN!</text>
      </view>
    </view>

    <!-- ===== ⑤ 新手常问卡：首页只保留高频三条，其余进入答疑页 ===== -->
    <view class="faq rw-enter rw-enter-5">
      <rw-card tag="新手常问" :tag-rotate="'-4deg'" padding="28rpx 32rpx 20rpx">
        <view
          v-for="(q, idx) in hotQuestions.slice(0, 3)"
          :key="idx"
          class="faq__row"
          :class="idx % 2 === 0 ? 'faq__row--odd' : 'faq__row--even'"
          hover-class="faq__row--pressed"
          :hover-stay-time="150"
          @click="goQuestion(q)"
        >
          <text class="faq__idx">{{ String(idx + 1).padStart(2, "0") }}</text>
          <text class="faq__q">{{ q }}</text>
          <view class="faq__underline"></view>
        </view>
        <view class="faq__more" hover-class="faq__more--pressed" :hover-stay-time="100" @click="goQa">
          <text>查看全部问题</text>
          <text class="faq__more-arrow">→</text>
        </view>
      </rw-card>
    </view>

    <!-- ===== ⑥ 未开放能力合并为一个轻量预告区 ===== -->
    <view class="soon" hover-class="soon--pressed" :hover-stay-time="100" @click="onComingSoon">
      <view class="soon__label">
        <text class="soon__eyebrow">NEXT UP</text>
        <text class="soon__title">更多训练能力正在准备</text>
      </view>
      <text class="soon__features">{{ comingSoonText }}</text>
      <text class="soon__arrow">→</text>
    </view>

    <!-- ===== ⑦ 底部跑马灯（仅首页挂） ===== -->
    <rw-marquee class="page__marquee" />

    <rw-toast ref="toastRef" />
  </view>
</template>

<script setup lang="ts">
/**
 * 首页 —— P5R 改造 v2（20260918 舞台重构：女孩 C 位 + 无框拼贴大字入口，对齐 Web HeroSection menu-item）
 * 数据逻辑与跳转语义保持原样，P2 接后端后替换 mock 数据源。
 */
import { ref, computed } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import { setTabBarIndex } from "@/utils/tabbar";
import { loadCheckinRecords, summarizeCheckins, toDateKey } from "@/utils/checkin-data";

onShow(() => {
  setTabBarIndex(0);
  refreshHomeData();
});

/* ===== 拼贴字母生成（确定性，避免重渲染抖动；规则对齐 Web HeroSection/RansomNote） ===== */
const ROTS = ["-5deg", "4deg", "-3deg", "5deg", "-4deg", "3deg"];
const SIZES = [1.1, 0.95, 1.05, 0.9, 1.0, 0.92];

function ransomLetters(word: string, seed = 0) {
  return word.split("").map((ch, i) => {
    if (ch === " ") return { ch, gap: true, kind: "", rot: "0deg", size: 1 };
    const k = (i + seed) % 3;
    return {
      ch,
      gap: false,
      // 三分法（对齐 Web .mlt）：invert 反白贴片 / wr 白字红影 / rb 红字黑影
      kind: k === 0 ? "invert" : k === 1 ? "wr" : "rb",
      rot: ROTS[(i + seed) % 6],
      size: SIZES[(i + seed) % 6],
    };
  });
}

/* 品牌条 RUN WISE（Anton 混合大小写拼贴，seed=1 对齐 Web 签名位） */
const brandLetters = ransomLetters("RuN wIsE", 1);

/* 快捷入口拼贴大字（Web 菜单 SmArT Q&A 同款混合大小写语言） */
const recordLetters = ransomLetters("ReCoRd", 0);
const askLetters = ransomLetters("AsK", 2);
const calendarLetters = ransomLetters("CaLeNdAr", 1);

/* 主标题逐字拼贴（RansomNote 规则：奇字 -2.5°、偶字 +2°、每 2~3 字 0.94~1.06em、垫红块 2 处） */
const TITLE_TEXT = "你的智能跑步训练中心";
const TITLE_SIZES = [1, 0.96, 1.05, 1, 0.94, 1.03, 1, 1.06, 0.95, 1];
const titleChars = TITLE_TEXT.split("").map((ch, i) => ({
  ch,
  rot: i % 2 === 0 ? -2.5 : 2,
  size: TITLE_SIZES[i % TITLE_SIZES.length],
  block: i === 2 || i === 7, // 红垫块固定 2 处（禁逐字换色，禁倾斜超 ±4°）
}));

/* ===== 数据（mock，P2 接后端） ===== */
const WEEKDAYS = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
const _now = new Date();
const todayDate = ref(
  `${String(_now.getMonth() + 1).padStart(2, "0")}/${String(_now.getDate()).padStart(2, "0")} ${
    WEEKDAYS[_now.getDay()]
  }`
);

const todayCheckin = ref({
  checkedIn: true,
  distance: 5.2,
  duration: 32,
  pace: "6'09\"",
  streak: 7,
});

const weekStats = ref({
  distance: 12.5,
  count: 3,
  duration: 98,
});

function refreshHomeData() {
  const records = loadCheckinRecords();
  const summary = summarizeCheckins(records);
  const todayKey = toDateKey(new Date());
  const todayRecord = records.find((item) => item.checkinDate === todayKey);

  todayCheckin.value = todayRecord
    ? {
        checkedIn: true,
        distance: todayRecord.distance,
        duration: todayRecord.duration,
        pace: formatHomePace(todayRecord.pace),
        streak: summary.currentStreak,
      }
    : {
        checkedIn: false,
        distance: 0,
        duration: 0,
        pace: "--",
        streak: summary.currentStreak,
      };

  const now = new Date();
  const monday = new Date(now);
  const weekday = now.getDay() || 7;
  monday.setHours(0, 0, 0, 0);
  monday.setDate(now.getDate() - weekday + 1);
  const mondayKey = toDateKey(monday);
  const weekRecords = records.filter(
    (item) => item.checkinDate >= mondayKey && item.checkinDate <= todayKey,
  );
  weekStats.value = {
    distance: Number(weekRecords.reduce((sum, item) => sum + item.distance, 0).toFixed(1)),
    count: weekRecords.length,
    duration: weekRecords.reduce((sum, item) => sum + item.duration, 0),
  };
}

function formatHomePace(pace: number): string {
  const seconds = Math.round(pace * 60);
  return `${Math.floor(seconds / 60)}'${String(seconds % 60).padStart(2, "0")}"`;
}

const WEEK_TARGET_KM = 40;
const weekPercent = computed(() =>
  Math.min(100, Math.round((weekStats.value.distance / WEEK_TARGET_KM) * 100))
);

const hotQuestions = ref([
  "第一次跑步该跑多远？",
  "跑完膝盖疼怎么办？",
  "新手怎么选跑鞋？",
  "跑步时心率多少合适？",
  "晨跑好还是夜跑好？",
  "跑步前要热身多久？",
  "如何提高跑步配速？",
]);

const comingSoon = [
  { name: "训练计划" },
  { name: "伤病预防" },
  { name: "数据统计" },
];
const comingSoonText = comingSoon.map((item) => item.name).join(" · ");

/* ===== 跳转语义（原样保留） ===== */
const goCheckin = () => {
  uni.switchTab({ url: "/pages/checkin/checkin" });
};

const goCreate = () => {
  // 暂时用 switchTab（create 页尚未独立 tab）
  uni.switchTab({ url: "/pages/checkin/checkin" });
};

const goQa = () => {
  const app = getApp();
  app.globalData = app.globalData || {};
  app.globalData.qaAutoFocus = true;
  uni.switchTab({ url: "/pages/qa/qa" });
};

const goCalendar = () => {
  // 通过 globalData 传递 tab 参数，让打卡页加载时自动切换到日历视图
  const app = getApp();
  app.globalData = app.globalData || {};
  app.globalData.checkinTab = 0;
  uni.switchTab({ url: "/pages/checkin/checkin" });
};

const goQuestion = (question: string) => {
  const app = getApp();
  app.globalData = app.globalData || {};
  app.globalData.qaAutoSend = question;
  uni.switchTab({ url: "/pages/qa/qa" });
};

/* 敬请期待：本期不做功能，点击提示（v5.2 拍板③） */
const toastRef = ref<any>(null);
const onComingSoon = () => {
  toastRef.value?.show("该模块即将上线", "success");
};

const onRefresh = () => {
  // mock 刷新：模拟拉取最新打卡数据
  setTimeout(() => {
    uni.stopPullDownRefresh();
  }, 800);
};

onPullDownRefresh(() => {
  onRefresh();
});
</script>

<style lang="scss" scoped>
.page {
  position: relative;
  min-height: 100vh;
  padding: 0 $rw-sp-4 calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  overflow-x: hidden; /* 横向裁切防出血溢出，纵向自然滚动 */
  background-color: $rw-black;
  /* 背景网点纹理（Web signature 元素，纯 CSS 零体积） */
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.04) 2rpx, transparent 2.2rpx);
  background-size: 32rpx 32rpx;
}

/* ==================== ① 品牌条 ==================== */
.brand {
  padding: 20rpx 0 10rpx;
}

.brand__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14rpx;
  margin-bottom: 18rpx;
  border-bottom: 2rpx solid $rw-line;
}

.brand__name {
  font-family: $rw-font-display;
  font-size: 28rpx;
  letter-spacing: 1rpx;
  color: $rw-white;
}

.brand__section {
  font-family: $rw-font-display;
  font-size: 22rpx;
  letter-spacing: 3rpx;
  color: $rw-gray;
}

/* 上下浮动动画挂在包装层，逐字 rotate 在子元素，互不覆盖 */
.brand__float {
  display: inline-flex;
  align-items: baseline;
  /* 字号必须在容器上：子字母的内联 fontSize 用 em 相对父级解析，
     放在 .brand__lt 上会被内联样式覆盖且 em 落到默认 16px（Web .ransom 同构）。
     76rpx 实测品牌区占屏宽 36.9%（20260921 按蓝框标定尺寸收定） */
  font-size: 70rpx;
  animation: brand-float 3.2s ease-in-out infinite;
}

@keyframes brand-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10rpx);
  }
}

/* 拼贴字母（Web .mlt 三分法 1:1，px×2=rpx；字号由 .brand__float 容器提供，em 逐字缩放） */
.brand__lt {
  font-family: $rw-font-display;
  line-height: 1.1;
  display: inline-block;
}

.brand__lt--invert {
  padding: 0 4rpx;
  color: $rw-white;
  background: $rw-black;
  border: 3rpx solid $rw-white;
}

.brand__lt--wr {
  color: $rw-white;
  text-shadow: 6rpx 6rpx 0 $rw-red;
}

.brand__lt--rb {
  color: $rw-red;
  text-shadow: 6rpx 6rpx 0 rgba(0, 0, 0, 0.85);
}

.brand__lt--gap {
  width: 18rpx;
}

/* ==================== ② Hero ==================== */
.hero {
  position: relative;
  padding: 20rpx 0 32rpx;
}

.hero__band {
  position: absolute;
  left: -12%;
  top: 8rpx;
  width: 124%;
  height: 150rpx;
  z-index: 0;
  pointer-events: none;
  background-color: $rw-red;
  background-image: radial-gradient(circle, rgba(176, 13, 36, 0.5) 2.5px, transparent 2.6px);
  background-size: 20rpx 20rpx;
  transform: rotate(-12deg);
}

.hero__kicker {
  position: relative;
  z-index: 1;
  padding-left: 20rpx;
  border-left: 8rpx solid $rw-white;
  transform: rotate(-3deg);
}

.hero__title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  /* 字号放容器：hero__char 内联 fontSize em 相对父级解析（同 .brand__float 道理） */
  font-size: 40rpx;
}

.hero__char {
  font-family: $rw-font-cn-title;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 2rpx;
  color: $rw-white;
  -webkit-text-stroke: 5px #000;
  paint-order: stroke fill;
}

.hero__char--block {
  background: $rw-red;
  padding: 0 4rpx;
  margin: 0 2rpx;
}

.hero__desc {
  display: block;
  margin-top: 10rpx;
  font-family: $rw-font-cn-title;
  font-weight: 400;
  font-size: 26rpx;
  line-height: 1.4;
  letter-spacing: 3rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* ==================== ③ 主视觉舞台 ==================== */
.stage {
  position: relative;
  margin-top: 16rpx;
}

.stage__visual {
  position: relative;
  height: 790rpx;
  margin-top: 28rpx;
  overflow: hidden;
}

/* 红色心电图波形线：200% 宽双周期波形，向左无缝滚动（Web signature，女孩身后） */
.stage__heartbeat {
  position: absolute;
  left: 0;
  top: 650rpx;
  width: 100%;
  height: 48rpx;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
  opacity: 0.85;
}

.stage__heartbeat-line {
  width: 200%;
  height: 100%;
  background: $rw-red;
  animation: heartbeat-run 5s linear infinite;
  clip-path: polygon(
    /* 第一周期（0~50%） */
    0 55%, 6% 55%, 8% 20%, 10% 85%, 12% 40%, 14% 55%, 22% 55%, 24% 10%, 26% 90%, 28% 35%, 30% 55%,
    37% 55%, 39% 25%, 41% 80%, 43% 45%, 45% 55%, 50% 55%,
    /* 第二周期（50~100%，同形重复） */
    56% 55%, 58% 20%, 60% 85%, 62% 40%, 64% 55%, 72% 55%, 74% 10%, 76% 90%, 78% 35%, 80% 55%,
    87% 55%, 89% 25%, 91% 80%, 93% 45%, 95% 55%, 100% 55%,
    /* 下缘回程 */
    100% 62%, 95% 62%, 93% 52%, 91% 87%, 89% 32%, 87% 62%, 80% 62%, 78% 42%, 76% 97%, 74% 17%,
    72% 62%, 64% 62%, 62% 47%, 60% 92%, 58% 27%, 56% 62%, 50% 62%, 45% 62%, 43% 52%, 41% 87%,
    39% 32%, 37% 62%, 30% 62%, 28% 42%, 26% 97%, 24% 17%, 22% 62%, 14% 62%, 12% 47%, 10% 92%,
    8% 27%, 6% 62%, 0 62%
  );
}

@keyframes heartbeat-run {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

/* 跑步女孩：前景主视觉（不透明），20260919 右移至页面中轴线附近，最小化与左侧标题重叠 */
.stage__runner {
  position: absolute;
  right: 74rpx;
  top: 0;
  width: 500rpx;
  height: 770rpx;
  z-index: 3;
  pointer-events: none;
}

/* RUN! 装饰字：女孩脚下左侧（Web 同款），白字红影 */
.stage__run {
  position: absolute;
  left: 88rpx;
  top: 610rpx;
  font-family: $rw-font-display;
  font-size: 44rpx;
  color: $rw-white;
  text-shadow: 5rpx 5rpx 0 $rw-red;
  transform: rotate(-4deg);
  z-index: 4;
  pointer-events: none;
}

/* ==================== 打卡卡（舞台内 z3，20260919 整体缩一档） ==================== */
.ck-section {
  position: relative;
  z-index: 4;
}

.ck__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ck__date {
  font-size: 22rpx;
  color: $rw-gray;
}

.ck__status {
  padding: 6rpx 14rpx;
  font-family: $rw-font-cn-data;
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: $rw-white;
  border: 2rpx solid rgba(255, 255, 255, 0.45);
}

.ck__title {
  display: block;
  margin-top: 14rpx;
  font-family: $rw-font-cn-title;
  font-weight: 400;
  font-size: 36rpx;
  letter-spacing: 2rpx;
  color: $rw-white;
}

.ck__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 20rpx;
  padding: 18rpx 0 20rpx;
  border-bottom: 2rpx solid $rw-line;
}

.ck__metric {
  min-width: 0;
}

.ck__metric-value,
.ck__metric-unit {
  display: block;
}

.ck__metric-value {
  font-family: $rw-font-display;
  font-size: 44rpx;
  line-height: 1;
  color: $rw-white;
  font-feature-settings: "tnum";
}

.ck__metric-unit {
  margin-top: 8rpx;
  font-family: $rw-font-body;
  font-size: 20rpx;
  line-height: 1.25;
  color: $rw-gray;
}

.ck__data {
  display: block;
  margin-top: 8rpx;
  font-family: $rw-font-display, $rw-font-cn-data;
  font-size: 27rpx;
  font-weight: 400;
  letter-spacing: 1rpx;
  color: $rw-cream;
  font-feature-settings: "tnum";

  &--empty {
    margin: 22rpx 0 4rpx;
    color: $rw-gray;
  }
}

.ck__progress {
  margin-top: 22rpx;
}

.ck__progress-meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.ck__progress-label {
  font-family: $rw-font-cn-title;
  font-size: 22rpx;
  font-weight: 400;
  letter-spacing: 2rpx;
  color: $rw-gray;
}

.ck__progress-num {
  font-family: $rw-font-display;
  font-size: 26rpx;
  color: $rw-white;
  font-feature-settings: "tnum";
}

.ck__progress-track {
  margin-top: 8rpx;
  height: 12rpx;
  background: rgba(255, 255, 255, 0.1);
  clip-path: polygon(6rpx 0, 100% 0, calc(100% - 6rpx) 100%, 0 100%);
}

.ck__progress-fill {
  height: 100%;
  background: $rw-red;
  clip-path: polygon(6rpx 0, 100% 0, calc(100% - 6rpx) 100%, 0 100%);
  transition: width 0.4s ease-out;
}

/* 查看详情/去打卡按钮（缩小版，缩幅大于卡片）：居中收窄，四周挂装饰 */
.ck__btn-wrap {
  position: relative;
  margin: 24rpx 28rpx 20rpx;
}

/* 深红实心影：与按钮同形偏移 8rpx（clip-path 裁 box-shadow，故独立层） */
.ck__btn-shadow {
  position: absolute;
  left: 8rpx;
  top: 8rpx;
  right: -8rpx;
  bottom: -8rpx;
  background: $rw-shadow-red;
  clip-path: polygon(20rpx 0, 100% 0, calc(100% - 20rpx) 100%, 0 100%);
  z-index: 0;
  pointer-events: none;
}

/* 左侧白菱形引出线 */
.ck__btn-lead {
  position: absolute;
  left: -44rpx;
  top: 50%;
  display: flex;
  align-items: center;
  transform: translateY(-50%);
  pointer-events: none;
}

.ck__btn-lead-line {
  width: 28rpx;
  height: 0;
  border-top: 3rpx dashed rgba(255, 255, 255, 0.5);
}

.ck__btn-lead-diamond {
  width: 12rpx;
  height: 12rpx;
  margin-left: 6rpx;
  background: $rw-white;
  transform: rotate(45deg);
}

/* 右下小红方块 */
.ck__btn-dot {
  position: absolute;
  right: -14rpx;
  bottom: -10rpx;
  width: 12rpx;
  height: 12rpx;
  background: $rw-red;
  transform: rotate(45deg);
  pointer-events: none;
}

.ck__btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  background: $rw-red;
  clip-path: polygon(20rpx 0, 100% 0, calc(100% - 20rpx) 100%, 0 100%);
  @include rw-tappable;
}

.ck__btn--hover {
  background: $rw-red-dark;
  transform: scale(0.97);
}

.ck__btn-text {
  font-family: $rw-font-cn-data;
  font-size: 34rpx;
  font-weight: 400;
  letter-spacing: 8rpx;
  text-indent: 8rpx;
  line-height: 1;
  color: $rw-white;
  white-space: nowrap;
}

/* ==================== 快捷入口：无框拼贴大字（Web menu-item 语言） ==================== */
.menu {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 8rpx 16rpx 16rpx; /* 扩热区，保证 ≥88rpx 高 */
  min-height: 120rpx;
  z-index: 2;
  opacity: 0.92;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

/* 红色 Anton 编号（Web .m-no） */
.menu__no {
  font-family: $rw-font-display;
  font-size: 32rpx;
  letter-spacing: 2rpx;
  color: $rw-red;
  margin-bottom: 4rpx;
  text-shadow: 0 0 10rpx rgba(230, 0, 18, 0.38);
}

/* 拼贴大字组：上下轻浮动（Web F2，周期错开） */
.menu__word {
  display: flex;
  align-items: baseline;
  font-family: $rw-font-display;
  line-height: 1.05;
  animation: menu-float 3.6s ease-in-out infinite;
}

.menu__lt {
  display: inline-block;
}

.menu__lt--invert {
  padding: 0 4rpx;
  color: #ffffff;
  background: #111114;
  border: 3rpx solid #ffffff;
  box-shadow: 0 0 8rpx rgba(255, 255, 255, 0.16);
}

.menu__lt--wr {
  color: #ffffff;
  text-shadow: 5rpx 5rpx 0 $rw-red;
}

.menu__lt--rb {
  color: #ff1730;
  text-shadow:
    5rpx 5rpx 0 rgba(0, 0, 0, 0.9),
    0 0 9rpx rgba(230, 0, 18, 0.28);
}

.menu__lt--gap {
  width: 12rpx;
}

/* 黄油体中文小注（Web .m-note） */
.menu__note {
  margin-top: 10rpx;
  font-family: $rw-font-cn-data;
  font-size: 26rpx;
  font-weight: 400;
  letter-spacing: 3rpx;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 2rpx 2rpx 0 rgba(0, 0, 0, 0.9);
}

/* 红斜杠下划线：按压瞬间从左侧滑出（Web F6 同款） */
.menu__underline {
  margin-top: 8rpx;
  width: 100%;
  height: 8rpx;
  background: $rw-red;
  transform: rotate(-3deg) scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s ease-out;
}

/* 按压态：下划线滑出 + 字母抖动一次 */
.menu--pressed .menu__underline {
  transform: rotate(-3deg) scaleX(1);
}

.menu--pressed {
  opacity: 1;
}

.menu--pressed .menu__lt {
  animation: menu-jitter 0.3s ease-in-out;
}

.menu--pressed .menu__lt:nth-child(2n) {
  animation-delay: 0.05s;
}

.menu--pressed .menu__lt:nth-child(3n) {
  animation-delay: 0.1s;
}

@keyframes menu-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8rpx);
  }
}

@keyframes menu-jitter {
  0% {
    transform: translate(0, 0);
  }
  30% {
    transform: translate(2rpx, -2rpx) rotate(3deg);
  }
  60% {
    transform: translate(-2rpx, 1rpx) rotate(-2deg);
  }
  100% {
    transform: translate(0, 0);
  }
}

/* 三格围绕女孩散落（对齐 Web m-1/m-2/m-3 角度语言，图层穿插） */

/* ReCoRd 记一笔：左中（20260919 提到 z4 浮于女孩之上，保证可见与可点） */
.menu--record {
  left: 8rpx;
  top: 260rpx;
  transform: rotate(-6deg);
  font-size: 52rpx;
}

.menu--record .menu__word {
  animation-duration: 4.5s;
  animation-delay: 1.4s;
}

/* AsK 问问题：右上空白列（20260919 移出画面，女孩完整露出） */
.menu--ask {
  right: 0;
  top: 82rpx;
  transform: rotate(4deg);
  font-size: 66rpx; /* 短词略大，但退居人物之后 */
}

.menu--ask .menu__word {
  animation-duration: 3.6s;
  animation-delay: 0.2s;
}

/* CaLeNdAr 看日历：右下空白列（20260919 移出画面） */
.menu--calendar {
  right: 0;
  top: 475rpx;
  transform: rotate(-5deg);
  font-size: 42rpx;
}

.menu--calendar .menu__word {
  animation-duration: 3.9s;
  animation-delay: 2.6s;
}

/* ==================== ⑤ 新手常问卡（交错斜排） ==================== */
.faq {
  position: relative;
  margin-top: 12rpx;
}

.faq__row {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 78rpx;
  padding: 8rpx 8rpx 10rpx;
}

/* 奇数行（01/03/05）：左对齐、微左倾；偶数行（02/04）：右移 60rpx、微右倾——交错呼吸 */
.faq__row--odd {
  transform: rotate(-1.5deg);
}

.faq__row--even {
  transform: rotate(1.5deg);
  padding-left: 44rpx;
}

/* 按压态：灰斜线从左滑出 + 整行缓抖一次（幅度小于舞台字组，显松弛） */
.faq__row--pressed .faq__underline {
  transform: rotate(-2deg) scaleX(1);
}

.faq__row--pressed .faq__idx {
  animation: faq-jitter-idx 0.35s ease-in-out;
}

.faq__row--pressed .faq__q {
  animation: faq-jitter 0.35s ease-in-out 0.08s;
}

@keyframes faq-jitter {
  0% {
    transform: translate(0, 0);
  }
  30% {
    transform: translate(1rpx, -2rpx) rotate(1deg);
  }
  60% {
    transform: translate(-1rpx, 1rpx) rotate(-0.8deg);
  }
  100% {
    transform: translate(0, 0);
  }
}

/* 编号抖动保留常驻 skewX(-6deg)，防动画覆盖倾斜 */
@keyframes faq-jitter-idx {
  0% {
    transform: skewX(-6deg) translate(0, 0);
  }
  30% {
    transform: skewX(-6deg) translate(1rpx, -2rpx);
  }
  60% {
    transform: skewX(-6deg) translate(-1rpx, 1rpx);
  }
  100% {
    transform: skewX(-6deg) translate(0, 0);
  }
}

.faq__underline {
  position: absolute;
  left: 8rpx;
  right: 8rpx;
  bottom: 6rpx;
  height: 6rpx;
  background: rgba(255, 255, 255, 0.35);
  transform: rotate(-2deg) scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s ease-out;
}

.faq__idx {
  flex-shrink: 0;
  width: 72rpx;
  font-family: $rw-font-display;
  font-size: 36rpx;
  color: $rw-red;
  transform: skewX(-6deg);
  font-feature-settings: "tnum";
}

.faq__q {
  flex: 1;
  font-family: $rw-font-cn-data; // 黄油体（20260919 用户指定，俏皮且与打卡数据行呼应）
  font-weight: 400;
  font-size: 29rpx;
  color: $rw-white;
  line-height: 1.4;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.faq__more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  min-height: 72rpx;
  margin-top: 8rpx;
  font-family: $rw-font-cn-data;
  font-size: 26rpx;
  letter-spacing: 2rpx;
  color: $rw-white;
  border-top: 2rpx solid $rw-line;
  transition: color 0.15s ease, transform 0.15s ease;
}

.faq__more-arrow {
  font-family: $rw-font-display;
  font-size: 34rpx;
  color: $rw-red;
}

.faq__more--pressed {
  color: $rw-red;
  transform: translateX(4rpx);
}

/* ==================== ⑥ 合并后的轻量预告区 ==================== */
.soon {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 96rpx;
  margin-top: 28rpx;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.035);
  border: 2rpx solid $rw-line;
  clip-path: polygon(14rpx 0, 100% 0, 100% calc(100% - 14rpx), calc(100% - 14rpx) 100%, 0 100%, 0 14rpx);
  @include rw-tappable;
}

.soon__label {
  flex: 1;
  min-width: 0;
}

.soon--pressed {
  transform: scale(0.97);
}

.soon__eyebrow,
.soon__title {
  display: block;
}

.soon__eyebrow {
  font-family: $rw-font-display;
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: $rw-red;
}

.soon__title {
  margin-top: 4rpx;
  font-family: $rw-font-cn-data;
  font-size: 26rpx;
  color: $rw-white;
}

.soon__features {
  max-width: 220rpx;
  font-size: 20rpx;
  line-height: 1.5;
  text-align: right;
  color: $rw-gray;
}

.soon__arrow {
  font-family: $rw-font-display;
  font-size: 34rpx;
  color: $rw-red;
}

/* ==================== ⑦ 跑马灯 ==================== */
.page__marquee {
  margin-top: 48rpx;
}

/* 降级：动效偏好用户全部静止 */
@media (prefers-reduced-motion: reduce) {
  .brand__float,
  .menu__word,
  .stage__heartbeat-line,
  .menu--pressed .menu__lt,
  .faq__row--pressed .faq__idx,
  .faq__row--pressed .faq__q {
    animation: none;
  }

  .menu__underline,
  .faq__underline {
    transition: none;
  }
}
</style>
