<template>
  <view class="rw-bib" :style="{ transform: `rotate(${rotate})` }">
    <text v-if="badge" class="rw-bib__badge">{{ badge }}</text>
    <text v-if="unit" class="rw-bib__unit">{{ unit }}</text>
    <text class="rw-bib__value">{{ value }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * RwNumberBib —— 号码布贴纸（对齐 Web NumberBib，签名元素 S1）
 * 黑底白框 + Anton 大数字 + 小字单位；数字必须来自真实数据，禁止装饰性假数字
 * 定位由使用处 style 覆写（top/right 等）；默认骑压卡片上缘
 */
defineProps({
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' }, // 顶部小字单位（KM / DAYS / BADGES / SEP）
  badge: { type: String, default: '' }, // 角标（如 1st），红字缩小上浮骑角
  rotate: { type: String, default: '-3deg' },
});
</script>

<style lang="scss" scoped>
.rw-bib {
  /* relative 而非 absolute：组件自身样式不受小程序样式隔离影响，必定生效。
     absolute 会让外层定位 wrapper 收缩为 0 宽、号码布从 wrapper 左缘向右冲出屏幕；
     relative 让 wrapper（absolute + 收缩宽）正确包裹号码布，定位由使用处 wrapper 完成 */
  position: relative;
  z-index: 2;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 22rpx 14rpx;
  background: $rw-black;
  border: 4rpx solid $rw-white;
  pointer-events: none;
}

.rw-bib__unit {
  font-family: $rw-font-display;
  font-size: 18rpx;
  line-height: 1;
  letter-spacing: 4rpx;
  text-indent: 4rpx;
  color: $rw-gray;
}

.rw-bib__value {
  font-family: $rw-font-display;
  font-size: 44rpx;
  line-height: 1.15;
  color: $rw-white;
  font-feature-settings: 'tnum';
}

.rw-bib__badge {
  position: absolute;
  top: -22rpx;
  right: -10rpx;
  font-family: $rw-font-display;
  font-size: 20rpx;
  line-height: 1;
  color: $rw-red;
  transform: rotate(9deg);
  text-shadow: 2rpx 2rpx 0 rgba(0, 0, 0, 0.65);
}
</style>
