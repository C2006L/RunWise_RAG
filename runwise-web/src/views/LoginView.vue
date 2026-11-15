<script setup>
import { reactive, ref } from "vue";
import P5Card from "../components/common/P5Card.vue";
import { useAuth } from "../composables/useAuth";
import {useRoute} from 'vue-router';
import {validateAccountForm} from '../../../shared/account-form.mjs';

// 登录页（M2 功能态，工程计划 4.2 / M2）：
// 本阶段仅功能与基础布局（表单 + 基础红黑配色），海报化视觉（红色斜切分屏、GlitchTitle）在 M7 统一实施
const { login, register } = useAuth();
const registering = ref(false);
const route=useRoute();

const form = reactive({ username: "", password: "", confirmPassword: "" });
const loading = ref(false);
const errorMsg = ref("");

function validate() {
  return validateAccountForm(form,registering.value);
}
function switchMode(){registering.value=!registering.value;form.password='';form.confirmPassword='';errorMsg.value='';}

async function handleSubmit() {
  if (loading.value) return;
  errorMsg.value = validate();
  if (errorMsg.value) return;
  loading.value = true;
  try {
    if(registering.value) await register(form.username, form.password,form.confirmPassword);
    else await login(form.username, form.password);
  } catch (err) {
    // mock 任意非空账密均成功；此分支兜底真实接口失败：
    // 「无账密」类错误 → 引导去小程序绑定；其余 → 提示重试（联调期生效）
    const msg =
      err?.response?.data?.message || err?.message || "登录失败，请稍后重试";
    errorMsg.value = msg;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="page">
    <P5Card tag="成员登录" tag-rotate="-5deg">
      <form class="login-card" @submit.prevent="handleSubmit">
        <p class="login-kicker">RUNWISE WEB</p>
        <h1 class="login-title">{{registering?'创建账号':'登录'}}</h1>
        <p class="login-desc">{{registering?'创建一个共享账号，即可在 Web 和小程序使用。':'使用共享账号登录，查看你的训练记录。'}}</p>
        <p v-if="route.query.reason==='unavailable'" class="login-error" role="alert">暂时无法验证登录状态，请检查后端服务后重试。</p>

        <label class="field">
          <span class="field-label">账号名</span>
          <input
            v-model.trim="form.username"
            type="text"
            autocomplete="username"
            placeholder="支持中文、英文或数字，2–32位"
            maxlength="32"
          />
        </label>

        <label class="field">
          <span class="field-label">密码</span>
          <input
            v-model="form.password"
            type="password"
            :autocomplete="registering?'new-password':'current-password'"
            :placeholder="registering?'设置密码，至少12位':'请输入密码'"
            maxlength="128"
          />
        </label>

        <label v-if="registering" class="field">
          <span class="field-label">确认密码</span>
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入密码" maxlength="128" />
        </label>

        <p v-if="errorMsg" class="login-error" role="alert">{{ errorMsg }}</p>

        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? "正在处理…" : registering ? "创建账号并进入" : "进入 RUNWISE" }}
        </button>

        <div class="login-tip">
          <button type="button" :disabled="loading" @click="switchMode">{{registering?'已有账号？返回登录':'首次使用？创建账号'}}</button>
          <p class="bind-note">已有微信训练记录？先在小程序登录原微信账号，并设置共享账密，再在这里登录。请勿重复注册。</p>
        </div>
      </form>
    </P5Card>
  </div>
</template>

<style scoped>
/* 基础红黑功能态样式（海报化在 M7 统一实施） */
.page {
  min-height: calc(100vh - var(--sp-7) * 2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 400px;
  max-width: 100%;
  padding: var(--sp-7);
  display: flex;
  flex-direction: column;
}

.login-kicker {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-3);
}

.login-title {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--p5-white);
  margin-bottom: var(--sp-3);
}

.login-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
  margin-bottom: var(--sp-6);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  margin-bottom: var(--sp-4);
}

.field-label {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  letter-spacing: 0.05em;
}

.field input {
  padding: var(--sp-3) var(--sp-4);
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  color: var(--p5-white);
  font-size: var(--fs-body);
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus {
  border-color: var(--p5-red);
}

.field input::placeholder {
  color: #55555c;
}

.login-error {
  font-size: var(--fs-sub);
  color: var(--p5-red);
  margin-bottom: var(--sp-3);
}

.login-submit {
  margin-top: var(--sp-2);
  padding: var(--sp-3) var(--sp-4);
  background: var(--p5-red);
  color: var(--p5-white);
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.15em;
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition: background-color 0.2s;
}

.login-submit:hover:not(:disabled) {
  background: var(--p5-red-dark);
}

.login-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-tip {
  margin-top: var(--sp-5);
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  line-height: 1.6;
}
.bind-note {font-size:13px;line-height:1.7;color:var(--p5-text-dim);margin-top:16px;}
</style>
