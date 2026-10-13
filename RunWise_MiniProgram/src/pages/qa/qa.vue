<template>
  <view class="qa-page">
    <!-- 背景网点纹理（P5 语言，纯 CSS 零体积） -->
    <view class="qa-page__dots"></view>

    <!-- 二级导航：问答 / 历史 / 伤病预防（红切角选中块 + 1px 地基线） -->
    <view class="sub-nav">
      <view
        v-for="(tab, idx) in tabs"
        :key="idx"
        class="sub-nav__item"
        :class="{ 'is-active': currentTab === idx }"
        @click="switchTab(idx)"
      >
        <text class="sub-nav__text">{{ tab }}</text>
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
      <view v-show="currentTab === 0" class="qa-view">
        <!-- 分类入口 V4（终版·锁死）：运动品牌控制面板件 -->
        <view class="category-grid">
          <view
            class="category-card"
            v-for="(item, idx) in categories"
            :key="item.name"
            :class="{ 'category-card--active': isCategoryHot(item.name) }"
            @touchstart="onCategoryPressStart(item.name)"
            @touchend="onCategoryPressEnd"
            @touchcancel="onCategoryPressEnd"
            @tap="onCategoryTap(item)"
          >
            <text class="category-card__num">0{{ idx + 1 }}</text>
            <uni-icons
              :type="item.icon"
              size="28"
              :color="isCategoryHot(item.name) ? '#E8442E' : '#FFFFFF'"
            ></uni-icons>
            <text class="category-name">{{ item.name }}</text>
          </view>
        </view>

        <!-- 热门问题（横向滑动：红切角胶带块，交错斜排 + 按压抖动） -->
        <view class="hot-section">
          <view class="section-header">
            <view class="section-icon-wrap">
              <uni-icons type="fire" size="16" color="#ffffff"></uni-icons>
            </view>
            <text class="section-title">热门问题</text>
          </view>

          <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false">
            <view
              v-for="(q, idx) in hotQuestions"
              :key="idx"
              class="hot-item"
              :class="idx % 2 === 0 ? 'hot-item--odd' : 'hot-item--even'"
              hover-class="hot-item--pressed"
              :hover-stay-time="150"
              @tap="onHotQuestionTap(q)"
            >
              <text class="hot-item__num">0{{ idx + 1 }}</text>
              <text class="hot-item__word">{{ q.word }}</text>
              <text class="hot-item__q">{{ q.question }}</text>
              <view class="hot-item__hint"></view>
            </view>
          </scroll-view>
        </view>

        <!-- AI 欢迎卡（黑底白描边切角 + 红撕贴标签 + 红菱形要点） -->
        <view class="welcome-zone" v-if="!messages.length">
          <rw-card tag="RUNWISE 助手" tag-rotate="-5deg" tag-variant="red" frame="red">
            <view class="welcome">
              <text class="welcome__title"
                >你好，我是 <text class="welcome__title-en">RunWise</text> 智能训练助手</text
              >
              <text class="welcome__desc"
                >跑量、配速、伤痛、装备，任何跑步问题都可以直接问我。</text
              >
              <view class="welcome__list">
                <view class="welcome__item" v-for="(tip, i) in welcomeTips" :key="i">
                  <view class="welcome__bullet"></view>
                  <text class="welcome__item-text">{{ tip }}</text>
                </view>
              </view>
            </view>
          </rw-card>
        </view>

        <!-- 对话区域 -->
        <view class="chat-area" v-if="messages.length">
          <view class="msg-wrap" v-for="(msg, idx) in messages" :key="idx">
            <!-- 用户气泡（红底白字切角） -->
            <view v-if="msg.role === 'user'" class="msg msg--user">
              <view class="bubble bubble--user">
                <text class="bubble-text">{{ msg.content }}</text>
              </view>
            </view>

            <!-- AI 气泡（黑底白字 + 灰描边 + 右上角白色四角星） -->
            <view v-else class="msg msg--assistant">
              <view class="ai-cloud-bubble">
                <view class="ai-sparkle"></view>
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
            </view>
          </view>

          <!-- AI 思考中：打字指示器气泡（紧贴最后一条消息） -->
          <view class="msg msg--assistant" v-if="loading">
            <view class="ai-cloud-bubble typing-bubble">
              <view class="typing-dots">
                <view class="typing-dot" v-for="n in 3" :key="n"></view>
              </view>
              <text class="typing-text">AI 正在思考中</text>
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
              hover-class="history-item--pressed"
              :hover-stay-time="120"
              v-for="(item, idx) in group.items"
              :key="idx"
              @tap="onHistoryTap(item)"
            >
              <text class="history-question">{{ item.question }}</text>
              <text class="history-summary">{{ item.summary }}</text>
              <view class="history-meta">
                <text class="history-time">{{ item.time || item.date }}</text>
                <text class="history-reask">点击重新提问 →</text>
              </view>
            </view>
          </view>
        </view>
        <view id="anchor-bottom" class="anchor-bottom"></view>
      </view>

      <!-- 伤病预防视图：分类筛选 + 科普文章列表（数据来自 /api/injury/articles） -->
      <view v-show="currentTab === 2" class="injury-view">
        <!-- 分类筛选 chips（全部 / 膝部 / 足踝 / 小腿，key 传参、label 展示） -->
        <view class="injury-filter">
          <view
            v-for="cat in injuryCategories"
            :key="cat.key"
            class="injury-filter__chip"
            :class="{ 'injury-filter__chip--active': injuryCategory === cat.key }"
            hover-class="injury-filter__chip--pressed"
            :hover-stay-time="80"
            @tap="switchInjuryCategory(cat.key)"
          >
            <text class="injury-filter__text">{{ cat.label }}</text>
          </view>
        </view>

        <!-- 文章列表 -->
        <view class="injury-list" v-if="injuryArticles.length">
          <view
            class="injury-card"
            v-for="article in injuryArticles"
            :key="article.id"
            hover-class="injury-card--pressed"
            :hover-stay-time="120"
            @tap="onInjuryTap(article)"
          >
            <view class="injury-card__tag">
              <text class="injury-card__tag-text">{{ article.categoryLabel }}</text>
            </view>
            <text class="injury-card__title">{{ article.title }}</text>
            <text class="injury-card__summary">{{ article.summary }}</text>
            <view class="injury-card__meta">
              <text class="injury-card__read">{{ article.readMinutes }} 分钟阅读</text>
              <text class="injury-card__more">阅读全文 →</text>
            </view>
          </view>
        </view>

        <!-- 空态（加载完成后才显示，避免闪烁） -->
        <rw-empty v-else-if="injuryLoaded" text="这个分类下暂时没有文章" />
      </view>
    </scroll-view>

    <!-- 底部输入区（textarea 支持多行换行，发送统一走右侧按钮） -->
    <view class="input-bar" v-if="currentTab === 0">
      <textarea
        class="input-field"
        :class="{ 'input-field--focus': shouldFocusInput }"
        v-model="inputText"
        :focus="shouldFocusInput"
        auto-height
        :show-confirm-bar="false"
        :disable-default-padding="true"
        :maxlength="500"
        placeholder="输入你的跑步问题...（可换行）"
        placeholder-class="input-placeholder"
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
import { setTabBarIndex } from "@/utils/tabbar";
// API 基础地址统一取自 utils/request.ts，禁止在页面内重复硬编码
import { BASE_URL } from "@/utils/request";


// 接口定义
interface ChatMessage {
  id?: number;
  role: "user" | "assistant";
  content: string;
  sources?: string[];
  safetyTip?: string;
}

interface HistoryItem {
  id?: number;
  date: string;
  time?: string;
  question: string;
  summary: string;
}

// 二级导航（伤病预防视图 P2 接入后端文章数据）
const tabs = ["问答", "历史", "伤病预防"];

// AI 欢迎卡要点
const welcomeTips = [
  "结合你的打卡记录给出跑量与配速建议",
  "伤病问题会附上警示提示，必要时请就医",
];

// 状态管理
const currentTab = ref(0);
const inputText = ref("");
const intoView = ref("");
const loading = ref(false);
const apiError = ref<string | null>(null);
const shouldFocusInput = ref(false);

const activeCategory = ref("");

// 分类名 → uni-icons 线性图标（后端返回 emoji，不可用；PRD 禁 emoji）
const CATEGORY_ICONS: Record<string, string> = {
  训练计划: "compose",
  装备选择: "cart",
  伤痛预防: "heart",
  跑步技术: "gear",
};

// 数据列表（初始为空，从后端加载）
const categories = ref<any[]>([]);
const hotQuestions = ref<{ question: string; word: string }[]>([]);
const messages = ref<ChatMessage[]>([]);
const history = ref<HistoryItem[]>([]);

// 分类 key → 首页 RUN! 同款大字母（Anton 展示字体、白字红影）
const HOT_WORD_MAP: Record<string, string> = {
  beginner: "START!",
  injury: "KNEE!",
  equipment: "SHOES!",
  training: "PACE!",
  nutrition: "FUEL!",
  recovery: "RESET!",
};

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
        url: `${BASE_URL}/api${url}`,
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
    // 注意：后端 /qa/categories 返回的是 emoji 图标，uni-icons 无法渲染，
    // 且 PRD 明确禁用 emoji 图标，因此这里按分类名本地映射线性图标。
    const source =
      categoriesData?.length > 0
        ? categoriesData
        : [
            { name: "训练计划" },
            { name: "装备选择" },
            { name: "伤痛预防" },
            { name: "跑步技术" },
          ];

    categories.value = source.map((item: any) => ({
      name: item.name,
      icon: CATEGORY_ICONS[item.name] || "compose",
    }));

    hotQuestions.value =
      hotQuestionsData?.length > 0
        ? hotQuestionsData.map((item: any) => ({
            question: item.question || String(item),
            word:
              HOT_WORD_MAP[item.category] || "RUN!",
          }))
        : [
            { question: "第一次跑步该跑多远？", category: "beginner" },
            { question: "跑完膝盖疼怎么办？", category: "injury" },
            { question: "新手怎么选跑鞋？", category: "equipment" },
            { question: "跑步时心率多少合适？", category: "training" },
            { question: "晨跑好还是夜跑好？", category: "training" },
            { question: "跑步前要热身多久？", category: "recovery" },
            { question: "如何提高跑步配速？", category: "training" },
          ].map((item) => ({
            question: item.question,
            word: HOT_WORD_MAP[item.category] || "RUN!",
          }));
  } catch (error) {
    console.error("初始化数据加载失败");
  }
});

// 页面显示时检查是否需要自动聚焦输入框（来自首页"问问题"按钮）
onShow(() => {
  setTabBarIndex(2);
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
    // 后端契约：GET /api/qa/history?pageNum&pageSize，返回 PageResult{ list, total, ... }
    const historyData = await request({
      url: "/qa/history",
      method: "GET",
      data: { pageNum: 1, pageSize: 20 },
    });

    history.value = (historyData?.list || []).map((item: any) => ({
      id: item.id,
      date:
        item.createTime?.split("T")[0] || item.createTime?.split(" ")[0] || "",
      time: (item.createTime || "").split("T")[1]?.substring(0, 5) || "",
      question: item.question,
      summary: item.answer?.substring(0, 50) + "..." || "",
    }));
  } catch (error) {
    console.error("加载历史记录失败");
    uni.showToast({ title: "加载历史失败", icon: "none" });
  } finally {
    loading.value = false;
  }
};

// ==================== 伤病预防 ====================
// 分类与后端 category key 对齐（key 传参、label 展示）：knee=膝部 / ankle=足踝 / shin=小腿
const injuryCategories = [
  { key: "", label: "全部" },
  { key: "knee", label: "膝部" },
  { key: "ankle", label: "足踝" },
  { key: "shin", label: "小腿" },
];
const injuryCategory = ref("");
const injuryArticles = ref<any[]>([]);
const injuryLoaded = ref(false);

// 加载文章列表（"全部"时 key 为空，不传 category，后端返回全部）
const loadInjuryArticles = async () => {
  injuryLoaded.value = false;
  try {
    const params: any = {};
    if (injuryCategory.value) {
      params.category = injuryCategory.value;
    }
    const data = await request({
      url: "/injury/articles",
      method: "GET",
      data: params,
    });
    injuryArticles.value = data || [];
  } catch (error) {
    console.error("加载伤病预防文章失败:", error);
    injuryArticles.value = [];
  } finally {
    injuryLoaded.value = true;
  }
};

// 切换分类
const switchInjuryCategory = (cat: string) => {
  if (injuryCategory.value === cat) return;
  injuryCategory.value = cat;
  loadInjuryArticles();
};

// 打开文章详情页
const onInjuryTap = (article: { id: number }) => {
  uni.navigateTo({ url: `/pages/qa/injury-detail?id=${article.id}` });
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

// 切换 Tab
const switchTab = async (i: number) => {
  currentTab.value = i;
  // 每次切到历史Tab都重新拉取，保证数据最新
  if (i === 1) {
    await loadHistory();
  }
  // 伤病预防：每次进入同样拉取（按当前选中分类）
  if (i === 2) {
    await loadInjuryArticles();
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
  messages.value.push({ role: "user", content });
  inputText.value = "";
  scrollToBottom();

  try {
    loading.value = true;
    apiError.value = null;
    // 打字指示器气泡出现后滚到底部
    scrollToBottom();

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
const onHotQuestionTap = (q: { question: string; word: string }) => {
  sendMessage(q.question);
};

const onMoreQuestions = () => {
  if (hotQuestions.value.length > 3) {
    const rest = hotQuestions.value.slice(3).map((q) => q.question);
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

// 分类卡点击/按压态（唯一反馈：红条拉长 + 图标变红）
const pressedCategory = ref("");
const isCategoryHot = (name: string) =>
  activeCategory.value === name || pressedCategory.value === name;
const onCategoryPressStart = (name: string) => {
  pressedCategory.value = name;
};
const onCategoryPressEnd = () => {
  pressedCategory.value = "";
};

// 点击分类
const onCategoryTap = (item: any) => {
  activeCategory.value = item.name;
  sendMessage(`请介绍一下${item.name}方面的建议`);
};

// 点击历史卡片：重新提问该问题
const onHistoryTap = (item: HistoryItem) => {
  currentTab.value = 0;
  sendMessage(item.question);
};
</script>

<style lang="scss">
.qa-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
  overflow: hidden;
  background: $rw-black;
}

/* 背景网点纹理（装饰层 z-index 0，不拦截手势） */
.qa-page__dots {
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

/* ========== 二级导航：红切角选中块（总高 44px + 1px 贯穿地基线） ========== */
.sub-nav {
  position: relative;
  z-index: 20;
  flex-shrink: 0;
  display: flex;
  gap: 16rpx;
  padding: 0 $rw-sp-4;
  background: $rw-black;
  border-bottom: 1rpx solid #2a2a2a;
}

.sub-nav__item {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(16rpx 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
  transition: $rw-transition;

  &.is-active {
    background: $rw-red;
  }
}

.sub-nav__text {
  font-size: 28rpx;
  color: #9a9a9a;
  letter-spacing: 2rpx;
}

.sub-nav__item.is-active .sub-nav__text {
  color: $rw-white;
  font-weight: 700;
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

/* ==================================================================
   分类入口 V4（终版 · 锁死，不再迭代）
   定位：运动品牌控制面板件 —— 安静的功能键
   卡体：#141414 实底 / 左上+右下各切 12px / 固定高 92px
         （切角复用欢迎卡 rw-card 的 24rpx 六点式，方向尺寸全站一致）
   三要素：18px Anton 编号 #E8442E（绝对定位左上 8px/8px）
           + 28px 白线条图标（水平垂直居中）
           + 13px #FFFFFF 粗体标签（与图标间距 8px）
   交互：按下/选中 → 背景 #1F1F1F + 图标 #E8442E
         仅过渡 background-color 0.15s linear，无缩放/旋转/上浮/发光
   禁止：边框 / 阴影 / 渐变 / 额外红色 / 任何倾斜
   ================================================================== */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  padding: 32rpx 24rpx;
}

.category-card {
  position: relative;
  height: 184rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #141414;
  clip-path: polygon(
    24rpx 0,
    100% 0,
    100% calc(100% - 24rpx),
    calc(100% - 24rpx) 100%,
    0 100%,
    0 24rpx
  );
  transition: background-color 0.15s linear;
}

.category-card--active {
  background: #1f1f1f;
}

/* ① 编号：Anton 18px #E8442E，绝对定位于卡左上角（8px/8px），不与图标文字重叠 */
.category-card__num {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  font-family: $rw-font-display;
  font-size: 36rpx;
  line-height: 1;
  color: #e8442e;
}

/* ③ 标签：13px #FFFFFF，600，与图标间距 8px */
.category-name {
  margin-top: 16rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #ffffff;
}

/* ================================================================
   热门问题 — 字母海报式（首页 RUN! 同款：Anton 大字母 + 白字红影）
   无底板块，纯排版；奇偶上下交错，横向滑动
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
  background: $rw-red;
  clip-path: polygon(12rpx 0, 100% 0, calc(100% - 12rpx) 100%, 0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-white;
  letter-spacing: 1rpx;
}

/* 横滑轨道：每项 = 红编号 + 大字母 + 问题 + 灰短线 */
/* 微信 scroll-view 横滚内容撑不起父级高度，必须显式给高（条目~235rpx + 交错36rpx + padding） */
.hot-scroll {
  width: 100%;
  height: 320rpx;
  white-space: nowrap;
  padding: 8rpx 24rpx 24rpx;
  box-sizing: border-box;
}

.hot-item {
  display: inline-flex;
  flex-direction: column;
  vertical-align: top;
  width: 256rpx;
  margin-right: 48rpx;
}

.hot-item:last-child {
  margin-right: 24rpx;
}

/* 偶数项下沉错位（海报交错节奏） */
.hot-item--even {
  margin-top: 36rpx;
}

.hot-item__num {
  font-family: $rw-font-display;
  font-size: 26rpx;
  font-style: italic;
  color: $rw-red;
  line-height: 1;
}

/* 大字母：首页 RUN! 同款白字红影 + 微旋转 */
.hot-item__word {
  margin-top: 6rpx;
  font-family: $rw-font-display;
  font-size: 60rpx;
  line-height: 1.05;
  color: $rw-white;
  text-shadow: 6rpx 6rpx 0 $rw-red;
  transform: rotate(-4deg);
  width: fit-content;
  transition: $rw-transition;
}

.hot-item--even .hot-item__word {
  transform: rotate(3deg);
}

.hot-item__q {
  margin-top: 14rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: $rw-white;
  line-height: 1.45;
  white-space: normal;
}

/* 底部灰短线：按压变红伸长 */
.hot-item__hint {
  margin-top: 10rpx;
  width: 52rpx;
  height: 4rpx;
  background: rgba(255, 255, 255, 0.25);
  transition: $rw-transition;
}

.hot-item--pressed .hot-item__word {
  color: #ffd6d9;
}

.hot-item--pressed .hot-item__hint {
  background: $rw-red;
  width: 84rpx;
}

/* ========== AI 欢迎卡 ========== */
.welcome-zone {
  position: relative;
  margin: 24rpx;
}

.welcome {
  display: flex;
  flex-direction: column;
}

.welcome__title {
  font-family: $rw-font-cn-title;
  font-size: 40rpx;
  font-weight: 400;
  color: $rw-white;
  line-height: 1.4;
  -webkit-text-stroke: 2rpx #000000;
  paint-order: stroke fill;
}

.welcome__title-en {
  font-family: $rw-font-display;
}

.welcome__desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $rw-gray;
  line-height: 1.7;
}

.welcome__list {
  margin-top: 20rpx;
}

.welcome__item {
  display: flex;
  align-items: flex-start;
  margin-top: 12rpx;
}

.welcome__bullet {
  width: 16rpx;
  height: 16rpx;
  background: $rw-red;
  transform: rotate(45deg);
  margin: 8rpx 16rpx 0 0;
  flex-shrink: 0;
}

.welcome__item-text {
  font-size: 26rpx;
  color: $rw-white;
  line-height: 1.6;
}

/* ========== AI 思考中：打字指示器 ========== */
.typing-bubble {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
}

.typing-dots {
  display: flex;
  align-items: center;
  margin-right: 16rpx;
}

.typing-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $rw-red;
  margin-right: 10rpx;
  animation: typing-blink 1s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  margin-right: 0;
  animation-delay: 0.4s;
}

@keyframes typing-blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-6rpx);
  }
}

.typing-text {
  font-size: 26rpx;
  color: $rw-gray;
  letter-spacing: 1rpx;
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
  max-width: 80%;
}

/* 用户气泡切角与 AI 气泡镜像：尾巴侧（右上）切角 */
.bubble--user {
  background: $rw-red;
  clip-path: polygon(0 0, calc(100% - 24rpx) 0, 100% 24rpx, 100% 100%, 0 100%);
}

.bubble-text {
  font-size: 28rpx;
  color: $rw-white;
  line-height: 1.6;
}

/* AI 气泡：黑底白字 + 1px 灰边框 + 右上角白色四角星 */
.ai-cloud-bubble {
  position: relative;
  max-width: 82%;
  padding: 26rpx 36rpx;
  background: $rw-panel;
  border: 2rpx solid #2a2a2e;
  clip-path: polygon(24rpx 0, 100% 0, 100% 100%, 0 100%, 0 24rpx);
}

/* 四角星：8px，落在右侧 padding 内，不侵入文字 16px 安全区 */
.ai-sparkle {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 16rpx;
  height: 16rpx;
  background: $rw-white;
  opacity: 0.7;
  clip-path: polygon(
    50% 0%,
    60% 40%,
    100% 50%,
    60% 60%,
    50% 100%,
    40% 60%,
    0% 50%,
    40% 40%
  );
  pointer-events: none;
}

.ai-cloud-text {
  font-size: 28rpx;
  color: $rw-white;
  line-height: 1.65;
}

/* 参考来源 */
.sources {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
}

.sources-label {
  color: $rw-gray;
}

.source-item {
  color: $rw-gray;
}

/* 安全提示（红左边框 + 红字） */
.safety-tip {
  margin-top: 16rpx;
  padding: 20rpx 24rpx;
  background: rgba(230, 0, 18, 0.08);
  border-left: 6rpx solid $rw-red;
}

.safety-text {
  font-size: 28rpx;
  color: $rw-red;
  line-height: 1.6;
}

/* ========== 底部输入区（页面流内 flex 收尾，底部让位 custom-tab-bar 110rpx） ========== */
.input-bar {
  position: relative;
  z-index: 20;
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  padding: 24rpx 28rpx;
  padding-bottom: calc(24rpx + 110rpx + env(safe-area-inset-bottom));
  background: $rw-black;
  border-top: 2rpx solid $rw-line;
}

/* textarea 自增高：min-height 起步，随内容换行向上生长 */
.input-field {
  flex: 1;
  min-height: 88rpx;
  height: auto;
  padding: 22rpx 28rpx;
  background: $rw-panel;
  border: 2rpx solid $rw-white;
  clip-path: polygon(20rpx 0, 100% 0, calc(100% - 20rpx) 100%, 0 100%);
  font-size: 28rpx;
  color: $rw-white;
  line-height: 1.5;
  box-sizing: border-box;
  width: auto;
  transition: $rw-transition;
}

.input-field--focus {
  border-color: $rw-red;
}

.input-placeholder {
  color: $rw-gray;
  font-size: 28rpx;
}

.send-btn {
  margin-left: 20rpx;
  flex-shrink: 0;
  width: 88rpx;
  height: 88rpx;
  background: $rw-panel;
  border: 2rpx solid $rw-white;
  clip-path: polygon(20rpx 0, 100% 0, calc(100% - 20rpx) 100%, 0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: $rw-transition;
}

.send-btn--active {
  background: $rw-red;
  border-color: $rw-red;
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
  color: $rw-gray;
  margin: 0 24rpx 12rpx;
}

.history-card {
  margin: 0 24rpx;
}

.history-item {
  padding: 24rpx 0;
  border-bottom: 2rpx solid $rw-line;
}

.history-item:last-child {
  border-bottom: none;
}

.history-question {
  display: block;
  font-size: 28rpx;
  color: $rw-white;
  margin-bottom: 8rpx;
}

.history-summary {
  display: block;
  font-size: 24rpx;
  color: $rw-gray;
  line-height: 1.5;
  margin-bottom: 8rpx;
}

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-time {
  font-size: 24rpx;
  color: $rw-gray;
  font-feature-settings: "tnum";
}

.history-reask {
  font-size: 22rpx;
  color: $rw-red;
  letter-spacing: 1rpx;
}

.history-item--pressed {
  opacity: 0.7;
}

/* ========== 伤病预防视图：分类筛选 + 文章卡片列表 ========== */
.injury-view {
  padding-bottom: 24rpx;
}

/* 分类筛选 chips：未选=灰描边灰字，选中=红底白字（斜切语言同二级导航） */
.injury-filter {
  display: flex;
  gap: 16rpx;
  padding: 28rpx 24rpx 4rpx;
}

.injury-filter__chip {
  padding: 14rpx 26rpx;
  background: $rw-panel;
  border: 2rpx solid rgba(255, 255, 255, 0.25);
  clip-path: polygon(14rpx 0, 100% 0, calc(100% - 14rpx) 100%, 0 100%);
  transition: $rw-transition;
}

.injury-filter__chip--active {
  background: $rw-red;
  border-color: $rw-red;
}

.injury-filter__chip--pressed {
  opacity: 0.75;
}

.injury-filter__text {
  font-size: 24rpx;
  color: $rw-gray;
  letter-spacing: 1rpx;
}

.injury-filter__chip--active .injury-filter__text {
  color: $rw-white;
  font-weight: 600;
}

/* 文章卡片：安静知识卡（灰描边 + 六点式切角） */
.injury-list {
  padding: 12rpx 24rpx 0;
}

.injury-card {
  margin-top: 24rpx;
  padding: 28rpx;
  background: $rw-panel;
  border: 2rpx solid #2a2a2e;
  @include rw-card-clip;
  transition: $rw-transition;
}

.injury-card--pressed {
  opacity: 0.7;
}

/* 分类小标签：红切角块白字 */
.injury-card__tag {
  display: flex;
  width: fit-content;
  padding: 6rpx 18rpx;
  background: $rw-red;
  clip-path: polygon(10rpx 0, 100% 0, calc(100% - 10rpx) 100%, 0 100%);
}

.injury-card__tag-text {
  font-size: 20rpx;
  color: $rw-white;
  letter-spacing: 1rpx;
}

.injury-card__title {
  display: block;
  margin-top: 18rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: $rw-white;
  line-height: 1.5;
}

.injury-card__summary {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $rw-gray;
  line-height: 1.6;
}

.injury-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}

.injury-card__read {
  font-size: 22rpx;
  color: $rw-gray;
  font-feature-settings: "tnum";
}

.injury-card__more {
  font-size: 22rpx;
  color: $rw-red;
  letter-spacing: 1rpx;
}

/* ========== 滚动锚点 ========== */
.anchor-bottom {
  height: 2rpx;
  width: 100%;
}

/* 输入栏已不再悬浮压住消息区，锚点只留常规收尾间距 */
.anchor-bottom--input {
  height: 32rpx;
}
</style>
