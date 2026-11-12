<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api, { setSession } from '../api'
const router = useRouter()
const username = ref(''), password = ref(''), error = ref(''), busy = ref(false)
async function submit() {
  busy.value = true; error.value = ''
  try { setSession(await api.post('/auth/login', { username: username.value, password: password.value })); password.value = ''; router.push('/articles') }
  catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>
<template><div class="login"><form class="panel login-panel" @submit.prevent="submit">
  <div class="brand">RUN<span>WISE</span></div><p class="eyebrow">ADMIN ACCESS</p><h1>内容管理</h1><p class="muted">使用独立管理员账号登录。</p>
  <label>管理员账号<input v-model="username" required maxlength="64" autocomplete="username"></label>
  <label>密码<input v-model="password" required maxlength="128" type="password" autocomplete="current-password"></label>
  <p v-if="error" class="error" role="alert">{{ error }}</p><button class="primary" :disabled="busy">{{ busy ? '正在验证…' : '登录 →' }}</button>
</form></div></template>
