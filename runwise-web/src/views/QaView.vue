<script setup>
import { nextTick, onMounted, reactive, ref } from "vue";
import P5Card from "../components/common/P5Card.vue";
import QaChatMessage from "../components/business/QaChatMessage.vue";
import * as qaApi from "../api/qa";

// 答疑页（工程计划 4.5 / M4 功能态）：聊天式 AI 问答
// - 会话消息为组件局部状态（不建 store，工程计划 4.5 约定）
// - 历史记录侧栏分页拉取，点击条目将问答对载入聊天流顶部
// - ask 成功后回拉历史首页：既刷新侧栏，也把新落库记录的 id 绑定到
//   助手消息上（反馈按钮依赖 recordId），契约保持 { answer, sources, safetyTip }
const HISTORY_PAGE_SIZE = 5;

// 分类快捷提问的预设问题（点击分类 chip 即以该问题发起提问）
const CATEGORY_PRESET = {
  训练计划: "初学者如何安排每周三次的跑步训练？",
  装备选择: "如何选择适合自己的第一双跑鞋？",
  伤痛预防: "跑步时膝盖疼，我该怎么办？",
  跑步技术: "怎样的跑步姿势才更省力？",
};

const welcomeMsg = {
  key: "welcome",
  role: "assistant",
  text: "你好，我是 RunWise 智能训练助手。训练计划、装备选择、伤痛预防、跑步技术方面的疑问都可以直接问我，也可以点击下方分类标签快速提问。",
  sources: [],
  safetyTip: null,
  feedback: 0,
  recordId: null,
  loading: false,
  error: false,
};

// ===== 状态 =====
const messages = ref([]); // 聊天流（user / assistant 消息）
const inputText = ref("");
const inputRef = ref(null);
const chatBody = ref(null);
const asking = ref(false);

const categories = ref([]);
const hotQuestions = ref([]);
const historyList = ref([]);
const historyTotal = ref(0);
const historyPage = ref(1);
const historyLoading = ref(false);

let msgSeq = 0;

// ===== 工具 =====
// 后端 sources 落库为字符串，JSON.parse 后渲染，容错处理空值
function parseSources(raw) {
  if (!raw) return [];
  try {
    const v = JSON.parse(raw);
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}

function shortTime(time) {
  return (time || "").slice(5, 16); // 'MM-DD HH:mm'
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
  loadHistoryPage(1);
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
      historyList.value.push(
        ...res.list.filter((r) => !existing.has(r.id)),
      );
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

// ===== 提问链路 =====
async function sendFromInput() {
  const q = inputText.value.trim();
  if (!q || asking.value) return;
  inputText.value = "";
  send(q);
}

function askByCategory(category) {
  if (asking.value) return;
  send(CATEGORY_PRESET[category.name] || `关于${category.name}的建议`);
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

// ===== 侧栏交互 =====
function fillInput(question) {
  inputText.value = question;
  inputRef.value?.focus();
}

// 历史条目 → 载入聊天流顶部（已载入过的仅滚动到顶部）
function loadHistoryIntoChat(record) {
  if (messages.value.some((m) => m.recordId === record.id)) {
    chatBody.value?.scrollTo({ top: 0, behavior: "smooth" });
    return;
  }
  messages.value.unshift(
    { key: `m${++msgSeq}`, role: "user", text: record.question },
    {
      key: `m${++msgSeq}`,
      role: "assistant",
      text: record.answer,
      sources: parseSources(record.sources),
      safetyTip: null,
      feedback: record.feedback,
      recordId: record.id,
      loading: false,
      error: false,
    },
  );
  nextTick(() => chatBody.value?.scrollTo({ top: 0 }));
}
</script>

<template>
  <div class="qa-page">
    <header class="page-head">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title">AI 答疑</h1>
      <p class="page-desc">
        训练计划、装备选择、伤痛预防 —— 有疑问，随时问 RunWise 助手。
      </p>
    </header>

    <div class="qa-layout">
      <!-- 主区：聊天流 -->
      <P5Card tag="RUNWISE 助手" tag-rotate="-5deg">
        <div class="qa-chat">
          <div ref="chatBody" class="chat-body">
            <QaChatMessage :message="welcomeMsg" />
            <QaChatMessage
              v-for="m in messages"
              :key="m.key"
              :message="m"
              @feedback="(v) => handleFeedback(m, v)"
              @retry="retry(m)"
            />
          </div>

          <div class="chat-foot">
            <div v-if="categories.length" class="chips">
              <button
                v-for="c in categories"
                :key="c.name"
                class="chip"
                type="button"
                :disabled="asking"
                @click="askByCategory(c)"
              >
                {{ c.icon }} {{ c.name }}
              </button>
            </div>
            <div class="input-row">
              <input
                ref="inputRef"
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
          </div>
        </div>
      </P5Card>

      <!-- 侧栏：热门问题 + 历史记录 -->
      <aside class="qa-side">
        <P5Card tag="热门问题" tag-rotate="3deg">
          <div class="side-card">
            <h3 class="side-title">大家都在问</h3>
            <button
              v-for="(h, i) in hotQuestions"
              :key="h.question"
              class="side-item side-item--hot"
              type="button"
              @click="fillInput(h.question)"
            >
              <span class="hot-idx">{{ String(i + 1).padStart(2, "0") }}</span>
              <span class="side-text">{{ h.question }}</span>
            </button>
            <p v-if="!hotQuestions.length" class="side-empty">暂无热门问题</p>
          </div>
        </P5Card>

        <P5Card tag="历史记录" tag-rotate="-4deg">
          <div class="side-card">
            <h3 class="side-title">我的问答</h3>
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
  margin-bottom: var(--sp-6);
}

.page-kicker {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-2);
}

.page-title {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--p5-white);
  margin-bottom: var(--sp-3);
}

.page-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
}

/* ===== 布局：主聊天区 + 侧栏 ===== */
.qa-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr);
  gap: var(--sp-5);
  align-items: start;
}

.qa-side {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
}

@media (max-width: 960px) {
  .qa-layout {
    grid-template-columns: 1fr;
  }
}

/* ===== 聊天区 ===== */
.qa-chat {
  display: flex;
  flex-direction: column;
  height: 640px;
}

.chat-body {
  flex: 1;
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

.chat-foot {
  padding: var(--sp-4) var(--sp-6) var(--sp-5);
  border-top: 1px solid var(--p5-line);
}

/* ===== 分类快捷提问 chips（米色胶带质感） ===== */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-3);
  margin-bottom: var(--sp-4);
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  font-size: var(--fs-sub);
  color: var(--p5-ink);
  background: var(--p5-cream);
  clip-path: polygon(7px 0, 100% 0, calc(100% - 7px) 100%, 0 100%);
  transform: rotate(-1.2deg);
  transition:
    background 0.2s,
    color 0.2s;
}

.chip:nth-child(even) {
  transform: rotate(1.2deg);
}

.chip:hover:not(:disabled) {
  background: var(--p5-red);
  color: var(--p5-white);
}

.chip:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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

.side-title {
  font-size: var(--fs-caption);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--p5-text-dim);
  margin-bottom: var(--sp-3);
}

.side-item {
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

.side-item:hover {
  border-color: var(--p5-red);
  background: var(--p5-red-soft);
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
  color: var(--p5-red);
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
