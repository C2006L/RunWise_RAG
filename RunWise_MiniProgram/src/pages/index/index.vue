<template>
  <view class="page">
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
              <text class="week-overview-num">{{ weekStats.distance }}</text>
              <text class="week-overview-label">本周里程</text>
            </view>
            <view class="week-overview-sep"></view>
            <view class="week-overview-col">
              <text class="week-overview-num">{{ weekStats.count }}</text>
              <text class="week-overview-label">本周打卡</text>
            </view>
            <view class="week-overview-sep"></view>
            <view class="week-overview-col">
              <text class="week-overview-num">{{ weekStats.duration }}</text>
              <text class="week-overview-label">本周时长</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 快捷入口（气泡卡片） -->
    <view class="quick-actions">
      <view class="action-bubble" hover-class="bubble-hover" @click="goCreate">
        <view class="action-icon icon-record">
          <uni-icons type="compose" size="24" color="#ffffff"></uni-icons>
        </view>
        <text class="action-label">记一笔</text>
      </view>
      <view class="action-bubble" hover-class="bubble-hover" @click="goQa">
        <view class="action-icon icon-ask">
          <uni-icons type="help" size="24" color="#ffffff"></uni-icons>
        </view>
        <text class="action-label">问问题</text>
      </view>
      <view class="action-bubble" hover-class="bubble-hover" @click="goCalendar">
        <view class="action-icon icon-calendar">
          <uni-icons type="calendar" size="24" color="#ffffff"></uni-icons>
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
          v-for="(q, idx) in hotQuestions"
          :key="idx"
          hover-class="cloud-bubble-hover"
          @click="goQuestion(q)"
        >
          <text class="cloud-bubble-text">{{ q }}</text>
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
  min-height: 100vh;
  padding: 0 32rpx 48rpx;
  box-sizing: border-box;
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
  transition: $rw-transition-smooth;

  &.primary {
    @include rw-primary-btn;
    padding: 14rpx 28rpx;
    font-size: 28rpx;
    font-weight: 500;
    border-radius: 24rpx;
    line-height: 1;
  }

  &.secondary {
    background-color: $rw-secondary-light;
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

/* ========== 快捷入口（毛玻璃卡片 v5.4） ========== */
.quick-actions {
  display: flex;
  justify-content: space-around;
  padding: 32rpx 16rpx;
  @include rw-glass-card;
  margin-top: 24rpx;
}

.action-bubble {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 180rpx;
  padding: 28rpx 0 24rpx;
  background: #ffffff;
  border-radius: $rw-radius-lg;
  box-shadow: $rw-shadow-md;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.bubble-hover {
  transform: scale(0.95) translateY(-2rpx);
  box-shadow: $rw-shadow-primary;
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-record {
  background: $rw-gradient-blue;
}

.icon-ask {
  background: $rw-gradient-pink;
}

.icon-calendar {
  background: $rw-gradient-cyan;
}

.action-label {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: $rw-text-primary;
  font-weight: 500;
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
  color: $rw-primary;
  line-height: 1.2;
  letter-spacing: 0.5px;
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
   棉花糖云朵区域 v6.0
   核心：flex 横向滑动 + 隐藏滚动条 + 负边距重叠 + 伪元素凸起 + 交错排列
   ================================================================ */

.chat-faq-section {
  padding: 0 0 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 32rpx 0 16rpx;
}

.robot-avatar-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: $rw-gradient-secondary;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  box-shadow: $rw-shadow-sm;
}

.section-title-text {
  font-size: 32rpx;
  font-weight: 600;
  color: $rw-text-primary;
}

/* 外层滚动容器 */
.cloud-scroll-wrap {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: visible;
  padding: 56rpx 32rpx 64rpx 24rpx;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

/* 单朵云 */
.cloud-bubble {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 38rpx 58rpx;
  margin-left: -36rpx;
  background: linear-gradient(135deg, #FFF8E7, #E8F4FD);
  border-radius: 100rpx;
  z-index: 1;
  box-shadow:
    0 12rpx 36rpx rgba(0, 0, 0, 0.07),
    0 4rpx 14rpx rgba(0, 0, 0, 0.04),
    inset 0 3rpx 8rpx rgba(255, 255, 255, 0.9);
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: visible;

  /* 第一朵云不缩进 */
  &:first-child {
    margin-left: 0;
  }

  /* ── 顶部蓬松凸起 ── */
  &::before {
    content: '';
    position: absolute;
    top: -44rpx;
    left: 8%;
    width: 52%;
    height: 84rpx;
    background: inherit;
    border-radius: 50%;
    z-index: -1;
    box-shadow:
      0 -6rpx 18rpx rgba(0, 0, 0, 0.04),
      inset 0 2rpx 6rpx rgba(255, 255, 255, 0.8);
  }

  /* ── 底部蓬松凸起 ── */
  &::after {
    content: '';
    position: absolute;
    bottom: -38rpx;
    right: 10%;
    width: 46%;
    height: 72rpx;
    background: inherit;
    border-radius: 50%;
    z-index: -1;
    box-shadow:
      0 6rpx 18rpx rgba(0, 0, 0, 0.04),
      inset 0 -2rpx 6rpx rgba(255, 255, 255, 0.8);
  }
}

/* ── 交错排列：奇数云朵在上，偶数云朵下移 ── */
.cloud-bubble:nth-child(odd) {
  margin-top: 0;
  z-index: 3;
  background: linear-gradient(135deg, #FFF8E7, #E8F4FD);
}

.cloud-bubble:nth-child(even) {
  margin-top: 44rpx;
  z-index: 2;
  background: linear-gradient(135deg, #E8F4FD, #FFF8E7);
}

/* ── 按压态 ── */
.cloud-bubble-hover {
  transform: scale(0.94) translateY(2rpx);
  box-shadow:
    0 6rpx 20rpx rgba(0, 0, 0, 0.05),
    0 2rpx 8rpx rgba(0, 0, 0, 0.03),
    inset 0 2rpx 4rpx rgba(255, 255, 255, 0.7);
}

/* 云朵文字 */
.cloud-bubble-text {
  font-size: 28rpx;
  color: #444;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
  position: relative;
  z-index: 1;
}

.faq-more {
  text-align: center;
  padding: 48rpx 0 32rpx;
  margin-top: 24rpx;
}

.more-text {
  font-size: 25rpx;
  color: $rw-text-placeholder;
  font-style: italic;
}

.more-link {
  font-size: 26rpx;
  color: $rw-primary;
  font-weight: 500;
  margin-left: 8rpx;
}

</style>