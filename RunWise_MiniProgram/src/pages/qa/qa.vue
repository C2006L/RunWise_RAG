<template>
  <view class="qa-page">
    <!-- 二级导航 -->
    <view class="sub-nav">
      <view
        class="nav-item"
        :class="{ 'nav-item--active': currentTab === 0 }"
        @tap="switchTab(0)"
      >
        <text class="nav-text">问答</text>
      </view>
      <view
        class="nav-item"
        :class="{ 'nav-item--active': currentTab === 1 }"
        @tap="switchTab(1)"
      >
        <text class="nav-text">历史</text>
      </view>
    </view>

    <!-- 可滚动内容 -->
    <scroll-view
      class="scroll-content"
      scroll-y
      :scroll-into-view="intoView"
      :scroll-with-animation="true"
    >
      <!-- 问答视图 -->
      <view v-if="currentTab === 0" class="qa-view">
        <!-- 顶部搜索条 -->
        <view class="search-bar">
          <text class="search-icon">🔍</text>
          <input
            class="search-input"
            v-model="searchText"
            placeholder="搜索跑步问题..."
            placeholder-class="search-placeholder"
            confirm-type="search"
          />
        </view>

        <!-- 分类入口 -->
        <view class="card category-card">
          <view
            class="category-item"
            v-for="item in categories"
            :key="item.name"
            @tap="onCategoryTap(item)"
          >
            <view class="category-icon">
              <text class="category-emoji">{{ item.icon }}</text>
            </view>
            <text class="category-name">{{ item.name }}</text>
          </view>
        </view>

        <!-- 热门问题 -->
        <view class="card hot-card">
          <text class="section-title">热门问题</text>
          <view
            class="hot-item"
            v-for="(q, idx) in hotQuestions"
            :key="idx"
            @tap="onHotQuestionTap(q)"
          >
            <text class="hot-text">{{ q }}</text>
            <text class="hot-arrow">›</text>
          </view>
        </view>

        <!-- Loading 提示 -->
        <view class="loading-tip" v-if="loading">
          <text class="loading-text">🤔 AI 正在思考中，请稍候...</text>
        </view>

        <!-- 对话区域 -->
        <view class="chat-area" v-if="messages.length">
          <view class="msg-wrap" v-for="(msg, idx) in messages" :key="idx">
            <!-- 用户气泡 -->
            <view v-if="msg.role === 'user'" class="msg msg--user">
              <view class="bubble bubble--user">
                <text class="bubble-text">{{ msg.content }}</text>
              </view>
            </view>

            <!-- AI 气泡 -->
            <view v-else class="msg msg--assistant">
              <view class="ai-header">
                <view class="ai-avatar">
                  <text class="ai-avatar-text">AI</text>
                </view>
                <text class="ai-name">RunWise 助手</text>
              </view>
              <view class="bubble bubble--assistant">
                <text class="bubble-text">{{ msg.content }}</text>
              </view>
              <!-- 参考来源 -->
              <view class="sources" v-if="msg.sources && msg.sources.length">
                <text class="sources-label">参考来源：</text>
                <text
                  class="source-item"
                  v-for="(s, i) in msg.sources || []"
                  :key="i"
                  >{{ i > 0 ? "、" : "" }}{{ s }}</text
                >
              </view>
              <!-- 安全提示 -->
              <view class="safety-tip" v-if="msg.safetyTip">
                <text class="safety-text">⚠️ {{ msg.safetyTip }}</text>
              </view>
              <!-- 反馈按钮 -->
              <view class="feedback">
                <view
                  class="feedback-btn"
                  :class="{ 'feedback-btn--useful': msg.feedback === 1 }"
                  @tap="onFeedback(msg, 1)"
                >
                  <text class="feedback-text">👍 有用</text>
                </view>
                <view
                  class="feedback-btn"
                  :class="{ 'feedback-btn--useless': msg.feedback === -1 }"
                  @tap="onFeedback(msg, -1)"
                >
                  <text class="feedback-text">👎 无用</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 滚动锚点 + 输入栏占位 -->
        <view
          id="anchor-bottom"
          class="anchor-bottom anchor-bottom--input"
        ></view>
      </view>

      <!-- 历史视图 -->
      <view v-else class="history-view">
        <view
          class="history-group"
          v-for="group in historyGroups"
          :key="group.date"
        >
          <text class="history-date">{{ group.date }}</text>
          <view class="card history-card">
            <view
              class="history-item"
              v-for="(item, idx) in group.items"
              :key="idx"
            >
              <text class="history-question">{{ item.question }}</text>
              <text class="history-summary">{{ item.summary }}</text>
              <view class="history-meta">
                <text class="history-feedback">{{
                  feedbackText(item.feedback)
                }}</text>
              </view>
            </view>
          </view>
        </view>
        <view id="anchor-bottom" class="anchor-bottom"></view>
      </view>
    </scroll-view>

    <!-- 底部输入区 -->
    <view class="input-bar" v-if="currentTab === 0">
      <input
        class="input-field"
        v-model="inputText"
        placeholder="输入你的跑步问题..."
        placeholder-class="input-placeholder"
        confirm-type="send"
        @confirm="onSend"
      />
      <view
        class="send-btn"
        :class="{ 'send-btn--active': inputText.trim().length > 0 }"
        @tap="onSend"
      >
        <text class="send-text">发送</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from "vue";

// 接口定义
interface ChatMessage {
  id?: number;
  role: "user" | "assistant";
  content: string;
  sources?: string[];
  safetyTip?: string;
  feedback: number;
}

interface HistoryItem {
  id?: number;
  date: string;
  question: string;
  summary: string;
  feedback: number;
}

// 状态管理
const currentTab = ref(0);
const inputText = ref("");
const searchText = ref("");
const intoView = ref("");
const loading = ref(false);  // 加载状态
const apiError = ref<string | null>(null);  // 错误信息

// 数据列表（初始为空，从后端加载）
const categories = ref<any[]>([]);
const hotQuestions = ref<string[]>([]);
const messages = ref<ChatMessage[]>([]);
const history = ref<HistoryItem[]>([]);

// API 基础地址配置
const API_BASE_URL = 'http://localhost:8080/api';

// 获取 Token（暂时为空，后续实现登录后填充）
const getToken = () => uni.getStorageSync('token') || '';

// 统一请求封装
const request = async (options: {
  url: string;
  method?: 'GET' | 'POST';
  data?: any;
  showLoading?: boolean;
}) => {
  const { url, method = 'GET', data, showLoading = false } = options;

  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true });
  }

  try {
    const res = await new Promise<any>((resolve, reject) => {
      uni.request({
        url: `${API_BASE_URL}${url}`,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        timeout: 15000,
        success: (res) => resolve(res.data),
        fail: (err) => reject(err)
      });
    });

    // 判断业务状态码
    if (res.code === 0 || res.code === 200) {
      return res.data;
    } else {
      throw new Error(res.message || '请求失败');
    }
  } catch (error: any) {
    console.error('API请求失败:', error);
    apiError.value = error.message || '网络连接失败';
    throw error;
  } finally {
    if (showLoading) {
      uni.hideLoading();
    }
  }
};

// 页面加载时获取数据
onMounted(async () => {
  try {
    // 并行加载分类和热门问题
    const [categoriesData, hotQuestionsData] = await Promise.all([
      request('/qa/categories').catch(() => null),
      request('/qa/hot').catch(() => null),
    ]);

    // 使用后端数据，如果失败则使用默认值
    categories.value = categoriesData?.length > 0 ? categoriesData : [
      { name: "训练计划", icon: "📋" },
      { name: "装备选择", icon: "👟" },
      { name: "伤痛预防", icon: "🩹" },
      { name: "跑步技术", icon: "🏃" },
    ];

    hotQuestions.value = hotQuestionsData?.length > 0
      ? hotQuestionsData.map((item: any) => item.question || item)
      : [
        "第一次跑步该跑多远？",
        "跑完膝盖疼怎么办？",
        "新手怎么选跑鞋？",
        "跑步时心率多少合适？",
      ];
  } catch (error) {
    console.error('初始化数据加载失败');
  }
});

// 加载历史记录
const loadHistory = async () => {
  if (loading.value) return;

  try {
    loading.value = true;
    const historyData = await request('/qa/history', 'POST', { page: 1, size: 20 });

    history.value = (historyData?.records || []).map((item: any) => ({
      id: item.id,
      date: item.createdAt?.split('T')[0] || new Date().toISOString().split('T')[0],
      question: item.question,
      summary: item.answer?.substring(0, 50) + '...' || '',
      feedback: item.feedback || 0,
    }));
  } catch (error) {
    console.error('加载历史记录失败');
    uni.showToast({ title: '加载历史失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

// 计算属性：按日期分组历史记录
const historyGroups = computed(() => {
  const map = new Map<string, HistoryItem[]>();
  history.value.forEach((item) => {
    if (!map.has(item.date)) map.set(item.date, []);
    map.get(item.date)!.push(item);
  });
  return Array.from(map.entries()).map(([date, items]) => ({ date, items }));
});

// 反馈文字
const feedbackText = (f: number) => {
  if (f === 1) return "已反馈：有用";
  if (f === -1) return "已反馈：无用";
  return "未反馈";
};

// 切换 Tab
const switchTab = async (i: number) => {
  currentTab.value = i;
  // 切换到历史Tab且没有数据时自动加载
  if (i === 1 && history.value.length === 0) {
    await loadHistory();
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    intoView.value = "";
    setTimeout(() => {
      intoView.value = "anchor-bottom";
    }, 30);
  });
};

// 发送消息（调用后端RAG接口）
const sendMessage = async (text: string) => {
  const content = text.trim();
  if (!content) return;

  // 立即显示用户消息
  messages.value.push({ role: "user", content, feedback: 0 });
  inputText.value = "";
  scrollToBottom();

  try {
    loading.value = true;
    apiError.value = null;

    // 调用 Spring Boot 后端 → 后端调用 Ollama 大模型
    const response = await request('/qa/ask', 'POST', { question: content });

    // 将AI回答添加到对话
    messages.value.push({
      role: "assistant",
      content: response.answer || '抱歉，未能获取有效回答。',
      sources: response.sources || [],
      safetyTip: response.safetyTip,
      feedback: 0,
    });

    scrollToBottom();
  } catch (error: any) {
    console.error('提问失败 - 完整错误:', error);
    console.error('错误信息:', error.message);
    console.error('错误详情:', JSON.stringify(error));

    // 显示详细错误提示（方便调试）
    const errorMsg = error.errMsg || error.message || '未知错误';
    messages.value.push({
      role: "assistant",
      content: `⚠️ 请求失败：${errorMsg}\n\n请查看控制台获取详细信息`,
      feedback: 0,
    });

    // 同时弹出Toast提示
    uni.showToast({
      title: `请求失败: ${errorMsg}`,
      icon: 'none',
      duration: 3000
    });

    scrollToBottom();
  } finally {
    loading.value = false;
  }
};

// 发送按钮点击
const onSend = () => {
  if (loading.value) return;  // 防止重复发送
  sendMessage(inputText.value);
};

// 点击热门问题
const onHotQuestionTap = (q: string) => {
  sendMessage(q);
};

// 点击分类
const onCategoryTap = (item: any) => {
  sendMessage(`请介绍一下${item.name}方面的建议`);
};

// 反馈按钮
const onFeedback = async (msg: ChatMessage, value: number) => {
  msg.feedback = msg.feedback === value ? 0 : value;

  // 如果有recordId，提交到后端
  if (msg.id && msg.feedback !== 0) {
    try {
      await request('/qa/feedback', 'POST', { messageId: msg.id, feedback: msg.feedback });
    } catch (error) {
      console.error('提交反馈失败');
    }
  }
};
</script>

<style lang="scss">
.qa-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: $rw-bg-page;
}

/* ========== 二级导航 ========== */
.sub-nav {
  flex-shrink: 0;
  display: flex;
  height: 80rpx;
  background-color: $rw-bg-card;
  border-bottom: 1rpx solid $rw-border-color;
}

.nav-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.nav-text {
  font-size: 28rpx;
  font-weight: 400;
  color: $rw-text-placeholder;
}

.nav-item--active .nav-text {
  font-weight: 500;
  color: $rw-primary;
}

.nav-item--active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background-color: $rw-primary;
  border-radius: 2rpx;
}

/* ========== 滚动内容 ========== */
.scroll-content {
  flex: 1;
  height: 0;
}

/* ========== 通用卡片 ========== */
.card {
  margin: 24rpx;
  padding: 24rpx;
  background-color: $rw-bg-card;
  border-radius: 32rpx;
  box-shadow: $rw-shadow-card;
}

/* ========== 问答视图 ========== */
.qa-view {
  padding-bottom: 24rpx;
}

/* 搜索条 */
.search-bar {
  display: flex;
  align-items: center;
  margin: 24rpx;
  padding: 0 24rpx;
  height: 72rpx;
  background-color: $rw-bg-hover;
  border-radius: 24rpx;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 16rpx;
  color: $rw-text-placeholder;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.search-placeholder {
  color: $rw-text-placeholder;
  font-size: 28rpx;
}

/* 分类入口 */
.category-card {
  display: flex;
  justify-content: space-around;
  padding: 32rpx 16rpx;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.category-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background-color: rgba(24, 144, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.category-emoji {
  font-size: 40rpx;
}

.category-name {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

/* 热门问题 */
.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 500;
  color: $rw-text-primary;
  margin-bottom: 16rpx;
}

.hot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-text {
  flex: 1;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.hot-arrow {
  font-size: 32rpx;
  color: $rw-text-placeholder;
  margin-left: 16rpx;
}

/* ========== Loading 提示 ========== */
.loading-tip {
  margin: 24rpx;
  padding: 20rpx;
  background-color: $rw-bg-card;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $rw-shadow-card;
}

.loading-text {
  font-size: 28rpx;
  color: $rw-text-secondary;
}

/* ========== 对话区域 ========== */
.chat-area {
  margin: 24rpx;
}

.msg-wrap {
  margin-bottom: 32rpx;
}

.msg--user {
  display: flex;
  justify-content: flex-end;
}

.msg--assistant {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.bubble {
  padding: 20rpx 24rpx;
  border-radius: 24rpx;
  max-width: 80%;
}

.bubble--user {
  background-color: $rw-secondary-light;
  border-left: 4rpx solid $rw-secondary-border;
  border-top-left-radius: 4rpx;
}

.bubble--assistant {
  background-color: $rw-bg-card;
  box-shadow: $rw-shadow-card;
  border-top-left-radius: 4rpx;
}

.bubble-text {
  font-size: 28rpx;
  color: $rw-text-primary;
  line-height: 1.6;
}

/* AI 头部 */
.ai-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.ai-avatar {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background-color: $rw-secondary;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.ai-avatar-text {
  font-size: 22rpx;
  font-weight: 600;
  color: #ffffff;
}

.ai-name {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

/* 参考来源 */
.sources {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
}

.sources-label {
  color: $rw-text-secondary;
}

.source-item {
  color: $rw-secondary;
}

/* 安全提示 */
.safety-tip {
  margin-top: 16rpx;
  padding: 20rpx 24rpx;
  background-color: rgba(245, 158, 11, 0.06);
  border-left: 6rpx solid $rw-warning;
  border-radius: 12rpx;
}

.safety-text {
  font-size: 28rpx;
  color: #d97706;
  line-height: 1.6;
}

/* 反馈按钮 */
.feedback {
  display: flex;
  margin-top: 16rpx;
}

.feedback-btn {
  padding: 8rpx 20rpx;
  margin-right: 16rpx;
}

.feedback-text {
  font-size: 24rpx;
  color: $rw-text-placeholder;
}

.feedback-btn--useful .feedback-text {
  color: $rw-primary;
}

.feedback-btn--useless .feedback-text {
  color: $rw-error;
}

/* ========== 底部输入区 ========== */
.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background-color: $rw-bg-card;
  border-top: 1rpx solid $rw-border-color;
  z-index: 10;
}

.input-field {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  background-color: $rw-bg-card;
  border: 1rpx solid rgba(0, 0, 0, 0.1);
  border-radius: 24rpx;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.input-placeholder {
  color: $rw-text-placeholder;
  font-size: 28rpx;
}

.send-btn {
  margin-left: 16rpx;
  padding: 0 32rpx;
  height: 72rpx;
  border-radius: 24rpx;
  background-color: $rw-text-placeholder;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn--active {
  background-color: $rw-primary;
}

.send-text {
  font-size: 28rpx;
  color: #ffffff;
}

/* ========== 历史视图 ========== */
.history-view {
  padding-bottom: 24rpx;
}

.history-group {
  margin-top: 24rpx;
}

.history-date {
  display: block;
  font-size: 28rpx;
  color: $rw-text-secondary;
  margin: 0 24rpx 12rpx;
}

.history-card {
  margin: 0 24rpx;
}

.history-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
}

.history-item:last-child {
  border-bottom: none;
}

.history-question {
  display: block;
  font-size: 28rpx;
  color: $rw-text-primary;
  margin-bottom: 8rpx;
}

.history-summary {
  display: block;
  font-size: 24rpx;
  color: $rw-text-secondary;
  line-height: 1.5;
  margin-bottom: 8rpx;
}

.history-meta {
  display: flex;
}

.history-feedback {
  font-size: 24rpx;
  color: $rw-text-placeholder;
}

/* ========== 滚动锚点 ========== */
.anchor-bottom {
  height: 2rpx;
  width: 100%;
}

.anchor-bottom--input {
  height: 200rpx;
}
</style>