<script setup>
// P5 装饰素材演示页（/shapes-preview，人工验收用，不进导航）
// 底板分配规则（按形状颜色，而非按组件分组）：
// - 黑色实例 → 灰底板（--p5-gray）：黑对黑页面不可见
// - 灰色实例 → 深灰底板（#1A1A1E，比页面黑稍亮）：灰对灰底板不可见
// - 红 / 白实例 → 直接放页面黑底，不垫板
// BubblePlate 自带黑底 + 白描边，始终无需垫板
import JaggedStar from "./JaggedStar.vue";
import SlashShape from "./SlashShape.vue";
import Halftone from "./Halftone.vue";
import BubblePlate from "./BubblePlate.vue";
import LightningDivider from "./LightningDivider.vue";
</script>

<template>
  <div class="preview-page">
    <header class="preview-head">
      <h1 class="p5-page-title">P5 装饰素材包</h1>
      <p class="preview-desc">
        5 组件 · 手写 path · 红黑白灰四色纪律 —— 仅用于人工验收（/shapes-preview）
      </p>
    </header>

    <!-- 组件 1：锯齿星 -->
    <section class="block">
      <h2 class="block-title">1 · JaggedStar 锯齿星</h2>
      <div class="row">
        <!-- 黑实例：垫灰板 -->
        <div class="pad pad--gray">
          <JaggedStar color="black" :size="88" :seed="1" />
        </div>
        <!-- 灰实例：垫深灰板 -->
        <div class="pad pad--dark">
          <JaggedStar color="gray" :size="44" :seed="2" />
        </div>
        <!-- 红 / 白实例：直接黑底 -->
        <div class="bare">
          <JaggedStar color="red" :size="72" :seed="1" />
          <JaggedStar color="white" :size="56" :seed="2" />
          <JaggedStar color="red" :size="120" :seed="2" />
        </div>
      </div>
    </section>

    <!-- 组件 2：斜撕色块 -->
    <section class="block">
      <h2 class="block-title">2 · SlashShape 斜撕色块</h2>
      <div class="row">
        <!-- 黑变体：补灰板 -->
        <div class="pad pad--gray">
          <SlashShape variant="black" :size="260" :seed="2" />
        </div>
        <div class="bare">
          <SlashShape variant="red" :size="260" :seed="1" />
          <SlashShape variant="red" :size="180" :seed="2" />
        </div>
      </div>
    </section>

    <!-- 组件 3：半调网点 -->
    <section class="block">
      <h2 class="block-title">3 · Halftone 半调网点</h2>
      <div class="row">
        <!-- 灰网点：垫深灰板 -->
        <div class="pad pad--dark">
          <Halftone color="gray" :size="150" direction="rtl" />
        </div>
        <div class="bare">
          <Halftone color="red" :size="200" direction="ltr" />
          <Halftone color="white" :size="200" direction="rtl" />
        </div>
      </div>
    </section>

    <!-- 组件 4：斜切名牌（自带黑底 + 白描边，无需垫板） -->
    <section class="block">
      <h2 class="block-title">4 · BubblePlate 斜切名牌</h2>
      <div class="row row--gap-lg">
        <div class="bare bare--row">
          <BubblePlate :min-width="240" :seed="1">RUN WISE</BubblePlate>
          <BubblePlate :min-width="180" :seed="2" color="red">今日打卡</BubblePlate>
          <BubblePlate :min-width="160" :seed="1">THIS WEEK</BubblePlate>
        </div>
      </div>
    </section>

    <!-- 组件 5：闪电碎块 -->
    <section class="block">
      <h2 class="block-title">5 · LightningDivider 闪电碎块</h2>
      <div class="row">
        <!-- 黑闪电：垫灰板 -->
        <div class="pad pad--gray">
          <LightningDivider color="black" :size="110" mode="fill" :seed="2" />
        </div>
        <!-- 灰闪电：垫深灰板 -->
        <div class="pad pad--dark">
          <LightningDivider color="gray" :size="64" mode="stroke" :seed="1" />
        </div>
        <!-- 红 / 白闪电：直接黑底 -->
        <div class="bare">
          <LightningDivider color="red" :size="96" mode="fill" :seed="1" />
          <LightningDivider color="white" :size="72" mode="stroke" :seed="2" />
          <LightningDivider color="red" :size="80" mode="fill" :seed="2" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.preview-page {
  max-width: var(--page-max);
  margin: 0 auto;
}

.preview-head {
  padding: var(--sp-6) 0 var(--sp-5);
}

.preview-desc {
  margin-top: var(--sp-3);
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

.block {
  padding: var(--sp-5) 0;
  border-top: 1px solid var(--p5-line);
}

.block-title {
  margin-bottom: var(--sp-4);
  font-size: var(--fs-sub);
  letter-spacing: 0.1em;
  color: var(--p5-red);
}

.row {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: var(--sp-4);
  min-height: 150px;
}

/* 黑实例底板：全站灰 token */
.pad--gray {
  display: flex;
  align-items: center;
  gap: var(--sp-5);
  padding: var(--sp-5);
  background: var(--p5-gray);
}

/* 灰实例底板：比页面黑稍亮的深灰（演示专用板色，仅本页使用） */
.pad--dark {
  display: flex;
  align-items: center;
  gap: var(--sp-5);
  padding: var(--sp-5);
  background: #1a1a1e;
}

/* 红 / 白实例：黑底直放 */
.bare {
  display: flex;
  align-items: center;
  gap: var(--sp-5);
  padding: var(--sp-5);
}

.bare--row {
  flex-wrap: wrap;
}

.row--gap-lg {
  gap: var(--sp-7);
}
</style>
