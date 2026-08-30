<template>
  <view class="qa-page">
    <!-- 背景图片层 -->
    <image class="page-bg" src="/static/blurry-gradient-haikei.png" mode="aspectFill"></image>

    <!-- 二级导航 -->
    <view class="sub-nav">
      <view
        class="nav-item"
        :class="{ 'nav-item--active': currentTab === 0 }"
        @tap="switchTab(0)"
      >
        <text class="nav-text">问答</text>
      </view>
      <!-- 历史Tab暂时隐藏，功能完成后恢复 -->
      <!--
      <view
        class="nav-item"
        :class="{ 'nav-item--active': currentTab === 1 }"
        @tap="switchTab(1)"
      >
        <text class="nav-text">历史</text>
      </view>
      -->
    </view>

    <!-- 可滚动内容 -->
    <scroll-view
      class="scroll-content"
      scroll-y
      :scroll-into-view="intoView"
      :scroll-with-animation="true"
    >
      <!-- 问答视图 -->
      <view v-show="currentTab === 0" class="qa-view">
        <!-- 分类入口（毛玻璃网格卡片 v5.0） -->
        <view class="category-grid">
          <view
            class="category-card"
            :class="{ 'category-card--active': activeCategory === item.name }"
            v-for="(item, index) in categories"
            :key="item.name"
            @tap="onCategoryTap(item)"
          >
            <view class="category-icon-wrap" :style="{ background: catIconBgs[index] }">
              <uni-icons :type="item.icon" size="22" color="#ffffff"></uni-icons>
            </view>
            <text class="category-name">{{ item.name }}</text>
          </view>
        </view>

        <!-- 热门问题（跑道气泡卡片：横向滑动，与首页新手常问风格统一） -->
        <view class="hot-section">
          <view class="section-header">
            <view class="section-icon-wrap">
              <uni-icons type="fire" size="16" color="#ffffff"></uni-icons>
            </view>
            <text class="section-title">热门问题</text>
          </view>

          <view class="cloud-scroll-wrap">
            <view
              class="cloud-bubble"
              v-for="(q, idx) in hotQuestions"
              :key="idx"
              hover-class="cloud-bubble-hover"
              @tap="onHotQuestionTap(q)"
            >
              <view class="cloud-bubble-inner">
                <view class="cloud-bubble-text">
                  <text class="bubble-line1">{{ splitQuestion(q).line1 }}</text>
                  <text class="bubble-line2">{{ splitQuestion(q).line2 }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 最新回答（暂未开发，注释保留） -->
        <!--
        <view class="latest-section" v-if="!messages.length">
          <view class="section-header">
            <view class="section-icon-wrap latest-icon">
              <uni-icons type="chat" size="16" color="#ffffff"></uni-icons>
            </view>
            <text class="section-title">最新回答</text>
          </view>
          <view class="latest-list">
            <view
              class="latest-item"
              v-for="(item, idx) in latestAnswers"
              :key="idx"
              @tap="onHotQuestionTap(item.question)"
            >
              <view class="latest-q">Q: {{ item.question }}</view>
              <view class="latest-a">A: {{ item.answer }}</view>
              <view class="latest-meta">
                <text>👍 {{ item.likes }}</text>
                <text class="latest-time">{{ item.time }}</text>
              </view>
            </view>
          </view>
        </view>
        -->

        <!-- AI教练引导文案 -->
        <view class="coach-greeting" v-if="!messages.length">
          <view class="coach-avatar">
            <uni-icons type="chatboxes-filled" size="20" color="#ffffff"></uni-icons>
          </view>
          <text class="coach-text">你好，我是你的AI跑步教练 🏃</text>
        </view>

        <!-- Loading 提示 -->
        <view class="loading-tip" v-if="loading">
          <text class="loading-text">AI 正在思考中，请稍候...</text>
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
              <view class="ai-cloud-bubble">
                <text class="ai-cloud-text">{{ msg.content }}</text>
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
                <text class="safety-text">{{ msg.safetyTip }}</text>
              </view>
              <!-- 反馈按钮 -->
              <view class="feedback">
                <view
                  class="feedback-btn"
                  :class="{ 'feedback-btn--useful': msg.feedback === 1 }"
                  @tap="onFeedback(msg, 1)"
                >
                  <text class="feedback-text">有用</text>
                </view>
                <view
                  class="feedback-btn"
                  :class="{ 'feedback-btn--useless': msg.feedback === -1 }"
                  @tap="onFeedback(msg, -1)"
                >
                  <text class="feedback-text">无用</text>
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
      <view v-show="currentTab === 1" class="history-view">
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
        :focus="shouldFocusInput"
        placeholder="输入你的跑步问题..."
        placeholder-class="input-placeholder"
        confirm-type="send"
        @confirm="onSend"
        @blur="shouldFocusInput = false"
      />
      <view
        class="send-btn"
        :class="{ 'send-btn--active': inputText.trim().length > 0 }"
        @tap="onSend"
      >
        <uni-icons type="paperplane" size="20" color="#ffffff"></uni-icons>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from "vue";
import { onShow } from "@dcloudio/uni-app";


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
const intoView = ref("");
const loading = ref(false);
const apiError = ref<string | null>(null);
const shouldFocusInput = ref(false);

// 分类图标背景色（低饱和度柔和色）
const catIconBgs = [
  "linear-gradient(135deg, #93c5fd, #60a5fa)",
  "linear-gradient(135deg, #fdba74, #fb923c)",
  "linear-gradient(135deg, #86efac, #4ade80)",
  "linear-gradient(135deg, #c4b5fd, #a78bfa)",
];
const activeCategory = ref("");

// 数据列表（初始为空，从后端加载）
const categories = ref<any[]>([]);
const hotQuestions = ref<string[]>([]);
const messages = ref<ChatMessage[]>([]);
const history = ref<HistoryItem[]>([]);

// API 基础地址配置（注意：微信开发者工具中 localhost 不可用，必须使用局域网 IP）
const API_BASE_URL = "http://192.168.0.103:8080/api";

// 获取 Token（暂时为空，后续实现登录后填充）
const getToken = () => uni.getStorageSync("token") || "";

// 统一请求封装
const request = async (options: {
  url: string;
  method?: "GET" | "POST";
  data?: any;
  showLoading?: boolean;
}) => {
  const { url, method = "GET", data, showLoading = false } = options;

  if (showLoading) {
    uni.showLoading({ title: "加载中...", mask: true });
  }

  try {
    const res = await new Promise<any>((resolve, reject) => {
      uni.request({
        url: `${API_BASE_URL}${url}`,
        method,
        data,
        header: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${getToken()}`,
        },
        timeout: 30000,
        success: (res) => resolve(res.data),
        fail: (err) => reject(err),
      });
    });

    // 判断业务状态码
    if (res.code === 0 || res.code === 200) {
      return res.data;
    } else {
      throw new Error(res.message || "请求失败");
    }
  } catch (error: any) {
    console.error("API请求失败:", error);
    apiError.value = error.message || "网络连接失败";
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
      request({ url: "/qa/categories" }).catch(() => null),
      request({ url: "/qa/hot" }).catch(() => null),
    ]);

    // 使用后端数据，如果失败则使用默认值
    categories.value =
      categoriesData?.length > 0
        ? categoriesData
        : [
            { name: "训练计划", icon: "compose" },
            { name: "装备选择", icon: "cart" },
            { name: "伤痛预防", icon: "heart" },
            { name: "跑步技术", icon: "gear" },
          ];

    hotQuestions.value =
      hotQuestionsData?.length > 0
        ? hotQuestionsData.map((item: any) => item.question || item)
        : [
            "第一次跑步该跑多远？",
            "跑完膝盖疼怎么办？",
            "新手怎么选跑鞋？",
            "跑步时心率多少合适？",
            "晨跑好还是夜跑好？",
            "跑步前要热身多久？",
            "如何提高跑步配速？",
          ];
  } catch (error) {
    console.error("初始化数据加载失败");
  }
});

// 页面显示时检查是否需要自动聚焦输入框（来自首页"问问题"按钮）
onShow(() => {
  const app = getApp();
  if (app.globalData?.qaAutoFocus) {
    currentTab.value = 0;
    shouldFocusInput.value = true;
    app.globalData.qaAutoFocus = false;

    setTimeout(() => {
      if (!inputText.value.trim()) {
        shouldFocusInput.value = false;
      }
    }, 3000);
  }
  // 首页云朵点击 → 自动填入问题并发送
  if (app.globalData?.qaAutoSend) {
    currentTab.value = 0;
    const question = app.globalData.qaAutoSend;
    app.globalData.qaAutoSend = undefined;
    // 延迟发送，确保页面完全渲染
    setTimeout(() => {
      sendMessage(question);
    }, 300);
  }
});

// 加载历史记录
const loadHistory = async () => {
  if (loading.value) return;

  try {
    loading.value = true;
    const historyData = await request({
      url: "/qa/history",
      method: "POST",
      data: { page: 1, size: 20 },
    });

    history.value = (historyData?.records || []).map((item: any) => ({
      id: item.id,
      date:
        item.createdAt?.split("T")[0] || new Date().toISOString().split("T")[0],
      question: item.question,
      summary: item.answer?.substring(0, 50) + "..." || "",
      feedback: item.feedback || 0,
    }));
  } catch (error) {
    console.error("加载历史记录失败");
    uni.showToast({ title: "加载历史失败", icon: "none" });
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
    const response = await request({
      url: "/qa/ask",
      method: "POST",
      data: { question: content },
    });

    // 将AI回答添加到对话
    messages.value.push({
      role: "assistant",
      content: response.answer || "抱歉，未能获取有效回答。",
      sources: response.sources || [],
      safetyTip: response.safetyTip,
      feedback: 0,
    });

    scrollToBottom();
  } catch (error: any) {
    console.error("提问失败 - 完整错误:", error);
    console.error("错误信息:", error.message);
    console.error("错误详情:", JSON.stringify(error));

    // 显示详细错误提示（方便调试）
    const errorMsg = error.errMsg || error.message || "未知错误";
    messages.value.push({
      role: "assistant",
      content: `请求失败：${errorMsg}\n\n请查看控制台获取详细信息`,
      feedback: 0,
    });

    // 同时弹出Toast提示
    uni.showToast({
      title: `请求失败: ${errorMsg}`,
      icon: "none",
      duration: 3000,
    });

    scrollToBottom();
  } finally {
    loading.value = false;
  }
};

// 发送按钮点击
const onSend = () => {
  if (loading.value) return; // 防止重复发送
  sendMessage(inputText.value);
};

// 点击热门问题
const onHotQuestionTap = (q: string) => {
  sendMessage(q);
};

const onMoreQuestions = () => {
  if (hotQuestions.value.length > 3) {
    const rest = hotQuestions.value.slice(3);
    uni.showActionSheet({
      itemList: rest,
      success: (res) => {
        sendMessage(rest[res.tapIndex]);
      },
    });
  } else {
    uni.showToast({ title: '暂无更多问题', icon: 'none' });
  }
};

const splitQuestion = (text: string) => {
  const len = text.length;
  const mid = Math.ceil(len / 2);
  return { line1: text.slice(0, mid), line2: text.slice(mid) };
};

const latestAnswers = ref([
  {
    question: "跑完膝盖疼怎么办？",
    answer: "先判断疼痛位置是髌骨还是外侧，髌骨疼可减少跑量加强股四头肌，外侧疼注意鞋底磨损…",
    likes: 23,
    time: "2小时前",
  },
  {
    question: "晨跑好还是夜跑好？",
    answer: "各有优势：晨跑提升代谢、夜跑释放压力。关键看个人作息规律，坚持比选择更重要…",
    likes: 18,
    time: "5小时前",
  },
  {
    question: "如何提高配速？",
    answer: "建议采用80/20法则：80%慢跑+20%间歇。每周一次节奏跑，逐步提升乳酸阈值…",
    likes: 15,
    time: "昨天",
  },
]);

// 点击分类
const onCategoryTap = (item: any) => {
  activeCategory.value = item.name;
  sendMessage(`请介绍一下${item.name}方面的建议`);
};

// 反馈按钮
const onFeedback = async (msg: ChatMessage, value: number) => {
  msg.feedback = msg.feedback === value ? 0 : value;

  // 如果有recordId，提交到后端
  if (msg.id && msg.feedback !== 0) {
    try {
      await request({
        url: "/qa/feedback",
        method: "POST",
        data: { messageId: msg.id, feedback: msg.feedback },
      });
    } catch (error) {
      console.error("提交反馈失败");
    }
  }
};
</script>

<style lang="scss">
.qa-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  overflow: hidden;
}

.page-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
  filter: brightness(1.15) saturate(1.3);
}

/* ========== 二级导航 ========== */
.sub-nav {
  flex-shrink: 0;
  display: flex;
  height: 80rpx;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
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
  @include rw-glass-card;
  margin: 24rpx;
  padding: 24rpx;
}

/* ========== 问答视图 ========== */
.qa-view {
  padding-bottom: 24rpx;
}

/* ========== 分类入口（毛玻璃网格卡片 v5.0） ========== */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  padding: 24rpx;
}

.category-card {
  @include rw-glass-item(24rpx);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.95);
  }
}

.category-card--active {
  box-shadow: $rw-shadow-primary;
  transform: scale(1.03);
}

.category-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.category-name {
  font-size: 24rpx;
  color: $rw-text-primary;
  font-weight: 500;
}

/* ================================================================
   热门问题 — 跑道气泡卡片：横向滚动，与首页新手常问风格统一
   ================================================================ */

.hot-section {
  margin: 0 0 16rpx;
  padding: 8rpx 0;
  overflow: visible;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 0 24rpx 24rpx;
}

.section-icon-wrap {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #FFAB91, #FF8A65);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  box-shadow: 0 3rpx 10rpx rgba(255, 138, 101, 0.25);
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-text-primary;
  letter-spacing: 1rpx;
}

.cloud-scroll-wrap {
  @include rw-glass-card;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: visible;
  padding: 28rpx 24rpx;
  margin: 0 0 16rpx;
  gap: 0;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

.cloud-bubble {
  flex-shrink: 0;
  position: relative;
  width: 260rpx;
  height: 220rpx;
  margin-right: 44rpx;
  border-radius: 28rpx;
  overflow: hidden;
  transition: all 0.3s;

  &:last-of-type {
    margin-right: 24rpx;
  }

  &:nth-child(1) { margin-top: 0; }
  &:nth-child(2) { margin-top: 10px; }
  &:nth-child(3) { margin-top: -10px; }

  &:active {
    transform: scale(0.95);
  }
}

.cloud-bubble-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-image: url('/static/track-field.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.65;
}

.cloud-bubble-text {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 12rpx;
  padding: 12rpx 20rpx;
}

.bubble-line1,
.bubble-line2 {
  font-size: 28rpx;
  color: #000000;
  font-weight: 700;
  line-height: 1.5;
  white-space: nowrap;
  text-shadow: 0 1rpx 3rpx rgba(255, 255, 255, 0.9);
}

.bubble-line2 {
  margin-left: 20rpx;
}

.cloud-bubble-hover {
  transform: scale(0.96);
  opacity: 0.85;
}

.cloud-bubble-more {
  flex-shrink: 0;
  width: 120rpx;
  height: 220rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.5);
  border: 2rpx dashed rgba(255, 140, 66, 0.4);

  &:active {
    transform: scale(0.95);
  }
}

.more-text-inner {
  font-size: 28rpx;
  color: $rw-primary;
  font-weight: 600;
}

/* AI教练引导文案 */
.coach-greeting {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 24rpx 16rpx;
  gap: 12rpx;
}

.coach-avatar {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $rw-primary, $rw-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(255, 107, 53, 0.25);
}

.coach-text {
  font-size: 28rpx;
  color: $rw-text-primary;
  font-weight: 500;
}

/* ========== Loading 提示 ========== */
.loading-tip {
  @include rw-glass-item(16rpx);
  margin: 24rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-text {
  font-size: 28rpx;
  color: $rw-text-secondary;
}

/* ========== 对话区域 ========== */
.chat-area {
  flex: 1;
  overflow-y: auto;
  margin: 24rpx;
  padding-bottom: 32rpx;
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
  background: rgba(240, 242, 245, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-top-left-radius: 4rpx;
}

.bubble-text {
  font-size: 28rpx;
  color: #ffffff;
  line-height: 1.6;
}

.bubble--user .bubble-text {
  color: $rw-text-primary;
}

/* AI 云朵气泡 - 三层立体朦胧感 v5.2 */
.ai-cloud-bubble {
  max-width: 82%;
  padding: 26rpx 36rpx;
  background: linear-gradient(
    145deg,
    rgba(59, 130, 246, 0.92) 0%,
    rgba(37, 99, 235, 0.88) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 40rpx;
  border-top-left-radius: 12rpx;
  border: 1rpx solid rgba(147, 197, 253, 0.4);
  /* 三层阴影：底层深蓝扩散 + 中层蓝色光晕 + 顶层高光 */
  box-shadow:
    0 12rpx 40rpx rgba(37, 99, 235, 0.25),
    0 4rpx 16rpx rgba(59, 130, 246, 0.18),
    inset 0 2rpx 6rpx rgba(255, 255, 255, 0.25),
    inset 0 -1rpx 3rpx rgba(30, 64, 175, 0.15);
  position: relative;
}

.ai-cloud-text {
  font-size: 28rpx;
  color: #ffffff;
  line-height: 1.65;
  text-shadow: 0 1rpx 3rpx rgba(0, 0, 0, 0.08);
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
  background: rgba(245, 158, 11, 0.06);
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
  padding: 10rpx 24rpx;
  margin-right: 16rpx;
  border-radius: 24rpx;
  background: rgba(0, 0, 0, 0.04);
  transition: $rw-transition-smooth;
}

.feedback-text {
  font-size: 24rpx;
  color: $rw-text-secondary;
}

.feedback-btn--useful .feedback-text {
  color: $rw-primary;
}

.feedback-btn--useless .feedback-text {
  color: $rw-error;
}

/* ========== 底部输入区（毛玻璃 v5.6 加高+分隔） ========== */
.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 24rpx 28rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-top: 1px solid rgba(0, 0, 0, 0.02);
  z-index: 10;
}

.input-field {
  flex: 1;
  height: 80rpx;
  padding: 0 36rpx;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: none;
  border-radius: 64rpx;
  font-size: 28rpx;
  color: $rw-text-primary;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.input-placeholder {
  color: $rw-text-placeholder;
  font-size: 28rpx;
}

.send-btn {
  margin-left: 20rpx;
  width: 88rpx;
  height: 88rpx;
  border-radius: 56rpx;
  background: rgba(255, 140, 66, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: none;
  box-shadow:
    0 6rpx 20rpx rgba(0, 0, 0, 0.06),
    0 2rpx 8rpx rgba(0, 0, 0, 0.04),
    inset 0 1rpx 3rpx rgba(255, 255, 255, 0.7);
}

.send-btn--active {
  background: $rw-primary;
  box-shadow:
    0 10rpx 28rpx rgba(255, 140, 66, 0.3),
    0 4rpx 12rpx rgba(255, 140, 66, 0.18),
    inset 0 1rpx 3rpx rgba(255, 255, 255, 0.5);

  &:active {
    transform: scale(0.92) translateY(2rpx);
    background: linear-gradient(135deg, #FFAB91, #FFCCBC);
  }
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