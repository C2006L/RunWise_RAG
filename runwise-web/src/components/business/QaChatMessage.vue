<script setup>
// 聊天气泡（工程计划 4.5 / M4）：
// - 用户消息：右侧红底气泡（右下切角）
// - 助手消息：左侧黑色双切角气泡 + 来源标签 + 👍/👎 反馈行
//   · loading：三点跳动动画（ask 期间）
//   · error：错误气泡 + 「重试」按钮（不允许静默失败）
//   · safetyTip：气泡下方米色提示条（命中伤病关键词时）
// - 反馈行仅在消息已绑定历史记录 id（recordId）时渲染
defineProps({
  message: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['feedback', 'retry'])
</script>

<template>
  <div
    class="msg"
    :class="message.role === 'user' ? 'msg--user' : 'msg--assistant'"
  >
    <!-- 用户消息：右侧红底气泡 -->
    <div v-if="message.role === 'user'" class="bubble bubble--user">
      {{ message.text }}
    </div>

    <!-- 助手消息：头像 + 气泡主体 -->
    <template v-else>
      <span class="avatar">RW</span>
      <div class="msg-main">
        <div class="bubble" :class="{ 'bubble--error': message.error }">
          <!-- loading：三点动画 -->
          <div v-if="message.loading" class="loading">
            <span></span><span></span><span></span>
          </div>

          <!-- 错误气泡 + 重试 -->
          <template v-else-if="message.error">
            <p class="error-text">回答生成失败，请稍后重试</p>
            <button class="retry-btn" type="button" @click="emit('retry')">
              重试
            </button>
          </template>

          <!-- 正文 + 来源标签 -->
          <template v-else>
            <p class="answer">{{ message.text }}</p>
            <div
              v-if="message.sources && message.sources.length"
              class="sources"
            >
              <span class="src-label">来源</span>
              <span v-for="s in message.sources" :key="s" class="src-tag">
                {{ s }}
              </span>
            </div>
          </template>
        </div>

        <!-- 米色安全提示条（气泡下方） -->
        <p
          v-if="!message.loading && !message.error && message.safetyTip"
          class="safety-tip"
        >
          安全提示：{{ message.safetyTip }}
        </p>

        <!-- 👍 / 👎 反馈行 -->
        <div
          v-if="!message.loading && !message.error && message.recordId"
          class="feedback"
        >
          <button
            class="fb-btn"
            :class="{ 'is-active': message.feedback === 1 }"
            type="button"
            aria-label="点赞"
            @click="emit('feedback', 1)"
          >
            👍
          </button>
          <button
            class="fb-btn"
            :class="{ 'is-active': message.feedback === -1 }"
            type="button"
            aria-label="点踩"
            @click="emit('feedback', -1)"
          >
            👎
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  gap: var(--sp-3);
  align-items: flex-start;
}

.msg--user {
  justify-content: flex-end;
}

.msg--assistant {
  justify-content: flex-start;
}

/* ===== 助手头像：红色切角小方块 ===== */
.avatar {
  flex: none;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.04em;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(
    0 0,
    calc(100% - 8px) 0,
    100% 8px,
    100% 100%,
    8px 100%,
    0 calc(100% - 8px)
  );
}

.msg-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 78%;
}

/* ===== 气泡通用 ===== */
.bubble {
  padding: var(--sp-3) var(--sp-4);
  font-size: var(--fs-sub);
  line-height: 1.75;
  word-break: break-word;
}

/* 用户：红底、右下切角 */
.bubble--user {
  max-width: 78%;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(
    0 0,
    100% 0,
    100% calc(100% - 12px),
    calc(100% - 12px) 100%,
    0 100%
  );
}

/* 助手：黑底双切角（呼应 p5-card 形态） */
.msg--assistant .bubble {
  color: var(--p5-white);
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  clip-path: polygon(
    0 0,
    calc(100% - 12px) 0,
    100% 12px,
    100% 100%,
    12px 100%,
    0 calc(100% - 12px)
  );
}

/* 错误气泡 */
.bubble--error {
  border-color: var(--p5-red);
  background: var(--p5-red-soft);
}

.error-text {
  color: var(--p5-white);
  margin-bottom: var(--sp-3);
}

.retry-btn {
  padding: 4px 20px;
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-red);
  border: 1px solid var(--p5-red);
  background: transparent;
  transition: all 0.2s;
}

.retry-btn:hover {
  color: var(--p5-white);
  background: var(--p5-red);
}

/* ===== loading 三点动画 ===== */
.loading {
  display: flex;
  gap: 7px;
  padding: 7px 2px;
}

.loading span {
  width: 8px;
  height: 8px;
  background: var(--p5-text-dim);
  animation: dot-bounce 1.2s infinite ease-in-out;
}

.loading span:nth-child(2) {
  animation-delay: 0.18s;
}

.loading span:nth-child(3) {
  animation-delay: 0.36s;
}

@keyframes dot-bounce {
  0%,
  80%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-4px);
  }
}

/* ===== 来源标签 ===== */
.sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: var(--sp-3);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--p5-line);
}

.src-label {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-text-dim);
}

.src-tag {
  padding: 2px 9px;
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  border: 1px solid var(--p5-line);
}

/* ===== 米色安全提示条 ===== */
.safety-tip {
  margin-top: var(--sp-2);
  padding: 7px 14px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--p5-ink);
  background: var(--p5-cream);
  clip-path: polygon(
    8px 0,
    100% 0,
    calc(100% - 8px) 100%,
    0 100%
  );
}

/* ===== 反馈行 ===== */
.feedback {
  display: flex;
  gap: var(--sp-2);
  margin-top: var(--sp-2);
}

.fb-btn {
  padding: 3px 14px;
  font-size: var(--fs-sub);
  line-height: 1.6;
  color: var(--p5-text-dim);
  border: 1px solid var(--p5-line);
  background: transparent;
  transition: all 0.2s;
}

.fb-btn:hover {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.fb-btn.is-active {
  color: var(--p5-white);
  border-color: var(--p5-red);
  background: var(--p5-red);
}
</style>
