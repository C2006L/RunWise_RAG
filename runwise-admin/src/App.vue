<script setup>
import { useRouter } from 'vue-router'
import api, { session, clearSession } from './api'
import { ref } from 'vue'
const router = useRouter()
const error = ref('')
async function logout() {
  try { await api.post('/auth/logout'); clearSession(); router.push('/login') }
  catch (e) { error.value = e.message }
}
</script>
<template>
  <div v-if="$route.path !== '/login'" class="shell">
    <aside class="sidebar">
      <div class="brand">RUN<span>WISE</span></div><p class="eyebrow">CONTENT MANAGEMENT</p>
      <nav aria-label="后台导航"><RouterLink to="/articles">01 / 伤病文章</RouterLink><RouterLink v-if="session.account?.role === 'SUPER_ADMIN'" to="/logs">02 / 操作档案</RouterLink></nav>
      <div class="account"><strong>{{ session.account?.username }}</strong><p>{{ session.account?.role === 'SUPER_ADMIN' ? '超级管理员' : '内容运营' }}</p><button @click="logout">退出登录 →</button><p role="alert">{{ error }}</p></div>
    </aside>
    <main><RouterView :key="$route.fullPath" /></main>
  </div>
  <RouterView v-else />
</template>
