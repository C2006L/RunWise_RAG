<template>
  <view class="rw-star" :style="style" aria-hidden="true">
    <view
      class="rw-star__svg"
      :class="{ 'rw-star__svg--breathe': duration > 0 }"
      :style="{
        width: size + 'rpx',
        height: size + 'rpx',
        animationDuration: duration + 's',
        animationDelay: delay + 's',
      }"
    >
      <image class="rw-star__img" :src="dataUri" mode="aspectFit" />
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * RwStarField —— 锯齿炸星装饰（对齐 Web StarField/JaggedStar，与四角星 SparkleStar 严格区分）
 * 档位尺寸/颜色/透明度固定：L 112rpx 红0.9 / M 72rpx 白0.7 / S 44rpx 白0.5 / XS 28rpx 白0.3
 * 动画：呼吸 4~7s 互不相同，delay 错开 1.3s；reduced-motion 静止取上限
 * 规则：每页 ≤2 颗骑卡角；装饰距文字/卡边 ≥40px(80rpx)；装饰层 z-index 0、pointer-events none
 * 窄屏(<900rpx 逻辑)：XS 隐藏（由页面媒体查询处理，组件提供 hideXsOnNarrow 类名约定）
 */
import { computed } from 'vue';

const props = defineProps({
  tier: { type: String, default: 'S' }, // L | M | S | XS
  color: { type: String, default: '' }, // 覆写颜色：red | white（缺省按档位）
  seed: { type: Number, default: 1 }, // 1|2 两套手绘 path 变体
  duration: { type: Number, default: 5 }, // 呼吸周期 s（同页互不相同）
  delay: { type: Number, default: 0 }, // s
  top: { type: String, default: '' },
  bottom: { type: String, default: '' },
  left: { type: String, default: '' },
  right: { type: String, default: '' },
});

const TIERS: Record<string, { size: number; color: string; opacity: number }> = {
  L: { size: 112, color: '#E60012', opacity: 0.9 },
  M: { size: 72, color: '#FFFFFF', opacity: 0.7 },
  S: { size: 44, color: '#FFFFFF', opacity: 0.5 },
  XS: { size: 28, color: '#FFFFFF', opacity: 0.3 },
};

// Web JaggedStar 手写 24 顶点 path（12 尖不规则星形，viewBox -12 -12 124 124，rotate 8°）
const PATHS: Record<number, string> = {
  1: 'M 94 53 L 67 55 L 83 65 L 65 65 L 71 92 L 54 66 L 53 88 L 45 69 L 24 87 L 38 62 L 21 69 L 29 56 L 2 47 L 36 46 L 17 34 L 37 37 L 30 12 L 45 30 L 48 14 L 54 35 L 76 12 L 64 36 L 82 29 L 67 45 Z',
  2: 'M 90 51 L 69 56 L 88 76 L 61 61 L 69 79 L 56 71 L 52 86 L 48 97 L 45 87 L 46 66 L 32 84 L 37 63 L 11 70 L 35 54 L 16 52 L 30 45 L 9 25 L 37 37 L 30 19 L 46 36 L 52 5 L 55 31 L 72 21 L 63 39 L 88 30 L 69 46 Z',
};

const tier = computed(() => TIERS[props.tier] || TIERS.S);
const size = computed(() => tier.value.size);
const fillColor = computed(() =>
  props.color === 'red' ? '#E60012' : props.color === 'white' ? '#FFFFFF' : tier.value.color
);

const dataUri = computed(() => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="-12 -12 124 124"><g transform="rotate(8 50 50)"><path d="${
    PATHS[props.seed] || PATHS[1]
  }" fill="${fillColor.value}" fill-opacity="${tier.value.opacity}"/></g></svg>`;
  return 'data:image/svg+xml,' + encodeURIComponent(svg);
});

const style = computed(() => ({
  position: 'absolute' as const,
  top: props.top || undefined,
  bottom: props.bottom || undefined,
  left: props.left || undefined,
  right: props.right || undefined,
  zIndex: 2, // 骑卡显示：高于卡片(1)
  pointerEvents: 'none' as const,
}));
</script>

<style lang="scss" scoped>
.rw-star__svg--breathe {
  /* animation-duration / delay 由内联 style 传入（同页互不相同、错开 1.3s） */
  animation-name: rw-star-breathe;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

@keyframes rw-star-breathe {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

.rw-star__img {
  width: 100%;
  height: 100%;
}

@media (prefers-reduced-motion: reduce) {
  .rw-star__svg--breathe {
    animation: none;
    opacity: 1;
  }
}
</style>
