<script setup>
import {ref,reactive} from 'vue'
import * as api from '../../api/qa'
defineProps({conversationId:{type:String,default:''},disabled:Boolean})
const emit=defineEmits(['generated','busy'])
const open=ref(false),context=ref(null),busy=ref(false),error=ref(''),questions=ref([])
const dateKey=date=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
const tomorrow=new Date();tomorrow.setDate(tomorrow.getDate()+1)
const latest=new Date();latest.setDate(latest.getDate()-((latest.getDay()+6)%7)+21)
const form=reactive({goal:'maintain',startDate:dateKey(tomorrow),sessionsPerWeek:3,noDiscomfortConfirmed:false,constraints:''})
async function show(){open.value=true;error.value='';try{context.value=await api.getTrainingContext()}catch(e){error.value=e.response?.data?.message||e.message}}
async function generate(conversationId){
  if(busy.value)return;busy.value=true;error.value='';questions.value=[];emit('busy',true)
  try{
    const result=await api.generatePlanDraft({...form,conversationId:conversationId||undefined})
    if(result.status==='NEEDS_INPUT'){questions.value=result.questions;context.value=result.trainingContext;return}
    emit('generated',result);open.value=false
  }catch(e){error.value=e.response?.data?.message||e.message||'生成失败，未保存课表'}
  finally{busy.value=false;emit('busy',false)}
}
</script>
<template>
  <button class="plan-entry" type="button" :disabled="disabled" @click="show">◆ 根据训练记录制定计划</button>
  <Teleport to="body"><div v-if="open" class="plan-mask" @click.self="!busy&&(open=false)"><form class="plan-dialog" role="dialog" aria-modal="true" aria-labelledby="plan-dialog-title" @submit.prevent="generate(conversationId)" @keydown.esc="!busy&&(open=false)">
    <p class="plan-kicker">RUNWISE / PLAN DRAFT</p><h2 id="plan-dialog-title">制定未来 7 天计划</h2>
    <p v-if="context" class="context-note">参考 {{context.periodStart}}—{{context.asOf}}：{{context.recordCount}} 次训练，{{context.totalKm}} km。周均 {{context.weeklyAverageKm}} km；单次最长 {{context.longestRunKm}} km。</p>
    <p class="context-note">这是基于已记录训练的保守草稿，生成不会更改现有课表。</p>
    <div class="plan-fields"><label>训练目标<select v-model="form.goal"><option value="maintain">保持训练习惯</option><option value="5k">5K 基础训练</option><option value="10k">10K 基础训练</option><option value="fitness">体能提升</option></select></label><label>开始日期<input v-model="form.startDate" type="date" :min="dateKey(tomorrow)" :max="dateKey(latest)" required></label><label>每周最多可跑<input v-model.number="form.sessionsPerWeek" type="number" min="1" max="6" required>次</label></div>
    <label>补充要求<textarea v-model="form.constraints" maxlength="500" rows="3" placeholder="例如：周三没有时间，只想安排轻松跑"></textarea></label>
    <label class="confirm-health"><input v-model="form.noDiscomfortConfirmed" type="checkbox">我确认近期没有疼痛或明显不适；如有不适，应先向助手说明。</label>
    <ul v-if="questions.length" class="plan-error"><li v-for="q in questions" :key="q">{{q}}</li></ul><p v-if="error" role="alert" class="plan-error">{{error}}</p>
    <div class="plan-actions"><button type="button" :disabled="busy" @click="open=false">取消</button><button type="submit" class="primary" :disabled="busy">{{busy?'正在分析记录与生成…':'生成草稿 →'}}</button></div>
  </form></div></Teleport>
</template>
<style scoped>
.plan-entry{background:transparent;border:1px solid #dedbd4;color:#f3efe7;padding:8px 12px;margin-bottom:10px;text-align:left;cursor:pointer}.plan-entry:hover{border-color:#f20d1a}.plan-mask{position:fixed;inset:0;z-index:1000;background:#000b;display:grid;place-items:center;padding:20px}.plan-dialog{background:#151518;color:#f5f5f5;border:1px solid #eee;border-top:4px solid #f20d1a;padding:26px;width:min(620px,100%);max-height:88vh;overflow-y:auto;font-family:inherit}.plan-kicker{color:#f20d1a;font-size:12px;letter-spacing:2px}.plan-dialog h2{margin:12px 0}.context-note{font-size:13px;color:#c1bec6;line-height:1.8;margin:10px 0}.plan-fields{display:grid;grid-template-columns:1fr 1fr 1fr;gap:14px;margin:20px 0}.plan-dialog label{display:block;font-size:14px;color:#dfdce2}.plan-dialog input:not([type=checkbox]),.plan-dialog select,.plan-dialog textarea{width:100%;box-sizing:border-box;background:#080808;border:1px solid #aaa;color:#fff;padding:10px;margin-top:8px;font:inherit;border-radius:0}.confirm-health{display:flex!important;gap:10px;align-items:flex-start;margin-top:20px;line-height:1.7}.confirm-health input{flex:none;margin-top:5px}.plan-actions{display:flex;gap:14px;justify-content:flex-end;margin-top:22px}.plan-actions button{border:1px solid #bbb;color:#fff;padding:10px 18px;background:#252528;cursor:pointer}.plan-actions .primary{background:#f20d1a;border-color:#f20d1a}.plan-error{color:#ff9c9c;line-height:1.8;margin-top:15px}:focus-visible{outline:2px solid #fff;outline-offset:3px}button:disabled{opacity:.5;cursor:wait}@media(max-width:600px){.plan-fields{grid-template-columns:1fr}}
</style>
