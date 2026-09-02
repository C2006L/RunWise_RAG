<script setup>
// 斜撕色块（P5 素材包 · 组件 2 / 工程计划 3.3）
// - 对角歪斜多边形（12 顶点手写坐标），上 / 下两条边带 3~5 个撕纸锯齿缺口
// - 整体倾斜 -12°；variant 控制大面积底色（red / black / outline 空心描边），
//   color prop 保持五组件统一接口（未传 variant 时作为底色回退）
// - seed: 1|2 两套手绘 path 变体（并排不同）
import { computed } from "vue";

const props = defineProps({
  color: { type: String, default: "red" }, // red | white | black | gray（接口统一）
  size: { type: Number, default: 220 }, // 宽度 px，高度按 268:188 比例
  variant: { type: String, default: "" }, // 'red' | 'black' | 'outline' 空心黑描边
  seed: { type: Number, default: 1 },
});

const COLOR_VARS = {
  red: "var(--p5-red, #e60012)",
  white: "var(--p5-white, #ffffff)",
  black: "var(--p5-black, #0a0a0a)",
  gray: "var(--p5-gray, #9c9ca3)",
};

const fill = computed(() => {
  if (props.variant === "outline") return "none";
  if (props.variant === "red") return COLOR_VARS.red;
  if (props.variant === "black") return COLOR_VARS.black;
  return COLOR_VARS[props.color] || COLOR_VARS.red;
});

// outline 变体：2px 黑描边（空心）
const stroke = computed(() =>
  props.variant === "outline"
    ? { stroke: COLOR_VARS.black, "stroke-width": 2 }
    : {}
);

// 手写顶点：上边锯齿（52/74/120/142 交替起伏）、下边锯齿（168/150/96/74 交替）
const PATHS = {
  1: "M 8 34 L 52 22 L 74 30 L 120 14 L 142 24 L 196 8 L 214 96 L 168 112 L 150 104 L 96 126 L 74 118 L 26 134 Z",
  2: "M 14 52 L 60 30 L 84 42 L 138 22 L 162 36 L 206 24 L 212 88 L 172 118 L 148 108 L 92 130 L 60 120 L 20 132 Z",
};

const path = computed(() => PATHS[props.seed] || PATHS[1]);
// viewBox 四周留 24px padding 容纳 rotate(-12°) 的出界顶点（右上/左下角），
// 高度换算按 268:188 比例同步调整
const height = computed(() => Math.round((props.size * 188) / 268));
</script>

<template>
  <svg
    :width="size"
    :height="height"
    viewBox="-24 -24 268 188"
    fill="none"
    aria-hidden="true"
  >
    <g transform="rotate(-12 110 70)">
      <path :d="path" :fill="fill" v-bind="stroke" />
    </g>
  </svg>
</template>
