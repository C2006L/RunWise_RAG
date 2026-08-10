<template>
  <view class="page">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="header-title">RunWise</text>
    </view>

    <!-- 今日打卡状态卡片 -->
    <view class="checkin-card">
      <view class="checkin-deco"></view>
      <view class="checkin-body">
        <text class="checkin-date">{{ todayDate }}</text>

        <view class="checkin-main">
          <view class="checkin-info">
            <text v-if="todayCheckin.checkedIn" class="checkin-title done">今日已打卡 ✓</text>
            <text v-else class="checkin-title">今日还未打卡</text>
            <text v-if="todayCheckin.checkedIn" class="checkin-data">
              {{ todayCheckin.distance }}km · {{ todayCheckin.duration }}min · {{ todayCheckin.pace }}/km
            </text>
          </view>

          <view
            v-if="todayCheckin.checkedIn"
            class="checkin-btn secondary"
            hover-class="btn-hover"
            @click="goCheckin"
          >查看详情 →</view>
          <view
            v-else
            class="checkin-btn primary"
            hover-class="btn-hover"
            @click="goCheckin"
          >去打卡 →</view>
        </view>

        <text class="checkin-streak">
          已连续打卡 <text class="streak-num">{{ todayCheckin.streak }}</text> 天
        </text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-card">
      <view class="quick-item" hover-class="item-hover" @click="goCreate">
        <view class="quick-icon icon-record"></view>
        <text class="quick-label">记一笔</text>
      </view>
      <view class="quick-item" hover-class="item-hover" @click="goQa">
        <view class="quick-icon icon-ask"></view>
        <text class="quick-label">问问题</text>
      </view>
      <view class="quick-item" hover-class="item-hover" @click="goCalendar">
        <view class="quick-icon icon-calendar"></view>
        <text class="quick-label">看日历</text>
      </view>
    </view>

    <!-- 本周数据 -->
    <view class="week-card">
      <view class="week-col">
        <view class="week-value">
          <text class="week-num">{{ weekStats.distance }}</text>
          <text class="week-unit">km</text>
        </view>
        <text class="week-label">本周里程</text>
      </view>
      <view class="week-divider"></view>
      <view class="week-col">
        <view class="week-value">
          <text class="week-num">{{ weekStats.count }}</text>
          <text class="week-unit">次</text>
        </view>
        <text class="week-label">本周打卡</text>
      </view>
      <view class="week-divider"></view>
      <view class="week-col">
        <view class="week-value">
          <text class="week-num">{{ weekStats.duration }}</text>
          <text class="week-unit">min</text>
        </view>
        <text class="week-label">本周时长</text>
      </view>
    </view>

    <!-- 新手常问 -->
    <view class="section-title">
      <text>新手常问</text>
    </view>
    <view class="qa-card">
      <view
        v-for="(q, idx) in hotQuestions"
        :key="idx"
        class="qa-item"
        :class="{ 'no-border': idx === hotQuestions.length - 1 }"
        hover-class="item-hover"
        @click="goQuestion(q)"
      >
        <text class="qa-text">{{ q }}</text>
        <view class="qa-arrow"></view>
      </view>
    </view>

    <!-- 底部激励语 -->
    <view class="footer">
      <text class="footer-text">坚持跑下去，身体会给你答案</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'

const todayDate = ref('08/06 周四')

const todayCheckin = ref({
  checkedIn: true,
  distance: 5.2,
  duration: 32,
  pace: "6'09\"",
  streak: 7
})

const weekStats = ref({
  distance: 12.5,
  count: 3,
  duration: 98
})

const hotQuestions = ref([
  '第一次跑步该跑多远？',
  '跑完膝盖疼怎么办？',
  '新手怎么选跑鞋？'
])

const goCheckin = () => {
  uni.switchTab({ url: '/pages/checkin/checkin' })
}

const goCreate = () => {
  // 暂时用 switchTab（create 页尚未独立 tab）
  uni.switchTab({ url: '/pages/checkin/checkin' })
}

const goQa = () => {
  uni.switchTab({ url: '/pages/qa/qa' })
}

const goCalendar = () => {
  uni.switchTab({ url: '/pages/checkin/checkin' })
}

const goQuestion = (_question: string) => {
  uni.switchTab({ url: '/pages/qa/qa' })
}

const onRefresh = () => {
  // mock 刷新：模拟拉取最新打卡数据
  setTimeout(() => {
    uni.stopPullDownRefresh()
  }, 800)
}

onPullDownRefresh(() => {
  onRefresh()
})
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 0 32rpx 48rpx;
  background-color: $rw-bg-page;
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
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}

/* ========== 今日打卡状态卡片 ========== */
.checkin-card {
  position: relative;
  width: 100%;
  min-height: 352rpx;
  margin-top: 16rpx;
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
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
  height: 100%;
  min-height: 352rpx;
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

  &.primary {
    background-color: $rw-primary;
    color: #ffffff;
  }

  &.secondary {
    background-color: $rw-secondary-light;
    color: $rw-secondary;
  }
}

.btn-hover {
  opacity: 0.85;
}

.checkin-streak {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.streak-num {
  color: $rw-primary;
  font-weight: 600;
}

/* ========== 快捷入口 ========== */
.quick-card {
  @extend .card-base;
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: 100%;
  min-height: 176rpx;
  margin-top: 24rpx;
  padding: 24rpx 0;
  box-sizing: border-box;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.15s;
}

.item-hover {
  transform: scale(0.95);
}

.quick-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background-color: #fff7f0;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 56rpx 56rpx;
}

.icon-record {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 20h9'/%3E%3Cpath d='M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z'/%3E%3C/svg%3E");
}

.icon-ask {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'/%3E%3Cpath d='M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3'/%3E%3Cline x1='12' y1='17' x2='12.01' y2='17'/%3E%3C/svg%3E");
}

.icon-calendar {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='3' y='4' width='18' height='18' rx='2' ry='2'/%3E%3Cline x1='16' y1='2' x2='16' y2='6'/%3E%3Cline x1='8' y1='2' x2='8' y2='6'/%3E%3Cline x1='3' y1='10' x2='21' y2='10'/%3E%3C/svg%3E");
}

.quick-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $rw-text-secondary;
}

/* ========== 本周数据 ========== */
.week-card {
  @extend .card-base;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 144rpx;
  margin-top: 24rpx;
  padding: 24rpx 0;
  box-sizing: border-box;
}

.week-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.week-value {
  display: flex;
  align-items: baseline;
}

.week-num {
  font-size: 40rpx;
  font-weight: 700;
  color: $rw-primary;
  line-height: 1.1;
}

.week-unit {
  margin-left: 4rpx;
  font-size: 22rpx;
  color: $rw-text-placeholder;
}

.week-label {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.week-divider {
  width: 2rpx;
  height: 56rpx;
  background-color: rgba(0, 0, 0, 0.04);
}

/* ========== 新手常问 ========== */
.section-title {
  margin: 32rpx 0 16rpx;
  font-size: 32rpx;
  font-weight: 500;
  color: $rw-text-primary;
}

.qa-card {
  @extend .card-base;
  width: 100%;
  padding: 0 32rpx;
  box-sizing: border-box;
  overflow: hidden;
}

.qa-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 0;
  border-bottom: 2rpx solid rgba(0, 0, 0, 0.04);

  &.no-border {
    border-bottom: none;
  }
}

.qa-text {
  flex: 1;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.qa-arrow {
  width: 16rpx;
  height: 16rpx;
  border-top: 2rpx solid $rw-text-placeholder;
  border-right: 2rpx solid $rw-text-placeholder;
  transform: rotate(45deg);
  margin-left: 16rpx;
}

/* ========== 底部激励语 ========== */
.footer {
  margin-top: 40rpx;
  text-align: center;
}

.footer-text {
  font-size: 28rpx;
  color: $rw-text-placeholder;
}
</style>
