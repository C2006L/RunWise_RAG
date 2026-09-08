<script setup>
// 打卡记录页装饰层（内页装饰系统 2.0 · 返工 v2）：
// 配额：L×1 骑角 + M×1 + S×2 + XS×1（星 5），另网点 1 + 斜杠 1 + 水印菱形 1
// - 近景星（卡后，z-index:0）：M 白 / S×2 / XS（坐标严格按新表）
// - 骑角星（卡上，z-index:2）：L 红 骑当日记录卡右上角（pointer-events:none 点不到）
// - 景深三层（由远及近）：水印菱形 → 网点/斜杠（StarField extras）→ 星光
import StarField from '../common/StarField.vue'
import Halftone from '../../assets/shapes/Halftone.vue'
import SlashShape from '../../assets/shapes/SlashShape.vue'

// 近景星光层施工图（坐标严格按新表）
const STAR_ITEMS = [
  { tier: 'M', top: '8%', right: '32%' }, // M 白
  { tier: 'S', top: '38%', left: '2%' }, // S 白
  { tier: 'S', bottom: '4%', right: '18%' }, // S 白
  { tier: 'XS', bottom: '6%', left: '45%' }, // XS（窄屏 <900px 隐藏）
]

// 骑角层施工图：L 红 骑当日记录卡右上角（right:3% top:30%，z-index 高于卡片）
const FRONT_ITEMS = [
  { tier: 'L', right: '3%', top: '30%' },
]

// 中景层：H1 半调网点 + SL1 红斜杠（复用 P5 素材包组件，原样保留）
const EXTRA_DECOR = [
  { component: Halftone, top: '4%', right: '3%', props: { color: 'red', size: 120 }, opacity: 0.12 },
  { component: SlashShape, bottom: '6%', right: '25%', props: { variant: 'red', size: 160 }, opacity: 0.2 },
]
</script>

<template>
  <!-- 卡后装饰层（z-index:0，卡片在其上） -->
  <div class="checkin-decor" aria-hidden="true">
    <!-- 远景层：W1 水印菱形（180px 红描边空心 0.05，top:45% left:1%），
         菱形语言同 ProfileView 徽章菱形 / 日历打卡菱形标记 -->
    <svg
      class="checkin-watermark"
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M50 5 L95 50 L50 95 L5 50 Z"
        stroke="var(--p5-red)"
        stroke-width="0.5"
      />
    </svg>
    <!-- 中景 + 近景：网点/斜杠 + 星光 -->
    <StarField :items="STAR_ITEMS" :extras="EXTRA_DECOR" />
  </div>

  <!-- 骑角层（z-index:2 高于卡片 z-index:1；pointer-events 穿透）：
       L 红星压在日常记录卡右上角之上 -->
  <div class="checkin-decor-front" aria-hidden="true">
    <StarField :items="FRONT_ITEMS" />
  </div>
</template>

<style scoped>
.checkin-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.checkin-watermark {
  position: absolute;
  top: 45%;
  left: 1%;
  width: 180px;
  height: 180px;
  opacity: 0.05;
  pointer-events: none;
}

.checkin-decor-front {
  position: absolute;
  inset: 0;
  z-index: 2; /* 高于卡片（z-index:1），L 星才能骑上卡角 */
  pointer-events: none;
}
</style>
