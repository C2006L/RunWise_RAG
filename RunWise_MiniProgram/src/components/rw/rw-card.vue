<template>
  <view class="rw-card-wrap">
    <!-- 实心影层：与卡体同形、偏移 12rpx（clip-path 会裁 box-shadow，故双层结构） -->
    <view v-if="frame" class="rw-card-shadow" :class="'rw-card-shadow--' + frame"></view>
    <!-- 卡体：面板黑 + 白描边 + 左上/右下斜切 + 可选红角标 -->
    <view
      class="rw-card-body"
      :class="[frame ? 'rw-card-body--' + frame : '', { 'rw-card-body--plain': !accent }]"
      :style="bodyStyle"
    >
      <view v-if="accent" class="rw-card-corner"></view>
      <slot />
    </view>
    <!-- 胶带挂在 wrap 上，避免被卡体 clip-path 裁掉 -->
    <rw-tape
      v-if="tag"
      :text="tag"
      :rotate="tagRotate"
      :variant="resolvedTag"
      :top="tagTop"
      :right="tagRight"
      :left="tagLeft"
      :bottom="tagBottom"
    />
    <slot name="extra" />
  </view>
</template>

<script setup lang="ts">
/**
 * RwCard —— P5R 切角卡片（对齐 Web P5Card / p5-effects.css）
 * 工艺：面板黑底、2px(4rpx) 白描边、左上/右下 12px(24rpx) 斜切角、
 *       右下 22px(44rpx) 红实心角标、6px(12rpx) 深红错位实心影（双层结构）
 * frame: '' 无影默认 | 'red' 红框工艺（深红影） | 'yellow' 深黄影（伤病场景专用）
 */
import { computed } from 'vue';

const props = defineProps({
  tag: { type: String, default: '' },
  tagRotate: { type: String, default: '-6deg' },
  tagTop: { type: String, default: '' },
  tagRight: { type: String, default: '' },
  tagBottom: { type: String, default: '' },
  tagLeft: { type: String, default: '' },
  tagVariant: { type: String, default: '' }, // 独立覆写贴纸配色，缺省跟随 frame
  accent: { type: Boolean, default: true }, // 右下红角标开关
  frame: { type: String, default: '' },
  padding: { type: String, default: '32rpx' },
});

const resolvedTag = computed(
  () =>
    props.tagVariant ||
    (props.frame === 'red' ? 'red' : props.frame === 'yellow' ? 'yellow' : 'cream')
);

const bodyStyle = computed(() => ({ padding: props.padding }));
</script>

<style lang="scss" scoped>
.rw-card-wrap {
  position: relative;
}

.rw-card-shadow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  transform: translate(12rpx, 12rpx);
  z-index: 0;
  pointer-events: none;
  clip-path: polygon(
    24rpx 0,
    100% 0,
    100% calc(100% - 24rpx),
    calc(100% - 24rpx) 100%,
    0 100%,
    0 24rpx
  );
}

.rw-card-shadow--red {
  background: $rw-shadow-red;
}

.rw-card-shadow--yellow {
  background: #8a6a1a; // 深黄影（对齐 Web 伤病页，黄=功能黄仅限伤病场景）
}

.rw-card-body {
  position: relative;
  z-index: 1;
  background: $rw-panel;
  clip-path: polygon(
    24rpx 0,
    100% 0,
    100% calc(100% - 24rpx),
    calc(100% - 24rpx) 100%,
    0 100%,
    0 24rpx
  );
}

.rw-card-body--red,
.rw-card-body--yellow {
  border: 4rpx solid $rw-white; // 描边恒白（对角标恒红同理：白=框体职能）
}

/* 右下红角标：44rpx 实心三角（Web 22px 同款） */
.rw-card-corner {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 44rpx;
  height: 44rpx;
  background: $rw-red;
  clip-path: polygon(100% 0, 100% 100%, 0 100%);
  pointer-events: none;
}

.rw-card-body--plain .rw-card-corner {
  display: none;
}
</style>
