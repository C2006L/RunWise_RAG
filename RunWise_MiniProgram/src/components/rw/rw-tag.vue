<template>
  <text class="rw-tag" :class="'rw-tag--' + variant"><text class="rw-tag__inner">{{ text }}</text></text>
</template>

<script setup lang="ts">
/**
 * RwTag —— 切角标签（对齐 Web 训练类型/心情标签定色，禁圆角、禁蓝绿）
 * variant: red 红底白字（默认/吃力） | white 白底黑字（适中） | outline 红描边白字 | gray 灰底白字（轻松）
 */
defineProps({
  text: { type: String, required: true },
  variant: { type: String, default: 'red' },
});
</script>

<style lang="scss" scoped>
.rw-tag {
  position: relative;
  display: inline-block;
  padding: 6rpx 18rpx;
  font-size: 22rpx;
  line-height: 1.4;
  letter-spacing: 1rpx;
  clip-path: polygon(8rpx 0, 100% 0, calc(100% - 8rpx) 100%, 0 100%);
}

.rw-tag__inner {
  position: relative;
  z-index: 1;
}

.rw-tag--red {
  background: $rw-red;
  color: $rw-white;
}

.rw-tag--white {
  background: $rw-white;
  color: $rw-black;
}

.rw-tag--outline {
  color: $rw-white;
  background: $rw-red; // 描边模拟：红底 + 内衬黑底伪元素（border 会被 clip-path 裁掉）

  &::before {
    content: '';
    position: absolute;
    inset: 2rpx;
    background: $rw-panel;
    clip-path: polygon(7rpx 0, 100% 0, calc(100% - 7rpx) 100%, 0 100%);
  }
}

.rw-tag--gray {
  background: rgba(156, 156, 163, 0.25);
  color: $rw-gray;
}
</style>
