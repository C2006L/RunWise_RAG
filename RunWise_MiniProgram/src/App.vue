<template>
  <!-- 协议同意弹窗 -->
  <view v-if="showAgreement" class="agreement-overlay">
    <view class="agreement-modal">
      <text class="agreement-title">用户协议与隐私政策</text>
      <scroll-view scroll-y class="agreement-body">
        <text class="agreement-text"
          >感谢您使用RunWise！在使用本服务前，请您仔细阅读并理解以下协议：</text
        >
        <text class="agreement-text"
          >1. 《用户服务协议》：规定了您使用RunWise服务的权利和义务，包括账号管理、行为规范、AI问答声明、免责条款等内容。</text
        >
        <text class="agreement-text"
          >2. 《隐私政策》：说明了我们如何收集、使用、存储和保护您的个人信息，包括昵称、头像、身高体重、跑步数据等信息。</text
        >
        <text class="agreement-text"
          >我们重视您的隐私保护和个人信息安全。点击"同意并继续"即表示您已阅读并同意上述协议的全部内容。</text
        >
      </scroll-view>
      <view class="agreement-links">
        <text class="link-text" @click="goAgreement">查看《用户服务协议》</text>
        <text class="link-divider">|</text>
        <text class="link-text" @click="goPrivacy">查看《隐私政策》</text>
      </view>
      <view class="agreement-actions">
        <view class="btn-disagree" hover-class="btn-hover" @click="onDisagree">
          <text class="btn-disagree-text">不同意</text>
        </view>
        <view class="btn-agree" hover-class="btn-hover" @click="onAgree">
          <text class="btn-agree-text">同意并继续</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { onLaunch } from "@dcloudio/uni-app";
import { post, get, isLoggedIn } from "@/utils/request";

const AGREEMENT_VERSION = "1.0.0";
const showAgreement = ref(false);

const checkAgreement = async () => {
  const localAgreed = uni.getStorageSync("agreementAgreed");
  const localVersion = uni.getStorageSync("agreementVersion");

  if (localAgreed && localVersion === AGREEMENT_VERSION) {
    return;
  }

  if (isLoggedIn()) {
    try {
      const data = await get<any>("/api/user/agreement-status");
      if (data?.agreed && data?.currentVersion === AGREEMENT_VERSION) {
        uni.setStorageSync("agreementAgreed", true);
        uni.setStorageSync("agreementVersion", AGREEMENT_VERSION);
        return;
      }
    } catch (error) {
      console.error("检查协议状态失败", error);
    }
  }

  showAgreement.value = true;
};

const onAgree = async () => {
  uni.setStorageSync("agreementAgreed", true);
  uni.setStorageSync("agreementVersion", AGREEMENT_VERSION);
  showAgreement.value = false;

  if (isLoggedIn()) {
    try {
      await post("/api/user/agree-agreement", { version: AGREEMENT_VERSION });
    } catch (error) {
      console.error("提交协议同意失败", error);
    }
  }
};

const onDisagree = () => {
  uni.showModal({
    title: "提示",
    content: "您需要同意用户协议和隐私政策才能使用RunWise服务。不同意将退出应用。",
    confirmText: "我再看看",
    cancelText: "退出",
    success: (res) => {
      if (!res.confirm) {
        if (typeof wx !== "undefined" && wx.exitMiniProgram) {
          wx.exitMiniProgram();
        }
      }
    },
  });
};

const goAgreement = () => {
  uni.navigateTo({ url: "/pages/mine/agreement" });
};

const goPrivacy = () => {
  uni.navigateTo({ url: "/pages/mine/privacy" });
};

onLaunch(() => {
  console.log("App Launch");
  checkAgreement();
});
</script>

<style lang="scss">
/* =====================================================================
   RunWise 小程序 P5R 全局样式（v11）
   黑底深色主题 · 字体 base64 内嵌 · 统一按压/入场动画 · 减弱动态降级
   ===================================================================== */
@import '@/styles/fonts.scss';

page {
  background: $rw-black;
  color: $rw-white;
  font-family: $rw-font-body;
  font-size: 28rpx;
  line-height: 1.7;
  min-height: 100vh;
}

/* 入场动画：页面首屏卡片依次 fade + translateY(24rpx)，级联延迟 60ms（PRD 13.2.4） */
@keyframes rw-rise {
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rw-enter {
  animation: rw-rise 0.25s cubic-bezier(0.4, 0, 0.2, 1) both;
}

.rw-enter-1 { animation-delay: 0ms; }
.rw-enter-2 { animation-delay: 60ms; }
.rw-enter-3 { animation-delay: 120ms; }
.rw-enter-4 { animation-delay: 180ms; }
.rw-enter-5 { animation-delay: 240ms; }

/* 系统减弱动态：全部入场/呼吸动画静止（微信基础库 ≥2.26 支持该媒体查询） */
@media (prefers-reduced-motion: reduce) {
  .rw-enter,
  .rw-enter-1, .rw-enter-2, .rw-enter-3, .rw-enter-4, .rw-enter-5 {
    animation: none;
    opacity: 1;
    transform: none;
  }
}

/* ===== 协议同意弹窗（P5R：面板黑 + 白描边切角 + 红主按钮） ===== */
.agreement-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.agreement-modal {
  width: 620rpx;
  background: $rw-panel;
  border: 4rpx solid $rw-white;
  box-shadow: 12rpx 12rpx 0 $rw-shadow-red;
  @include rw-card-clip;
  overflow: hidden;
}

.agreement-title {
  display: block;
  text-align: center;
  font-size: 34rpx;
  font-weight: 700;
  color: $rw-white;
  padding: 40rpx 32rpx 16rpx;
}

.agreement-body {
  max-height: 400rpx;
  padding: 16rpx 32rpx;
}

.agreement-text {
  display: block;
  font-size: 26rpx;
  color: $rw-gray;
  line-height: 1.8;
  margin-bottom: 12rpx;
}

.agreement-links {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 32rpx;
  border-top: 2rpx solid $rw-line;
}

.link-text {
  font-size: 24rpx;
  color: $rw-red;
  text-decoration: underline;
}

.link-divider {
  font-size: 24rpx;
  color: $rw-gray;
  margin: 0 16rpx;
}

.agreement-actions {
  display: flex;
  padding: 24rpx 32rpx 40rpx;
  gap: 24rpx;
}

.btn-disagree {
  flex: 1;
  height: 80rpx;
  border: 4rpx solid $rw-white;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $rw-black;
  @include rw-btn-clip;
}

.btn-disagree-text {
  font-size: 28rpx;
  color: $rw-white;
}

.btn-agree {
  flex: 1.5;
  height: 80rpx;
  background: $rw-red;
  display: flex;
  align-items: center;
  justify-content: center;
  @include rw-btn-clip;
}

.btn-agree-text {
  font-size: 28rpx;
  font-weight: 500;
  color: $rw-white;
}

.btn-hover {
  opacity: 0.9;
  transform: scale(0.97);
}
</style>