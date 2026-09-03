<script setup>
// 全局星光装饰组件（Phase G G2，设计宪法 C-3 星光纪律）：
// - 只允许摆放在留白区容器内（父容器需 position: relative），
//   远离任何文字 40px 以上由使用方通过容器范围保证
// - props:
//     count       星星数量（默认 7）
//     colors      配比 [白, 红, 缀色]，默认 [4, 2, 1]（白4:红2:缀色1）
//     accentColor 缀色（默认 --accent-blue；伤病预防页传 --accent-yellow）
//     positions   可选精确布点 [{ left, top?, bottom?, size?, color?, dim?, dur?, delay? }]
//                 传入时按点位渲染（忽略 count/colors，供迁移页保持视觉不变）
//     minSize / maxSize  星体尺寸范围 px（默认 6~12）
// - 星形：四角星（复用 SparkleStar，与 AI 答疑页手工星同款，CSS 绘制）
// - 动画：透明度 0.2~0.8 呼吸，周期 4~7s 互相错开；
//   白色星用暗档（峰值 0.55），红/缀星用亮档（峰值 0.8）；
//   prefers-reduced-motion 时全部静止
import { onMounted, ref } from 'vue'
import SparkleStar from './SparkleStar.vue'

const props = defineProps({
  count: { type: Number, default: 7 },
  colors: { type: Array, default: () => [4, 2, 1] },
  accentColor: { type: String, default: 'var(--accent-blue)' },
  positions: { type: Array, default: null },
  minSize: { type: Number, default: 6 },
  maxSize: { type: Number, default: 12 },
})

const host = ref(null)

// 配比展开：[4,2,1] → white×4, red×2, accent×1（循环至 count 个）
function colorOf(kind) {
  if (kind === 'red') return 'var(--p5-red)'
  if (kind === 'accent') return props.accentColor
  return 'rgba(255, 255, 255, 0.45)' // 白（暗档基色，亮度由动画控制）
}

function kindsByRatio() {
  const [w = 4, r = 2, a = 1] = props.colors
  const pool = [
    ...Array.from({ length: w }, () => 'white'),
    ...Array.from({ length: r }, () => 'red'),
    ...Array.from({ length: a }, () => 'accent'),
  ]
  return Array.from({ length: props.count }, (_, i) => pool[i % pool.length])
}

// 随机布点：容器内 6% 边距，互不重叠（中心距 ≥ 两星半径和 + 8px）
function randomStars() {
  const el = host.value
  const w = el?.clientWidth || 800
  const h = el?.clientHeight || 400
  const padX = Math.max(40, w * 0.06)
  const padY = Math.max(40, h * 0.1)
  const kinds = kindsByRatio()
  const placed = []
  const out = []
  for (let i = 0; i < kinds.length; i++) {
    const size = props.minSize + Math.round(Math.random() * (props.maxSize - props.minSize))
    let p = null
    for (let t = 0; t < 50; t++) {
      const cand = {
        x: padX + Math.random() * (w - padX * 2),
        y: padY + Math.random() * (h - padY * 2),
      }
      const ok = placed.every(
        (q) => Math.hypot(q.x - cand.x, q.y - cand.y) >= q.r + size / 2 + 8,
      )
      if (ok) {
        p = cand
        break
      }
    }
    if (!p) continue // 50 次仍重叠则放弃该星（宁缺毋滥）
    placed.push({ ...p, r: size / 2 })
    out.push({
      size,
      color: colorOf(kinds[i]),
      dim: kinds[i] === 'white',
      left: `${((p.x - size / 2) / w) * 100}%`,
      top: `${((p.y - size / 2) / h) * 100}%`,
      dur: 4 + Math.random() * 3,
      delay: i * 0.7 + Math.random() * 0.5,
    })
  }
  return out
}

// 精确布点（positions 传入时）：沿用点位，缺省属性补默认值
function fixedStars() {
  return props.positions.map((p, i) => ({
    size: p.size ?? 8,
    color: p.color ?? 'rgba(255, 255, 255, 0.45)',
    dim: p.dim ?? false,
    bright: p.bright ?? false, // 亮档呼吸 0.3~0.8（P1-3 重做档，Qa 页不传保持原视觉）
    left: p.left ?? 'auto',
    top: p.top ?? 'auto',
    right: p.right ?? 'auto',
    bottom: p.bottom ?? 'auto',
    dur: p.dur ?? 4 + ((i * 1.3) % 3),
    delay: p.delay ?? i * 0.7,
  }))
}

const stars = ref([])

onMounted(() => {
  stars.value = props.positions ? fixedStars() : randomStars()
})

function styleOf(s) {
  return {
    left: s.left,
    top: s.top,
    right: s.right,
    bottom: s.bottom,
    animationDuration: `${s.dur}s`,
    animationDelay: `${s.delay}s`,
  }
}
</script>

<template>
  <span ref="host" class="stars" aria-hidden="true">
    <span
      v-for="(s, i) in stars"
      :key="i"
      class="star"
      :class="{ 'star--dim': s.dim, 'star--bright': s.bright }"
      :style="styleOf(s)"
    >
      <SparkleStar :size="s.size" :color="s.color" />
    </span>
  </span>
</template>

<style scoped>
/* 组件容器铺满最近定位父级（使用方容器需 position: relative 且为留白区） */
.stars {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.star {
  position: absolute;
  line-height: 0;
  animation: stars-blink 5s ease-in-out infinite;
}

.star--dim {
  animation-name: stars-blink-dim;
}

/* 亮档（0.3~0.8）：个人中心 P1-3 星星重做档，其余页面不受影响 */
.star--bright {
  animation-name: stars-blink-bright;
}

@keyframes stars-blink {
  0%,
  100% {
    opacity: 0.2;
  }
  50% {
    opacity: 0.8;
  }
}

@keyframes stars-blink-bright {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.8;
  }
}

@keyframes stars-blink-dim {
  0%,
  100% {
    opacity: 0.2;
  }
  50% {
    opacity: 0.55;
  }
}

@media (prefers-reduced-motion: reduce) {
  .star {
    animation: none;
    opacity: 0.4;
  }
}
</style>
