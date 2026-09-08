<script setup>
// 号码布贴纸（签名元素 S1 · 视觉工程最终轮）：黑底白框 Anton 数字 + 小字单位
// 工艺照主页里程牌缩至约 70%；数字必须来自真实数据（禁止装饰性假数字）
// 定位（top/right 等）由使用处经 style 透传覆写，父级需提供定位上下文；
// 贴纸骑压卡片上缘（z-index:2），不遮卡内内容、不压切角
defineProps({
  unit: {
    type: String,
    default: '', // 顶部小字单位（KM / DAYS / BADGES）
  },
  value: {
    type: [String, Number],
    required: true,
  },
  rotate: {
    type: String,
    default: '-3deg', // 斜贴角度，按实例覆写
  },
  // X-5 角标（如 "1st"）：红字缩小上浮骑角，主数字独占一行，不占版位
  badge: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <span
    class="numbib"
    :style="{ transform: `rotate(${rotate})` }"
    aria-hidden="true"
  >
    <span v-if="badge" class="numbib-badge p5-num">{{ badge }}</span>
    <span v-if="unit" class="numbib-unit">{{ unit }}</span>
    <span class="numbib-value p5-num">{{ value }}</span>
  </span>
</template>

<style scoped>
.numbib {
  position: absolute;
  z-index: 2; /* 骑压卡片上缘（与胶带贴纸同层），装饰性质不拦截交互 */
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 7px 13px 8px;
  background: var(--p5-black);
  border: 2px solid var(--p5-white);
  pointer-events: none;
}

/* 小字单位：Anton 9px 灰（主页里程牌 ms-km 同工艺） */
.numbib-unit {
  font-family: var(--font-display);
  font-size: 9px;
  line-height: 1;
  letter-spacing: 0.18em;
  text-indent: 0.18em; /* 抵消末字符字距，视觉居中 */
  color: var(--p5-text-dim);
}

/* 主数字：Anton 22px 白（主页里程牌 26px 的约 70% 档） */
.numbib-value {
  font-size: 22px;
  line-height: 1;
  color: var(--p5-white);
}

/* X-5 角标（1st 等）：红字小号，骑右上角上浮（负 top 跨边出血），
   不参与 column 布局——主数字独占一行；1px 黑影保证黑底上可读 */
.numbib-badge {
  position: absolute;
  top: -13px;
  right: -6px;
  z-index: 3;
  font-size: 10px;
  line-height: 1;
  letter-spacing: 0.06em;
  color: var(--p5-red);
  transform: rotate(9deg);
  text-shadow: 1px 1px 0 rgba(0, 0, 0, 0.65);
}
</style>
