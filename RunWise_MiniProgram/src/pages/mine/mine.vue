<template>
  <view class="container">
    <text class="page-title">我的</text>
    <text class="page-desc">个人中心与设置占位</text>
  </view>
</template>

<script setup lang="ts">
</script>

<style>
.container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #18181B;
}

.page-title {
  font-size: 48rpx;
  font-weight: 600;
  color: #00FF7F;
  margin-bottom: 24rpx;
}

.page-desc {
  font-size: 28rpx;
  color: #A1A1AA;
}
</style>
