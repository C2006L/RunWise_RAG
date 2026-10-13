<template>
  <view class="rw-empty">
    <view class="rw-empty__mark"></view>
    <text class="rw-empty__text">{{ text }}</text>
    <view
      v-if="action"
      class="rw-empty__btn"
      hover-class="rw-empty__btn--active"
      :hover-stay-time="80"
      @tap="$emit('action')"
    >
      <text class="rw-empty__btn-text">{{ action }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * RwEmpty —— 空状态（灰字 + 红色切角引导按钮）
 * 例：text「这一天还没有记录」action「去首页打卡 →」
 */
defineProps({
  text: { type: String, default: '暂无内容' },
  action: { type: String, default: '' },
});

defineEmits<{ (e: 'action'): void }>();
</script>

<style lang="scss" scoped>
.rw-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 32rpx;
}

/* 红菱形标记（打卡日历同款语言） */
.rw-empty__mark {
  width: 24rpx;
  height: 24rpx;
  background: $rw-red;
  transform: rotate(45deg);
  margin-bottom: 24rpx;
}

.rw-empty__text {
  font-size: 26rpx;
  color: $rw-gray;
  line-height: 1.7;
}

.rw-empty__btn {
  margin-top: 24rpx;
  padding: 16rpx 32rpx;
  background: $rw-red;
  clip-path: polygon(12rpx 0, 100% 0, calc(100% - 12rpx) 100%, 0 100%);
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.rw-empty__btn--active {
  background: $rw-red-dark;
  transform: scale(0.97);
}

.rw-empty__btn-text {
  font-size: 26rpx;
  color: $rw-white;
}
</style>
