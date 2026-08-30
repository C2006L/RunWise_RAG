<script setup>
// 切角卡片容器（工程计划 4.1）：
// - tag：胶带文案（可选），有值时内部渲染 TapeTag（挂在外层 wrap 上，避免被卡片 clip-path 裁掉）
// - tagRotate：胶带旋转角度（默认 -6deg）
// - accent：右下红色三角角标开关，默认开
import TapeTag from "./TapeTag.vue";

defineProps({
  tag: {
    type: String,
    default: "",
  },
  tagRotate: {
    type: String,
    default: "-6deg",
  },
  accent: {
    type: Boolean,
    default: true,
  },
});
</script>

<template>
  <div class="p5-card-wrap">
    <TapeTag v-if="tag" :text="tag" :rotate="tagRotate" />
    <div class="p5-card has-card-shadow" :class="{ 'p5-card--plain': !accent }">
      <slot />
    </div>
  </div>
</template>

<style scoped>
/* 外层 wrap：承载胶带定位；卡片本体（切角 / 角标 / 投影）样式来自全局 p5-effects.css */
.p5-card-wrap {
  position: relative;
}
</style>
