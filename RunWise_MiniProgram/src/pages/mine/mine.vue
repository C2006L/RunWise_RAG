<template>
  <view class="page">
    <view class="page-dots"></view>

    <view class="page-heading">
      <view>
        <text class="page-kicker">MY RUNWISE</text>
        <text class="page-title">跑者档案</text>
      </view>
      <text class="page-number">FILE / 001</text>
    </view>

    <!-- 与首页角色资产联动的怪盗风跑者档案 -->
    <view class="profile-card">
      <image
        class="profile-runner"
        src="/static/images/hero-runner.png"
        mode="aspectFit"
      ></image>
      <view class="profile-card__top">
        <text class="profile-card__label">RUNNER PROFILE</text>
        <text class="profile-card__number">NO. 001</text>
      </view>

      <view class="profile-main">
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
          <text class="profile-code">PHANTOM RUNNER · LV.{{ runnerLevel }}</text>
          <text class="nickname">{{ userInfo.nickname }}</text>
          <text class="profile-motto">坚持奔跑，记录每一步</text>
          <view class="streak-tape">
            CURRENT STREAK · {{ achievements.streak }} DAY{{ achievements.streak === 1 ? "" : "S" }}
          </view>
        </view>
      </view>

      <view class="profile-stats">
        <view class="profile-stat">
          <text class="profile-stat__value">{{ achievements.totalDistance }}</text>
          <text class="profile-stat__label">总公里</text>
        </view>
        <view class="profile-stat">
          <text class="profile-stat__value">{{ achievements.checkinCount }}</text>
          <text class="profile-stat__label">打卡次数</text>
        </view>
        <view class="profile-stat">
          <text class="profile-stat__value">{{ achievements.longestStreak }}</text>
          <text class="profile-stat__label">最长连续天数</text>
        </view>
      </view>
    </view>

    <!-- 金色只承担奖励语义 -->
    <view class="achievements-card">
      <view class="achievement-heading">
        <view>
          <text class="achievement-kicker">ACHIEVEMENTS</text>
          <text class="achievement-title">跑者荣誉</text>
        </view>
        <text class="achievement-progress">{{ unlockedAchievementCount }} / {{ achievementItems.length }}</text>
      </view>
      <view class="achievement-list">
        <view
          v-for="item in achievementItems"
          :key="item.key"
          class="achievement-item"
          :class="{
            'achievement-item--unlocked': item.unlocked,
            'achievement-item--locked': !item.unlocked,
          }"
          hover-class="achievement-item--pressed"
          @tap="onAchievementTap(item)"
        >
          <view class="achievement-medal">
            <uni-icons
              :type="item.icon"
              size="24"
              :color="item.unlocked ? '#F6D47A' : '#777169'"
            ></uni-icons>
          </view>
          <text class="achievement-name">{{ item.name }}</text>
          <text class="achievement-state">{{ item.unlocked ? "已获得" : item.target }}</text>
        </view>
      </view>
    </view>

    <view class="settings-heading">
      <text class="settings-heading__en">ACCOUNT</text>
      <text class="settings-heading__cn">设置与支持</text>
    </view>
    <view class="settings-card">
      <view
        v-for="item in settingItems"
        :key="item.key"
        class="setting-item"
        hover-class="setting-item--pressed"
        @tap="onSettingTap(item)"
      >
        <view class="setting-left">
          <view class="setting-icon-wrap">
            <uni-icons :type="item.icon" size="19" color="#F5F5F5"></uni-icons>
          </view>
          <text class="setting-label">{{ item.label }}</text>
        </view>
        <text class="setting-arrow">›</text>
      </view>
    </view>

    <view class="logout-btn" hover-class="logout-btn--pressed" @tap="onLogout">
      <text class="logout-text">退出登录 / LOG OUT</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { setTabBarIndex } from "@/utils/tabbar";
import { clearTokens } from "@/utils/request";
import { loadCheckinRecords, summarizeCheckins } from "@/utils/checkin-data";

onShow(() => {
  setTabBarIndex(3);
  refreshAchievements();
});

const userInfo = ref({
  nickname: "跑者小明",
  avatarUrl: "",
  joinDays: 28,
});

const achievements = ref({
  totalDistance: 0,
  checkinCount: 0,
  streak: 0,
  longestStreak: 0,
});

function refreshAchievements() {
  const summary = summarizeCheckins(loadCheckinRecords());
  achievements.value = {
    totalDistance: summary.totalDistance,
    checkinCount: summary.totalCount,
    streak: summary.currentStreak,
    longestStreak: summary.longestStreak,
  };
}

const runnerLevel = computed(() => Math.max(1, Math.floor(achievements.value.checkinCount / 5) + 1));

const achievementItems = computed(() => [
  {
    key: "first-run",
    icon: "flag",
    name: "初次出发",
    target: "完成首次打卡",
    unlocked: achievements.value.checkinCount >= 1,
  },
  {
    key: "seven-days",
    icon: "fire",
    name: "连续 7 天",
    target: "连续打卡 7 天",
    unlocked: achievements.value.longestStreak >= 7,
  },
  {
    key: "hundred-km",
    icon: "map-pin",
    name: "百公里",
    target: "累计跑步 100km",
    unlocked: achievements.value.totalDistance >= 100,
  },
]);

const unlockedAchievementCount = computed(
  () => achievementItems.value.filter((item) => item.unlocked).length,
);

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

function onAchievementTap(item: (typeof achievementItems.value)[number]) {
  uni.showToast({
    title: item.unlocked ? `${item.name} · 已解锁` : item.target,
    icon: "none",
  });
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
  padding: 28rpx 24rpx;
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  overflow: hidden;
  background: $rw-black;
}

.page-dots {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.065) 2rpx, transparent 2rpx);
  background-size: 28rpx 28rpx;
}

.page-heading,
.profile-card,
.achievements-card,
.settings-heading,
.settings-card,
.logout-btn {
  position: relative;
  z-index: 1;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.page-kicker,
.page-title {
  display: block;
}

.page-kicker {
  width: fit-content;
  padding: 7rpx 18rpx;
  background: $rw-red;
  color: $rw-white;
  font-family: $rw-font-display;
  font-size: 24rpx;
  line-height: 1;
  letter-spacing: 2rpx;
  transform: rotate(-2deg);
  clip-path: polygon(10rpx 0, 100% 0, calc(100% - 10rpx) 100%, 0 100%);
}

.page-title {
  margin-top: 12rpx;
  color: $rw-white;
  font-family: $rw-font-cn-title;
  font-size: 48rpx;
  line-height: 1;
}

.page-number {
  padding-bottom: 2rpx;
  color: #77777d;
  font-family: $rw-font-display;
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

/* ========== 怪盗风跑者档案 ========== */
.profile-card {
  min-height: 458rpx;
  padding: 26rpx 28rpx 0;
  box-sizing: border-box;
  overflow: hidden;
  background: #111114;
  border: 2rpx solid #3b3b40;
  clip-path: polygon(24rpx 0, 100% 0, 100% calc(100% - 24rpx), calc(100% - 24rpx) 100%, 0 100%, 0 24rpx);
}

.profile-card::before {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 220rpx;
  height: 18rpx;
  background: $rw-red;
  transform: skewX(-28deg) translateX(10rpx);
}

.profile-runner {
  position: absolute;
  z-index: 0;
  top: 34rpx;
  right: -44rpx;
  width: 292rpx;
  height: 430rpx;
  opacity: 0.24;
  -webkit-mask-image: linear-gradient(
    to bottom,
    #000 0%,
    #000 52%,
    rgba(0, 0, 0, 0.78) 68%,
    transparent 94%
  );
  mask-image: linear-gradient(
    to bottom,
    #000 0%,
    #000 52%,
    rgba(0, 0, 0, 0.78) 68%,
    transparent 94%
  );
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  pointer-events: none;
}

.profile-card__top,
.profile-main,
.profile-stats {
  position: relative;
  z-index: 1;
}

.profile-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-card__label,
.profile-card__number {
  font-family: $rw-font-display;
  line-height: 1;
}

.profile-card__label {
  color: $rw-white;
  font-size: 28rpx;
  letter-spacing: 1rpx;
}

.profile-card__number {
  color: $rw-red;
  font-size: 24rpx;
}

.profile-main {
  display: flex;
  align-items: center;
  margin-top: 32rpx;
}

.avatar {
  width: 124rpx;
  height: 124rpx;
  border-radius: 50%;
  border: 4rpx solid $rw-red;
  overflow: hidden;
  flex-shrink: 0;
  background: #09090a;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 8rpx 8rpx 0 rgba(230, 0, 18, 0.3);
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  font-family: $rw-font-cn-title;
  font-size: 50rpx;
  color: $rw-red;
}

.profile-info {
  min-width: 0;
  max-width: calc(100% - 148rpx);
  display: flex;
  flex-direction: column;
  margin-left: 24rpx;
}

.profile-code {
  font-family: $rw-font-display;
  font-size: 20rpx;
  color: $rw-red;
  letter-spacing: 1rpx;
}

.nickname {
  margin-top: 6rpx;
  font-family: $rw-font-cn-title;
  font-size: 38rpx;
  color: $rw-white;
  line-height: 1.25;
}

.profile-motto {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #a6a6ad;
}

.streak-tape {
  width: fit-content;
  margin-top: 15rpx;
  padding: 8rpx 18rpx;
  background: rgba(230, 0, 18, 0.2);
  color: #ff6a76;
  font-family: $rw-font-display;
  font-size: 20rpx;
  line-height: 1;
  letter-spacing: 1rpx;
  clip-path: polygon(8rpx 0, 100% 0, calc(100% - 8rpx) 100%, 0 100%);
}

.profile-stats {
  z-index: 2;
  display: flex;
  align-items: center;
  margin-top: 34rpx;
  margin-right: -28rpx;
  margin-left: -28rpx;
  padding: 28rpx 28rpx 26rpx;
  border-top: 2rpx solid #3b3b40;
  background: linear-gradient(180deg, rgba(17, 17, 20, 0.82) 0%, #111114 38%);
  box-shadow: 0 -18rpx 30rpx rgba(17, 17, 20, 0.45);
}

.profile-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.profile-stat + .profile-stat {
  border-left: 1rpx solid #343438;
}

.profile-stat__value {
  font-family: $rw-font-display;
  font-size: 40rpx;
  font-weight: 700;
  color: $rw-white;
  line-height: 1;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.78);
}

.profile-stat__label {
  margin-top: 10rpx;
  font-size: 20rpx;
  color: #b0b0b6;
}

/* ========== 成就：金色仅承担荣誉与奖励语义 ========== */
.achievements-card {
  margin-top: 24rpx;
  padding: 26rpx 24rpx 24rpx;
  background: #d4a44f;
  color: #171310;
  clip-path: polygon(22rpx 0, 100% 0, 100% calc(100% - 22rpx), calc(100% - 22rpx) 100%, 0 100%, 0 22rpx);
}

.achievement-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid rgba(23, 19, 16, 0.35);
}

.achievement-kicker,
.achievement-title {
  display: block;
}

.achievement-kicker {
  width: fit-content;
  padding: 8rpx 20rpx 7rpx 16rpx;
  color: #d4a44f;
  background: #171310;
  font-family: $rw-font-display;
  font-size: 28rpx;
  line-height: 1;
  letter-spacing: 1rpx;
  clip-path: polygon(0 8%, 94% 0, 100% 82%, 7% 100%);
  transform: rotate(-2deg);
  transform-origin: left center;
}

.achievement-title {
  margin-top: 14rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.achievement-progress {
  font-family: $rw-font-display;
  font-size: 25rpx;
}

.achievement-list {
  display: flex;
  padding-top: 24rpx;
}

.achievement-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.achievement-item--pressed {
  transform: scale(0.96);
}

.achievement-item--locked {
  opacity: 1;
}

.achievement-medal {
  position: relative;
  width: 86rpx;
  height: 86rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3rpx solid transparent;
  border-radius: 50%;
  background: #171310;
  box-shadow: 0 8rpx 0 rgba(23, 19, 16, 0.2);
}

.achievement-item--unlocked .achievement-medal {
  border-color: #f6d47a;
  box-shadow:
    0 0 0 3rpx rgba(23, 19, 16, 0.72),
    0 8rpx 0 rgba(23, 19, 16, 0.24),
    inset 0 0 18rpx rgba(246, 212, 122, 0.18);
}

.achievement-item--unlocked .achievement-medal::after {
  position: absolute;
  top: 13rpx;
  left: 17rpx;
  width: 20rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.42);
  content: '';
  transform: rotate(-28deg);
}

.achievement-item--locked .achievement-medal {
  border-color: rgba(23, 19, 16, 0.28);
  background: #3f3a34;
  box-shadow: 0 6rpx 0 rgba(23, 19, 16, 0.16);
}

.achievement-name {
  margin-top: 14rpx;
  font-size: 23rpx;
  font-weight: 700;
  color: #171310;
}

.achievement-state {
  margin-top: 6rpx;
  max-width: 180rpx;
  font-size: 18rpx;
  color: rgba(23, 19, 16, 0.72);
  text-align: center;
  line-height: 1.3;
}

.achievement-item--unlocked .achievement-state {
  color: #171310;
  font-weight: 700;
}

.achievement-item--locked .achievement-name {
  color: rgba(23, 19, 16, 0.78);
}

.achievement-item--locked .achievement-state {
  color: rgba(23, 19, 16, 0.62);
}

/* ========== 设置列表 ========== */
.settings-heading {
  display: flex;
  align-items: baseline;
  margin: 34rpx 4rpx 14rpx;
}

.settings-heading__en {
  font-family: $rw-font-display;
  font-size: 27rpx;
  color: $rw-white;
  letter-spacing: 1rpx;
}

.settings-heading__cn {
  margin-left: 14rpx;
  font-size: 21rpx;
  color: #7f7f86;
}

.settings-card {
  overflow: hidden;
  background: #151518;
  border: 2rpx solid #2d2d32;
  clip-path: polygon(18rpx 0, 100% 0, 100% calc(100% - 18rpx), calc(100% - 18rpx) 100%, 0 100%, 0 18rpx);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 26rpx;
  border-bottom: 1rpx solid #2b2b30;
  transition: transform 0.15s ease, background-color 0.15s ease;

  &:last-child {
    border-bottom: none;
  }
}

.setting-item--pressed {
  transform: scale(0.985);
  background: #202024;
}

.setting-left {
  display: flex;
  align-items: center;
}

.setting-icon-wrap {
  width: 46rpx;
  height: 46rpx;
  background: #26262b;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.setting-label {
  font-size: 26rpx;
  color: #efeff2;
}

.setting-arrow {
  color: #707077;
  font-size: 36rpx;
  line-height: 1;
}

/* 退出登录属于重要操作，保持克制但明确，并继续要求二次确认 */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 82rpx;
  margin-top: 24rpx;
  background: transparent;
  border: 2rpx solid rgba(230, 0, 18, 0.7);
  clip-path: polygon(16rpx 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
  transition: transform 0.15s ease, background-color 0.15s ease;
}

.logout-btn--pressed {
  transform: scale(0.98);
  background: rgba(230, 0, 18, 0.12);
}

.logout-text {
  font-family: $rw-font-display;
  font-size: 24rpx;
  color: #ff4e5c;
  letter-spacing: 2rpx;
}
</style>
