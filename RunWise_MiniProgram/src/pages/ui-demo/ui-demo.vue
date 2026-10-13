<template>
  <view class="demo-page">
    <!-- 页头（红斜杠签名元素） -->
    <view class="demo-header rw-enter rw-enter-1">
      <text class="demo-kicker">RUNWISE MP · P5R</text>
      <text class="demo-title">RWUI 组件预览</text>
      <text class="demo-sub">P0 令牌与组件库验收页（P1 逐页改造后删除）</text>
    </view>

    <!-- RwCard 默认（无影） -->
    <view class="demo-block rw-enter rw-enter-2">
      <rw-card tag="PLAIN CARD" :accent="false" :padding="'28rpx'">
        <text class="demo-card-title">默认卡</text>
        <text class="demo-card-text">面板黑底 · 左上/右下斜切 · 米色胶带（撕口锯齿）</text>
      </rw-card>
    </view>

    <!-- RwCard 红框工艺 -->
    <view class="demo-block rw-enter rw-enter-3">
      <view class="demo-rel">
        <rw-card frame="red" tag="CHECK-IN" tag-variant="red" :padding="'28rpx'">
          <text class="demo-card-title">红框工艺卡</text>
          <text class="demo-card-data">6.3km · 36min · 5'43"/km</text>
        </rw-card>
        <rw-number-bib value="28.5" unit="KM" style="top: -18rpx; right: 16rpx" />
      </view>
    </view>

    <!-- RwButton 三态 -->
    <view class="demo-block rw-enter rw-enter-4">
      <rw-card tag="BUTTONS" :padding="'28rpx'">
        <view class="demo-row">
          <rw-button text="主按钮 →" @tap="toast('主按钮')" />
          <rw-button text="次按钮" variant="ghost" @tap="toast('次按钮')" />
        </view>
        <view class="demo-row demo-row--mt">
          <rw-button text="小按钮" size="sm" @tap="toast('小按钮')" />
          <rw-button text="禁用态" disabled />
          <rw-button text="加载中…" loading block="false" @tap="toast('x')" />
        </view>
      </rw-card>
    </view>

    <!-- RwInput -->
    <view class="demo-block rw-enter rw-enter-5">
      <rw-card tag="INPUTS" :padding="'28rpx'">
        <rw-input v-model="text1" placeholder="默认输入框（聚焦描边变红）" />
        <view style="height: 24rpx"></view>
        <rw-input v-model="text2" placeholder="错误态：距离需为数字" error="请输入有效距离" />
      </rw-card>
    </view>

    <!-- RwTag 心情/类型定色 -->
    <view class="demo-block">
      <rw-card tag="TAGS" :padding="'28rpx'">
        <view class="demo-row">
          <rw-tag text="吃力" variant="red" />
          <rw-tag text="适中" variant="white" />
          <rw-tag text="轻松" variant="gray" />
          <rw-tag text="长距离" variant="outline" />
        </view>
      </rw-card>
    </view>

    <!-- RwEmpty + RwSkeleton + 黄框 -->
    <view class="demo-block">
      <rw-card frame="yellow" tag="EMPTY / SKELETON" tag-variant="yellow" :padding="'28rpx'">
        <rw-skeleton height="36rpx" width="60%" />
        <view style="height: 20rpx"></view>
        <rw-skeleton height="36rpx" width="40%" />
        <rw-empty text="这一天还没有记录" action="去首页打卡 →" @action="toast('回首页')" />
      </rw-card>
    </view>

    <!-- RwMarquee + 炸星 -->
    <view class="demo-block">
      <view class="demo-rel">
        <rw-star-field tier="M" color="red" :duration="5.2" :delay="0" top="-30rpx" right="8%" />
        <rw-card :padding="'28rpx'">
          <text class="demo-card-title">跑马灯 + 炸星（M 档红）</text>
        </rw-card>
      </view>
      <view style="height: 24rpx"></view>
      <rw-marquee />
    </view>

    <view style="height: 160rpx"></view>

    <!-- Toast 挂载点 -->
    <rw-toast ref="toastRef" />
  </view>
</template>

<script setup lang="ts">
/**
 * RWUI 组件预览页（P0 验收用临时页，P1 逐页改造完成后删除）
 * 验收对照：PRD 13.2.1 色值表 / 13.2.3 组件规格表 / Web 端截图并排比对
 */
import { ref } from 'vue';

const text1 = ref('');
const text2 = ref('abc');
const toastRef = ref<any>(null);

const toast = (msg: string) => {
  toastRef.value?.show(msg);
};
</script>

<style lang="scss" scoped>
.demo-page {
  min-height: 100vh;
  background: $rw-black;
  padding: 32rpx;
  box-sizing: border-box;
}

.demo-header {
  @include rw-header-slash;
  margin-bottom: 40rpx;
}

.demo-kicker {
  display: block;
  font-family: $rw-font-display;
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: $rw-red;
}

.demo-title {
  display: block;
  font-family: $rw-font-cn-title;
  font-size: 56rpx;
  font-weight: 400;
  color: $rw-white;
  line-height: 1.2;
  margin: 8rpx 0;
}

.demo-sub {
  display: block;
  font-size: 24rpx;
  color: $rw-gray;
}

.demo-block {
  margin-bottom: 48rpx;
}

.demo-rel {
  position: relative;
}

.demo-card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: $rw-white;
  margin-bottom: 8rpx;
}

.demo-card-text {
  display: block;
  font-size: 26rpx;
  color: $rw-gray;
  line-height: 1.7;
}

.demo-card-data {
  display: block;
  font-family: $rw-font-cn-data;
  font-size: 38rpx;
  color: $rw-white;
}

.demo-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  flex-wrap: wrap;
}

.demo-row--mt {
  margin-top: 24rpx;
}
</style>
