<script setup>
// 根组件：按路由 meta 决定 NavBar 显隐（login 页隐藏，见 4.2）
// 底部跑马灯（S2 签名元素）：五个内页统一，由路由 meta.marquee 标记
// （AI 答疑页桌面端高度锁死、页面级禁止滚动，不参与跑马灯）
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/layout/NavBar.vue'
import MarqueeBar from './components/common/MarqueeBar.vue'

const route = useRoute()
const showMarquee = computed(() => Boolean(route.meta.marquee))
</script>

<template>
  <NavBar v-if="!$route.meta.hideNav" />
  <main
    class="app-main"
    :class="{
      'app-main--flat': $route.meta.hideNav,
      'app-main--full': $route.meta.fullBleed,
      'app-main--marquee': showMarquee,
    }"
  >
    <router-view />
  </main>
  <MarqueeBar v-if="showMarquee" />
</template>

<style scoped>
.app-main {
  max-width: var(--page-max);
  margin: 0 auto;
  padding: var(--sp-7) var(--sp-5) var(--sp-6);
  /* NavBar 为固定定位，主内容区下移一个导航高度 */
  padding-top: calc(var(--nav-h) + var(--sp-6));
}

.app-main--flat {
  padding-top: var(--sp-7);
}

/* 全出血页（首页 Hero）：去掉通用内边距，Hero 自 100vh 从页面顶开始 */
.app-main--full {
  max-width: none;
  padding: 0;
}

/* X-7：跑马灯页底部让位——跑马灯固定吸附视口底（28px 高），
   底部 padding 抬到 60px = 28px 跑马灯 + ≥24px 间距 + 余量，
   合计条/卡片永不被压住 */
.app-main--marquee {
  padding-bottom: 60px;
}
</style>
