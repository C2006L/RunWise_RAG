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
  @include rw-tappable;

  &.primary {
    @include rw-glossy-pill(14rpx, 28rpx);
    font-size: 28rpx;
    font-weight: 500;
    border-radius: 24rpx;
    line-height: 1;
    color: #FF6B35;
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
  @include rw-glass-card(32rpx);
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 32rpx 16rpx;
  margin-top: 24rpx;
}

/* 第二层：纯白按钮（不加模糊，保证清晰） */
.action-bubble {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 180rpx;
  padding: 28rpx 0 22rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  border: 1rpx solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.96);
    background: #F5F7FA;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
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
  background: rgba(255, 107, 53, 0.18);
  border: 1rpx solid rgba(255, 107, 53, 0.25);
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
    color: #FF6B35 !important;
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
   新手常问 v9.0 — 宽胶囊 + 交错云朵 + 玻璃透亮
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
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.15), rgba(24, 144, 255, 0.08));
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

/* 横向滚动容器 — 使用毛玻璃卡片做底 */
.cloud-scroll-wrap {
  @include rw-glass-card(40rpx);
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: visible;
  padding: 32rpx 20rpx;
  margin: 0 0;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

/* 气泡 — 纯白底，无模糊，保证清晰和性能 */
.cloud-bubble {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx 52rpx;
  margin-right: 20rpx;
  white-space: nowrap;
  background: #FFFFFF;
  border-radius: 9999rpx;
  border: 1rpx solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

  &:last-child {
    margin-right: 0;
  }

  &:nth-child(2n) {
    transform: translateY(-16rpx);
  }

  &:nth-child(2n+1) {
    transform: translateY(12rpx);
  }

  &:active {
    transform: scale(0.96);
    background: #F5F7FA;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }
}

.cloud-bubble-text {
  font-size: 28rpx;
  color: #374151;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
}

.cloud-bubble-hover {
  @include rw-glossy-pill-active;
}

.faq-more {
  text-align: center;
  padding: 48rpx 0 32rpx;
  margin-top: 24rpx;
}

.more-text {
  font-size: 25rpx;
  color: #4B5563;
  font-style: italic;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.more-link {
  font-size: 26rpx;
  color: $rw-primary;
  font-weight: 500;
  margin-left: 8rpx;
}

</style>