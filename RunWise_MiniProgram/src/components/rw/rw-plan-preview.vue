<script setup lang="ts">
import {ref,onMounted} from 'vue';
import {request} from '@/utils/request';
import {planMarkdown} from '../../../../shared/plan-format.mjs';
const props=defineProps<{id:string}>();
const draft=ref<any>(null),busy=ref(false),error=ref('');
const labels:Record<string,string>={easy:'轻松跑',tempo:'节奏跑',interval:'间歇跑',long:'长距离',rest:'休息'};
async function load(){try{draft.value=await request(`/api/qa/plan-drafts/${props.id}`);}catch(e:any){error.value=e.message||'草稿读取失败';}}
onMounted(load);
function copy(){if(draft.value)uni.setClipboardData({data:planMarkdown(draft.value)});}
async function confirm(){
  if(busy.value)return;
  const result=await uni.showModal({title:'确认加入训练计划',content:'已有日期的课表不会被覆盖。是否保存这7天计划？'});
  if(!result.confirm)return;
  busy.value=true;error.value='';
  try{draft.value=await request(`/api/qa/plan-drafts/${props.id}/confirm`,'POST');}catch(e:any){error.value=e.message||'保存失败';}finally{busy.value=false;}
}
</script>
<template><view class="plan-preview"><text class="plan-title">7 天计划 · {{draft?.status==='APPLIED'?'已加入课表':'待确认草稿'}}</text>
  <template v-if="draft"><text class="plan-note">依据 {{draft.trainingContext.recordCount}} 次真实记录。生成内容需核对，不能代替健康评估。</text><scroll-view scroll-x><view class="plan-table"><view class="plan-row header"><text>日期</text><text>训练</text><text>距离</text><text>说明</text></view><view v-for="day in draft.plan.days" :key="day.date" class="plan-row"><text>{{day.date}}</text><text>{{labels[day.type]}}</text><text>{{day.targetKm}} km</text><text>{{day.note}}</text></view></view></scroll-view>
  <button v-if="draft.status!=='APPLIED'" :disabled="busy" @tap="confirm">{{busy?'正在保存…':'确认加入训练计划 →'}}</button><text v-else class="plan-note">已保存，可在 Web 的训练计划页查看和编辑。</text><button @tap="copy">复制完整计划</button></template>
  <text v-else class="plan-note">正在读取草稿…</text><text v-if="error" class="plan-error">{{error}}</text>
</view></template>
<style scoped>.plan-preview{max-width:100%;min-width:0;border-top:3px solid #f20d1a;margin-top:24rpx;padding-top:20rpx}.plan-title{display:block;color:#fff;font-weight:700;font-size:29rpx}.plan-note,.plan-error{display:block;margin:16rpx 0;font-size:24rpx;color:#bdbac3;line-height:1.8}.plan-error{color:#ff969d}.plan-table{min-width:700rpx;font-size:24rpx}.plan-row{display:grid;grid-template-columns:170rpx 100rpx 100rpx minmax(260rpx,1fr)}.plan-row text{padding:14rpx;border:1px solid #56535d;line-height:1.8;box-sizing:border-box}.header{background:#28282b}button{background:#f20d1a;color:#fff;border-radius:0;font-size:26rpx;margin-top:20rpx}</style>
