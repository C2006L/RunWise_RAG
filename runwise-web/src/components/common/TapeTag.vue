<script setup>
// 米色胶带标签（工程计划 3.2② / 4.1 / v2.0 3.4.2 规则 4）：
// 撕口锯齿 + 斜贴 + 位置覆写
// - 默认定位父容器左上（父容器需 position: relative），z-tape 压在卡片之上
// - v2.0 胶带自由化：top / right / bottom / left 支持实例覆写（可为负值
//   横跨卡片边界），rotate 建议取 -8°~+6° 且同页不重复
defineProps({
  text: {
    type: String,
    required: true,
  },
  rotate: {
    type: String,
    default: "-6deg", // 斜贴角度，可按实例覆写
  },
  // 位置覆写：传入即覆盖默认左上定位（CSS 合法值，负值 = 跨边界出血）
  top: {
    type: String,
    default: "",
  },
  right: {
    type: String,
    default: "",
  },
  bottom: {
    type: String,
    default: "",
  },
  left: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <span
    class="tape"
    :style="{
      transform: `rotate(${rotate})`,
      // 位置覆写：right/bottom 定位时清掉默认 left，避免双向拉伸
      ...(right ? { right, left: 'auto' } : left ? { left } : {}),
      ...(bottom ? { bottom, top: 'auto' } : top ? { top } : {}),
    }"
    >{{ text }}</span
  >
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
