<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import api, { statusLabel } from '../api'
const route=useRoute(),router=useRouter(), busy=ref(false), loaded=ref(false), error=ref(''), message=ref(''), preview=ref(false), saved=ref('')
const article=reactive({ title:'',category:'knee',summary:'',tags:'',readMinutes:5,sort:0,sections:[{heading:'',body:''}],revision:0,status:'DRAFT' })
const isNew=computed(()=>route.params.id===undefined)
const dirty=computed(()=>loaded.value && JSON.stringify(article)!==saved.value)
function apply(data) { Object.assign(article,data,{sections:typeof data.sections==='string'?JSON.parse(data.sections):data.sections}); saved.value=JSON.stringify(article); loaded.value=true }
async function load() {
  if(isNew.value) { saved.value=JSON.stringify(article); loaded.value=true; return }
  busy.value=true
  try { apply(await api.get(`/articles/${route.params.id}`)) } catch(e) { error.value=e.message } finally {busy.value=false}
}
async function save() {
  busy.value=true;error.value='';message.value=''
  try {
    const data=await (isNew.value?api.post('/articles',article):api.put(`/articles/${route.params.id}`,article))
    apply(data);message.value='草稿已保存。确认内容后再发布。'
    if(isNew.value) await router.replace(`/articles/${data.id}`)
  } catch(e) {error.value=e.message} finally {busy.value=false}
}
async function changeStatus(status) {
  if(!window.confirm(status==='PUBLISHED'?'确认发布？内容将对 Web 和小程序用户可见。':'确认下架？用户端将无法查看此文章。')) return
  busy.value=true;error.value='';message.value=''
  try {apply(await api.put(`/articles/${route.params.id}/status`,{status,revision:article.revision}));message.value=status==='PUBLISHED'?'文章已发布。':'文章已下架。'}
  catch(e) {error.value=e.message} finally {busy.value=false}
}
onBeforeRouteLeave(()=>!dirty.value || window.confirm('存在未保存的修改，确认离开？'))
onMounted(load)
</script>
<template>
  <header><p class="eyebrow">RUNWISE / ARTICLE EDITOR</p><h1>{{ isNew?'新建文章':'文章编辑' }}</h1><RouterLink to="/articles">← 返回文章档案</RouterLink></header>
  <p v-if="error" class="error" role="alert">{{ error }} <button v-if="!loaded" @click="load">重新加载</button></p><p v-if="message" class="notice" role="status">{{ message }}</p>
  <form v-if="loaded" class="panel editor" @submit.prevent="save">
    <div class="toolbar"><span class="status" :class="article.status">{{ statusLabel(article.status) }} · 版本 {{ article.revision }}</span><button type="button" @click="preview=!preview">{{ preview?'返回编辑':'预览正文' }}</button></div>
    <div v-if="preview" class="preview"><h2>{{ article.title || '未命名文章' }}</h2><p class="muted">{{ article.summary }}</p><p>{{ article.tags }}</p><section v-for="(section,index) in article.sections" :key="index"><h3>{{ section.heading }}</h3><p>{{ section.body }}</p></section></div>
    <fieldset v-else :disabled="busy">
      <label>标题<input v-model="article.title" required maxlength="128"></label>
      <div class="form-grid"><label>部位<select v-model="article.category"><option value="knee">膝部</option><option value="ankle">足踝</option><option value="shin">小腿</option></select></label><label>阅读时长（分钟）<input v-model.number="article.readMinutes" type="number" required min="1" max="120"></label><label>排序（越小越靠前）<input v-model.number="article.sort" type="number" required min="0" max="100000"></label></div>
      <label>简介<textarea v-model="article.summary" maxlength="255" rows="2" required></textarea></label><label>关键词（英文逗号分隔）<input v-model="article.tags" maxlength="255"></label>
      <div v-for="(section,index) in article.sections" :key="index" class="section-editor"><div class="toolbar"><h3>正文 / {{ String(index+1).padStart(2,'0') }}</h3><button type="button" :disabled="article.sections.length===1" @click="article.sections.splice(index,1)">移除此节</button></div><label>小节标题<input v-model="section.heading" required maxlength="128"></label><label>正文<textarea v-model="section.body" required maxlength="8000" rows="6"></textarea></label></div>
      <button type="button" :disabled="article.sections.length>=20" @click="article.sections.push({heading:'',body:''})">+ 添加小节</button>
    </fieldset>
    <div class="editor-actions"><button class="primary" :disabled="busy || preview">{{ busy?'正在处理…':'保存为草稿' }}</button><button v-if="!isNew && article.status!=='PUBLISHED'" type="button" :disabled="busy || dirty" @click="changeStatus('PUBLISHED')">确认发布 →</button><button v-if="!isNew && article.status==='PUBLISHED'" type="button" :disabled="busy || dirty" @click="changeStatus('OFFLINE')">下架文章</button></div>
    <p class="muted">已发布文章保存后会转为草稿；未保存的修改不能发布。预览为后台内容预览，用户端继续沿用原有排版。</p>
  </form>
</template>
