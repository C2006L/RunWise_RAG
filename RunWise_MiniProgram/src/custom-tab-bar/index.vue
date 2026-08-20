<template>
  <view class="tab-bar">
    <view
      v-for="(item, index) in tabList"
      :key="index"
      class="tab-item"
      :class="{ 'tab-item--active': current === index }"
      @tap="onTabTap(item, index)"
    >
      <image
        class="tab-icon"
        :src="current === index ? item.selectedIconPath : item.iconPath"
        mode="aspectFit"
      />
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const tabList = [
  {
    pagePath: '/pages/index/index',
    text: '首页',
    iconPath: '/static/tabbar/home.png',
    selectedIconPath: '/static/tabbar/home-active.png',
  },
  {
    pagePath: '/pages/checkin/checkin',
    text: '打卡',
    iconPath: '/static/tabbar/checkin.png',
    selectedIconPath: '/static/tabbar/checkin-active.png',
  },
  {
    pagePath: '/pages/qa/qa',
    text: '答疑',
    iconPath: '/static/tabbar/qa.png',
    selectedIconPath: '/static/tabbar/qa-active.png',
  },
  {
    pagePath: '/pages/mine/mine',
    text: '我的',
    iconPath: '/static/tabbar/mine.png',
    selectedIconPath: '/static/tabbar/mine-active.png',
  },
];

const current = ref(0);

const onTabTap = (item: any, index: number) => {
  if (current.value === index) return;
  current.value = index;
  uni.switchTab({
    url: item.pagePath,
  });
};
</script>

<style lang="scss">
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  padding-bottom: env(safe-area-inset-bottom);
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
  padding: 8rpx 0;
  color: #9CA3AF;
  font-size: 20rpx;
  transition: all 0.2s ease;
}

.tab-item--active {
  color: #FF6B35;
}

.tab-icon {
  width: 44rpx;
  height: 44rpx;
  margin-bottom: 4rpx;
}

.tab-text {
  font-size: 20rpx;
  line-height: 1;
}
</style>