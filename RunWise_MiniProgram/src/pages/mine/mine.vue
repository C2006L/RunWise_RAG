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

    <!-- 数据成就（3列并排 + 点击交互） -->
    <view class="achievements-card">
      <view class="achievement-item" @click="onAchievementTap('mileage')">
        <text class="achievement-number">{{ achievements.totalDistance }}</text>
        <text class="achievement-unit">km</text>
        <text class="achievement-label">总里程</text>
      </view>
      <view class="achievement-item" @click="onAchievementTap('checkin')">
        <text class="achievement-number">{{ achievements.checkinCount }}</text>
        <text class="achievement-unit">次</text>
        <text class="achievement-label">打卡次数</text>
      </view>
      <view class="achievement-item" @click="onAchievementTap('streak')">
        <text class="achievement-number">{{ achievements.streak }}</text>
        <text class="achievement-unit">天</text>
        <text class="achievement-label">连续天数</text>
      </view>
    </view>

    <!-- 橙色分隔线 -->
    <view class="section-divider"></view>

    <!-- 设置列表 -->
    <view class="settings-card">
      <view
        v-for="item in settingItems"
        :key="item.key"
        class="setting-item"
        @click="onSettingTap(item)"
      >
        <view class="setting-left">
          <view class="setting-icon-wrap">
            <uni-icons :type="item.icon" size="20" color="#FF6B35"></uni-icons>
          </view>
          <text class="setting-label">{{ item.label }}</text>
        </view>
        <view class="setting-arrow"></view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="onLogout">
      <text class="logout-text">退出登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { clearTokens } from "@/utils/request";

const userInfo = ref({
  nickname: "跑者小明",
  avatarUrl: "",
  joinDays: 28,
});

const achievements = ref({
  totalDistance: 125.75,
  checkinCount: 42,
  streak: 12,
});

const settingItems = [
   { key: "profile", icon: "person", label: "个人资料" },
  { key: "notification", icon: "notification", label: "消息通知" },
  { key: "update", icon: "refresh", label: "检查更新" },
  { key: "privacy", icon: "locked", label: "隐私政策" },
  { key: "agreement", icon: "paperclip", label: "用户协议" },
];

const nicknameInitial = computed(
  () => userInfo.value.nickname.charAt(0) || "跑",
);

function onSettingTap(item: (typeof settingItems)[number]) {
  const routeMap: Record<string, string> = {
    profile: "/pages/mine/profile",
    notification: "/pages/mine/notifications",
    privacy: "/pages/mine/privacy",
    agreement: "/pages/mine/agreement",
  };

  const url = routeMap[item.key];
  if (url) {
    uni.navigateTo({ url });
  } else if (item.key === "update") {
    uni.showModal({
      title: "检查更新",
      content: "当前版本：v1.0.0\n\n已是最新版本",
      showCancel: false,
      confirmText: "确定",
    });
  }
}

function onAchievementTap(type: string) {
  uni.showToast({ title: "敬请期待", icon: "none" });
}

function onLogout() {
  uni.showModal({
    title: "提示",
    content: "确定要退出登录吗？",
    success: (res) => {
      if (res.confirm) {
        clearTokens();
        uni.showToast({ title: "已退出登录", icon: "success" });
      }
    },
  });
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* ========== 个人资料卡片 ========== */
.profile-card {
  display: flex;
  align-items: center;
  @include rw-glass-card;
  padding: 32rpx;
  transition: $rw-transition-bounce;
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

/* ========== 数据成就（3列并排） ========== */
.achievements-card {
  display: flex;
  align-items: center;
  @include rw-glass-card;
  padding: 32rpx 0;
  margin-top: 24rpx;
}

.achievement-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: $rw-transition-smooth;

  &:active {
    transform: scale(0.96);
    opacity: 0.8;
  }
}

.achievement-number {
  font-size: 40rpx;
  font-weight: 700;
  color: $rw-secondary;
  line-height: 1.2;
  letter-spacing: 0.5px;
}

.achievement-unit {
  font-size: 20rpx;
  font-weight: 400;
  color: $rw-text-secondary;
  margin-top: 2rpx;
  margin-bottom: 6rpx;
}

.achievement-label {
  font-size: 22rpx;
  color: $rw-text-secondary;
}

/* ========== 橙色分隔线 ========== */
.section-divider {
  height: 2rpx;
  background: $rw-primary;
  opacity: 0.6;
  margin: 32rpx 0 8rpx 0;
}

/* ========== 设置列表 ========== */
.settings-card {
  @include rw-glass-card;
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
  transition: $rw-transition-smooth;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    transform: scale(0.98);
    background-color: $rw-bg-hover;
  }
}

.setting-left {
  display: flex;
  align-items: center;
}

.setting-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: $rw-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.setting-label {
  font-size: 28rpx;
  color: $rw-text-primary;
}

/* CSS 纯箭头 */
.setting-arrow {
  flex-shrink: 0;
  width: 16rpx;
  height: 16rpx;
  border-right: 2rpx solid #c0c4cc;
  border-bottom: 2rpx solid #c0c4cc;
  transform: rotate(-45deg);
}

/* ========== 退出登录（橙色渐变胶囊） ========== */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  margin-top: 48rpx;
  border-radius: 48rpx;
  background: $rw-gradient-primary;
  box-shadow: $rw-shadow-btn;
  transition: $rw-transition-smooth;

  &:active {
    transform: scale(0.95);
    box-shadow: $rw-shadow-btn-pressed;
  }
}

.logout-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #ffffff;
}
</style>