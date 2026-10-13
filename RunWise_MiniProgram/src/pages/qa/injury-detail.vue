<template>
  <view class="detail-page">
    <!-- 背景网点纹理（P5 语言，与答疑页同源） -->
    <view class="detail-page__dots"></view>

    <!-- 页头：红斜杠 + kicker + 大标题 + 灰副题 -->
    <view class="page-header">
      <text class="page-header__kicker">INJURY PREVENTION</text>
      <text class="page-header__title">伤病预防</text>
      <text class="page-header__sub">科学认知 · 远离伤痛</text>
    </view>

    <!-- 文章正文（分节结构） -->
    <view class="article" v-if="article">
      <view class="article__top">
        <view class="article__tag">
          <text class="article__tag-text">{{ article.categoryLabel }}</text>
        </view>
        <text class="article__read">{{ article.readMinutes }} 分钟阅读</text>
      </view>

      <text class="article__title">{{ article.title }}</text>

      <view class="section" v-for="(sec, idx) in article.sections || []" :key="idx">
        <view class="section__head">
          <view class="section__bullet"></view>
          <text class="section__title">{{ sec.heading }}</text>
        </view>
        <text class="section__body">{{ sec.body }}</text>
      </view>

      <!-- 就医提示（红左边框，同答疑页安全提示语言） -->
      <view class="medical-tip">
        <text class="medical-tip__text"
          >以上内容仅供科普参考，不能替代专业诊断。若疼痛持续、加重或影响正常行走，请及时就医。</text
        >
      </view>
    </view>

    <!-- 加载 / 失败态 -->
    <view class="detail-state" v-else>
      <rw-empty :text="loadError ? '文章加载失败，请返回重试' : '加载中...'" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
// API 基础地址统一取自 utils/request.ts，禁止在页面内重复硬编码
import { BASE_URL } from "@/utils/request";

interface InjurySection {
  heading: string;
  body: string;
}

interface InjuryArticle {
  id: number;
  category: string;
  categoryLabel?: string;
  title: string;
  summary: string;
  tags?: string[];
  readMinutes?: number;
  sections?: InjurySection[];
}

const article = ref<InjuryArticle | null>(null);
const loadError = ref(false);

// Token 读取与答疑页保持一致（P3 登录统一后合并）
const getToken = () => uni.getStorageSync("token") || "";

onLoad((options: any) => {
  const id = options?.id;
  if (!id) {
    loadError.value = true;
    return;
  }

  uni.request({
    url: `${BASE_URL}/api/injury/articles/${id}`,
    method: "GET",
    header: { Authorization: `Bearer ${getToken()}` },
    success: (res: any) => {
      const body = res.data;
      if (body && (body.code === 200 || body.code === 0) && body.data) {
        article.value = body.data;
      } else {
        loadError.value = true;
      }
    },
    fail: () => {
      loadError.value = true;
    },
  });
});
</script>

<style lang="scss">
.detail-page {
  position: relative;
  min-height: 100vh;
  background: $rw-black;
  padding-bottom: 64rpx;
  box-sizing: border-box;
}

/* 背景网点纹理（装饰层 z-index 0，不拦截手势） */
.detail-page__dots {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.07) 2rpx, transparent 2rpx);
  background-size: 28rpx 28rpx;
}

/* ========== 页头（红斜杠签名件） ========== */
.page-header {
  position: relative;
  z-index: 1;
  @include rw-header-slash;
  margin: 40rpx 32rpx 32rpx;
}

.page-header__kicker {
  display: block;
  font-family: $rw-font-display;
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: $rw-red;
  margin-bottom: 10rpx;
}

.page-header__title {
  display: block;
  font-family: $rw-font-cn-title;
  font-size: 56rpx;
  color: $rw-white;
  line-height: 1.2;
}

.page-header__sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $rw-gray;
  letter-spacing: 1rpx;
}

/* ========== 文章卡（分节阅读） ========== */
.article {
  position: relative;
  z-index: 1;
  margin: 0 32rpx;
  padding: 32rpx 28rpx;
  background: $rw-panel;
  border: 2rpx solid #2a2a2e;
  @include rw-card-clip;
}

.article__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.article__tag {
  display: flex;
  width: fit-content;
  padding: 6rpx 18rpx;
  background: $rw-red;
  clip-path: polygon(10rpx 0, 100% 0, calc(100% - 10rpx) 100%, 0 100%);
}

.article__tag-text {
  font-size: 20rpx;
  color: $rw-white;
  letter-spacing: 1rpx;
}

.article__read {
  font-size: 22rpx;
  color: $rw-gray;
  font-feature-settings: "tnum";
}

.article__title {
  display: block;
  margin-top: 20rpx;
  font-size: 38rpx;
  font-weight: 700;
  color: $rw-white;
  line-height: 1.45;
}

/* 分节：红菱形 bullet + 白字小节标题 + 正文 1.8 行高 */
.section {
  margin-top: 36rpx;
}

.section__head {
  display: flex;
  align-items: center;
  margin-bottom: 14rpx;
}

.section__bullet {
  width: 16rpx;
  height: 16rpx;
  background: $rw-red;
  transform: rotate(45deg);
  margin-right: 14rpx;
  flex-shrink: 0;
}

.section__title {
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-white;
  letter-spacing: 1rpx;
}

.section__body {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.8;
}

/* 就医提示 */
.medical-tip {
  margin-top: 40rpx;
  padding: 24rpx;
  background: rgba(230, 0, 18, 0.08);
  border-left: 6rpx solid $rw-red;
}

.medical-tip__text {
  font-size: 26rpx;
  color: $rw-red;
  line-height: 1.7;
}

/* ========== 加载 / 失败态 ========== */
.detail-state {
  position: relative;
  z-index: 1;
  margin: 0 32rpx;
  padding: 48rpx 0;
  background: $rw-panel;
  border: 2rpx solid #2a2a2e;
}
</style>