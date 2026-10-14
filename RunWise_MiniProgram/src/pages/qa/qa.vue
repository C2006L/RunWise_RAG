<template>
  <view class="qa-page">
    <!-- 背景网点纹理（P5 语言，纯 CSS 零体积） -->
    <view class="qa-page__dots"></view>

    <!-- 三个视图始终共用同一套顶部导航 -->
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
        <!-- 尚未提问：恢复旧版 P5R 四入口与漫画热门问题 -->
        <view v-if="!hasConversation" class="ask-landing">
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
                size="25"
                :color="isCategoryHot(item.name) ? '#E8442E' : '#FFFFFF'"
              ></uni-icons>
              <text class="category-name">{{ item.name }}</text>
            </view>
          </view>

          <view class="hot-section">
            <view class="section-header">
              <view class="section-icon-wrap">
                <uni-icons type="fire" size="16" color="#ffffff"></uni-icons>
              </view>
              <text class="section-title">热门问题 / HOT MISSIONS</text>
              <text class="section-more">SWIPE →</text>
            </view>

            <scroll-view
              class="hot-poster-scroll"
              scroll-x
              :show-scrollbar="false"
              :scroll-with-animation="true"
            >
              <view class="hot-poster-track">
                <view
                  v-for="(q, idx) in hotQuestions.slice(0, 8)"
                  :key="q.question"
                  class="hot-poster"
                  :class="`hot-poster--${(idx % 4) + 1}`"
                  hover-class="hot-poster--pressed"
                  :hover-stay-time="140"
                  @tap="onHotQuestionTap(q)"
                >
                  <text class="hot-poster__num">0{{ idx + 1 }}</text>
                  <text class="hot-poster__word">{{ q.word }}</text>
                  <text class="hot-poster__question">{{ q.question }}</text>
                  <view class="hot-poster__slash"></view>
                </view>
              </view>
            </scroll-view>
          </view>

          <view class="assistant-note">
            <text class="assistant-note__title">ASK RUNWISE!</text>
            <text class="assistant-note__text">
              训练、装备、伤痛都可以问。输入问题，开始你的任务简报。
            </text>
          </view>
        </view>

        <!-- 对话区域 -->
        <view class="chat-area" v-if="hasConversation">
          <view class="chat-session-head">
            <view>
              <text class="chat-session-head__en">CURRENT MISSION</text>
              <text class="chat-session-head__cn">RunWise 训练对话</text>
            </view>
            <text v-if="!loading" class="chat-session-head__new" @tap="startNewConversation">
              + 新对话
            </text>
          </view>
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
                <text v-if="msg.missionTitle" class="ai-mission-title">{{ msg.missionTitle }}</text>
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
          id="qa-anchor-bottom"
          class="anchor-bottom anchor-bottom--input"
        ></view>
      </view>

      <!-- 历史视图 -->
      <view v-show="currentTab === 1" class="history-view">
        <view v-if="historyGroups.length">
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
                v-for="item in group.items"
                :key="item.key"
                @tap="onHistoryTap(item)"
              >
                <text class="history-question">{{ item.question }}</text>
                <text class="history-summary">{{ item.summary }}</text>
                <view class="history-meta">
                  <text class="history-time">更新于 {{ item.time || item.date }}</text>
                  <text class="history-reask">打开会话 →</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view v-else-if="historyLoading" class="history-sync">SYNCING CASE FILES...</view>
        <view v-else-if="historyLoaded" class="history-empty">
          <text class="history-empty__en">NO CASE FILES!</text>
          <text class="history-empty__title">还没有真实问答记录</text>
          <text class="history-empty__desc">完成一次提问后，会话会自动保存在这里。</text>
          <view class="history-empty__button" hover-class="history-empty__button--pressed" @tap="startFromHistory">
            开始第一次提问 →
          </view>
        </view>
        <view id="history-anchor-bottom" class="anchor-bottom"></view>
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

        <!-- 空态只陈述真实数据，不伪造医学文章 -->
        <view v-else-if="injuryLoaded" class="injury-empty">
          <text class="injury-empty__en">NO INTEL FOUND!</text>
          <text class="injury-empty__title">当前分类暂无资料。</text>
          <text class="injury-empty__desc">你可以直接描述不适位置和运动情况，让 RunWise 帮你整理下一步。</text>
          <view class="injury-empty__button" hover-class="injury-empty__button--pressed" @tap="askAboutInjury">
            向 RunWise 提问 →
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部输入区（textarea 支持多行换行，发送统一走右侧按钮） -->
    <view
      class="input-bar"
      :class="{ 'input-bar--landing': !hasConversation }"
      v-if="currentTab === 0"
    >
      <textarea
        class="input-field"
        :class="{ 'input-field--focus': shouldFocusInput }"
        v-model="inputText"
        :focus="shouldFocusInput"
        auto-height
        :show-confirm-bar="false"
        :disable-default-padding="true"
        :adjust-position="true"
        :cursor-spacing="20"
        :maxlength="500"
        :placeholder="hasConversation ? '继续追问...' : '输入你的跑步问题...'"
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
import {
  createQaSessionId,
  findQaSession,
  getCurrentQaSessionId,
  loadQaSessions,
  saveQaSession,
  setCurrentQaSessionId,
  type QaSession,
} from "@/utils/qa-history";


// 接口定义
interface ChatMessage {
  id?: number;
  role: "user" | "assistant";
  content: string;
  missionTitle?: string;
  sources?: string[];
  safetyTip?: string;
}

interface HistoryItem {
  key: string;
  id?: number;
  sessionId?: string;
  date: string;
  time?: string;
  updatedAt: string;
  question: string;
  summary: string;
  answer?: string;
  sources?: string[];
  messages?: ChatMessage[];
  source: "local" | "server";
}

// 二级导航（伤病预防视图 P2 接入后端文章数据）
const tabs = ["问答", "历史", "伤病预防"];

// 状态管理
const currentTab = ref(0);
const inputText = ref("");
const intoView = ref("");
const loading = ref(false);
const apiError = ref<string | null>(null);
const shouldFocusInput = ref(false);
const activeSessionId = ref("");

const activeCategory = ref("");

// 分类名 → uni-icons 线性图标（后端返回 emoji，不可用；PRD 禁 emoji）
const CATEGORY_ICONS: Record<string, string> = {
  训练计划: "compose",
  装备选择: "cart",
  伤痛预防: "heart",
  跑步技术: "gear",
};

const CATEGORY_QUESTIONS: Record<string, string> = {
  训练计划: "请帮我制定一个适合当前水平的跑步训练计划。",
  装备选择: "我该如何根据自己的跑步情况选择跑鞋和装备？",
  伤痛预防: "跑步训练中应该如何预防常见伤痛？",
  跑步技术: "请分析新手最需要掌握的跑步技术要点。",
};

// 数据列表（初始为空，从后端加载）
const categories = ref<any[]>([]);
const hotQuestions = ref<{ question: string; word: string }[]>([]);
const messages = ref<ChatMessage[]>([]);
const history = ref<HistoryItem[]>([]);
const hasConversation = computed(() => messages.value.length > 0);
const historyLoaded = ref(false);
const historyLoading = ref(false);

// 分类 key → 首页 RUN! 同款大字母（Anton 展示字体、白字红影）
const HOT_WORD_MAP: Record<string, string> = {
  beginner: "START!",
  injury: "KNEE!",
  equipment: "SHOE!",
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
  restoreCurrentConversation();
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
      messages.value = [];
      activeSessionId.value = "";
      setCurrentQaSessionId("");
      sendMessage(question);
    }, 300);
  }
});

function toHistoryDate(value: string) {
  return value?.split("T")[0] || value?.split(" ")[0] || "未知日期";
}

function toHistoryTime(value: string) {
  const normalized = value?.replace(" ", "T") || "";
  return normalized.split("T")[1]?.substring(0, 5) || "";
}

function sessionToHistoryItem(session: QaSession): HistoryItem {
  const firstQuestion = session.messages.find((message) => message.role === "user");
  const lastAnswer = [...session.messages]
    .reverse()
    .find((message) => message.role === "assistant");
  return {
    key: `local-${session.id}`,
    sessionId: session.id,
    date: toHistoryDate(session.updatedAt),
    time: toHistoryTime(session.updatedAt),
    updatedAt: session.updatedAt,
    question: session.title || firstQuestion?.content || "未命名会话",
    summary: lastAnswer?.content?.slice(0, 72) || "会话已创建，等待回答",
    messages: session.messages.map((message) => ({ ...message })),
    source: "local",
  };
}

function persistCurrentConversation() {
  if (!activeSessionId.value) activeSessionId.value = createQaSessionId();
  const firstQuestion = messages.value.find((message) => message.role === "user");
  const now = new Date().toISOString();
  const existing = findQaSession(activeSessionId.value);
  const validMessages = messages.value.filter(
    (message) => !message.content.startsWith("请求失败："),
  );
  if (!firstQuestion || !validMessages.some((message) => message.role === "assistant")) return;

  try {
    saveQaSession({
      id: activeSessionId.value,
      title: firstQuestion.content.slice(0, 40),
      createdAt: existing?.createdAt || now,
      updatedAt: now,
      messages: validMessages.map((message) => ({ ...message })),
    });
  } catch (error) {
    console.warn("本地会话保存失败，不影响当前回答展示", error);
  }
}

function restoreCurrentConversation() {
  if (messages.value.length) return;
  const id = getCurrentQaSessionId();
  const session = id ? findQaSession(id) : undefined;
  if (!session) return;
  activeSessionId.value = session.id;
  messages.value = session.messages.map((message) => ({ ...message }));
  scrollToBottom();
}

// 本地保存多轮会话，服务端真实单条问答作为兼容回退；两者都不注入 Mock。
const loadHistory = async () => {
  if (historyLoading.value) return;
  historyLoading.value = true;

  const localItems = loadQaSessions().map(sessionToHistoryItem);
  history.value = localItems;
  historyLoaded.value = true;
  let serverItems: HistoryItem[] = [];

  try {
    const historyData = await request({
      url: "/qa/history",
      method: "GET",
      data: { pageNum: 1, pageSize: 50 },
    });

    const localPairs = new Set<string>();
    localItems.forEach((item) => {
      const sessionMessages = item.messages || [];
      sessionMessages.forEach((message, index) => {
        if (message.role !== "user") return;
        const answer = sessionMessages
          .slice(index + 1)
          .find((candidate) => candidate.role === "assistant");
        if (answer) localPairs.add(`${message.content}\n${answer.content}`);
      });
    });

    serverItems = (historyData?.list || [])
      .filter((item: any) => !localPairs.has(`${item.question}\n${item.answer || ""}`))
      .map((item: any) => {
        const updatedAt = item.updateTime || item.createTime || "";
        return {
          key: `server-${item.id}`,
          id: item.id,
          date: toHistoryDate(updatedAt),
          time: toHistoryTime(updatedAt),
          updatedAt,
          question: item.question,
          summary: (item.answer || "").slice(0, 72),
          answer: item.answer || "",
          sources: item.sources ? String(item.sources).split("、").filter(Boolean) : [],
          source: "server" as const,
        };
      });
  } catch (error) {
    console.warn("服务端历史加载失败，继续展示本地真实会话", error);
  } finally {
    history.value = [...localItems, ...serverItems].sort((a, b) =>
      b.updatedAt.localeCompare(a.updatedAt),
    );
    historyLoading.value = false;
    historyLoaded.value = true;
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
const currentInjuryLabel = computed(
  () => injuryCategories.find((item) => item.key === injuryCategory.value)?.label || "伤病预防",
);

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

const askAboutInjury = () => {
  currentTab.value = 0;
  messages.value = [];
  activeSessionId.value = "";
  setCurrentQaSessionId("");
  inputText.value =
    injuryCategory.value === ""
      ? "我有一个跑步伤病预防方面的问题："
      : `我有一个关于${currentInjuryLabel.value}不适的问题：`;
  nextTick(() => {
    shouldFocusInput.value = true;
  });
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
      intoView.value = "qa-anchor-bottom";
    }, 30);
  });
};

// 根据提问内容给回答加一个简短、有辨识度的任务标题。
// 标题只负责表达语气，专业结论仍完全来自后端回答与安全提示。
const buildMissionTitle = (question: string) => {
  if (/疼|痛|伤|肿|发热|头晕|不适/.test(question)) {
    return "SAFETY CHECK · 先判断是否该停跑";
  }
  if (/不想|疲劳|累|硬撑|没状态|没动力/.test(question)) {
    return "MISSION CHECK · 今天必须硬撑吗？";
  }
  if (/配速|心率|节奏|间歇/.test(question)) {
    return "PACE CHECK · 先稳住节奏";
  }
  if (/计划|目标|跑量|训练/.test(question)) {
    return "TRAINING BRIEF · 把目标拆成下一步";
  }
  return "RUNWISE BRIEF · 先给你一个可执行答案";
};

// 发送消息（调用后端RAG接口）
const sendMessage = async (text: string) => {
  const content = text.trim();
  if (!content) return;

  // 立即显示用户消息
  if (!activeSessionId.value) activeSessionId.value = createQaSessionId();
  messages.value.push({ role: "user", content });
  inputText.value = "";
  shouldFocusInput.value = false;
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
      missionTitle: buildMissionTitle(content),
      sources: response.sources || [],
      safetyTip: response.safetyTip,
    });

    persistCurrentConversation();
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

const startNewConversation = () => {
  if (loading.value) return;
  messages.value = [];
  inputText.value = "";
  activeSessionId.value = "";
  setCurrentQaSessionId("");
  activeCategory.value = "";
  intoView.value = "";
  nextTick(() => {
    shouldFocusInput.value = true;
  });
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
  sendMessage(CATEGORY_QUESTIONS[item.name] || `请介绍一下${item.name}方面的建议。`);
};

const startFromHistory = () => {
  currentTab.value = 0;
  startNewConversation();
};

// 点击历史记录还原真实消息，不重复调用模型。
const onHistoryTap = (item: HistoryItem) => {
  let restoredMessages = item.messages?.map((message) => ({ ...message })) || [];

  if (!restoredMessages.length && item.answer) {
    restoredMessages = [
      { role: "user", content: item.question },
      {
        role: "assistant",
        content: item.answer,
        missionTitle: buildMissionTitle(item.question),
        sources: item.sources || [],
      },
    ];
  }

  if (!restoredMessages.length) return;

  const sessionId = item.sessionId || `server-${item.id}`;
  activeSessionId.value = sessionId;
  messages.value = restoredMessages;
  const now = item.updatedAt || new Date().toISOString();
  try {
    saveQaSession({
      id: sessionId,
      title: item.question.slice(0, 40),
      createdAt: now,
      updatedAt: now,
      messages: restoredMessages.map((message) => ({ ...message })),
    });
  } catch (error) {
    console.warn("历史会话缓存失败", error);
  }
  currentTab.value = 0;
  scrollToBottom();
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

/* ========== 未提问状态：恢复四入口 + 三张错位漫画海报 ========== */
.ask-landing {
  padding: 8rpx 0 22rpx;
  overflow: hidden;
}

.ask-landing .category-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
  padding: 20rpx 24rpx 28rpx;
}

.ask-landing .category-card {
  min-width: 0;
  height: 132rpx;
  flex-direction: column;
  gap: 0;
  background: transparent;
  border: 0;
  clip-path: none;
  transition: transform 0.14s ease, opacity 0.14s ease;
}

.ask-landing .category-card::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: 8rpx;
  width: 34rpx;
  height: 4rpx;
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(-50%) skewX(-28deg);
  transition: width 0.14s ease, background-color 0.14s ease;
}

.ask-landing .category-card--active {
  background: transparent;
  transform: translateY(4rpx);
}

.ask-landing .category-card--active::after {
  width: 64rpx;
  background: $rw-red;
}

.ask-landing .category-card__num {
  top: 12rpx;
  left: 12rpx;
  font-size: 27rpx;
}

.ask-landing .category-name {
  margin-top: 10rpx;
  max-width: 100%;
  font-size: 21rpx;
  line-height: 1.2;
  white-space: nowrap;
}

.section-more {
  margin-left: auto;
  font-family: $rw-font-display;
  font-size: 22rpx;
  color: $rw-red;
  letter-spacing: 1rpx;
}

.hot-poster-scroll {
  width: 100%;
  height: 326rpx;
  white-space: nowrap;
  box-sizing: border-box;
}

.hot-poster-track {
  height: 100%;
  padding: 0 92rpx 18rpx 24rpx;
  box-sizing: border-box;
  white-space: nowrap;
}

.hot-poster {
  position: relative;
  display: inline-flex;
  vertical-align: top;
  width: 254rpx;
  min-height: 238rpx;
  margin-right: 24rpx;
  padding: 18rpx 12rpx 22rpx;
  box-sizing: border-box;
  flex-direction: column;
  overflow: visible;
  background: transparent;
  border: 0;
  clip-path: none;
  white-space: normal;
  transition: opacity 0.14s ease;
}

.hot-poster:last-child {
  margin-right: 92rpx;
}

.hot-poster--1 {
  margin-top: 2rpx;
}

.hot-poster--2 {
  margin-top: 34rpx;
}

.hot-poster--3 {
  margin-top: 12rpx;
}

.hot-poster--4 {
  margin-top: 28rpx;
}

.hot-poster--pressed {
  opacity: 0.62;
}

.hot-poster__num {
  font-family: $rw-font-display;
  font-size: 22rpx;
  font-style: italic;
  color: $rw-red;
  line-height: 1;
}

.hot-poster__word {
  display: block;
  width: fit-content;
  max-width: 230rpx;
  font-family: $rw-font-display;
  margin-top: 8rpx;
  font-size: 56rpx;
  color: $rw-white;
  line-height: 1;
  letter-spacing: -1rpx;
  white-space: nowrap;
  text-shadow: 6rpx 6rpx 0 $rw-red;
  transform: rotate(-5deg) translateX(4rpx);
  transform-origin: left center;
}

.hot-poster--2 .hot-poster__word {
  transform: rotate(3deg) translateX(-2rpx);
}

.hot-poster--3 .hot-poster__word {
  transform: rotate(-2deg) translateX(8rpx);
}

.hot-poster--4 .hot-poster__word {
  transform: rotate(4deg) translateX(2rpx);
}

.hot-poster__question {
  display: block;
  margin-top: 18rpx;
  max-width: 230rpx;
  font-size: 23rpx;
  font-weight: 600;
  color: $rw-white;
  line-height: 1.4;
  word-break: break-all;
  white-space: normal;
}

.hot-poster__slash {
  position: absolute;
  left: 12rpx;
  bottom: 8rpx;
  width: 82rpx;
  height: 6rpx;
  background: $rw-red;
  transform: skewX(-34deg);
}

.assistant-note {
  margin: 18rpx 24rpx 0;
  padding: 18rpx 22rpx;
  display: flex;
  align-items: center;
  background: #18181b;
  border-left: 6rpx solid $rw-red;
  clip-path: polygon(0 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
}

.assistant-note__title {
  flex-shrink: 0;
  font-family: $rw-font-display;
  font-size: 27rpx;
  color: $rw-white;
  text-shadow: 4rpx 3rpx 0 $rw-red;
}

.assistant-note__text {
  margin-left: 22rpx;
  font-size: 21rpx;
  color: $rw-gray;
  line-height: 1.4;
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

.chat-session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
  padding: 18rpx 22rpx;
  background: #151518;
  border-left: 6rpx solid $rw-red;
  clip-path: polygon(0 0, 100% 0, calc(100% - 16rpx) 100%, 0 100%);
}

.chat-session-head__en,
.chat-session-head__cn {
  display: block;
}

.chat-session-head__en {
  font-family: $rw-font-display;
  font-size: 24rpx;
  color: $rw-white;
  letter-spacing: 1rpx;
}

.chat-session-head__cn {
  margin-top: 5rpx;
  font-size: 20rpx;
  color: $rw-gray;
}

.chat-session-head__new {
  flex-shrink: 0;
  margin-left: 18rpx;
  font-size: 22rpx;
  color: $rw-red;
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
  display: block;
  font-size: 28rpx;
  color: $rw-white;
  line-height: 1.65;
}

.ai-mission-title {
  display: block;
  margin-bottom: 16rpx;
  padding-right: 18rpx;
  font-family: $rw-font-display;
  font-size: 25rpx;
  font-weight: 700;
  color: $rw-white;
  line-height: 1.35;
  letter-spacing: 1rpx;
}

.ai-mission-title::after {
  content: "";
  display: block;
  width: 56rpx;
  height: 4rpx;
  margin-top: 12rpx;
  background: $rw-red;
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

.input-bar--landing {
  border-top-color: $rw-red;
}

/* textarea 自增高：min-height 起步，随内容换行向上生长 */
.input-field {
  flex: 1;
  min-height: 88rpx;
  max-height: 180rpx;
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
  background: #141417;
  border: 2rpx solid #333338;
  clip-path: polygon(18rpx 0, 100% 0, 100% calc(100% - 18rpx), calc(100% - 18rpx) 100%, 0 100%, 0 18rpx);
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

.history-sync {
  margin: 72rpx 24rpx 0;
  font-family: $rw-font-display;
  font-size: 30rpx;
  color: $rw-gray;
  letter-spacing: 2rpx;
  text-align: center;
}

.history-empty,
.injury-empty {
  margin: 54rpx 24rpx 0;
  padding: 34rpx 30rpx;
  display: flex;
  flex-direction: column;
  background: #141417;
  border: 2rpx solid #3a3a40;
  border-left: 8rpx solid $rw-red;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 24rpx), calc(100% - 24rpx) 100%, 0 100%);
}

.history-empty__en,
.injury-empty__en {
  font-family: $rw-font-display;
  font-size: 48rpx;
  color: $rw-white;
  line-height: 1;
  text-shadow: 6rpx 5rpx 0 $rw-red;
  transform: rotate(-1deg);
  transform-origin: left center;
}

.history-empty__title,
.injury-empty__title {
  margin-top: 26rpx;
  font-family: $rw-font-cn-title;
  font-size: 32rpx;
  color: $rw-white;
}

.history-empty__desc,
.injury-empty__desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $rw-gray;
  line-height: 1.55;
}

.history-empty__button,
.injury-empty__button {
  align-self: flex-start;
  margin-top: 26rpx;
  padding: 18rpx 30rpx;
  background: $rw-red;
  color: $rw-white;
  font-size: 25rpx;
  font-weight: 700;
  clip-path: polygon(14rpx 0, 100% 0, calc(100% - 14rpx) 100%, 0 100%);
  transition: transform 0.14s ease;
}

.history-empty__button--pressed,
.injury-empty__button--pressed {
  transform: translateX(6rpx) scale(0.98);
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

.injury-empty {
  margin-top: 42rpx;
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
