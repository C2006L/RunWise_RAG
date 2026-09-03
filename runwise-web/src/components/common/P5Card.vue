<script setup>
// 切角卡片容器（工程计划 4.1 / v2.0 3.4.2 规则 4）：
// - tag：胶带文案（可选），有值时内部渲染 TapeTag（挂在外层 wrap 上，避免被卡片 clip-path 裁掉）
// - tagRotate：胶带旋转角度（默认 -6deg）
// - tagTop / tagRight / tagBottom / tagLeft：胶带位置覆写（v2.0 胶带自由化，
//   支持负值跨边界出血；未传时保持默认左上定位）
// - accent：右下红色三角角标开关，默认开
// - frame="red"：红框工艺变体（首页打卡卡同款：白 2px 描边 + 左上/右下 12px 斜切
//   + 22px 红角标 + 6px 深红错位实心影），贴纸同步切红底白字
import TapeTag from "./TapeTag.vue";

defineProps({
  tag: {
    type: String,
    default: "",
  },
  tagRotate: {
    type: String,
    default: "-6deg",
  },
  tagTop: {
    type: String,
    default: "",
  },
  tagRight: {
    type: String,
    default: "",
  },
  tagBottom: {
    type: String,
    default: "",
  },
  tagLeft: {
    type: String,
    default: "",
  },
  accent: {
    type: Boolean,
    default: true,
  },
  frame: {
    type: String,
    default: "", // "red" = 打卡卡同款红框工艺；"yellow" = 伤病预防页深黄影工艺（F2）
  },
  // 贴纸配色独立覆写（B1）：不启用整套框体工艺、只换贴纸色时使用；
  // 缺省时跟随 frame（red → 红底白字 / yellow → 黄底黑字 / 其余米色）
  tagVariant: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <div
    class="p5-card-wrap"
    :class="{
      'p5-wrap--redframe': frame === 'red',
      'p5-wrap--yellowframe': frame === 'yellow',
    }"
  >
    <TapeTag
      v-if="tag"
      :text="tag"
      :rotate="tagRotate"
      :top="tagTop"
      :right="tagRight"
      :bottom="tagBottom"
      :left="tagLeft"
      :variant="
        tagVariant ||
        (frame === 'red' ? 'red' : frame === 'yellow' ? 'yellow' : 'cream')
      "
    />
    <div
      class="p5-card has-card-shadow"
      :class="{
        'p5-card--plain': !accent,
        'p5-card--redframe': frame === 'red',
        'p5-card--yellowframe': frame === 'yellow',
      }"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
/* 外层 wrap：承载胶带定位；卡片本体（切角 / 角标 / 投影）样式来自全局 p5-effects.css */
.p5-card-wrap {
  position: relative;
  /* grid 等高场景（如打卡页双栏拉伸）：列高填满，卡片随 wrap 拉伸 */
  display: flex;
  flex-direction: column;
}

.p5-card-wrap :deep(.p5-card) {
  flex: 1;
}
</style>
