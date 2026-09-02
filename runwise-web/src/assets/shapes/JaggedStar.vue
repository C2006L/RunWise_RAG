<script setup>
// 锯齿星（P5 素材包 · 组件 1 / 工程计划 3.3 装饰语言）
// - 12 尖不规则星形：24 顶点手写伪随机坐标（非循环公式生成），
//   尖角长短交错（最长角 ~47、最短角 ~34，差 >40%）
// - seed: 1|2 两套手绘变体（并排不同）；变体 2 含一处尖角分叉毛刺
// - 整体旋转约 8°；color 仅限全站四色 CSS 变量
import { computed } from "vue";

const props = defineProps({
  color: { type: String, default: "red" }, // red | white | black | gray
  size: { type: Number, default: 48 }, // px
  seed: { type: Number, default: 1 }, // 1 | 2 两套 path 变体
});

const COLOR_VARS = {
  red: "var(--p5-red, #e60012)",
  white: "var(--p5-white, #ffffff)",
  black: "var(--p5-black, #0a0a0a)",
  gray: "var(--p5-gray, #9c9ca3)",
};

const fill = computed(() => COLOR_VARS[props.color] || COLOR_VARS.red);

// 手写顶点：外点（长短交错）/ 内点交替；变体 2 在下方尖端分叉出小毛刺
const PATHS = {
  1: "M 94 53 L 67 55 L 83 65 L 65 65 L 71 92 L 54 66 L 53 88 L 45 69 L 24 87 L 38 62 L 21 69 L 29 56 L 2 47 L 36 46 L 17 34 L 37 37 L 30 12 L 45 30 L 48 14 L 54 35 L 76 12 L 64 36 L 82 29 L 67 45 Z",
  2: "M 90 51 L 69 56 L 88 76 L 61 61 L 69 79 L 56 71 L 52 86 L 48 97 L 45 87 L 46 66 L 32 84 L 37 63 L 11 70 L 35 54 L 16 52 L 30 45 L 9 25 L 37 37 L 30 19 L 46 36 L 52 5 L 55 31 L 72 21 L 63 39 L 88 30 L 69 46 Z",
};

const path = computed(() => PATHS[props.seed] || PATHS[1]);
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="-12 -12 124 124"
    fill="none"
    aria-hidden="true"
  >
    <!-- viewBox 四周留 12px padding，容纳 rotate(8°) 后出界的尖端 -->
    <g transform="rotate(8 50 50)">
      <path :d="path" :fill="fill" />
    </g>
  </svg>
</template>
