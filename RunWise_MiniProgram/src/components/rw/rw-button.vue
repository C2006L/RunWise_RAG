<template>
  <view
    class="rw-btn"
    :class="[
      'rw-btn--' + variant,
      size ? 'rw-btn--' + size : '',
      { 'rw-btn--disabled': disabled, 'rw-btn--loading': loading, 'rw-btn--block': block },
    ]"
    hover-class="rw-btn--active"
    :hover-stay-time="80"
    @tap="onTap"
  >
    <text class="rw-btn__text"><slot>{{ text }}</slot></text>
  </view>
</template>

<script setup lang="ts">
/**
 * RwButton —— P5R 切角按钮（对齐 Web LoginView / PRD 13.2.3）
 * variant: primary 红底白字 | ghost 黑底白描边 | danger 深红
 * 形态：左右 20rpx 斜切（Web 10px 同款），禁圆角胶囊；按压 #8F000B + scale(0.97)
 * 热区：最小高度 88rpx（PRD 13.2.4）
 */
const props = defineProps({
  text: { type: String, default: '' },
  variant: { type: String, default: 'primary' },
  size: { type: String, default: '' }, // '' 默认 | 'sm'
  block: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits<{ (e: 'tap'): void }>();

const onTap = () => {
  if (props.disabled || props.loading) return;
  emit('tap');
};
</script>

<style lang="scss" scoped>
.rw-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  padding: 24rpx 48rpx;
  background: $rw-red;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  clip-path: polygon(20rpx 0, 100% 0, calc(100% - 20rpx) 100%, 0 100%);
}

.rw-btn__text {
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-white;
  line-height: 1;
}

.rw-btn--active {
  background: $rw-red-dark;
  transform: scale(0.97);
}

.rw-btn--ghost {
  background: $rw-black;
  border: 4rpx solid $rw-white;

  &.rw-btn--active {
    background: rgba(255, 255, 255, 0.08);
  }
}

.rw-btn--danger {
  background: $rw-red-dark;
}

.rw-btn--sm {
  min-height: 64rpx;
  padding: 14rpx 32rpx;

  .rw-btn__text {
    font-size: 24rpx;
  }
}

.rw-btn--block {
  display: flex;
  width: 100%;
}

.rw-btn--disabled {
  opacity: 0.4;
}

.rw-btn--loading {
  opacity: 0.75;
}
</style>
