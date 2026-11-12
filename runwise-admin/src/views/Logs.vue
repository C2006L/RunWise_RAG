<script setup>
import {ref,onMounted} from 'vue'
import api,{dateLabel} from '../api'
const rows=ref([]),total=ref(0),page=ref(1),error=ref(''),busy=ref(false)
const labels={BOOTSTRAP:'初始化管理员',ARTICLE_CREATE:'创建文章',ARTICLE_EDIT:'编辑草稿',ARTICLE_PUBLISH:'发布文章',ARTICLE_OFFLINE:'下架文章'}
async function load(delta=0) {page.value+=delta;busy.value=true;error.value='';try{const data=await api.get('/audit-logs',{params:{page:page.value,size:10}});rows.value=data.list;total.value=data.total}catch(e){error.value=e.message}finally{busy.value=false}}
onMounted(()=>load())
</script>
<template><header><p class="eyebrow">RUNWISE / AUDIT FILES</p><h1>操作档案</h1><p class="muted">文章变更与日志在同一数据库事务中保存。</p></header><section class="panel"><div class="toolbar"><h2>操作记录</h2><button :disabled="busy" @click="load()">刷新</button></div><p v-if="error" class="error" role="alert">{{error}}</p><div class="table-wrap"><table><thead><tr><th>时间</th><th>操作人</th><th>操作</th><th>目标文章</th><th>说明</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{dateLabel(row.create_time)}}</td><td>{{row.username}}</td><td>{{labels[row.action]||row.action}}</td><td>{{row.target_id||'—'}}</td><td>{{row.detail}}</td></tr><tr v-if="!rows.length"><td colspan="5">{{busy?'正在加载…':'暂无操作记录'}}</td></tr></tbody></table></div><footer class="pagination"><span>共 {{total}} 条 / 第 {{page}} 页</span><button :disabled="busy||page===1" @click="load(-1)">上一页</button><button :disabled="busy||page*10>=total" @click="load(1)">下一页</button></footer></section></template>
