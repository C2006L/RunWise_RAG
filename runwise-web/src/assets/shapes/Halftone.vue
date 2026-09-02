<script setup>
// 半调网点（P5 素材包 · 组件 3 / 工程计划 3.3）
// - 平行四边形裁切区域内铺圆点，左→右三档 pattern 密度递减（大→小，
//   渐变消失）；direction 控制左密右疏 / 右密左疏
// - 圆点与裁切区均为手写坐标；颜色仅四色 CSS 变量
// - 注意：pattern/clipPath 的 id 带随机 uid 后缀，避免多实例 id 冲突
import { computed } from "vue";

const props = defineProps({
  color: { type: String, default: "red" }, // red | white | black | gray
  size: { type: Number, default: 160 }, // 宽度 px，高度按 160:100 比例
  direction: { type: String, default: "ltr" }, // ltr 左密右疏 | rtl 右密左疏
});

// 每实例独立执行 setup → 用随机串保证 id 唯一（Vue 3.4 无 useId()，3.5+ 可换 useId）
const uid = `ht-${Math.random().toString(36).slice(2, 8)}`;

const COLOR_VARS = {
  red: "var(--p5-red, #e60012)",
  white: "var(--p5-white, #ffffff)",
  black: "var(--p5-black, #0a0a0a)",
  gray: "var(--p5-gray, #9c9ca3)",
};

const fill = computed(() => COLOR_VARS[props.color] || COLOR_VARS.red);
const height = computed(() => Math.round((props.size * 100) / 160));

// 三档密度：密（间距 8 / r 2.6）→ 中（11 / 1.8）→ 疏（15 / 1.1）
const DENSITY = [
  { gap: 8, r: 2.6, x: 0, w: 58 },
  { gap: 11, r: 1.8, x: 54, w: 56 },
  { gap: 15, r: 1.1, x: 106, w: 54 },
];
</script>

<template>
  <svg
    :width="size"
    :height="height"
    viewBox="0 0 160 100"
    fill="none"
    aria-hidden="true"
  >
    <defs>
      <clipPath :id="`${uid}-clip`">
        <!-- 手写平行四边形裁切区：左右边斜切，呼应全站切角语言 -->
        <polygon points="14 0, 160 0, 146 100, 0 100" />
      </clipPath>
      <pattern
        v-for="(d, i) in DENSITY"
        :key="i"
        :id="`${uid}-p${i}`"
        :width="d.gap"
        :height="d.gap"
        patternUnits="userSpaceOnUse"
      >
        <circle :cx="d.gap / 2" :cy="d.gap / 2" :r="d.r" :fill="fill" />
      </pattern>
    </defs>

    <!-- rtl：整体镜像（疏密方向翻转） -->
    <g
      :transform="
        direction === 'rtl' ? 'translate(160 0) scale(-1 1)' : undefined
      "
      :clip-path="`url(#${uid}-clip)`"
    >
      <rect
        v-for="(d, i) in DENSITY"
        :key="i"
        :x="d.x"
        y="0"
        :width="d.w"
        height="100"
        :fill="`url(#${uid}-p${i})`"
      />
    </g>
  </svg>
</template>
