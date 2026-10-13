<template>
  <view class="rw-skeleton" :style="{ height: height, width: width }">
    <view class="rw-skeleton__shine"></view>
  </view>
</template>

<script setup lang="ts">
/**
 * RwSkeleton —— 黑底红斜纹骨架屏（替代旧灰块加载态）
 */
defineProps({
  height: { type: String, default: '32rpx' },
  width: { type: String, default: '100%' },
});
</script>

<style lang="scss" scoped>
.rw-skeleton {
  position: relative;
  overflow: hidden;
  background: repeating-linear-gradient(
    -45deg,
    $rw-panel,
    $rw-panel 16rpx,
    rgba(230, 0, 18, 0.08) 16rpx,
    rgba(230, 0, 18, 0.08) 24rpx
  );
}

.rw-skeleton__shine {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 40%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.06), transparent);
  animation: rw-skel-move 1.4s ease-in-out infinite;
}

@keyframes rw-skel-move {
  from {
    left: -40%;
  }
  to {
    left: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rw-skeleton__shine {
    animation: none;
    left: 30%;
  }
}
</style>
