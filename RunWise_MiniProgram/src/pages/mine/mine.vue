<template>
  <view class="page">
    <!-- 背景图片层 -->
    <image class="page-bg" src="/static/blurry-gradient-haikei.png" mode="aspectFill"></image>

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
        <text class="join-days">🔥 已坚持 {{ userInfo.joinDays }} 天</text>
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
  { key: "feedback", icon: "chat", label: "意见反馈" },
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
  } else if (item.key === "feedback") {
    // 微信原生反馈面板
    // @ts-ignore
    wx.openCustomerServiceChat?.({
      extInfo: { url: "" },
      corpId: "",
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
  position: relative;
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
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

/* ========== 个人资料卡片 ========== */
.profile-card {
  display: flex;
  align-items: center;
  @include rw-glass-card;
  padding: 32rpx;
  @include rw-tappable;
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
  color: $rw-accent;
  background: rgba(255, 107, 53, 0.1);
  padding: 4rpx 16rpx;
  border-radius: 9999rpx;
  display: inline-flex;
  align-items: center;
}

/* ========== 数据成就（3列并排 v8.0） ========== */
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
  @include rw-tappable;
}

.achievement-number {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 36rpx;
  font-weight: 700;
  color: $rw-accent;
  line-height: 1.2;
  letter-spacing: -0.5px;
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
  @include rw-tappable;

  &:last-child {
    border-bottom: none;
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

/* ========== 退出登录（v9.0 苹果风格毛玻璃胶囊） ========== */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 96rpx;
  margin-top: 48rpx;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 48rpx;
  border: 1px solid #E8E8E8;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.95);
    background: rgba(255, 255, 255, 0.6);
  }
}

.logout-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #FF3B30;
}
</style>