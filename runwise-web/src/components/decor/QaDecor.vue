<script setup>
// AI 答疑页装饰层（内页装饰系统 2.0 · 返工 v2）：
// 配额：M×1 + S×2（星 3，维持"安静"人设，不骑卡），另网点 1（无斜杠水印）
// - 坐标严格按新表（D4 已按讨论移出卡区）
// - 所有装饰挂页面级层：position:absolute / inset:0 / pointer-events:none / z-index:0
import StarField from '../common/StarField.vue'
import Halftone from '../../assets/shapes/Halftone.vue'

// 星光层施工图（坐标严格按新表）
const STAR_ITEMS = [
  { tier: 'M', top: '10%', right: '6%' }, // M 白
  { tier: 'S', bottom: '6%', left: '8%' }, // S 白
  { tier: 'S', bottom: '12%', left: '4%' }, // S 白
]

// 额外装饰：仅网点 1 个
const EXTRA_DECOR = [
  { component: Halftone, top: '3%', right: '3%', props: { color: 'red', size: 120 }, opacity: 0.12 },
]
</script>

<template>
  <div class="qa-decor" aria-hidden="true">
    <StarField :items="STAR_ITEMS" :extras="EXTRA_DECOR" />
  </div>
</template>

<style scoped>
.qa-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
