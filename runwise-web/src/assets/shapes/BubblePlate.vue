<script setup>
// 斜切名牌（P5 素材包 · 组件 4 / 工程计划 3.3）
// - 黑色底板：双层 path（外层白色 2px 描边 + 内层黑色填充），
//   两个对角斜切 12px；右下（seed 2 为左下）小红色三角旗标
// - 插槽承载文字（RUN WISE 大标题 / 卡片标题 / eyebrow），整体 skew(-2deg)
// - min-width 可配置；color prop 控制底板色（默认 black，保持五组件接口统一）
import { computed } from "vue";

const props = defineProps({
  color: { type: String, default: "black" }, // 底板色：black | red（白描边+红旗标恒定）
  size: { type: Number, default: 0 }, // 可选：固定宽度 px；0 = 由内容 / min-width 决定
  minWidth: { type: Number, default: 160 }, // 最小宽度 px
  seed: { type: Number, default: 1 }, // 1 斜切右上/左下 | 2 斜切左上/右下（旗标换侧）
});

const PLATE_H = 88; // viewBox 高（宽 300），preserveAspectRatio 拉伸贴合内容

const COLOR_VARS = {
  red: "var(--p5-red, #e60012)",
  white: "var(--p5-white, #ffffff)",
  black: "var(--p5-black, #0a0a0a)",
  gray: "var(--p5-gray, #9c9ca3)",
};

const plateFill = computed(() => COLOR_VARS[props.color] || COLOR_VARS.black);

// 双层 path：外描边层与内填充层共用轮廓（对角斜切 12px，手写坐标）
const PATHS = {
  1: "M 2 2 L 285 2 L 298 14 L 298 86 L 15 86 L 2 74 Z", // 切右上 + 左下
  2: "M 14 2 L 298 2 L 298 74 L 286 86 L 2 86 L 2 14 Z", // 切左上 + 右下
};

const path = computed(() => PATHS[props.seed] || PATHS[1]);

const rootStyle = computed(() => ({
  minWidth: `${props.minWidth}px`,
  ...(props.size ? { width: `${props.size}px` } : {}),
}));
</script>

<template>
  <span class="bubble-plate" :style="rootStyle">
    <!-- 底板：preserveAspectRatio none 拉伸贴合文字区域 -->
    <svg
      class="plate"
      viewBox="0 0 300 88"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <path :d="path" :fill="plateFill" stroke="var(--p5-white)" stroke-width="2" />
    </svg>
    <!-- 红色小旗标：独立 svg 定位在斜切角对侧底部，不被底板拉伸 -->
    <svg
      class="flag"
      :class="seed === 2 ? 'flag--left' : 'flag--right'"
      width="26"
      height="16"
      viewBox="0 0 26 16"
      aria-hidden="true"
    >
      <polygon points="1,1 25,1 13,15" fill="var(--p5-red)" />
    </svg>
    <span class="content"><slot /></span>
  </span>
</template>

<style scoped>
.bubble-plate {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14px 44px 16px;
  transform: skewX(-2deg);
}

.plate {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.flag {
  position: absolute;
  bottom: -6px;
}

.flag--right {
  right: 14px;
}

.flag--left {
  left: 14px;
}

.content {
  position: relative;
  z-index: 1;
  font-weight: 900;
  font-size: var(--fs-h3);
  letter-spacing: 0.04em;
  color: var(--p5-white);
  white-space: nowrap;
}
</style>
