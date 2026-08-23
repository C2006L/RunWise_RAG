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

<style>
.agreement-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.agreement-modal {
  width: 600rpx;
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.15);
}

.agreement-title {
  display: block;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
  padding: 40rpx 32rpx 16rpx;
}

.agreement-body {
  max-height: 400rpx;
  padding: 16rpx 32rpx;
}

.agreement-text {
  display: block;
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.8;
  margin-bottom: 12rpx;
}

.agreement-links {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 32rpx;
  border-top: 1rpx solid #f3f4f6;
}

.link-text {
  font-size: 24rpx;
  color: #ff6b35;
  text-decoration: underline;
}

.link-divider {
  font-size: 24rpx;
  color: #d1d5db;
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
  border-radius: 40rpx;
  border: 2rpx solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.btn-disagree-text {
  font-size: 28rpx;
  color: #6b7280;
}

.btn-agree {
  flex: 1.5;
  height: 80rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #ff6b35, #ff8f5e);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 53, 0.3);
}

.btn-agree-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #ffffff;
}

.btn-hover {
  opacity: 0.85;
  transform: scale(0.97);
}

page {
  background-color: #F6F7F9;
  background-image: radial-gradient(circle at 10% 10%, rgba(255, 107, 53, 0.1) 0%, transparent 50%);
  background-attachment: fixed;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>