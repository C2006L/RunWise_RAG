<template>
  <view class="page">
    <!-- 背景图片层 -->
    <image class="page-bg" src="/static/blurry-gradient-haikei.png" mode="aspectFill"></image>

    <!-- 顶部标题 -->
    <view class="header">
      <text class="header-title">RunWise</text>
    </view>

    <!-- 今日打卡状态卡片（含本周概览） -->
    <view class="checkin-card">
      <view class="checkin-deco"></view>
      <view class="checkin-body">
        <text class="checkin-date">{{ todayDate }}</text>

        <view class="checkin-main">
          <view class="checkin-info">
            <text v-if="todayCheckin.checkedIn" class="checkin-title done"
              >今日已打卡 <uni-icons type="checkmarkempty" size="14" color="#00c853"></uni-icons></text
            >
            <text v-else class="checkin-title">今日还未打卡</text>
            <text v-if="todayCheckin.checkedIn" class="checkin-data">
              {{ todayCheckin.distance }}km · {{ todayCheckin.duration }}min ·
              {{ todayCheckin.pace }}/km
            </text>
          </view>

          <view
            v-if="todayCheckin.checkedIn"
            class="checkin-btn secondary"
            hover-class="btn-hover"
            @click="goCheckin"
            >查看详情 →</view
          >
          <view
            v-else
            class="checkin-btn primary"
            hover-class="btn-hover"
            @click="goCheckin"
            >去打卡 →</view
          >
        </view>

        <text class="checkin-streak">
          已连续打卡
          <text class="streak-num">{{ todayCheckin.streak }}</text> 天
        </text>

        <!-- 本周概览（整合进卡片底部） -->
        <view class="week-overview">
          <view class="week-divider-line"></view>
          <view class="week-overview-row">
            <view class="week-overview-col">
              <text class="week-overview-num week-num-accent">{{ weekStats.distance }}</text>
              <text class="week-overview-label">本周里程</text>
            </view>
            <view class="week-overview-sep"></view>
            <view class="week-overview-col">
              <text class="week-overview-num week-num-sub">{{ weekStats.count }}</text>
              <text class="week-overview-label">本周打卡</text>
            </view>
            <view class="week-overview-sep"></view>
            <view class="week-overview-col">
              <text class="week-overview-num week-num-sub">{{ weekStats.duration }}</text>
              <text class="week-overview-label">本周时长</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 快捷入口（毛玻璃底座 + 玻璃按钮） -->
    <view class="quick-actions">
      <view class="action-bubble" @click="goCreate">
        <view class="action-icon icon-record">
          <uni-icons type="compose" size="22" color="#FF6B35"></uni-icons>
        </view>
        <text class="action-label">记一笔</text>
      </view>
      <view class="action-bubble" @click="goQa">
        <view class="action-icon icon-ask">
          <uni-icons type="help" size="22" color="#1890FF"></uni-icons>
        </view>
        <text class="action-label">问问题</text>
      </view>
      <view class="action-bubble" @click="goCalendar">
        <view class="action-icon icon-calendar">
          <uni-icons type="calendar" size="22" color="#00C853"></uni-icons>
        </view>
        <text class="action-label">看日历</text>
      </view>
    </view>

    <!-- 新手常问（棉花糖云朵 v6.0：横向滑动 + 交错重叠） -->
    <view class="chat-faq-section">
      <view class="section-header">
        <view class="robot-avatar-wrap">
          <uni-icons type="chatboxes-filled" size="22" color="#ffffff"></uni-icons>
        </view>
        <text class="section-title-text">新手常问</text>
      </view>

      <view class="cloud-scroll-wrap">
        <view
          class="cloud-bubble"
          v-for="(q, idx) in hotQuestions.slice(0, 4)"
          :key="idx"
          hover-class="cloud-bubble-hover"
          @click="goQuestion(q)"
        >
          <view class="cloud-bubble-inner">
            <view class="cloud-bubble-text">
              <text class="bubble-line1">{{ splitQuestion(q).line1 }}</text>
              <text class="bubble-line2">{{ splitQuestion(q).line2 }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="faq-more" @click="goQa">
        <text class="more-text">坚持跑下去，身体会给你答案</text>
        <text class="more-link">查看更多 ›</text>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onPullDownRefresh } from "@dcloudio/uni-app";


const todayDate = ref("08/06 周四");

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

const hotQuestions = ref([
  "第一次跑步该跑多远？",
  "跑完膝盖疼怎么办？",
  "新手怎么选跑鞋？",
  "跑步时心率多少合适？",
  "晨跑好还是夜跑好？",
  "跑步前要热身多久？",
  "如何提高跑步配速？",
]);

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

const splitQuestion = (text: string) => {
  const len = text.length;
  const mid = Math.ceil(len / 2);
  return { line1: text.slice(0, mid), line2: text.slice(mid) };
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

<style lang="scss">
.page {
  position: relative;
  min-height: 100vh;
  padding: 0 32rpx 48rpx;
  box-sizing: border-box;
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

/* ========== 顶部标题 ========== */
.header {
  padding: 24rpx 0 16rpx;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $rw-text-primary;
}

/* ========== 通用卡片 ========== */
.card-base {
  background: $rw-card-bg;
  border: $rw-card-border;
  border-radius: $rw-card-radius;
  box-shadow: $rw-shadow-card;
  transition: $rw-transition-bounce;
}

/* ========== 今日打卡状态卡片 ========== */
.checkin-card {
  position: relative;
  width: 100%;
  margin-top: 16rpx;
  @include rw-glass-card;
  overflow: hidden;
}

.checkin-deco {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6rpx;
  background-color: $rw-primary;
}

.checkin-body {
  padding: 40rpx 32rpx 32rpx;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  justify-content: space-between;
}

.checkin-date {
  font-size: 28rpx;
  color: $rw-text-secondary;
}

.checkin-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.checkin-info {
  display: flex;
  flex-direction: column;
}

.checkin-title {
  font-size: 32rpx;
  font-weight: 500;
  color: $rw-text-primary;

  &.done {
    color: $rw-success;
  }
}

.checkin-data {
  margin-top: 12rpx;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.checkin-btn {
  padding: 14rpx 28rpx;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: 24rpx;
  line-height: 1;
  @include rw-tappable;

  &.primary {
    @include rw-glossy-pill(14rpx, 28rpx);
    font-size: 28rpx;
    font-weight: 500;
    border-radius: 24rpx;
    line-height: 1;
    color: #FF8A65;
  }

  &.secondary {
    @include rw-glossy-pill(14rpx, 28rpx);
    font-size: 28rpx;
    font-weight: 500;
    border-radius: 24rpx;
    line-height: 1;
    color: $rw-secondary;
  }
}

.btn-hover {
  opacity: 0.9;
  transform: scale(0.95);
}

.checkin-streak {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.streak-num {
  color: $rw-primary;
  font-weight: 600;
}

/* ========== 快捷入口（v9.0 轻毛玻璃底座） ========== */
.quick-actions {
  @include rw-glass-card;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 32rpx 16rpx;
  margin-top: 24rpx;
}

/* 第二层：毛玻璃按钮（加强立体感） */
.action-bubble {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 180rpx;
  padding: 28rpx 0 22rpx;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 24rpx;
  border: 1px solid #E8E8E8;
  box-shadow:
    0 8rpx 24rpx rgba(0, 0, 0, 0.08),
    0 4rpx 12rpx rgba(0, 0, 0, 0.04),
    inset 0 2rpx 4rpx rgba(255, 255, 255, 0.9);
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.95) translateY(2rpx);
    background: rgba(255, 255, 255, 0.7);
    box-shadow:
      0 4rpx 12rpx rgba(0, 0, 0, 0.06),
      0 2rpx 6rpx rgba(0, 0, 0, 0.03),
      inset 0 1rpx 2rpx rgba(255, 255, 255, 0.8);
  }
}

/* 第三层：图标圆（实心主题色底，确保可见） */
.action-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-record {
  background: rgba(255, 138, 101, 0.18);
  border: 1rpx solid rgba(255, 138, 101, 0.25);
}

.icon-ask {
  background: rgba(24, 144, 255, 0.18);
  border: 1rpx solid rgba(24, 144, 255, 0.25);
}

.icon-calendar {
  background: rgba(0, 200, 83, 0.18);
  border: 1rpx solid rgba(0, 200, 83, 0.25);
}

.action-label {
  margin-top: 14rpx;
  font-size: 25rpx;
  color: #374151;
  font-weight: 500;
}

/* 强制覆盖 uni-icons 颜色（小程序兼容） */
.icon-record {
  :deep(.uni-icons) {
    color: #FF8A65 !important;
  }
}

.icon-ask {
  :deep(.uni-icons) {
    color: #1890FF !important;
  }
}

.icon-calendar {
  :deep(.uni-icons) {
    color: #00C853 !important;
  }
}

/* ========== 本周概览（内嵌于状态卡片） ========== */
.week-overview {
  margin-top: 24rpx;
}

.week-divider-line {
  width: 100%;
  height: 2rpx;
  background-color: rgba(0, 0, 0, 0.04);
  margin-bottom: 24rpx;
}

.week-overview-row {
  display: flex;
  align-items: center;
}

.week-overview-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.week-overview-num {
  font-size: 32rpx;
  font-weight: 700;
}

.week-num-accent {
  color: $rw-accent;
}

.week-num-sub {
  color: $rw-text-secondary;
}

.week-overview-label {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: $rw-text-secondary;
}

.week-overview-sep {
  width: 2rpx;
  height: 48rpx;
  background-color: rgba(0, 0, 0, 0.04);
}

/* ================================================================
   新手常问 v10.0 — 大圆角胶囊 + margin-top 上下交错
   ================================================================ */

.chat-faq-section {
  padding: 0 0 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 32rpx 24rpx 16rpx;
  margin-bottom: 16rpx;
}

.robot-avatar-wrap {
  width: 44rpx;
  height: 44rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, rgba(255, 138, 101, 0.15), rgba(255, 138, 101, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.section-title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-text-primary;
}

/* 泡泡容器（与快捷按钮区域统一样式） */
.cloud-scroll-wrap {
  @include rw-glass-card;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: visible;
  padding: 28rpx 24rpx;
  margin: 0 0 16rpx;
  gap: 0;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

/* 单个气泡样式（田径场贴图+文字叠加） */
.cloud-bubble {
  flex-shrink: 0;
  position: relative;
  width: 260rpx;
  height: 220rpx;
  margin-right: 44rpx;
  border-radius: 28rpx;
  overflow: hidden;
  transition: all 0.3s;

  &:last-child {
    margin-right: 28rpx;
  }

  &:nth-child(1) { margin-top: 0; }
  &:nth-child(2) { margin-top: 10px; }
  &:nth-child(3) { margin-top: -10px; }
  &:nth-child(4) { margin-top: 0; }
  &:nth-child(5) { margin-top: 10px; }
  &:nth-child(6) { margin-top: -10px; }

  &:active {
    transform: scale(0.95);
  }
}

.cloud-bubble-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-image: url('/static/track-field.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.65;
}

.cloud-bubble-text {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 12rpx;
  padding: 12rpx 20rpx;
}

.bubble-line1,
.bubble-line2 {
  font-size: 28rpx;
  color: #000000;
  font-weight: 700;
  line-height: 1.5;
  white-space: nowrap;
  text-shadow: 0 1rpx 3rpx rgba(255, 255, 255, 0.9);
}

.bubble-line2 {
  margin-left: 20rpx;
}

.cloud-bubble-hover {
  transform: scale(0.96);
  opacity: 0.85;
}

.faq-more {
  text-align: center;
  padding: 48rpx 0 calc(32rpx + env(safe-area-inset-bottom));
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.more-text {
  font-size: 25rpx;
  color: #4B5563;
  font-style: italic;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.more-link {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 600;
  background: $rw-primary;
  padding: 12rpx 32rpx;
  border-radius: 9999rpx;
  box-shadow: $rw-shadow-btn;
}

</style>