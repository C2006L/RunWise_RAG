<script setup>
// 错位投影标题（工程计划 4.1 / M6）：纯色实心红色错位层，禁用模糊与半透明残影
// - text：标题文本；level：渲染标签（h1 / h2）
// - size：'hero' 首页 RUN WISE 海报级 | 'page' 内容页大标题级
// 样式收敛在全局 p5-effects.css（.p5-glitch--*），业务层零写死
defineProps({
  text: {
    type: String,
    required: true,
  },
  level: {
    type: String,
    default: 'h1',
  },
  size: {
    type: String,
    default: 'page',
  },
});
</script>

<template>
  <component :is="level" class="p5-glitch" :class="`p5-glitch--${size}`">
    {{ text }}
  </component>
</template>
