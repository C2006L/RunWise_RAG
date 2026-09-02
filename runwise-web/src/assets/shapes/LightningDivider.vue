<script setup>
// 闪电碎块（P5 素材包 · 组件 5 / 工程计划 3.3）
// - 经典闪电锯齿多边形：7 顶点手写坐标，左右不对称、轮廓歪斜
// - mode: fill 填充 | stroke 描边（4px，斜接角）
// - seed: 1|2 两套手绘 path 变体（并排不同）；颜色仅四色 CSS 变量
import { computed } from "vue";

const props = defineProps({
  color: { type: String, default: "red" }, // red | white | black | gray
  size: { type: Number, default: 56 }, // 高度 px，宽度按 100:140 比例
  mode: { type: String, default: "fill" }, // fill | stroke
  seed: { type: Number, default: 1 },
});

const COLOR_VARS = {
  red: "var(--p5-red, #e60012)",
  white: "var(--p5-white, #ffffff)",
  black: "var(--p5-black, #0a0a0a)",
  gray: "var(--p5-gray, #9c9ca3)",
};

const color = computed(() => COLOR_VARS[props.color] || COLOR_VARS.red);

// 两套歪斜变体：转折点与倾角均不同（手写坐标，非镜像复制）
const PATHS = {
  1: "M 62 4 L 18 62 L 44 66 L 30 136 L 84 58 L 56 54 L 78 4 Z",
  2: "M 40 6 L 84 56 L 58 60 L 74 134 L 16 66 L 46 60 L 24 10 Z",
};

const path = computed(() => PATHS[props.seed] || PATHS[1]);
const width = computed(() => Math.round((props.size * 100) / 140));
</script>

<template>
  <svg
    :width="width"
    :height="size"
    viewBox="0 0 100 140"
    fill="none"
    aria-hidden="true"
  >
    <path
      :d="path"
      :fill="mode === 'stroke' ? 'none' : color"
      :stroke="mode === 'stroke' ? color : 'none'"
      stroke-width="4"
      stroke-linejoin="miter"
    />
  </svg>
</template>
