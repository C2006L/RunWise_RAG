<script setup lang="ts">
import {ref,reactive} from 'vue';
import {request} from '@/utils/request';
const props=defineProps<{conversationId?:string;disabled?:boolean}>();
const emit=defineEmits(['generated','busy']);
const open=ref(false),busy=ref(false),error=ref(''),context=ref<any>(null),questions=ref<string[]>([]);
const key=(d:Date)=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
const tomorrow=new Date();tomorrow.setDate(tomorrow.getDate()+1);
const latest=new Date();latest.setDate(latest.getDate()-((latest.getDay()+6)%7)+21);
const goals=[{key:'maintain',label:'保持训练习惯'},{key:'5k',label:'5K 基础训练'},{key:'10k',label:'10K 基础训练'},{key:'fitness',label:'体能提升'}];
const goalIndex=ref(0),sessions=ref(2);
const form=reactive({startDate:key(tomorrow),noDiscomfortConfirmed:false,constraints:''});
async function show(){if(props.disabled)return;open.value=!open.value;if(open.value)try{context.value=await request('/api/qa/training-context');}catch(e:any){error.value=e.message;}}
async function generate(){
  if(busy.value||props.disabled)return;busy.value=true;error.value='';questions.value=[];emit('busy',true);
  try{
    const result:any=await request('/api/qa/plan-drafts','POST',{...form,goal:goals[goalIndex.value].key,sessionsPerWeek:sessions.value+1,conversationId:props.conversationId||undefined});
    if(result.status==='NEEDS_INPUT'){questions.value=result.questions;return;}
    emit('generated',result);open.value=false;
  }catch(e:any){error.value=e.message||'生成失败，未保存任何课表';}finally{busy.value=false;emit('busy',false);}
}
</script>
<template><view class="plan-entry"><button :disabled="disabled" @tap="show">◆ 根据训练记录制定计划</button><view v-if="open" class="plan-form">
  <text class="plan-title">未来 7 天 · 先预览再保存</text><text v-if="context" class="plan-note">近28天 {{context.recordCount}} 次记录，{{context.totalKm}} km；周均 {{context.weeklyAverageKm}} km。</text>
  <picker :range="goals.map(g=>g.label)" :value="goalIndex" @change="goalIndex=Number($event.detail.value)"><view class="plan-field">目标：{{goals[goalIndex].label}} ›</view></picker>
  <picker mode="date" :value="form.startDate" :start="key(tomorrow)" :end="key(latest)" @change="form.startDate=$event.detail.value"><view class="plan-field">开始日期：{{form.startDate}} ›</view></picker>
  <picker :range="[1,2,3,4,5,6]" :value="sessions" @change="sessions=Number($event.detail.value)"><view class="plan-field">每周最多 {{sessions+1}} 次跑步 ›</view></picker>
  <textarea v-model="form.constraints" maxlength="500" placeholder="补充要求，例如周三没有时间" class="plan-field" />
  <checkbox-group @change="form.noDiscomfortConfirmed=$event.detail.value.length>0"><label class="plan-note"><checkbox value="confirmed" :checked="form.noDiscomfortConfirmed" color="#f20d1a" />我确认近期没有疼痛或明显不适。</label></checkbox-group>
  <text v-for="q in questions" :key="q" class="plan-error">{{q}}</text><text v-if="error" class="plan-error">{{error}}</text>
  <button :disabled="busy" @tap="generate">{{busy?'正在分析记录与生成…':'生成草稿 →'}}</button>
</view></view></template>
<style scoped>.plan-entry{margin:20rpx 24rpx}button{border-radius:0;background:#161619;color:#f5f1e8;border:1px solid #d8d2c7;font-size:26rpx;text-align:left;line-height:1.7;padding:16rpx 20rpx}.plan-form{padding:24rpx;background:#151518;border:1px solid #eee;border-top:3px solid #f20d1a}.plan-title,.plan-note,.plan-error{display:block;line-height:1.8;margin:12rpx 0}.plan-title{font-weight:700;color:#fff}.plan-note{font-size:24rpx;color:#c6c3cb}.plan-field{box-sizing:border-box;width:100%;border:1px solid #777;padding:16rpx;margin:16rpx 0;color:#eee;background:#080808;font-size:26rpx}.plan-error{color:#ff939b;font-size:25rpx}textarea{height:130rpx}.plan-form button{background:#f20d1a;border-color:#f20d1a;margin-top:20rpx}</style>
