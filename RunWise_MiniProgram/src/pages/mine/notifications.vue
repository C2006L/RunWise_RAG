<template>
  <view class="page">
    <view v-if="notifications.length === 0" class="empty-state">
      <view class="empty-icon"></view>
      <text class="empty-text">暂无消息通知</text>
    </view>

    <view v-else class="notification-list">
      <view
        v-for="(item, index) in notifications"
        :key="index"
        class="notification-item"
        :class="{ unread: !item.read }"
      >
        <view class="noti-icon">
          <uni-icons :type="item.icon" size="22" color="#FF6B35"></uni-icons>
        </view>
        <view class="noti-content">
          <text class="noti-title">{{ item.title }}</text>
          <text class="noti-desc">{{ item.description }}</text>
          <text class="noti-time">{{ item.time }}</text>
        </view>
        <view v-if="!item.read" class="noti-dot"></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const notifications = ref([
  {
    icon: "gift",
    title: "恭喜达成新成就！",
    description: "你已完成连续打卡7天成就",
    time: "10分钟前",
    read: false,
  },
  {
    icon: "calendar",
    title: "今日打卡提醒",
    description: "今天还没有打卡哦，快来记录吧！",
    time: "2小时前",
    read: true,
  },
]);
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #e6f7ff 100%);
  padding: 24rpx;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}

.empty-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(24, 144, 255, 0.15);
  background: rgba(24, 144, 255, 0.04);
}

.empty-text {
  font-size: 26rpx;
  color: $rw-text-placeholder;
  margin-top: 16rpx;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  background: $rw-card-bg;
  border: $rw-card-border;
  border-radius: $rw-card-radius;
  box-shadow: $rw-shadow-card;
  padding: 24rpx;
  position: relative;

  &.unread {
    border-left: 6rpx solid $rw-primary;
  }
}

.noti-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(255, 107, 53, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.noti-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.noti-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $rw-text-primary;
  margin-bottom: 8rpx;
}

.noti-desc {
  font-size: 24rpx;
  color: $rw-text-secondary;
  line-height: 1.5;
  margin-bottom: 8rpx;
}

.noti-time {
  font-size: 22rpx;
  color: $rw-text-placeholder;
}

.noti-dot {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background-color: $rw-primary;
}
</style>