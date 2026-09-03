<script setup>
import { nextTick, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import P5Card from "../components/common/P5Card.vue";
import Stars from "../components/common/Stars.vue";
import QaChatMessage from "../components/business/QaChatMessage.vue";
import * as qaApi from "../api/qa";
import * as statsApi from "../api/stats";

// 答疑页（工程计划 4.5 / v2.0 交互修正）：聊天式 AI 问答
// - 会话消息为组件局部状态（不建 store，工程计划 4.5 约定）
// - v2.0 交互硬规则：热门问题 / 历史 / 空状态 / 分类候选一律「填入输入框并聚焦」，
//   由用户确认发送——页面内不存在任何未确认的自动发送行为
// - 分类 chips 为二级候选条：点击分类展开 3~4 条候选问题，第一条由
//   getWeeklyStats 实时组装（数据注入），点击候选填入草稿
// - 伤病预防页「问 AI 助手」跳转携带 ?draft=，进页填入草稿并聚焦
const HISTORY_PAGE_SIZE = 5;

// ===== 分类候选问题（第一条为数据注入占位，运行时由 weekly 数据组装） =====
const CATEGORY_CANDIDATES = {
  训练计划: {
    injected: (w) =>
      `我这周跑了 ${w.totalKm}km、打卡 ${w.checkinCount} 次，已连续 ${w.streakDays} 天，下周课表怎么安排？`,
    fallback: "我每周跑 3 次，下周课表怎么安排比较好？",
    rest: [
      "5K 破 25 分钟需要什么样的训练结构？",
      "每周跑步频率和休息日怎么分配最科学？",
    ],
  },
  装备选择: {
    fallback: "",
    rest: [
      "如何选择适合自己的第一双跑鞋？",
      "跑鞋多久该换一次？看哪些信号？",
      "跑步手表有必要买吗？入门款怎么选？",
    ],
  },
  伤痛预防: {
    fallback: "",
    rest: [
      "跑步时膝盖疼，我该怎么办？",
      "跑完小腿紧绷是正常的吗？怎么缓解？",
      "如何判断该休息还是该就医？",
    ],
  },
  跑步技术: {
    fallback: "",
    rest: [
      "怎样的跑步姿势才更省力？",
      "步频和步幅，先练哪个更重要？",
      "跑步时该怎么呼吸？",
    ],
  },
};

// 空状态点击时填入的预设第一问（热门问题榜首）
const FIRST_QUESTION = "初学者应该怎么开始跑步？";

// ===== 状态 =====
const messages = ref([]); // 聊天流（user / assistant 消息）
const inputText = ref("");
const chatInput = ref(null); // 输入框元素（聚焦用）
const chatBody = ref(null);
const asking = ref(false);

const categories = ref([]);
const hotQuestions = ref([]);
const historyList = ref([]);
const historyTotal = ref(0);
const historyPage = ref(1);
const historyLoading = ref(false);

// 二级候选条状态：当前展开的分类名（'' = 收起）与候选问题列表
const activeCategory = ref("");
const candidateQuestions = ref([]);
const weeklyStats = ref(null); // 数据注入用（getWeeklyStats，失败降级 fallback）

let msgSeq = 0;

const route = useRoute();

// ===== 背景点缀星（Phase G 重构）：改用全局 <Stars> 组件精确布点 =====
// 7 颗 = 4 白（暗档 0.2~0.55 呼吸）+ 2 红 + 1 蓝（--accent-blue 全页唯一）；
// 位置/尺寸/周期与原手工实现逐一对应，视觉不变；分布避开卡片文字区
const QA_STAR_POSITIONS = [
  {
    left: "62%",
    top: "26px",
    size: 10,
    color: "rgba(255,255,255,0.45)",
    dim: true,
    dur: 5.2,
  },
  {
    left: "80%",
    top: "64px",
    size: 6,
    color: "rgba(255,255,255,0.35)",
    dim: true,
    dur: 6.4,
  },
  {
    left: "92%",
    top: "118px",
    size: 9,
    color: "var(--p5-red)",
    dim: false,
    dur: 4.6,
  },
  {
    left: "71%",
    top: "152px",
    size: 7,
    color: "rgba(255,255,255,0.4)",
    dim: true,
    dur: 7,
  },
  {
    left: "74%",
    bottom: "16%",
    size: 9,
    color: "var(--p5-red)",
    dim: false,
    dur: 5.8,
  },
  {
    left: "90%",
    bottom: "7%",
    size: 12,
    color: "rgba(255,255,255,0.45)",
    dim: true,
    dur: 4.2,
  },
  {
    left: "65%",
    bottom: "5%",
    size: 6,
    color: "var(--accent-blue)",
    dim: false,
    dur: 6.8,
  },
];

// ===== 工具 =====
function shortTime(time) {
  return (time || "").slice(5, 16); // 'MM-DD HH:mm'
}

// 草稿填写：全站唯一入口，填入 + 聚焦光标（v2.0 交互规则）
function fillDraft(text) {
  inputText.value = text;
  nextTick(() => chatInput.value?.focus());
}

async function scrollToBottom() {
  await nextTick();
  chatBody.value?.scrollTo({ top: chatBody.value.scrollHeight });
}

// ===== 数据加载 =====
onMounted(() => {
  qaApi
    .getCategories()
    .then((list) => {
      categories.value = list || [];
    })
    .catch(() => {});
  qaApi
    .getHotQuestions(5)
    .then((list) => {
      hotQuestions.value = list || [];
    })
    .catch(() => {});
  // 数据注入候选问题用（失败静默降级 fallback 文案）
  statsApi
    .getWeeklyStats()
    .then((w) => {
      weeklyStats.value = w;
    })
    .catch(() => {});
  loadHistoryPage(1);

  // 伤病预防页跳转携带 ?draft=：填入草稿并聚焦（不自动发送）
  if (route.query.draft) {
    fillDraft(String(route.query.draft));
  }
});

async function loadHistoryPage(page) {
  historyLoading.value = true;
  try {
    const res = await qaApi.getHistory(page, HISTORY_PAGE_SIZE);
    historyTotal.value = res.total;
    if (page === 1) {
      historyList.value = res.list;
    } else {
      const existing = new Set(historyList.value.map((r) => r.id));
      historyList.value.push(...res.list.filter((r) => !existing.has(r.id)));
    }
    historyPage.value = page;
  } catch {
    // 侧栏历史加载失败不打断对话，保持空态
  } finally {
    historyLoading.value = false;
  }
}

function loadMoreHistory() {
  if (historyLoading.value) return;
  loadHistoryPage(historyPage.value + 1);
}

// ===== 空状态：点击填入预设第一问（不发送） =====
function startFromEmpty() {
  fillDraft(FIRST_QUESTION);
}

// ===== 分类 chips：二级候选条（点击展开 / 再点收起） =====
function toggleCategory(category) {
  if (activeCategory.value === category.name) {
    activeCategory.value = "";
    return;
  }
  const preset = CATEGORY_CANDIDATES[category.name];
  if (!preset) return;
  const candidates = [];
  if (preset.injected && weeklyStats.value) {
    candidates.push(preset.injected(weeklyStats.value));
  } else if (preset.fallback || preset.rest.length) {
    candidates.push(preset.fallback || preset.rest[0]);
  }
  candidates.push(...(preset.injected ? preset.rest : preset.rest.slice(1)));
  candidateQuestions.value = candidates;
  activeCategory.value = category.name;
}

// 候选问题点击 → 填入草稿并收起候选条
function pickCandidate(question) {
  fillDraft(question);
  activeCategory.value = "";
}

// ===== 提问链路（仅输入框回车 / 发送按钮触发） =====
async function sendFromInput() {
  const q = inputText.value.trim();
  if (!q || asking.value) return;
  inputText.value = "";
  send(q);
}

async function send(question) {
  if (asking.value) return;
  asking.value = true;
  messages.value.push({ key: `m${++msgSeq}`, role: "user", text: question });
  const assistantMsg = reactive({
    key: `m${++msgSeq}`,
    role: "assistant",
    question, // 供失败重试使用
    text: "",
    sources: [],
    safetyTip: null,
    feedback: 0,
    recordId: null,
    loading: true,
    error: false,
  });
  messages.value.push(assistantMsg);
  scrollToBottom();
  await askInto(assistantMsg, question);
  asking.value = false;
}

// 助手消息填充（发送与重试共用）
async function askInto(msg, question) {
  msg.loading = true;
  msg.error = false;
  try {
    const result = await qaApi.askQuestion(question);
    msg.loading = false;
    msg.text = result.answer;
    msg.sources = result.sources || [];
    msg.safetyTip = result.safetyTip || null;
    scrollToBottom();
    await syncAfterAsk(question, result, msg);
  } catch {
    // 失败不静默：气泡转为错误态并提供重试
    msg.loading = false;
    msg.error = true;
  }
}

// ask 成功后回拉历史首页：刷新侧栏并把新记录 id 绑定到消息（反馈依赖）
async function syncAfterAsk(question, result, msg) {
  try {
    const res = await qaApi.getHistory(1, HISTORY_PAGE_SIZE);
    historyTotal.value = res.total;
    const existing = new Set(historyList.value.map((r) => r.id));
    res.list
      .filter((r) => !existing.has(r.id))
      .forEach((r) => historyList.value.unshift(r));
    const rec = res.list.find(
      (r) => r.question === question && r.answer === result.answer,
    );
    if (rec && msg && !msg.recordId) {
      msg.recordId = rec.id;
      msg.feedback = rec.feedback;
    }
  } catch {
    // 历史同步失败不影响当前对话展示
  }
}

function retry(msg) {
  if (asking.value || !msg.question) return;
  asking.value = true;
  askInto(msg, msg.question).finally(() => {
    asking.value = false;
  });
}

// ===== 反馈（乐观更新，失败回滚） =====
async function handleFeedback(msg, value) {
  if (!msg.recordId || msg.feedbackBusy) return;
  const next = msg.feedback === value ? 0 : value; // 再点同值取消
  if (next === msg.feedback) return;
  const prev = msg.feedback;
  msg.feedback = next;
  msg.feedbackBusy = true;
  try {
    await qaApi.feedback(msg.recordId, next);
    const rec = historyList.value.find((r) => r.id === msg.recordId);
    if (rec) rec.feedback = next;
  } catch {
    msg.feedback = prev;
  } finally {
    msg.feedbackBusy = false;
  }
}

// ===== 侧栏交互（v2.0：一律填入草稿 + 聚焦，不发送） =====
// 热门问题点击 → 问题文本填入输入框
function askHot(question) {
  fillDraft(question);
}

// 历史条目点击 → 该问题填入输入框（可编辑后重发）
function loadHistoryIntoChat(record) {
  fillDraft(record.question);
}
</script>

<template>
  <div class="qa-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">AI 答疑</h1>
      <p class="page-desc">
        训练计划、装备选择、伤痛预防 —— 有疑问，随时问 RunWise 助手。
      </p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- 背景点缀星：全局 <Stars> 组件精确布点（卡片 DOM 在后，越界星被卡片自然盖住） -->
    <Stars :positions="QA_STAR_POSITIONS" />

    <div class="qa-layout">
      <!-- 主区：聊天流（红框工艺 = 首页打卡卡同款；F-2 三段式等高） -->
      <div class="chat-col">
        <P5Card tag="RUNWISE 助手" tag-rotate="-5deg" frame="red">
          <div class="qa-chat">
            <!-- 第一段：欢迎横幅（固定，不随消息滚动） -->
            <button
              v-if="!messages.length"
              class="chat-welcome"
              type="button"
              @click="startFromEmpty"
            >
              <!-- 跑者剪影：红色错位层 + 白色主体，双影海报语言 -->
              <span class="empty-runner" aria-hidden="true">
                <svg
                  class="runner-shadow"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path
                    d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4Z"
                  />
                </svg>
                <svg
                  class="runner-body"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path
                    d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4Z"
                  />
                </svg>
              </span>
              <span class="welcome-text">
                <span class="empty-title">你好，我是 RunWise 智能训练助手</span>
                <span class="empty-desc">
                  训练计划、装备选择、伤痛预防、跑步技术方面的疑问都可以直接问我
                </span>
                <span class="welcome-meta">
                  <span class="empty-hint">点击此处，从第一个问题开始 →</span>
                  <span class="empty-note">（填入草稿，可编辑后发送）</span>
                </span>
              </span>
            </button>

            <!-- 第二段：消息区（flex:1，消息多时内部滚动） -->
            <div ref="chatBody" class="chat-body">
              <!-- F-3：空态提示（有消息后消失） -->
              <p v-if="!messages.length" class="chat-empty-hint">
                试试下方快捷标签，或直接输入你的问题
              </p>
              <QaChatMessage
                v-for="m in messages"
                :key="m.key"
                :message="m"
                @feedback="(v) => handleFeedback(m, v)"
                @retry="retry(m)"
              />
            </div>

            <!-- 第三段：输入区（贴底，结构不变） -->
            <div class="chat-foot">
              <!-- 分类快捷提问（v2.0 二级候选条）：点击分类展开候选问题，点击候选填入草稿 -->
              <div v-if="categories.length" class="chips">
                <button
                  v-for="c in categories"
                  :key="c.name"
                  class="chip"
                  type="button"
                  :class="{ 'chip--active': activeCategory === c.name }"
                  @click="toggleCategory(c)"
                >
                  {{ c.name }}
                </button>
              </div>
              <!-- 二级候选条：该分类下的候选问题（第一条可能含实时数据注入） -->
              <div v-if="activeCategory" class="candidates">
                <button
                  v-for="(q, i) in candidateQuestions"
                  :key="q"
                  class="candidate"
                  type="button"
                  @click="pickCandidate(q)"
                >
                  <span class="candidate-idx p5-num">{{ i + 1 }}</span>
                  <span class="candidate-text">{{ q }}</span>
                </button>
              </div>
              <div class="input-row">
                <input
                  ref="chatInput"
                  v-model="inputText"
                  class="chat-input"
                  type="text"
                  maxlength="200"
                  placeholder="输入你的跑步问题，回车发送…"
                  @keyup.enter="sendFromInput"
                />
                <button
                  class="send-btn"
                  type="button"
                  :disabled="asking || !inputText.trim()"
                  @click="sendFromInput"
                >
                  发送
                </button>
              </div>
              <!-- A-4：免责声明移入卡内（输入区正下方 8px），不再单独占页面高度 -->
              <p class="ai-disclaimer">内容由 AI 生成，仅供参考 · RunWise</p>
            </div>
          </div>
        </P5Card>
      </div>

      <!-- 侧栏：热门问题 + 历史记录（同款红框工艺） -->
      <aside class="qa-side">
        <P5Card tag="热门问题" tag-rotate="3deg" frame="red">
          <div class="side-card">
            <h3 class="side-title">大家都在问</h3>
            <button
              v-for="(h, i) in hotQuestions"
              :key="h.question"
              class="side-item side-item--hot"
              type="button"
              @click="askHot(h.question)"
            >
              <span class="hot-idx p5-num">{{
                String(i + 1).padStart(2, "0")
              }}</span>
              <span class="side-text">{{ h.question }}</span>
            </button>
            <p v-if="!hotQuestions.length" class="side-empty">暂无热门问题</p>
          </div>
        </P5Card>

        <P5Card
          tag="历史记录"
          tag-rotate="-4deg"
          tag-top="-18px"
          tag-left="44px"
          frame="red"
        >
          <!-- A-3：历史卡 = 标题 + 列表（flex:1 内滚）+ 加载更多（贴卡底） -->
          <div class="side-card side-card--history">
            <h3 class="side-title">我的问答</h3>
            <div class="history-list">
              <button
                v-for="r in historyList"
                :key="r.id"
                class="side-item"
                type="button"
                @click="loadHistoryIntoChat(r)"
              >
                <span class="side-text">{{ r.question }}</span>
                <span class="side-meta">
                  {{ shortTime(r.createTime)
                  }}<template v-if="r.feedback === 1"> · 已赞</template
                  ><template v-else-if="r.feedback === -1"> · 已踩</template>
                </span>
              </button>
              <p
                v-if="!historyList.length && !historyLoading"
                class="side-empty"
              >
                暂无历史记录，提问后自动保存
              </p>
            </div>
            <button
              v-if="historyList.length < historyTotal"
              class="more-btn"
              type="button"
              :disabled="historyLoading"
              @click="loadMoreHistory"
            >
              {{ historyLoading ? "加载中…" : "加载更多" }}
            </button>
          </div>
        </P5Card>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: 0; /* 间距由 p5-page-header 的 padding-bottom 提供 */
}

.page-kicker {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-2);
}

/* 眉标装饰线：电光蓝（答疑页蓝色点缀，全页唯二的蓝元素之一） */
.page-kicker::before {
  content: "";
  width: 28px;
  height: 2px;
  background: var(--accent-blue);
}

.page-title {
  font-size: 48px;
  line-height: 1.2;
  margin-bottom: var(--sp-3);
}

.page-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
}

/* ===== 布局：主聊天区 + 侧栏 ===== */
.qa-page {
  position: relative; /* 承载 <Stars> 背景点缀星（组件样式/动画内聚于 Stars.vue） */
  /* A-1：页面高度锁死 = 100vh - 导航(64) - app-main 上下内边距(32+32)，
     页面级滚动被根除——滚动只允许发生在卡片内部 */
  height: calc(100vh - var(--nav-h) - var(--sp-6) * 2);
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 双保险：任何溢出不外泄为页面滚动条 */
}

.qa-layout {
  flex: 1;
  min-height: 0; /* flex 链路归零：容器高只由视口决定，绝不由内容撑 */
  display: grid;
  grid-template-columns: minmax(0, 8fr) minmax(0, 4fr);
  grid-template-rows: minmax(0, 1fr); /* 行高 = 容器剩余高度锁死（A-1） */
  gap: var(--sp-5);
  align-items: stretch; /* 两栏拉伸填满行高，底边齐平 */
}

.qa-side {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
  min-height: 0; /* grid 项 min-height:auto 会把列表内容高度计入行高——归零 */
}

/* A-3：右栏两段式——热门问题卡自适应，历史记录卡吃掉剩余全部高度 */
.qa-side > .p5-card-wrap:last-child {
  flex: 1;
  min-height: 0;
}

/* P5Card 内部链路：卡体允许收缩，历史列表才能内滚 */
.qa-side > .p5-card-wrap:last-child :deep(.p5-card) {
  min-height: 0;
}

@media (max-width: 900px) {
  /* F-5/A：窄屏单栏顺排，撤销视口锁高，恢复页面自然滚动 */
  .qa-page {
    height: auto;
    overflow: visible;
  }

  .qa-layout {
    grid-template-columns: 1fr;
    grid-template-rows: none;
    align-items: start;
  }

  /* 撤销桌面端等高用的绝对定位：窄屏下恢复文档流，
     否则 .chat-col 内无在流子元素会塌缩为 0 高，卡片被裁切 */
  .chat-col > .p5-card-wrap {
    position: static;
  }

  .qa-side > .p5-card-wrap:last-child {
    flex: none;
  }

  .side-card--history {
    height: auto;
  }

  .history-list {
    overflow-y: visible;
  }

  .qa-chat {
    height: clamp(460px, calc(100vh - 340px), 640px);
  }

  /* 单列布局下背景星会压到卡片文字，可读性优先：窄屏整体隐藏
     （.stars 为 <Stars> 组件根元素，父 scoped 样式可命中） */
  .stars {
    display: none;
  }

  /* 欢迎横幅窄屏纵向堆叠 */
  .chat-welcome {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--sp-4);
    margin: var(--sp-4) var(--sp-4) 0;
    padding: var(--sp-4) var(--sp-5);
  }
}

/* A-1 等高链路：grid 行高已锁死为容器高，聊天卡 absolute 化填满整格，
   内容（消息流）对行高零贡献——消息再多也只走内滚。
   链条：行高锁死 → chat-col 拉伸 → 卡体 inset 填满
   → .qa-chat height:100% → 消息区 flex:1 + min-height:0 内滚 */
.chat-col {
  position: relative; /* 卡体的定位父级 */
  min-width: 0;
}

.chat-col > .p5-card-wrap {
  position: absolute;
  inset: 0; /* A-4 后免责声明已入卡，卡体占满整格 */
}

/* ===== 聊天区 ===== */
/* A-2：三段式——欢迎横幅（固定）→ 消息区 flex:1 内滚 → 输入区贴底；
   卡片总高由视口锁高决定，不再由内容或视口 clamp 硬凑 */
.qa-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-body {
  flex: 1;
  min-height: 0; /* flex 项默认 min-height:auto 会撑破等高——必须归零，长对话走内滚 */
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
  padding: var(--sp-5) var(--sp-6);
}

.chat-body::-webkit-scrollbar {
  width: 6px;
}

.chat-body::-webkit-scrollbar-thumb {
  background: var(--p5-line);
}

.chat-body::-webkit-scrollbar-track {
  background: transparent;
}

/* A-4：免责声明入卡，紧贴输入区下方 8px（12px 灰字） */
.ai-disclaimer {
  margin-top: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
  text-align: center;
}

/* F-3：消息区空态提示（横幅下方居中，有消息后随 v-if 消失） */
.chat-empty-hint {
  margin: auto 0; /* 消息区剩余空间内垂直居中 */
  text-align: center;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
}

/* ===== 欢迎横幅（答疑页视觉重构）：黑底 + 白描边 + 灰色网点底纹 + 红色角标 ===== */
.chat-welcome {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--sp-5);
  margin: var(--sp-5) var(--sp-6) 0;
  padding: var(--sp-5) var(--sp-6);
  text-align: left;
  /* 黑底 + 灰色网点底纹（radial 双值铺点） */
  background:
    radial-gradient(rgba(255, 255, 255, 0.08) 1.5px, transparent 1.5px) 0 0 /
      18px 18px,
    var(--p5-black);
  border: 2px solid var(--p5-white);
  transition: transform 0.2s;
}

/* 红色角标：右下三角（P5 卡片标志性收尾语言） */
.chat-welcome::after {
  content: "";
  position: absolute;
  right: 0;
  bottom: 0;
  width: 26px;
  height: 26px;
  background: var(--p5-red);
  clip-path: polygon(100% 0, 100% 100%, 0 100%);
}

.chat-welcome:hover {
  transform: translateY(-2px);
}

.empty-runner {
  position: relative;
  flex: none;
  width: 72px;
  height: 72px;
}

.empty-runner svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 红色错位层：纯实心偏移，无模糊 */
.runner-shadow {
  color: var(--p5-red);
  transform: translate(6px, 6px);
}

.runner-body {
  color: var(--p5-white);
}

.welcome-text {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  min-width: 0;
}

.empty-title {
  font-size: var(--fs-h3);
  font-weight: 700;
  color: var(--p5-white);
}

.empty-desc {
  max-width: 480px;
  font-size: var(--fs-sub);
  line-height: 1.7;
}

.welcome-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--sp-3);
  margin-top: var(--sp-1);
}

.empty-hint {
  font-size: var(--fs-caption);
  letter-spacing: 0.12em;
  color: var(--p5-red);
}

.empty-note {
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  opacity: 0.7;
}

.chat-foot {
  padding: var(--sp-4) var(--sp-6) var(--sp-5);
  border-top: 1px solid var(--p5-line);
}

/* ===== 分类快捷提问 chips（米色胶带质感 + 红方块前缀） ===== */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-3);
  margin-bottom: var(--sp-4);
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 6px 16px;
  /* 趣味点缀 1/3：ZCOOL KuaiLe 贴纸手写感 */
  font-family: var(--font-fun);
  font-size: var(--fs-sub);
  color: var(--p5-ink);
  background: var(--p5-cream);
  clip-path: polygon(7px 0, 100% 0, calc(100% - 7px) 100%, 0 100%);
  transform: rotate(-1.2deg);
  transition:
    background 0.2s,
    color 0.2s,
    transform 0.2s,
    filter 0.2s;
}

/* 红色小方块前缀：替代彩色 emoji，守住红黑米三色纪律（UI 精修 P2-4） */
.chip::before {
  content: "";
  width: 7px;
  height: 7px;
  background: var(--p5-red);
  transform: rotate(45deg);
}

.chip:nth-child(even) {
  transform: rotate(1.2deg);
}

/* hover：贴纸被「揭起」——上浮 + 投影加深 + 再旋 1° */
.chip:hover:not(:disabled) {
  background: var(--p5-red);
  color: var(--p5-white);
  transform: rotate(0.2deg) translateY(-2px);
  filter: drop-shadow(0 4px 8px var(--p5-shadow));
}

.chip:hover:not(:disabled)::before {
  background: var(--p5-white);
}

.chip:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 激活态 chip：红底白字（候选条展开中） */
.chip--active {
  background: var(--p5-red);
  color: var(--p5-white);
  transform: rotate(0);
}

.chip--active::before {
  background: var(--p5-white);
}

/* ===== 二级候选条（v2.0：分类展开的候选问题，点击填入草稿） ===== */
.candidates {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  margin: 0 0 var(--sp-4);
  padding: var(--sp-3);
  background: var(--p5-black);
  border: 1px dashed var(--p5-red);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
}

.candidate {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  width: 100%;
  padding: var(--sp-2) var(--sp-3);
  text-align: left;
  transition: background 0.2s;
}

.candidate:hover {
  background: var(--p5-red-soft);
}

.candidate-idx {
  flex: none;
  font-size: var(--fs-h3);
  line-height: 1;
  color: var(--p5-red);
}

.candidate-text {
  font-size: var(--fs-sub);
  line-height: 1.6;
  color: var(--p5-cream);
}

/* ===== 输入行 ===== */
.input-row {
  display: flex;
  gap: var(--sp-3);
}

.chat-input {
  flex: 1;
  min-width: 0;
  padding: 12px 16px;
  font-size: var(--fs-body);
  font-family: var(--font-body);
  color: var(--p5-white);
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  outline: none;
  transition: border-color 0.2s;
}

.chat-input::placeholder {
  color: var(--p5-text-dim);
}

/* 聚焦蓝描边（答疑页唯一的蓝色交互元素，主视觉仍为红黑） */
.chat-input:focus {
  border-color: var(--accent-blue);
}

.send-btn {
  flex: none;
  padding: 0 30px;
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--p5-red-dark);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 侧栏卡片 ===== */
.side-card {
  padding: var(--sp-5);
}

/* A-3：历史卡三段——标题 / 列表（flex:1 内滚）/ 加载更多（贴卡底） */
.side-card--history {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.history-list {
  flex: 1;
  min-height: 0; /* flex 链路归零，列表溢出走内滚 */
  overflow-y: auto;
}

.history-list::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-thumb {
  background: var(--p5-line);
}

.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.side-title {
  font-size: var(--fs-caption);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--p5-text-dim);
  margin-bottom: var(--sp-3);
}

.side-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  text-align: left;
  padding: var(--sp-3);
  border: 1px solid var(--p5-line);
  transition:
    border-color 0.2s,
    background 0.2s;
}

.side-item + .side-item {
  margin-top: var(--sp-2);
}

/* hover：红蒙层底 + 左侧红色竖条，明确「可点」（UI 精修 P2-2） */
.side-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--p5-red);
  opacity: 0;
  transition: opacity 0.2s;
}

.side-item:hover {
  border-color: var(--p5-red);
  background: var(--p5-red-soft);
}

.side-item:hover::before {
  opacity: 1;
}

.side-item--hot {
  flex-direction: row;
  align-items: center;
  gap: var(--sp-3);
}

.hot-idx {
  flex: none;
  font-family: var(--font-display);
  font-size: var(--fs-h3);
  line-height: 1;
  /* 电光蓝编号（答疑页蓝色点缀）：01-05 */
  color: var(--accent-blue);
}

.side-text {
  font-size: var(--fs-sub);
  line-height: 1.6;
  color: var(--p5-white);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.side-meta {
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
}

.side-empty {
  padding: var(--sp-4) 0;
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  text-align: center;
}

.more-btn {
  width: 100%;
  margin-top: var(--sp-3);
  padding: 8px 0;
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-text-dim);
  border: 1px dashed var(--p5-line);
  background: transparent;
  transition:
    color 0.2s,
    border-color 0.2s;
}

.more-btn:hover:not(:disabled) {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.more-btn:disabled {
  opacity: 0.5;
}
</style>
