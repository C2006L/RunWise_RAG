<script setup>
// 底部跑马灯（签名元素 S2 · 五个内页统一）：白底黑字横向滚动条
// 规格：高 28px · 13px Anton · 6px 白/黑斜纹分隔符 · 30s 一循环 · reduced-motion 静止
// 挂载于 App.vue（main 之后，路由 meta.marquee 控制）
// 20260907：改为固定吸附视口底部 + 像首页飘带一样微倾斜（-1.8°），
// 任何时刻都不需下滑即可见到（原先贴文档流末尾，页面高度使之下沉到折叠线下方）
const PHRASES = ['RUN WISE', 'KEEP RUNNING', "DON'T STOP", 'EVERY STEP COUNTS']
</script>

<template>
  <div class="marquee" aria-hidden="true">
    <div class="marquee-track">
      <!-- 4 份相同 seg：平移 -25% 恰好一份宽，30s 无缝循环 -->
      <span v-for="i in 4" :key="i" class="marquee-seg">
        <template v-for="(p, j) in PHRASES" :key="j">
          <span class="seg-phrase">{{ p }}</span>
          <i class="seg-sep"></i>
        </template>
      </span>
    </div>
  </div>
</template>

<style scoped>
.marquee {
  /* 20260907：固定吸附视口底部，无需下滑即见（首页飘带同款微倾斜 -1.8°） */
  position: fixed;
  left: -6%;
  right: -6%;
  bottom: -1px;
  height: 28px;
  background: var(--p5-white);
  overflow: hidden;
  transform: rotate(-1.8deg);
  z-index: 30;
  pointer-events: none; /* 纯装饰，不拦截底部内容的点击 */
}

.marquee-track {
  display: flex;
  width: max-content;
  height: 100%;
  animation: marquee-scroll 30s linear infinite;
}

.marquee-seg {
  display: flex;
  align-items: center;
  white-space: nowrap;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 4px;
  color: var(--p5-black);
}

.seg-phrase {
  padding: 0 16px;
}

/* 6px 白/黑斜纹分隔符（3px 黑 + 3px 白一周期，承接词间「·」位） */
.seg-sep {
  flex: none;
  width: 6px;
  height: 14px;
  background: repeating-linear-gradient(
    -45deg,
    var(--p5-black) 0 3px,
    var(--p5-white) 3px 6px
  );
}

@keyframes marquee-scroll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-25%);
  }
}

/* reduced-motion：静止（条仍在，仅不滚动） */
@media (prefers-reduced-motion: reduce) {
  .marquee-track {
    animation: none;
  }
}
</style>
