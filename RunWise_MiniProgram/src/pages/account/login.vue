<template>
  <view class="account-page">
    <text class="kicker">RUNWISE ACCOUNT</text><text class="title">{{binding?'绑定 Web 账号':'登录 RunWise'}}</text>
    <text class="description">{{binding?'共享账号保持同一用户身份，Web 与小程序读取同一份训练数据。':'已有 Web 账号请用同一账密登录，再绑定微信；已有微信训练记录请先微信登录，再设置共享账密。'}}</text>
    <text v-if="account" class="description">用户编号：{{account.userId}} · {{account.username?'已设置账号：'+account.username:'尚未设置账密'}} · {{account.wechatBound?'微信已绑定':'微信未绑定'}}</text>
    <view v-if="!binding || !account?.username">
    <input v-model="username" placeholder="用户名（3–32位）" maxlength="32" />
    <input v-model="password" placeholder="密码（至少12位）" password maxlength="128" />
    <text v-if="error" class="error">{{error}}</text>
    <button :disabled="busy" @tap="submit">{{busy?'正在处理…':binding?'确认绑定':'账号登录'}}</button>
    </view>
    <button v-if="binding && account && !account.wechatBound" :disabled="busy" @tap="bindWechat">绑定当前微信</button>
    <button v-if="!binding" :disabled="busy" @tap="wechat">微信登录</button>
  </view>
</template>
<script setup lang="ts">
import {ref} from 'vue';
import {onLoad} from '@dcloudio/uni-app';
import {request,saveTokens,wxLogin,BASE_URL} from '@/utils/request';
const username=ref(''),password=ref(''),error=ref(''),busy=ref(false),binding=ref(false);
const account=ref<{userId:number;username:string|null;wechatBound:boolean}|null>(null);
onLoad(async options=>{binding.value=options?.mode==='bind';if(binding.value)try{account.value=await request('/api/auth/account');}catch(e:any){error.value=e.message||'账号信息读取失败';}});
async function bindWechat(){
  if(busy.value)return;busy.value=true;error.value='';
  try{
    const answer=await new Promise<boolean>(resolve=>uni.showModal({title:'绑定微信',content:'将当前微信关联到此账号，保留当前用户编号和训练数据。已有其他微信账号数据时会拒绝自动合并。',success:r=>resolve(r.confirm),fail:()=>resolve(false)}));
    if(!answer)return;
    const {code}=await uni.login({provider:'weixin'});
    if(!code)throw new Error('无法获取微信登录凭证');
    await request('/api/auth/bind-wechat','POST',{code});
    account.value=await request('/api/auth/account');uni.showToast({title:'绑定成功',icon:'success'});
  }catch(e:any){error.value=e.message||'绑定失败';}finally{busy.value=false;}
}
function finish(){uni.switchTab({url:'/pages/mine/mine'});}
async function submit(){
  if(busy.value)return;
  if(!/^[A-Za-z0-9_.-]{3,32}$/.test(username.value)||password.value.length<12){error.value='用户名格式不正确或密码少于12位';return;}
  busy.value=true;error.value='';
  try{
    const payload={username:username.value,password:password.value};
    if(binding.value){await request('/api/auth/bind-password','POST',payload);}
    else{
      const response=await uni.request({url:BASE_URL+'/api/auth/login-password',method:'POST',data:payload,timeout:15000});
      const body=response.data as any;
      if(response.statusCode!==200||body.code!==200)throw new Error(body.message||'登录失败');
      saveTokens(body.data.accessToken,body.data.refreshToken);uni.setStorageSync('userId',body.data.userId);
    }
    password.value='';finish();
  }catch(e:any){error.value=e.message||'连接失败，请检查服务地址';}finally{busy.value=false;}
}
async function wechat(){busy.value=true;error.value='';try{if(await wxLogin())finish();else error.value='微信登录失败，请检查后端微信配置';}catch{error.value='微信登录失败';}finally{busy.value=false;}}
</script>
<style scoped>
.account-page{min-height:100vh;background:#080808;color:#fff;padding:60rpx 40rpx;box-sizing:border-box}.kicker{display:block;color:#f20d1a;font-size:24rpx;letter-spacing:3rpx}.title{display:block;font-size:48rpx;font-weight:800;margin:24rpx 0}.description{display:block;color:#b9b6bd;line-height:1.8;margin-bottom:40rpx}input{border:1px solid #ffffff80;background:#151518;color:#fff;margin:24rpx 0;padding:22rpx;height:55rpx}button{border-radius:0;background:#f20d1a;color:#fff;margin-top:25rpx;font-size:30rpx}.error{display:block;color:#ff9696}
</style>
