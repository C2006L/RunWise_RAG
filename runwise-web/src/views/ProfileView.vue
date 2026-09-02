<script setup>
// 个人中心（工程计划 4.9 / M9 骨架态）：资料卡 + 成就徽章 + 本周摘要 + 设置退出
// - 资料与 joinedDays 来自 stores/user（fetchProfile）
// - 本周摘要来自 api/stats.getWeeklyStats（与统计页同源）
// - 徽章：本期静态占位（达成状态推导属后续里程碑），红色实心菱形章 / 灰描边虚章
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import P5Card from '../components/common/P5Card.vue'
import { useAuth } from '../composables/useAuth'
import * as statsApi from '../api/stats'

const router = useRouter()
const { store, logout } = useAuth()

const weekly = ref(null)
const loggingOut = ref(false)

onMounted(() => {
  if (store.isLoggedIn) {
    store.fetchProfile().catch(() => {})
  }
  statsApi
    .getWeeklyStats()
    .then((w) => {
      weekly.value = w
    })
    .catch(() => {})
})

// 成就徽章（M9 静态占位：达成态取固定布尔，后续里程碑从打卡记录推导）
const badges = [
  { key: 'first', name: '首次打卡', earned: true, note: '完成第一次跑步打卡' },
  { key: 'streak7', name: '连续 7 天', earned: true, note: '连续一周不间断' },
  { key: 'km100', name: '累计 100km', earned: true, note: '总里程破百' },
  { key: 'weekend', name: '周末跑者', earned: true, note: '完成 10 次周末打卡' },
  { key: 'night', name: '夜跑侠', earned: false, note: '完成 20 次夜跑（18 点后）' },
  { key: 'full-month', name: '全勤月', earned: false, note: '单月每日打卡' },
]

const earnedCount = computed(() => badges.filter((b) => b.earned).length)

function gotoStats() {
  router.push('/stats')
}

async function handleLogout() {
  loggingOut.value = true
  await logout()
}
</script>

<template>
  <div class="profile-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">个人中心</h1>
      <p class="page-desc">你的跑步档案 —— 坚持的每一天都在这里留下印记。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- 资料卡 -->
    <P5Card tag="RUNNER" tag-rotate="-4deg" tag-top="-16px" tag-left="32px">
      <div class="profile-card">
        <div class="avatar" aria-hidden="true">
          <span v-if="store.userInfo && store.userInfo.avatar">
            <img :src="store.userInfo.avatar" alt="头像" />
          </span>
          <span v-else class="avatar-fallback p5-num">RW</span>
        </div>
        <div class="profile-info">
          <p class="nickname">{{ store.userInfo?.nickname || '跑者' }}</p>
          <p class="username" v-if="store.userInfo?.username">
            @{{ store.userInfo.username }}
          </p>
        </div>
        <div class="profile-days">
          <span class="days-value p5-num">{{
            store.userInfo?.joinedDays ?? '--'
          }}</span>
          <span class="days-label">已坚持 / 天</span>
        </div>
      </div>
    </P5Card>

    <!-- 成就徽章区 -->
    <P5Card tag="BADGES" tag-rotate="3deg" tag-top="-14px" tag-right="36px">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">成就徽章</h3>
          <span class="section-meta p5-num"
            >{{ earnedCount }} / {{ badges.length }}</span
          >
        </div>
        <div class="badge-grid">
          <div
            v-for="b in badges"
            :key="b.key"
            class="badge"
            :class="{ 'badge--earned': b.earned }"
            :title="b.note"
          >
            <span class="badge-diamond" aria-hidden="true"></span>
            <span class="badge-name">{{ b.name }}</span>
            <span class="badge-note">{{ b.earned ? b.note : '未达成' }}</span>
          </div>
        </div>
      </div>
    </P5Card>

    <!-- 本周数据摘要 -->
    <P5Card tag="THIS WEEK" tag-rotate="-3deg" tag-top="-18px" tag-left="48px">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">本周数据</h3>
          <button class="stats-link" type="button" @click="gotoStats">
            查看统计 →
          </button>
        </div>
        <button class="summary-bar" type="button" @click="gotoStats">
          <span class="summary-item">
            <span class="summary-value p5-num">{{
              weekly ? weekly.totalKm : '--'
            }}</span>
            <span class="summary-label">km · 本周里程</span>
          </span>
          <span class="summary-item">
            <span class="summary-value p5-num">{{
              weekly ? weekly.checkinCount : '--'
            }}</span>
            <span class="summary-label">次 · 打卡</span>
          </span>
          <span class="summary-item">
            <span class="summary-value p5-num">{{
              weekly ? weekly.streakDays : '--'
            }}</span>
            <span class="summary-label">天 · 连续</span>
          </span>
        </button>
      </div>
    </P5Card>

    <!-- 设置区 -->
    <P5Card tag="SETTINGS" tag-rotate="4deg" tag-bottom="-13px" tag-right="40px">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">设置</h3>
        </div>
        <div class="settings-list">
          <div class="setting-item setting-item--disabled">
            <span>消息通知</span>
            <span class="setting-meta">开发中</span>
          </div>
          <div class="setting-item setting-item--disabled">
            <span>深色模式</span>
            <span class="setting-meta">开发中</span>
          </div>
          <button class="setting-item setting-item--logout" type="button" :disabled="loggingOut" @click="handleLogout">
            <span>{{ loggingOut ? '正在退出…' : '退出登录' }}</span>
            <span class="setting-meta">→</span>
          </button>
        </div>
      </div>
    </P5Card>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
  max-width: 880px;
  margin: 0 auto;
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
  line-height: 1.2;
  margin-bottom: var(--sp-3);
}

.page-desc {
  font-size: var(--fs-body);
  color: var(--p5-text-dim);
}

/* ===== 资料卡 ===== */
.profile-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--sp-5);
  padding: var(--sp-6);
}

.avatar {
  width: 84px;
  height: 84px;
  overflow: hidden;
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  clip-path: polygon(
    0 0,
    calc(100% - 14px) 0,
    100% 14px,
    100% 100%,
    14px 100%,
    0 calc(100% - 14px)
  );
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: var(--fs-h2);
  color: var(--p5-red);
}

.nickname {
  font-size: var(--fs-h2);
  font-weight: 900;
  color: var(--p5-white);
}

.username {
  margin-top: var(--sp-1);
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

.profile-days {
  text-align: right;
}

.days-value {
  display: block;
  font-size: var(--fs-data);
  line-height: 1;
  color: var(--p5-red);
}

.days-label {
  display: block;
  margin-top: var(--sp-2);
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-text-dim);
}

/* ===== 通用分区 ===== */
.section-body {
  padding: var(--sp-5) var(--sp-6) var(--sp-6);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-4);
}

.section-title {
  font-size: var(--fs-caption);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--p5-text-dim);
}

.section-meta {
  font-size: var(--fs-h3);
  color: var(--p5-red);
}

.stats-link {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-red);
}

.stats-link:hover {
  text-decoration: underline;
}

/* ===== 徽章网格（2×3，非均分留给 M10 构图） ===== */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-4);
}

@media (max-width: 720px) {
  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-5) var(--sp-3);
  text-align: center;
  border: 1px dashed var(--p5-line);
}

.badge--earned {
  border: 1px solid var(--p5-line);
  background: var(--p5-red-soft);
}

/* 菱形章：已达成红色实心，未达成灰描边 */
.badge-diamond {
  width: 28px;
  height: 28px;
  transform: rotate(45deg);
  border: 2px solid var(--p5-text-dim);
  opacity: 0.4;
}

.badge--earned .badge-diamond {
  background: var(--p5-red);
  border-color: var(--p5-red);
  opacity: 1;
}

.badge-name {
  font-size: var(--fs-sub);
  font-weight: 700;
  color: var(--p5-white);
}

.badge-note {
  font-size: var(--fs-caption);
  line-height: 1.5;
  color: var(--p5-text-dim);
}

/* ===== 本周摘要 ===== */
.summary-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-5);
  padding: var(--sp-4) var(--sp-2);
  text-align: left;
  transition: background 0.2s;
}

.summary-bar:hover {
  background: var(--p5-red-soft);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.summary-value {
  font-size: var(--fs-data);
  line-height: 1;
  color: var(--p5-white);
}

.summary-label {
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
}

/* ===== 设置区 ===== */
.settings-list {
  display: flex;
  flex-direction: column;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-4) var(--sp-2);
  font-size: var(--fs-sub);
  color: var(--p5-white);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: background 0.2s;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-meta {
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
}

.setting-item--disabled {
  color: var(--p5-text-dim);
  opacity: 0.55;
  cursor: not-allowed;
}

.setting-item--logout {
  width: 100%;
  text-align: left;
  color: var(--p5-red);
  font-weight: 700;
}

.setting-item--logout:hover:not(:disabled) {
  background: var(--p5-red-soft);
}

.setting-item--logout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
