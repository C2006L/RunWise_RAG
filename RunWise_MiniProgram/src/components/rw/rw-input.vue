<template>
  <view class="rw-input" :class="{ 'rw-input--focus': focused, 'rw-input--error': error }">
    <input
      class="rw-input__el"
      :value="modelValue"
      :type="type"
      :password="password"
      :placeholder="placeholder"
      :placeholder-class="'rw-input__ph'"
      :maxlength="maxlength"
      :disabled="disabled"
      @input="onInput"
      @focus="focused = true"
      @blur="focused = false"
    />
    <text v-if="error" class="rw-input__err">{{ error }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * RwInput —— P5R 输入框（对齐 Web QA 页 .chat-input 工艺）
 * 黑底、2rpx 白线描边、无圆角、左上/右下切角；聚焦描边变红；错误态红描边+提示
 */
import { ref } from 'vue';

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  type: { type: String, default: 'text' },
  password: { type: Boolean, default: false },
  maxlength: { type: Number, default: 140 },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: '' },
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
}>();

const focused = ref(false);

const onInput = (e: any) => {
  emit('update:modelValue', e.detail.value);
};
</script>

<style lang="scss" scoped>
.rw-input {
  background: $rw-black;
  border: 2rpx solid $rw-line;
  padding: 20rpx 24rpx;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  clip-path: polygon(20rpx 0, 100% 0, 100% calc(100% - 20rpx), calc(100% - 20rpx) 100%, 0 100%, 0 20rpx);
}

.rw-input--focus {
  border-color: $rw-red; // 聚焦描边红（P5 合规补丁轮决议：蓝改红）
}

.rw-input--error {
  border-color: $rw-red;
}

.rw-input__el {
  width: 100%;
  font-size: 28rpx;
  color: $rw-white;
  line-height: 1.6;
}

.rw-input__ph {
  color: $rw-gray;
  font-size: 28rpx;
}

.rw-input__err {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $rw-red;
}
</style>
