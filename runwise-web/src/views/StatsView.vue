<script setup>
// StatsView 占位页（M1 视觉优化版）：周/月切换 + ECharts 图表在 M5 实装
import P5Card from "../components/common/P5Card.vue";
</script>

<template>
  <div class="page">
    <P5Card tag="阶段预览" tag-rotate="-5deg">
      <div class="page-card">
        <p class="page-kicker">RUNWISE WEB</p>
        <h1 class="page-title">数据统计</h1>
        <p class="page-desc">周里程与月度趋势一目了然，用图表见证你的成长。</p>
        <p class="page-foot">功能开发中，敬请期待</p>
      </div>
    </P5Card>
  </div>
</template>

<style scoped>
.page {
  padding-top: var(--sp-6);
}

.page-card {
  padding: var(--sp-7); /* 48px 内边距，保证呼吸感 */
}

.page-kicker {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-3);
}

.page-title {
  font-size: 48px; /* 海报级主标题（视觉优化指定字号） */
  font-weight: 900;
  letter-spacing: -0.01em; /* 字距收紧 */
  line-height: 1.2;
  color: var(--p5-white);
  margin-bottom: var(--sp-4);
}

.page-desc {
  font-size: var(--fs-body);
  line-height: 1.8;
  color: var(--p5-text-dim);
}

.page-foot {
  margin-top: var(--sp-7);
  font-size: var(--fs-caption);
  letter-spacing: 0.15em;
  color: var(--p5-text-dim);
}
</style>
