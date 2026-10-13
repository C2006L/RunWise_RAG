<template>
  <text
    class="rw-tape"
    :class="'rw-tape--' + variant"
    :style="{
      transform: `rotate(${rotate})`,
      ...(right ? { right, left: 'auto' } : left ? { left } : {}),
      ...(bottom ? { bottom, top: 'auto' } : top ? { top } : {}),
    }"
    >{{ text }}</text
  >
</template>

<script setup lang="ts">
/**
 * RwTape —— 胶带贴纸（对齐 Web TapeTag）
 * 撕口锯齿 clip-path + 斜贴；默认挂父容器左上（父需 position:relative）
 * variant: cream 米底黑字 | red 红底白字 | yellow 黄底黑字（仅伤病场景）
 */
defineProps({
  text: { type: String, required: true },
  rotate: { type: String, default: '-6deg' },
  variant: { type: String, default: 'cream' },
  top: { type: String, default: '' },
  right: { type: String, default: '' },
  bottom: { type: String, default: '' },
  left: { type: String, default: '' },
});
</script>

<style lang="scss" scoped>
.rw-tape {
  position: absolute;
  top: -20rpx;
  left: -24rpx;
  z-index: $rw-z-tape;
  padding: 8rpx 36rpx;
  font-size: 22rpx;
  line-height: 1.4;
  letter-spacing: 2rpx;
  color: $rw-ink;
  white-space: nowrap;
  background: linear-gradient(
    100deg,
    rgba(245, 240, 230, 0.95),
    rgba(228, 220, 200, 0.82)
  );
  clip-path: polygon(
    4% 0,
    96% 5%,
    100% 28%,
    97% 52%,
    100% 78%,
    95% 100%,
    6% 97%,
    0 76%,
    3% 48%,
    0 20%
  );
}

.rw-tape--red {
  color: $rw-white;
  background: $rw-red;
}

.rw-tape--yellow {
  color: $rw-ink;
  background: $rw-yellow;
}
</style>
