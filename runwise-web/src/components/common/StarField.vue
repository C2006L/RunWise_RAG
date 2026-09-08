<script setup>
// 内页装饰系统 2.0 · 分层星光组件（返工 v2：星形换首页锯齿星 / 尺寸全面放大）：
// - 五档规格（tier）：L(56px 红#E8323E 0.9) / M(36px 白 0.7) / S(22px 白 0.5) / XS(14px 白 0.3)
// - 星形：首页锯齿星（JaggedStar 12 尖 · 24 顶点手写 path，SVG 原样搬入 StarField），
//   与 SparkleStar（四角星，仅 AI 气泡角标用）彻底分离，两者不许混用
// - 装饰统一挂页面级层：position:absolute / inset:0 / pointer-events:none / z-index:0
// - 防乱三铁律：间距≥60px / 距文字/卡片≥40px / 坐标严格按表
// - 动画：L 呼吸 scale 0.9~1.1（6s）；M/S 透明度呼吸（峰值=各档上限）；XS 静止
//   时长同档按出现次序 +0.45s 错开 → 同页所有星星时长互不相同；delay 依次 +1.3s
// - prefers-reduced-motion：全部静止，透明度取各档上限
// - 窄屏 <900px：XS 星隐藏，其余保留
// - 景深三层（由远及近）：水印（使用方渲染在最前）→ 网点/斜杠（extras）→ 星光
// - 骑卡角 L 星由使用方放入独立前层（z-index 高于卡片、pointer-events:none）
import { onMounted, ref } from 'vue'

const props = defineProps({
  // 星光施工图（坐标严格按表）：[{ tier, color?, top, left/right, bottom, delay? }]
  items: {
    type: Array,
    required: true,
    validator: (list) => {
      const validTiers = ['L', 'M', 'S', 'XS']
      return list.every((item) => validTiers.includes(item.tier))
    },
  },
  // 中景层装饰（网点/斜杠）：[{ component, top, left/right, bottom, props, opacity }]
  extras: {
    type: Array,
    default: () => [],
  },
})

// 五档规格表（严格照抄，禁止自行发明）
const TIER_SPECS = {
  L: { size: 56, color: '#E8323E', opacity: 0.9 },
  M: { size: 36, color: 'rgba(255,255,255,0.7)', opacity: 0.7 },
  S: { size: 22, color: 'rgba(255,255,255,0.5)', opacity: 0.5 },
  XS: { size: 14, color: 'rgba(255,255,255,0.3)', opacity: 0.3 },
}

// 首页锯齿星 path（JaggedStar seed 1 原样搬入：24 顶点手写伪随机，长短角交错）
const STAR_PATH =
  'M 94 53 L 67 55 L 83 65 L 65 65 L 71 92 L 54 66 L 53 88 L 45 69 L 24 87 L 38 62 L 21 69 L 29 56 L 2 47 L 36 46 L 17 34 L 37 37 L 30 12 L 45 30 L 48 14 L 54 35 L 76 12 L 64 36 L 82 29 L 67 45 Z'

// 动画时长基线：L 固定 6s / M 5~7s / S 4~6s（同档第 n 颗 +0.45n，保同页互不相同）
const TIER_BASE = { L: 6, M: 5.4, S: 4.6 }

const stars = ref([])

onMounted(() => {
  const tierSeq = { L: 0, M: 0, S: 0, XS: 0 }
  stars.value = props.items.map((item, i) => {
    const spec = TIER_SPECS[item.tier]
    const seq = tierSeq[item.tier]++
    return {
      tier: item.tier,
      size: spec.size,
      color: item.color || spec.color,
      top: item.top,
      left: item.left,
      right: item.right,
      bottom: item.bottom,
      // XS 取静止档（规格允许"静止或 8s 极微弱"）
      dur: item.tier === 'XS' ? 0 : TIER_BASE[item.tier] + seq * 0.45,
      delay: item.delay ?? i * 1.3, // 依次错开 1.3s
    }
  })
})

// 注意：animation-name 一律由档位 class 提供（scoped keyframes 会被哈希改名，
// 内联 animationName 会失配导致动画失效），此处只给时长/延时
function styleOf(s) {
  return {
    top: s.top,
    left: s.left,
    right: s.right,
    bottom: s.bottom,
    animationDuration: s.dur ? `${s.dur}s` : undefined,
    animationDelay: `${s.delay}s`,
  }
}
</script>

<template>
  <div class="star-field" aria-hidden="true">
    <!-- 中景层：网点 / 斜杠（先渲染，位于星光之下） -->
    <component
      v-for="(ex, i) in props.extras"
      :key="'ex' + i"
      :is="ex.component"
      :style="{
        position: 'absolute',
        top: ex.top,
        left: ex.left,
        right: ex.right,
        bottom: ex.bottom,
        opacity: ex.opacity,
      }"
      v-bind="ex.props"
    />

    <!-- 近景层：星光（首页锯齿星，SVG 原样：viewBox -12 -12 124 124 / rotate 8°） -->
    <span
      v-for="(s, i) in stars"
      :key="'star' + i"
      class="star"
      :class="`star--${s.tier}`"
      :style="styleOf(s)"
    >
      <svg :width="s.size" :height="s.size" viewBox="-12 -12 124 124" fill="none">
        <g transform="rotate(8 50 50)">
          <path :d="STAR_PATH" :fill="s.color" />
        </g>
      </svg>
    </span>
  </div>
</template>

<style scoped>
/* 页面级装饰层（整层 pointer-events 穿透，装饰点不到） */
.star-field {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* 星光公共样式 */
.star {
  position: absolute;
  display: inline-block;
  line-height: 0;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

/* L 档（56px 红）：呼吸 scale 0.9~1.1，透明度恒定 0.9 */
.star--L {
  opacity: 0.9;
  animation-name: star-breathe;
}

@keyframes star-breathe {
  0%,
  100% {
    transform: scale(0.9);
  }
  50% {
    transform: scale(1.1);
  }
}

/* M 档（36px 白）：透明度呼吸，峰值 0.7 */
.star--M {
  animation-name: star-blink-m;
}

@keyframes star-blink-m {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 0.7;
  }
}

/* S 档（22px 白）：透明度呼吸，峰值 0.5 */
.star--S {
  animation-name: star-blink-s;
}

@keyframes star-blink-s {
  0%,
  100% {
    opacity: 0.25;
  }
  50% {
    opacity: 0.5;
  }
}

/* XS 档（14px 白）：静止，0.3 */
.star--XS {
  animation: none;
  opacity: 0.3;
}

/* 窄屏 <900px：XS 隐藏，其余保留 */
@media (max-width: 900px) {
  .star--XS {
    display: none;
  }
}

/* prefers-reduced-motion：全部静止，透明度取各档上限 */
@media (prefers-reduced-motion: reduce) {
  .star {
    animation: none;
  }
  .star--L {
    opacity: 0.9;
  }
  .star--M {
    opacity: 0.7;
  }
  .star--S {
    opacity: 0.5;
  }
  .star--XS {
    opacity: 0.3;
  }
}
</style>
