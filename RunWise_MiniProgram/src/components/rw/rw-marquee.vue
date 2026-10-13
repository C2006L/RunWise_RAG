<template>
  <view class="rw-marquee">
    <view class="rw-marquee__track">
      <text v-for="n in 2" :key="n" class="rw-marquee__group">
        <text v-for="(seg, i) in segments" :key="i" class="rw-marquee__seg">{{ seg }}</text>
        <text class="rw-marquee__sep"></text>
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * RwMarquee —— 底部跑马灯（对齐 Web MarqueeBar：白底黑字、30s/循环、reduced-motion 静止）
 * v5.1 裁定：仅首页挂一条，内页不挂
 */
const segments = ['RUN WISE', 'KEEP RUNNING', "DON'T STOP", 'EVERY STEP COUNTS'];
</script>

<style lang="scss" scoped>
.rw-marquee {
  position: relative;
  overflow: hidden;
  height: 56rpx;
  background: $rw-white;
  transform: rotate(-1.8deg);
  margin: 24rpx -6% 0;
  pointer-events: none;
}

.rw-marquee__track {
  display: inline-flex;
  align-items: center;
  height: 100%;
  white-space: nowrap;
  animation: rw-marquee-move 30s linear infinite;
}

.rw-marquee__group {
  display: inline-flex;
  align-items: center;
}

.rw-marquee__seg {
  font-family: $rw-font-display;
  font-size: 26rpx;
  letter-spacing: 2rpx;
  color: $rw-black;
  line-height: 1;
  padding: 0 12rpx;
}

/* 6px 白/黑斜纹分隔符 */
.rw-marquee__sep {
  display: inline-block;
  width: 12rpx;
  height: 28rpx;
  margin: 0 16rpx;
  background: repeating-linear-gradient(
    -45deg,
    $rw-black,
    $rw-black 4rpx,
    $rw-white 4rpx,
    $rw-white 8rpx
  );
}

@keyframes rw-marquee-move {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .rw-marquee__track {
    animation: none;
  }
}
</style>
