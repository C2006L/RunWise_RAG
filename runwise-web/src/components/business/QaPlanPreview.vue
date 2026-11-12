<script setup>
import {ref,onMounted} from 'vue'
import * as api from '../../api/qa'
import {planMarkdown} from '../../../../shared/plan-format.mjs'
const props=defineProps({id:String,initial:Object})
const draft=ref(props.initial||null),busy=ref(false),error=ref('')
const copied=ref('')
async function copy(){try{await navigator.clipboard.writeText(planMarkdown(draft.value));copied.value='已复制完整计划'}catch{copied.value='复制失败，请手动选择'}}
const labels={easy:'轻松跑',tempo:'节奏跑',interval:'间歇跑',long:'长距离',rest:'休息'}
async function load(){error.value='';try{draft.value=await api.getPlanDraft(props.id)}catch(e){error.value=e.response?.data?.message||'草稿暂时无法读取'}}
onMounted(()=>{if(!draft.value)load()})
async function confirm(){
  if(busy.value||!draft.value)return
  if(!window.confirm('确认将这7天草稿加入训练计划？已有日期的课表不会被覆盖。'))return
  busy.value=true;error.value=''
  try{draft.value=await api.confirmPlanDraft(props.id)}catch(e){error.value=e.response?.data?.message||'保存失败，未覆盖任何已有课表'}finally{busy.value=false}
}
</script>
<template><section class="plan-preview" aria-label="AI计划草稿">
  <header><strong>7 天计划 · {{draft?.status==='APPLIED'?'已加入课表':'待确认草稿'}}</strong></header>
  <template v-if="draft"><p class="data-note">依据 {{draft.trainingContext.periodStart}}—{{draft.trainingContext.asOf}} 的 {{draft.trainingContext.recordCount}} 次真实记录。{{draft.plan.model?'模型：'+draft.plan.model:''}}</p>
  <div class="schedule-scroll" tabindex="0"><table><thead><tr><th>日期</th><th>训练</th><th>距离</th><th>说明</th></tr></thead><tbody><tr v-for="day in draft.plan.days" :key="day.date"><td>{{day.date}}</td><td>{{labels[day.type]}}</td><td>{{day.targetKm}} km</td><td>{{day.note}}</td></tr></tbody></table></div>
  <p class="data-note">基于记录的保守建议，不代表健康评估。请核对安排后确认；调整要求可重新生成。</p>
  <button v-if="draft.status!=='APPLIED'" type="button" :disabled="busy" @click="confirm">{{busy?'正在保存…':'确认加入训练计划 →'}}</button><RouterLink v-else to="/plan">查看训练计划 →</RouterLink>
  <button type="button" @click="copy">{{copied||'复制完整计划'}}</button>
  </template><p v-else>正在读取草稿…</p><p v-if="error" class="plan-error" role="alert">{{error}} <button v-if="!draft" @click="load">重试</button></p>
</section></template>
<style scoped>.plan-preview{margin-top:16px;border-top:2px solid #f20d1a;padding-top:14px;min-width:0;max-width:100%}.plan-preview header{color:#fff}.data-note{font-size:12px;color:#bfbcc5;line-height:1.8;margin:10px 0}.schedule-scroll{overflow-x:auto;max-width:100%}table{min-width:440px;width:100%;border-collapse:collapse;font-size:13px}td,th{padding:10px 8px;border:1px solid #55535b;text-align:left;line-height:1.7}th{background:#252528}button{margin-top:10px;background:#f20d1a;color:#fff;border:1px solid #f20d1a;padding:9px 14px;cursor:pointer}button:disabled{opacity:.5}.plan-error{color:#ff9c9c;line-height:1.8}a{color:#f7f4eb;text-decoration:underline}:focus-visible{outline:2px solid #fff;outline-offset:3px}</style>
