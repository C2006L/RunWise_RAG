<script setup>
// =====================================================================
// 【永久设计标准 · 拼贴效果范围锁定（2026-09-08 定稿）】
// RansomNote 错落排版与黑色描边样式，全站仅允许以下两类场景使用：
//   ① 主页大标题「你的智能跑步训练中心」（HeroSection .sk-title）
//   ② 五个内页大标题：AI 答疑 / 训练计划 / 打卡记录 / 伤病预防 / 个人中心
//     （经 p5-page-title / 各页 <RansomNote> 实现）
// 除此之外的一切文字元素严格禁止错落排版与描边样式；
// 字号 <18px 的文字无条件禁止任何特殊效果（描边/错落/拼贴均算）。
// 新增页面/组件涉及文字样式时，以本注释为唯一依据，违例打回。
// =====================================================================
// RansomNote 拼贴组件（仅限中文使用，禁止用于英文/数字；R-1 硬性限制）：
// 逐字包 span；奇字 rotate(-2deg) / 偶字 rotate(2deg)（R-2 收窄，不超 ±2°）；
// 约每 2~3 字随机 0.94~1.06em；每个标题最多 1 字垫红色小色块（R-2 定稿）；
// 禁止逐字换色（所有字沿用继承色）
import { computed } from "vue";

const props = defineProps({
  text: { type: String, required: true },
  seed: { type: Number, default: 0 },
  level: { type: String, default: "span" }, // 渲染标签：span / h1 / h2
});

// mulberry32 种子随机：同一 (text, seed) 渲染结果恒定，重渲染不抖动
function mulberry32(a) {
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const letters = computed(() => {
  const rnd = mulberry32(props.seed ^ (props.text.length * 2654435761));
  const chars = props.text.split("");
  const real = chars.length; // 空格也占位，不参与块选择
  // R-2 定稿：红色垫块每个标题最多 1 处（空格不垫）
  const blockCount = 1;
  const blockIdx = new Set();
  let guard = 0;
  while (blockIdx.size < blockCount && guard++ < 64) {
    const i = Math.floor(rnd() * real);
    if (chars[i] !== " ") blockIdx.add(i);
  }
  return chars.map((ch, i) => {
    if (ch === " ") {
      return { ch: " ", gap: true, rot: "0deg", size: 1, block: false };
    }
    // R-2 定稿：幅度收窄到 ±2°（奇字 -2deg，偶字 2deg；0 基索引：偶数位为第 1/3/5… 奇字）
    const rot = i % 2 === 0 ? "-2deg" : "2deg";
    // 每 2~3 字随机 0.94~1.06em（约 40% 概率变异）
    const size = rnd() < 0.4 ? +(0.94 + rnd() * 0.12).toFixed(3) : 1;
    return { ch, gap: false, rot, size, block: blockIdx.has(i) };
  });
});
</script>

<template>
  <component
    :is="level"
    class="rn-note"
    :aria-label="text"
  >
    <template v-for="(l, i) in letters" :key="i">
      <span
        v-if="l.gap"
        class="rn-char rn-char--space"
        aria-hidden="true"
      ></span>
      <span
        v-else
        class="rn-char"
        :class="{ 'rn-char--block': l.block }"
        :style="{ transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
      >
        {{ l.ch }}
        <span v-if="l.block" class="rn-block" aria-hidden="true"></span>
      </span>
    </template>
  </component>
</template>

<style scoped>
.rn-note {
  display: inline-block;
}

.rn-char {
  display: inline-block;
  position: relative;
  transform-origin: 50% 62%; /* 绕基线附近旋转，节奏更稳 */
}

/* 空格占位：保持原文词间距，不参与旋转 */
.rn-char--space {
  width: 0.3em;
}

/* 红色小色块：垫在字下，露出下缘（被字形下沿盖住的部分即"垫"感） */
.rn-block {
  position: absolute;
  left: 50%;
  bottom: -0.08em;
  transform: translateX(-50%);
  width: 0.85em;
  height: 0.18em;
  background: var(--p5-red);
}
</style>
