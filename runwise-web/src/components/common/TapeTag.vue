<script setup>
// 米色胶带标签（工程计划 3.2② / 4.1）：撕口锯齿 + 斜贴
// 默认绝对定位于父容器左上（父容器需 position: relative），z-tape 保证压在卡片之上
defineProps({
  text: {
    type: String,
    required: true,
  },
  rotate: {
    type: String,
    default: '-6deg', // 斜贴角度，可按实例覆写（如 -5deg）
  },
})
</script>

<template>
  <span class="tape" :style="{ transform: `rotate(${rotate})` }">{{ text }}</span>
</template>

<style scoped>
/* 米色半透明纸质感 + 单元素 clip-path 两端撕口锯齿（工程计划 3.2②） */
.tape {
  position: absolute;
  top: -12px;
  left: -16px;
  z-index: var(--z-tape);
  padding: 5px 20px;
  font-size: var(--fs-caption);
  color: var(--p5-ink);
  white-space: nowrap;
  background: linear-gradient(
    100deg,
    rgba(245, 240, 230, 0.95),
    rgba(228, 220, 200, 0.82)
  );
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
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
</style>
