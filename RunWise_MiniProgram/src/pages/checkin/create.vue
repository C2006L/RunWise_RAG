<template>
  <view class="page">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="header-title">新建打卡</text>
      <text class="header-sub">记录今天的跑步训练</text>
    </view>

    <!-- 表单卡片 -->
    <view class="form-card">
      <!-- 日期 -->
      <view class="form-item">
        <text class="form-label">日期</text>
        <picker mode="date" :value="form.date" @change="onDateChange">
          <view class="form-picker">
            <text class="picker-text">{{ form.date }}</text>
            <text class="picker-arrow"></text>
          </view>
        </picker>
      </view>

      <!-- 距离 -->
      <view class="form-item">
        <text class="form-label">距离 (km)</text>
        <input
          class="form-input"
          type="digit"
          v-model="form.distance"
          placeholder="如 5.2"
          placeholder-class="input-placeholder"
        />
      </view>

      <!-- 时长 -->
      <view class="form-item">
        <text class="form-label">时长 (分钟)</text>
        <input
          class="form-input"
          type="number"
          v-model="form.duration"
          placeholder="如 32"
          placeholder-class="input-placeholder"
        />
      </view>

      <!-- 心情 -->
      <view class="form-item">
        <text class="form-label">心情</text>
        <view class="mood-row">
          <view
            v-for="mood in moods"
            :key="mood.value"
            class="mood-tag"
            :class="[moodClass(mood.value), { 'mood-tag--active': form.mood === mood.value }]"
            @tap="form.mood = mood.value"
          >
            <text class="mood-tag-text">{{ mood.label }}</text>
          </view>
        </view>
      </view>

      <!-- 备注 -->
      <view class="form-item">
        <text class="form-label">备注</text>
        <textarea
          class="form-textarea"
          v-model="form.remark"
          placeholder="今天的感觉如何？"
          placeholder-class="input-placeholder"
          maxlength="200"
        />
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-btn" hover-class="submit-btn--pressed" @click="onSubmit">
      <text class="submit-text">提交打卡</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const today = new Date()
const pad = (n: number) => String(n).padStart(2, '0')
const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`

const form = ref({
  date: todayStr,
  distance: '',
  duration: '',
  mood: '轻松',
  remark: ''
})

const moods = [
  { value: '轻松', label: '轻松' },
  { value: '适中', label: '适中' },
  { value: '吃力', label: '吃力' }
]

function moodClass(mood: string): string {
  if (mood === '轻松') return 'mood-easy'
  if (mood === '适中') return 'mood-moderate'
  return 'mood-hard'
}

function onDateChange(e: any) {
  form.value.date = e.detail.value
}

function onSubmit() {
  if (!form.value.distance || !form.value.duration) {
    uni.showToast({ title: '请填写距离和时长', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({ title: '打卡成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }, 800)
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #F8FAFC 0%, #E6F7FF 100%);
  padding: 0 32rpx 48rpx;
  box-sizing: border-box;
}

/* ========== 顶部标题 ========== */
.header {
  padding: 24rpx 0 32rpx;
}

.header-title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: $rw-text-primary;
}

.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $rw-text-secondary;
}

/* ========== 表单卡片 ========== */
.form-card {
  background: $rw-card-bg;
  border: $rw-card-border;
  border-radius: $rw-card-radius;
  box-shadow: $rw-shadow-card;
  padding: 16rpx 32rpx;
  transition: $rw-transition-bounce;
}

.form-item {
  padding: 24rpx 0;
  border-bottom: 2rpx solid rgba(0, 0, 0, 0.04);

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: $rw-text-primary;
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  background-color: $rw-bg-page;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: $rw-text-primary;
  box-sizing: border-box;
  border: 2rpx solid transparent;
  transition: $rw-transition-bounce;

  &:focus {
    border-color: $rw-secondary;
    box-shadow: 0 0 0 6rpx rgba(24, 144, 255, 0.1);
  }
}

.form-textarea {
  width: 100%;
  min-height: 120rpx;
  padding: 20rpx 24rpx;
  background-color: $rw-bg-page;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: $rw-text-primary;
  box-sizing: border-box;
}

.input-placeholder {
  color: $rw-text-placeholder;
  font-size: 28rpx;
}

.form-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 24rpx;
  background-color: $rw-bg-page;
  border-radius: 16rpx;
}

.picker-text {
  font-size: 28rpx;
  color: $rw-text-primary;
}

.picker-arrow {
  flex-shrink: 0;
  width: 14rpx;
  height: 14rpx;
  border-right: 2rpx solid #C0C4CC;
  border-bottom: 2rpx solid #C0C4CC;
  transform: rotate(-45deg);
}

/* ========== 心情标签 ========== */
.mood-row {
  display: flex;
  gap: 16rpx;
}

.mood-tag {
  padding: 12rpx 32rpx;
  border-radius: 24rpx;
  border: 2rpx solid;
  transition: $rw-transition-bounce;

  &.mood-easy {
    border-color: $rw-mood-easy-border;
    .mood-tag-text { color: $rw-text-secondary; }
  }

  &.mood-moderate {
    border-color: $rw-mood-moderate-border;
    .mood-tag-text { color: $rw-text-secondary; }
  }

  &.mood-hard {
    border-color: $rw-mood-hard-border;
    .mood-tag-text { color: $rw-text-secondary; }
  }

  &.mood-tag--active.mood-easy {
    background-color: $rw-mood-easy;
    border-color: $rw-mood-easy;
    .mood-tag-text { color: #FFFFFF; }
  }

  &.mood-tag--active.mood-moderate {
    background-color: $rw-mood-moderate;
    border-color: $rw-mood-moderate;
    .mood-tag-text { color: #FFFFFF; }
  }

  &.mood-tag--active.mood-hard {
    background-color: $rw-mood-hard;
    border-color: $rw-mood-hard;
    .mood-tag-text { color: #1F2937; }
  }
}

.mood-tag-text {
  font-size: 24rpx;
}

/* ========== 提交按钮 ========== */
.submit-btn {
  margin-top: 48rpx;
  width: 240rpx;
  height: 88rpx;
  @include rw-primary-btn;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 500;
}

.submit-btn--pressed {
  transform: scale(0.95);
  background: $rw-gradient-primary-pressed;
  box-shadow: $rw-shadow-btn-pressed;
}

.submit-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #FFFFFF;
}
</style>
