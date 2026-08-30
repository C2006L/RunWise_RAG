<script setup>
// 根组件：按路由 meta 决定 NavBar 显隐（login 页隐藏，见 4.2）
import NavBar from './components/layout/NavBar.vue'
</script>

<template>
  <NavBar v-if="!$route.meta.hideNav" />
  <main class="app-main" :class="{ 'app-main--flat': $route.meta.hideNav }">
    <router-view />
  </main>
</template>

<style scoped>
.app-main {
  max-width: var(--page-max);
  margin: 0 auto;
  padding: var(--sp-7) var(--sp-5) var(--sp-8);
  /* NavBar 为固定定位，主内容区下移一个导航高度 */
  padding-top: calc(var(--nav-h) + var(--sp-6));
}

.app-main--flat {
  padding-top: var(--sp-7);
}
</style>
