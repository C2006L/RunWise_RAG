<script setup>
// 四角星装饰（答疑页 · 气泡角标 / 背景点缀共用）：
// 纯 CSS clip-path 绘制，currentColor 取色，禁止任何交互（pointer-events 穿透）
defineProps({
  size: { type: Number, default: 8 }, // px
  color: { type: String, default: 'var(--p5-white)' },
})
</script>

<template>
  <span
    class="sparkle"
    :style="{ width: size + 'px', height: size + 'px', color }"
    aria-hidden="true"
  ></span>
</template>

<style scoped>
/* 四角星：内凹弧线感的菱形星（与 JaggedStar 锯齿星区分） */
.sparkle {
  display: inline-block;
  flex: none;
  background: currentColor;
  clip-path: polygon(
    50% 0%,
    62% 38%,
    100% 50%,
    62% 62%,
    50% 100%,
    38% 62%,
    0% 50%,
    38% 38%
  );
  pointer-events: none;
}
</style>
