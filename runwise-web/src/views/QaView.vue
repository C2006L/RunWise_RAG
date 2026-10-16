<script setup>
import { nextTick, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import P5Card from "../components/common/P5Card.vue";
import RansomNote from "../components/common/RansomNote.vue";
import StarField from "../components/common/StarField.vue";
import QaDecor from "../components/decor/QaDecor.vue";
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
const HOT_LABELS = ["START!", "KNEE!", "SHOE!", "PLAN!", "PULSE!"];

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

// 背景点缀星已移至 QaDecor 组件（内页装饰系统 2.0）

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
      <h1 class="page-title p5-page-title" aria-label="AI 答疑">
        <RansomNote text="AI 答疑" :seed="33" />
      </h1>
      <p class="page-desc">
        训练决策 · 装备情报 · 恢复判断 · 跑步技术
      </p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- 内页装饰系统 2.0：页面级装饰层（z-index:0，卡片 z-index:1） -->
    <QaDecor />

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
                <span class="terminal-label">RUNWISE AI</span>
                <span class="empty-title">智能训练助手</span>
                <span class="empty-desc">
                  训练计划 · 装备选择 · 伤病预防 · 跑步技术
                </span>
                <span class="welcome-meta">
                  <span class="empty-hint">点击载入起始问题 →</span>
                  <span class="empty-note">可编辑后发送</span>
                </span>
              </span>
              <!-- W-2：卡片右下角红色炸星（StarField S 档 22px，色覆盖为红） -->
              <span class="welcome-star" aria-hidden="true">
                <StarField :items="[{ tier: 'S', color: '#E8323E' }]" />
              </span>
            </button>

            <!-- 第二段：消息区（flex:1，消息多时内部滚动） -->
            <div ref="chatBody" class="chat-body">
              <p v-if="!messages.length" class="chat-empty-hint">
                选择下方快捷标签，或直接输入你的跑步问题
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
                  发送 →
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
        <section class="hot-panel" aria-labelledby="hot-title">
          <div class="side-card side-card--hot">
            <div class="side-heading">
              <p>QUICK INTEL</p>
              <h3 id="hot-title" class="side-title">大家都在问</h3>
              <span>CLICK TO DRAFT</span>
            </div>
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
              <span class="hot-copy">
                <span class="hot-en">{{ HOT_LABELS[i] || "RUN!" }}</span>
                <span class="side-text">{{ h.question }}</span>
              </span>
              <span class="hot-mark" aria-hidden="true"></span>
            </button>
            <p v-if="!hotQuestions.length" class="side-empty">暂无热门问题</p>
          </div>
        </section>

        <section class="history-panel" aria-labelledby="history-title">
          <!-- A-3：历史卡 = 标题 + 列表（flex:1 内滚）+ 加载更多（贴卡底） -->
          <div class="side-card side-card--history">
            <div class="side-heading side-heading--history">
              <p>MISSION FILES</p>
              <h3 id="history-title" class="side-title">我的问答</h3>
              <span>{{ historyTotal }} FILES</span>
            </div>
            <div class="history-list">
              <button
                v-for="(r, i) in historyList"
                :key="r.id"
                class="side-item side-item--history"
                type="button"
                @click="loadHistoryIntoChat(r)"
              >
                <span class="history-file">FILE {{ String(i + 1).padStart(2, "0") }}</span>
                <span class="history-copy">
                  <span class="side-text">{{ r.question }}</span>
                  <span class="side-meta">
                    {{ shortTime(r.createTime)
                    }}<template v-if="r.feedback === 1"> · 已赞</template
                    ><template v-else-if="r.feedback === -1"> · 已踩</template>
                  </span>
                </span>
                <span class="history-arrow" aria-hidden="true">→</span>
              </button>
              <p
                v-if="!historyList.length && !historyLoading"
                class="side-empty"
              >
                <strong>NO FILE FOUND!</strong>
                <span>暂无历史记录，提问后自动保存</span>
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
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  padding-top: clamp(10px, 1.4vh, 16px);
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

/* 眉标装饰线统一使用 RunWise 主红，避免出现通用 AI 产品常见的蓝紫提示色。 */
.page-kicker::before {
  content: "";
  width: 28px;
  height: 2px;
  background: var(--p5-red);
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
  position: relative; /* 承载 QaDecor 装饰层 */
  /* QA 恢复全局底部跑马灯后，主区保留稳定工作高度；视口不足时允许页面滚动，
     聊天与历史各自仍在明确高度内独立滚动，避免侧栏被 flex 压缩裁切。 */
  min-height: calc(100vh - var(--nav-h) - var(--sp-6) - 60px);
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.qa-layout {
  flex: 0 0 auto;
  height: clamp(540px, calc(100vh - 300px), 610px);
  min-height: 0; /* flex 链路归零：容器高只由视口决定，绝不由内容撑 */
  display: grid;
  grid-template-columns: minmax(0, 8fr) minmax(0, 4fr);
  grid-template-rows: minmax(0, 1fr); /* 行高 = 容器剩余高度锁死（A-1） */
  gap: var(--sp-5);
  align-items: stretch; /* 两栏拉伸填满行高，底边齐平 */
}

.qa-side {
  position: relative;
  display: flex;
  height: 100%;
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
    height: auto;
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

/* S-1 滚动链修复：卡体是 wrap（P5Card 内部为 flex 列容器）的 flex 项，
   默认 min-height:auto 取内容最小高度 → 实测长对话时卡体被内容撑到 1608px、
   撤破 455px 的 wrap（.chat-body scrollHeight==clientHeight 永久无内滚，
   溢出内容被 .qa-page overflow:hidden 裁切）。归零后卡体锁回 wrap 高，
   .qa-chat height:100% → .chat-body flex:1 内滚链路恢复。
   与下方 .qa-side 历史卡的既有 :deep(.p5-card) min-height:0 同款修法 */
.chat-col > .p5-card-wrap :deep(.p5-card) {
  min-height: 0;
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

/* 拼贴收缩定稿（H-5，永久标准见 RansomNote.vue 组件头注释）：
   标题中文部分恢复黄油体白字——无描边、无错落；
   RunWise 四字母恒为 Anton（英文/数字禁用拼贴效果与描边） */
.empty-title {
  font-family: var(--font-cn-display);
  font-weight: 400; /* 黄油体自带粗度，叠加 700 会假粗 */
  font-size: var(--fs-h3);
  color: var(--p5-white);
}

.empty-title .et-en {
  font-family: var(--font-display);
  font-weight: 400;
  letter-spacing: 0.02em;
}

.empty-desc {
  max-width: 480px;
  /* W-2：说明行灰色单行小字，行高 ≥1.6（此处 1.7），删叠字红字 */
  font-size: var(--fs-sub);
  line-height: 1.7;
  color: var(--p5-text-dim);
  /* B-4 加固：长文本任何宽度下都断行，绝不横向溢出/截断 */
  overflow-wrap: break-word;
  word-break: break-word;
}

.welcome-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--sp-3);
  margin-top: var(--sp-1);
  min-width: 0; /* B-4 加固：允许收缩到列宽内，触发换行而非溢出/重叠 */
}

/* B-4 加固：灰两段各保持完整成块，空间不足时整体换行而非彼此挤压重叠 */
.empty-hint,
.empty-note {
  white-space: nowrap;
}

/* W-2：强调改用「红菱形 bullet + 白字」，删除原红色小字叠字 */
.empty-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-white);
}

.empty-hint::before {
  content: "";
  flex: none;
  width: 8px;
  height: 8px;
  background: var(--p5-red);
  transform: rotate(45deg); /* 红菱形 */
}

.empty-note {
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  opacity: 0.7;
}

/* W-2：右下角红色炸星（S 档 22px 红）——置于卡内、红三角角标稍内上方，不遮文字 */
.welcome-star {
  position: absolute;
  right: 34px;
  bottom: 14px;
  z-index: 1;
  width: 26px;
  height: 26px;
  pointer-events: none;
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
  /* B-2 合规补丁：去圆角（无 border-radius）、黑底白描边、
     左上/右下切角（同全站卡片 clip-path），聚焦描边变红 */
  border: 2px solid var(--p5-white);
  border-radius: 0;
  outline: none;
  clip-path: polygon(
    10px 0,
    100% 0,
    100% calc(100% - 10px),
    calc(100% - 10px) 100%,
    0 100%,
    0 10px
  );
  transition: border-color 0.2s;
}

.chat-input::placeholder {
  color: var(--p5-text-dim);
}

/* B-2：聚焦描边变红（原电光蓝 → 红，回归红黑主视觉） */
.chat-input:focus {
  border-color: var(--p5-red);
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
  /* B-1 合规补丁：热门问题 01-05 序号主题色红 #E8323E + Anton + -6° 斜切，
     hover 变白（原电光蓝 → 红，回归红黑主视觉） */
  color: #e8323e;
  transform: skewX(-6deg);
  transition: color 0.2s;
}

/* B-1：条目 hover 时序号反白（配合整条目暗红底） */
.side-item:hover .hot-idx {
  color: var(--p5-white);
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

/* ===== v3 · 漫画情报面板（仅非核心区域去方正化） ===== */
.chat-welcome {
  isolation: isolate;
  gap: clamp(18px, 2.2vw, 30px);
  margin: var(--sp-5) var(--sp-6) 0;
  padding: 24px 38px 24px 30px;
  overflow: hidden;
  border: 0;
  background:
    linear-gradient(112deg, rgba(230, 0, 18, 0.18), transparent 44%),
    radial-gradient(rgba(255, 255, 255, 0.09) 1.25px, transparent 1.25px) 0 0 / 17px 17px,
    #0c0c0e;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.42), 8px 7px 0 rgba(92, 21, 24, 0.68);
  clip-path: polygon(0 13px, 18px 0, 95% 0, 100% 24px, 98% 100%, 7% 100%, 0 82%);
  transition: transform 180ms ease, filter 180ms ease;
}

.chat-welcome::before {
  content: "";
  position: absolute;
  z-index: -1;
  top: 12px;
  right: 30px;
  width: 72px;
  height: 4px;
  background: var(--p5-red);
  clip-path: polygon(7px 0, 100% 0, calc(100% - 7px) 100%, 0 100%);
}

.chat-welcome::after {
  right: 0;
  bottom: 0;
  width: 48px;
  height: 31px;
  background:
    linear-gradient(-18deg, transparent 0 38%, var(--p5-red) 40% 53%, transparent 55%),
    linear-gradient(-18deg, transparent 0 60%, #fff 62% 68%, transparent 70%);
  clip-path: none;
}

.chat-welcome:hover {
  transform: translate(2px, -2px);
  filter: drop-shadow(-4px 5px 0 rgba(230, 0, 18, 0.2));
}

.chat-welcome:focus-visible {
  outline: 2px solid var(--p5-white);
  outline-offset: -5px;
}

.empty-runner {
  width: 72px;
  height: 72px;
  opacity: 0.7;
  transform: rotate(-2deg);
}

.empty-runner::before {
  content: "";
  position: absolute;
  inset: 8px -7px 2px 4px;
  z-index: -1;
  background: rgba(230, 0, 18, 0.64);
  clip-path: polygon(15% 0, 100% 8%, 88% 100%, 0 82%);
}

.runner-shadow { color: #09090a; transform: translate(6px, 6px); }
.runner-body { color: var(--p5-white); filter: drop-shadow(2px 2px 0 var(--p5-red)); }

.terminal-label {
  align-self: flex-start;
  padding: 4px 15px 4px 10px;
  color: #fff;
  background: var(--p5-red);
  font-family: var(--font-display);
  font-size: 11px;
  line-height: 1;
  letter-spacing: 0.14em;
  clip-path: polygon(0 0, 100% 0, calc(100% - 8px) 100%, 5px 100%);
}

.empty-title {
  font-family: var(--font-cn-title), var(--font-body);
  font-size: clamp(21px, 1.8vw, 25px);
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0.04em;
  text-shadow: 2px 2px 0 rgba(230, 0, 18, 0.56);
}

.empty-title em {
  color: var(--p5-red);
  font-style: normal;
  text-shadow: 2px 2px 0 #fff;
}

.empty-desc {
  display: flex;
  flex-direction: column;
  gap: 1px;
  color: #c5c5cc;
  font-size: 13px;
  line-height: 1.55;
}

.empty-desc span:last-child { color: #f0f0f2; }
.empty-hint { font-family: var(--font-cn-display), var(--font-body); font-weight: 400; }

/* 侧栏不再使用两张同款红框卡，热门菜单开放、历史档案有边界。 */
.qa-side {
  gap: clamp(15px, 1.8vh, 20px);
  padding: 0 2px 0 10px;
  background: linear-gradient(rgba(255,255,255,.22), rgba(255,255,255,.22)) 0 8px / 1px calc(100% - 16px) no-repeat;
}

.hot-panel {
  position: relative;
  flex: 0 1 auto;
  min-height: 0;
  padding: 6px 0 2px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.7)) 0 18px / 1px 46% no-repeat,
    linear-gradient(rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0.55)) 0 18px / 31% 1px no-repeat,
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)) 100% 100% / 42% 1px no-repeat;
}

.hot-panel::before {
  content: "";
  position: absolute;
  top: 0;
  left: -8px;
  width: 38%;
  height: 5px;
  background: var(--p5-red);
  transform: skewX(-32deg);
}

.side-card--hot { padding: 11px 6px 0 10px; }

.side-heading {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  margin-bottom: 4px;
  padding: 0 7px 5px 4px;
}

.side-heading p {
  grid-column: 1 / -1;
  color: var(--p5-red);
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.17em;
}

.side-heading .side-title {
  margin: 0;
  color: #fff;
  font-family: var(--font-cn-title), var(--font-body);
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.05em;
  text-transform: none;
}

.side-heading > span {
  color: #777780;
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.12em;
}

.side-item--hot {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 17px;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 3px 5px 4px 1px;
  overflow: visible;
  border: 0;
  background: transparent;
  transition: transform 180ms ease, background-color 180ms ease;
}

.side-item--hot + .side-item--hot {
  position: relative;
  margin-top: 3px;
}

.side-item--hot + .side-item--hot::after {
  content: "";
  position: absolute;
  top: -2px;
  left: 47px;
  width: 46%;
  height: 1px;
  background: linear-gradient(90deg, var(--p5-red), transparent);
}

.side-item--hot::before { display: none; }
.side-item--hot:nth-of-type(2) { padding-left: 10px; }
.side-item--hot:nth-of-type(3) { padding-left: 3px; }
.side-item--hot:nth-of-type(4) { padding-left: 14px; }
.side-item--hot:nth-of-type(5) { padding-left: 6px; }

.side-item--hot:hover,
.side-item--hot:focus-visible {
  border-color: transparent;
  background: linear-gradient(90deg, rgba(230, 0, 18, 0.16), transparent 78%);
  transform: translateX(4px);
}

.side-item--hot:focus-visible {
  outline: 1px solid rgba(255, 255, 255, 0.68);
  outline-offset: 2px;
}

.hot-idx {
  align-self: start;
  padding-top: 4px;
  font-size: 16px;
  transform: none;
}

.hot-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1;
}

.hot-en {
  display: inline-block;
  color: #fff;
  font-family: var(--font-display);
  font-size: clamp(20px, 1.7vw, 25px);
  letter-spacing: 0.035em;
  line-height: 0.9;
  text-shadow: 3px 3px 0 var(--p5-red);
  transition: color 180ms ease, text-shadow 180ms ease, transform 180ms ease;
}

.side-item--hot:nth-of-type(2) .hot-en { transform: rotate(-1.4deg) translateY(1px); }
.side-item--hot:nth-of-type(3) .hot-en { transform: rotate(1deg) translateX(4px); }
.side-item--hot:nth-of-type(4) .hot-en { transform: rotate(-0.6deg) translateY(-1px); }
.side-item--hot:nth-of-type(5) .hot-en { transform: rotate(1.3deg) translateX(-2px); }

.side-item--hot .side-text {
  margin-top: 6px;
  color: #c2c2c9;
  font-family: var(--font-cn-display), var(--font-body);
  font-size: 13.5px;
  font-weight: 400;
  line-height: 1.4;
  -webkit-line-clamp: 2;
}

.side-item--hot:hover .hot-en,
.side-item--hot:focus-visible .hot-en {
  color: var(--p5-red);
  text-shadow: 2px 2px 0 #fff, 5px 4px 0 #080808;
  transform: translate(3px, -1px) rotate(0deg);
}

.hot-mark {
  width: 12px;
  height: 12px;
  border: 2px solid #5f5f68;
  transform: rotate(45deg);
  transition: border-color 180ms ease, background-color 180ms ease;
}

.side-item--hot:hover .hot-mark,
.side-item--hot:focus-visible .hot-mark {
  border-color: var(--p5-red);
  background: var(--p5-red);
}

/* 历史记录作为任务档案：边框局部断开，正文保持水平。 */
.history-panel {
  position: relative;
  flex: 1 0 220px;
  min-height: 220px;
  overflow: hidden;
  background:
    linear-gradient(#fff, #fff) 0 0 / 36% 1px no-repeat,
    linear-gradient(var(--p5-red), var(--p5-red)) 100% 0 / 27% 4px no-repeat,
    linear-gradient(rgba(255,255,255,.58), rgba(255,255,255,.58)) 100% 18px / 1px 53% no-repeat,
    linear-gradient(rgba(255,255,255,.42), rgba(255,255,255,.42)) 0 100% / 22% 1px no-repeat,
    linear-gradient(#fff, #fff) 100% 100% / 58% 1px no-repeat,
    #141416;
  box-shadow: inset 1px 0 0 rgba(255, 255, 255, 0.22);
  clip-path: polygon(0 0, 100% 8px, 100% 92%, 94% 100%, 0 100%);
}

.side-card--history { padding: 18px 16px 14px; }
.side-heading--history { padding-right: 2px; padding-left: 2px; }

.side-item--history {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 9px 7px 9px 5px;
  overflow: hidden;
  border: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.14)) 0 100% / 72% 1px no-repeat;
  transition: transform 180ms ease, background-color 180ms ease;
}

.side-item--history::before {
  top: 16px;
  bottom: 16px;
  width: 4px;
}

.side-item--history:hover,
.side-item--history:focus-visible {
  border-color: transparent;
  background:
    linear-gradient(var(--p5-red), var(--p5-red)) 0 100% / 88% 2px no-repeat,
    linear-gradient(90deg, rgba(230, 0, 18, 0.17), transparent 82%);
  transform: translateX(3px);
}

.side-item--history:focus-visible {
  outline: 1px solid rgba(255, 255, 255, 0.65);
  outline-offset: -2px;
}

.history-file {
  color: var(--p5-red);
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.history-copy { min-width: 0; }
.side-item--history .side-text { font-size: 13px; -webkit-line-clamp: 1; }
.side-item--history .side-meta { display: block; margin-top: 3px; font-size: 10px; }
.history-arrow { color: #696971; font-family: var(--font-display); transition: color 180ms ease; }
.side-item--history:hover .history-arrow,
.side-item--history:focus-visible .history-arrow { color: var(--p5-red); }

.history-panel .side-empty {
  display: flex;
  min-height: 112px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-top: 1px solid rgba(255, 255, 255, 0.11);
}

.history-panel .side-empty strong {
  color: #fff;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.08em;
  text-shadow: 2px 2px 0 var(--p5-red);
}

.history-panel .side-empty span { color: var(--p5-text-dim); }

.more-btn:focus-visible,
.chip:focus-visible,
.candidate:focus-visible,
.send-btn:focus-visible {
  outline: 2px solid var(--p5-white);
  outline-offset: 2px;
}

.history-panel .history-list {
  padding-right: 3px;
}

.history-panel .more-btn {
  flex: none;
  min-height: 32px;
  margin-top: 8px;
}

/* 左侧继续保留明确内容边界，但将完整矩形描边拆成局部线段。 */
.chat-col > .p5-card-wrap {
  filter: drop-shadow(7px 7px 0 var(--p5-shadow-red));
}

.chat-col > .p5-card-wrap :deep(.p5-card--redframe) {
  border: 0;
  background:
    linear-gradient(var(--p5-white), var(--p5-white)) 12px 0 / 62% 2px no-repeat,
    linear-gradient(var(--p5-white), var(--p5-white)) 0 12px / 2px 78% no-repeat,
    linear-gradient(var(--p5-white), var(--p5-white)) 100% 12px / 2px 34% no-repeat,
    linear-gradient(var(--p5-white), var(--p5-white)) 0 100% / 31% 2px no-repeat,
    linear-gradient(var(--p5-white), var(--p5-white)) 100% 100% / 46% 2px no-repeat,
    linear-gradient(var(--p5-red), var(--p5-red)) 100% 0 / 22% 4px no-repeat,
    var(--p5-panel);
  box-shadow: none;
  clip-path: polygon(12px 0, 100% 0, 100% 92%, 96% 100%, 0 100%, 0 12px);
}

/* ===== v4 · 回归 RunWise AI 问答工作台 ===== */
/* 欢迎区保持克制，只说明身份与能力，不再承担控制台式信息展示。 */
.chat-welcome {
  min-height: 118px;
  gap: 18px;
  margin: 16px 24px 0;
  padding: 19px 30px 19px 24px;
  cursor: pointer;
}

.empty-runner {
  width: 60px;
  height: 60px;
  opacity: 0.6;
}

.welcome-text { gap: 5px; }
.terminal-label { padding-top: 3px; padding-bottom: 3px; font-size: 9px; }

.chat-welcome .empty-title {
  color: var(--p5-white);
  font-family: var(--font-cn-title), var(--font-body);
  font-size: clamp(20px, 1.8vw, 24px);
  font-weight: 400;
  line-height: 1.15;
  letter-spacing: .04em;
  text-shadow: 2px 2px 0 rgba(230, 0, 18, .55);
}

.chat-welcome .empty-desc {
  display: block;
  margin-top: 4px;
  color: #c8c8ce;
  font-size: 12px;
  line-height: 1.4;
}

.welcome-star { right: 18px; bottom: 8px; width: 18px; height: 18px; opacity: 0.7; }

/* 沿用原有空状态，只增加极轻的品牌识别，不创建新的视觉模块。 */
.chat-empty-hint {
  display: flex;
  align-items: center;
  align-self: flex-start;
  justify-content: flex-start;
  gap: 11px;
  margin: clamp(38px, 7vh, 62px) 0 auto 5%;
  text-align: left;
  color: #92929a;
}

.chat-empty-hint::before {
  content: "";
  width: 8px;
  height: 8px;
  flex: none;
  background: var(--p5-red);
  transform: rotate(45deg);
}

.chat-body { padding-top: 16px; padding-bottom: 10px; }
.chat-foot { padding-top: 13px; }

.chips { margin-bottom: 13px; }
.chip--active { transform: rotate(-.8deg); }
.chip:nth-child(even).chip--active { transform: rotate(.8deg); }
.chip--active:hover:not(:disabled) { transform: translateY(-2px) rotate(-.4deg); }
.chip:nth-child(even).chip--active:hover:not(:disabled) { transform: translateY(-2px) rotate(.4deg); }

.chat-input:focus-visible {
  border-color: var(--p5-red);
  box-shadow: inset 4px 0 0 var(--p5-red);
  outline: none;
}

.send-btn { transition: background-color 180ms ease, transform 180ms ease, filter 180ms ease; }
.send-btn:hover:not(:disabled) { transform: translate(2px, -1px) skewX(-2deg); filter: brightness(1.08); }
.send-btn:active:not(:disabled) { transform: translate(1px, 1px); }

.chip:active:not(:disabled) { transform: translateY(1px) rotate(0); }
.side-item--hot:active { background: var(--p5-red-soft); transform: translateX(6px); }
.side-item--history:active { background: linear-gradient(90deg, rgba(230,0,18,.25), transparent 82%); }
.side-item--history:hover::before,
.side-item--history:focus-visible::before,
.side-item--history:active::before { opacity: 1; }

@media (max-width: 900px) {
  .qa-side { height: auto; padding-left: 0; background: none; }
  .history-panel { flex: none; min-height: 280px; }
  .chat-welcome { margin-right: var(--sp-4); margin-left: var(--sp-4); }
}

@media (max-width: 600px) {
  .chat-welcome { padding: 16px 20px; }
  .empty-runner { width: 48px; height: 48px; }
  .side-item--hot { grid-template-columns: 34px minmax(0, 1fr) 15px; }
  .hot-en { font-size: 24px; }
}

@media (prefers-reduced-motion: reduce) {
  .chat-welcome,
  .side-item--hot,
  .hot-en,
  .hot-mark,
  .side-item--history,
  .history-arrow,
  .send-btn {
    transition: none;
  }

  .chat-welcome:hover,
  .side-item--hot:hover,
  .side-item--history:hover {
    transform: none;
  }
}
</style>
