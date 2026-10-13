<template>
  <view v-if="visible" class="rw-toast" :class="'rw-toast--' + type">
    <text class="rw-toast__text">{{ message }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * RwToast —— 切角轻提示（红底米字，顶部居中；error 红 / success 深红）
 * 用法：const toast = ref(); toast.value.show('消息')；或全局 showRwToast()
 */
import { ref } from 'vue';

const visible = ref(false);
const message = ref('');
const type = ref<'error' | 'success'>('error');
let timer: ReturnType<typeof setTimeout> | null = null;

const show = (msg: string, t: 'error' | 'success' = 'error', duration = 2200) => {
  message.value = msg;
  type.value = t;
  visible.value = true;
  if (timer) clearTimeout(timer);
  timer = setTimeout(() => {
    visible.value = false;
  }, duration);
};

defineExpose({ show });
</script>

<style lang="scss" scoped>
.rw-toast {
  position: fixed;
  top: 120rpx;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9998;
  max-width: 560rpx;
  padding: 20rpx 40rpx;
  background: $rw-red;
  clip-path: polygon(16rpx 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
}

.rw-toast--success {
  background: $rw-red-dark;
}

.rw-toast__text {
  font-size: 26rpx;
  color: $rw-cream;
  line-height: 1.5;
}
</style>
