<script setup>
import { ref, onMounted } from 'vue'
import api, { dateLabel, statusLabel } from '../api'
const keyword=ref(''), category=ref(''), status=ref(''), page=ref(1), rows=ref([]), total=ref(0), busy=ref(false), error=ref('')
async function load(reset=false) {
  if (reset) page.value=1
  busy.value=true; error.value=''
  try { const data=await api.get('/articles', { params: { keyword: keyword.value, category: category.value, status: status.value, page: page.value, size:10 } }); rows.value=data.list; total.value=data.total }
  catch(e) { error.value=e.message }
  finally { busy.value=false }
}
async function next(delta) { page.value+=delta; await load() }
onMounted(load)
</script>
<template>
  <header><p class="eyebrow">RUNWISE / CONTENT FILES</p><h1>伤病文章管理</h1><p class="muted">草稿 → 预览 → 发布。所有修改均保留操作档案。</p></header>
  <section class="panel">
    <div class="toolbar"><h2>文章档案 <small>{{ total }} 篇</small></h2><RouterLink class="button primary" to="/articles/new">+ 新建草稿</RouterLink></div>
    <form class="filters" @submit.prevent="load(true)">
      <label>标题关键词<input v-model="keyword" maxlength="128" placeholder="搜索文章"></label>
      <label>部位<select v-model="category"><option value="">全部部位</option><option value="knee">膝部</option><option value="ankle">足踝</option><option value="shin">小腿</option></select></label>
      <label>状态<select v-model="status"><option value="">全部状态</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下架</option></select></label>
      <button :disabled="busy">查询</button>
    </form>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="table-wrap" :aria-busy="busy"><table><thead><tr><th>文章</th><th>部位</th><th>状态</th><th>更新于</th><th>操作</th></tr></thead><tbody>
      <tr v-for="row in rows" :key="row.id"><td><strong>{{ row.title }}</strong><p class="muted">FILE {{ row.id }} · 版本 {{ row.revision }}</p></td><td>{{ {knee:'膝部',ankle:'足踝',shin:'小腿'}[row.category] }}</td><td><span class="status" :class="row.status">{{ statusLabel(row.status) }}</span></td><td>{{ dateLabel(row.updateTime) }}</td><td><RouterLink :to="`/articles/${row.id}`">编辑 / 预览 →</RouterLink></td></tr>
      <tr v-if="!rows.length"><td colspan="5">{{ busy ? '正在加载…' : error ? '加载失败，请重试查询。' : '当前条件下暂无文章。' }}</td></tr>
    </tbody></table></div>
    <footer class="pagination"><span>第 {{ page }} 页 / 共 {{ Math.max(1,Math.ceil(total/10)) }} 页</span><button :disabled="busy || page===1" @click="next(-1)">上一页</button><button :disabled="busy || page*10>=total" @click="next(1)">下一页</button></footer>
  </section>
</template>
