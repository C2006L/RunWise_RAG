<template>
  <view class="page">
    <!-- 个人资料卡片 -->
    <view class="profile-card">
      <view class="avatar">
        <image
          v-if="userInfo.avatarUrl"
          class="avatar-img"
          :src="userInfo.avatarUrl"
          mode="aspectFill"
        />
        <view v-else class="avatar-placeholder">{{ nicknameInitial }}</view>
      </view>
      <view class="profile-info">
        <text class="nickname">{{ userInfo.nickname }}</text>
        <text class="join-days">已加入 {{ userInfo.joinDays }} 天</text>
      </view>
    </view>

    <!-- 数据成就 -->
    <view class="achievements-card">
      <view class="achievement-item">
        <text class="achievement-number number-primary">{{ achievements.totalDistance }}</text>
        <text class="achievement-label">总里程</text>
      </view>
      <view class="achievement-item">
        <text class="achievement-number number-secondary">{{ achievements.checkinCount }}</text>
        <text class="achievement-label">打卡次数</text>
      </view>
      <view class="achievement-item">
        <text class="achievement-number number-primary">{{ achievements.streak }}</text>
        <text class="achievement-label">连续天数</text>
      </view>
      <view class="achievement-item">
        <text class="achievement-number number-secondary">{{ achievements.questionCount }}</text>
        <text class="achievement-label">提问次数</text>
      </view>
    </view>

    <!-- 设置列表 -->
    <view class="settings-card">
      <view
        v-for="item in settingItems"
        :key="item.key"
        class="setting-item"
        @click="onSettingClick"
      >
        <view class="setting-left">
          <text class="setting-icon">{{ item.icon }}</text>
          <text class="setting-label">{{ item.label }}</text>
        </view>
        <text class="setting-arrow">></text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="onLogout">
      <text class="logout-text">退出登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { clearTokens } from '@/utils/request'

const userInfo = ref({
  nickname: '跑者小明',
  avatarUrl: '',
  joinDays: 28
})

const achievements = ref({
  totalDistance: 125.75,
  checkinCount: 42,
  streak: 12,
  questionCount: 8
})

const settingItems = [
  { key: 'profile', icon: '📝', label: '个人资料' },
  { key: 'notification', icon: '🔔', label: '消息通知' },
  { key: 'update', icon: '🔄', label: '检查更新' },
  { key: 'privacy', icon: '📜', label: '隐私政策' },
  { key: 'agreement', icon: '📄', label: '用户协议' }
]

const nicknameInitial = computed(() => userInfo.value.nickname.charAt(0) || '跑')

function onSettingClick() {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearTokens()
        uni.showToast({ title: '已退出登录', icon: 'success' })
      }
    }
  })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background-color: $rw-bg-page;
  padding: 24rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.profile-card {
  display: flex;
  align-items: center;
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: $rw-shadow-card;
  padding: 32rpx;
}

.avatar {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  border: 4rpx solid $rw-primary;
  overflow: hidden;
  flex-shrink: 0;
  background-color: $rw-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  font-size: 56rpx;
  font-weight: 600;
  color: $rw-primary;
}

.profile-info {
  display: flex;
  flex-direction: column;
  margin-left: 24rpx;
}

.nickname {
  font-size: 36rpx;
  font-weight: 600;
  color: $rw-text-primary;
  margin-bottom: 8rpx;
}

.join-days {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.achievements-card {
  display: flex;
  flex-wrap: wrap;
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: $rw-shadow-card;
  padding: 24rpx 0;
  margin-top: 24rpx;
}

.achievement-item {
  width: 50%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
}

.achievement-number {
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8rpx;
}

.number-primary {
  color: $rw-primary;
}

.number-secondary {
  color: $rw-secondary;
}

.achievement-label {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.settings-card {
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: $rw-shadow-card;
  margin-top: 24rpx;
  overflow: hidden;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
  padding: 0 32rpx;
  border-bottom: 1rpx solid $rw-divider;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background-color: $rw-bg-hover;
  }
}

.setting-left {
  display: flex;
  align-items: center;
}

.setting-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
  line-height: 1;
}

.setting-label {
  font-size: 28rpx;
  color: $rw-text-primary;
}

.setting-arrow {
  font-size: 36rpx;
  color: $rw-text-placeholder;
  line-height: 1;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 96rpx;
  background-color: $rw-bg-card;
  border-radius: 24rpx;
  box-shadow: $rw-shadow-card;
  margin-top: 48rpx;

  &:active {
    background-color: $rw-bg-hover;
  }
}

.logout-text {
  font-size: 28rpx;
  color: $rw-error;
}
</style>
