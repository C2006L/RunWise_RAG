<script setup>
// 个人中心（工程计划 4.9 / M9 骨架态）：资料卡 + 成就徽章 + 本周摘要 + 设置退出
// - 资料与 joinedDays 来自 stores/user（fetchProfile）
// - 本周摘要来自 api/stats.getWeeklyStats（与统计页同源）
// - 徽章：本期静态占位（达成状态推导属后续里程碑），红色实心菱形章 / 灰描边虚章
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import P5Card from '../components/common/P5Card.vue'
import Stars from '../components/common/Stars.vue'
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

// P0-3：迷你红柱 mock 数据（周一~周日里程 km，本期静态，后续里程碑接真实数据）
const WEEK_KM = [3.2, 0, 5.1, 6, 4.3, 8.5, 12]
const MC_MAX_H = 48 // 柱最大高度 px

function barHeight(km) {
  const max = Math.max(...WEEK_KM)
  return Math.max(2, Math.round((km / max) * MC_MAX_H)) // 0 里程日显示 2px 底柱
}

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

    <!-- 资料卡（D5：统计斜切卡对齐 .panel-frame 工艺，深红影 + 红角标） -->
    <P5Card tag="RUNNER" tag-rotate="-4deg" tag-top="-16px" tag-left="32px" frame="red">
      <div class="profile-card">
        <div class="avatar" aria-hidden="true">
          <span v-if="store.userInfo && store.userInfo.avatar">
            <img :src="store.userInfo.avatar" alt="头像" />
          </span>
          <span v-else class="avatar-fallback p5-num">RW</span>
        </div>
        <div class="profile-info">
          <p class="nickname">{{ store.userInfo?.nickname || '跑者' }}</p>
          <!-- D4：@句柄读登录态（nickname 优先，替代登录账号残留如 @111），禁止硬编码 -->
          <p class="username" v-if="store.userInfo?.nickname || store.userInfo?.username">
            @{{ store.userInfo?.nickname || store.userInfo?.username }}
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

    <!-- 成就徽章区（P1-2：贴纸红底白字，与 RUNNER 统一） -->
    <!-- P1-3：徽章卡右上空档星（高度 0 锚点，星向上出血进卡片间隙） -->
    <div class="badge-star-anchor" aria-hidden="true">
      <Stars
        :count="1"
        :positions="[
          {
            top: '-18px',
            right: '4px',
            size: 9,
            color: 'rgba(255, 255, 255, 0.5)',
            bright: true,
            dur: 5,
          },
        ]"
      />
    </div>
    <P5Card tag="BADGES" tag-rotate="3deg" tag-top="-14px" tag-right="36px" frame="red">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">成就徽章</h3>
          <span class="section-meta p5-num"
            >{{ earnedCount }} / {{ badges.length }}</span
          >
        </div>
        <div class="badge-grid">
          <div
            v-for="(b, i) in badges"
            :key="b.key"
            class="badge"
            :class="{ 'badge--earned': b.earned }"
            :title="b.note"
          >
            <!-- P1-1：左上角编号 01~06（10px 灰色等宽字） -->
            <span class="badge-idx">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="badge-diamond" aria-hidden="true"></span>
            <span class="badge-name">{{ b.name }}</span>
            <span class="badge-note">{{ b.earned ? b.note : '未达成' }}</span>
          </div>
        </div>
      </div>
    </P5Card>

    <!-- 本周数据摘要（D5：同款红框工艺） -->
    <P5Card tag="THIS WEEK" tag-rotate="-3deg" tag-top="-18px" tag-left="48px" frame="red">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">本周数据</h3>
          <button class="stats-link" type="button" @click="gotoStats">
            查看统计 →
            <span class="m-underline" aria-hidden="true"></span>
          </button>
        </div>
        <button class="summary-bar" type="button" @click="gotoStats">
          <span class="summary-nums">
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
          </span>
          <!-- P0-3：右半空档迷你红柱（周一~周日里程 mock，最高柱亮红） -->
          <span class="mini-chart" aria-hidden="true">
            <span
              v-for="(d, i) in WEEK_KM"
              :key="i"
              class="mc-col"
            >
              <span
                class="mc-bar"
                :class="{ 'mc-bar--peak': d === Math.max(...WEEK_KM) }"
                :style="{ height: barHeight(d) + 'px' }"
              ></span>
              <span class="mc-day">{{ '一二三四五六日'[i] }}</span>
            </span>
          </span>
        </button>
      </div>
    </P5Card>

    <!-- 设置区（P1-2：贴纸红底白字，与 RUNNER 统一） -->
    <P5Card tag="SETTINGS" tag-rotate="4deg" tag-bottom="-13px" tag-right="40px" frame="red">
      <div class="section-body">
        <div class="section-head">
          <h3 class="section-title">设置</h3>
        </div>
        <div class="settings-list">
          <!-- D2：移除「消息通知」「深色模式」占位行，仅保留退出登录 -->
          <button class="setting-item setting-item--logout" type="button" :disabled="loggingOut" @click="handleLogout">
            <span>{{ loggingOut ? '正在退出…' : '退出登录' }}</span>
            <span class="setting-meta">→</span>
          </button>
        </div>
      </div>
    </P5Card>

    <!-- P1-3：页面底部留白星（1 白 1 红；另 1 白在徽章卡右上，共 3 颗 = 白2红1） -->
    <div class="stars-zone" aria-hidden="true">
      <Stars
        :count="2"
        :positions="[
          { left: '14%', bottom: '22px', size: 11, color: 'rgba(255, 255, 255, 0.55)', bright: true, dur: 4.6 },
          { left: '76%', bottom: '48px', size: 8, color: 'var(--p5-red)', bright: true, dur: 5.8 },
        ]"
      />
    </div>
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
  text-align: left;
}

/* P0-1：全页第一主角 —— 128 巨字（Anton ≥120px 白 + 红错位影） */
.days-value {
  display: block;
  font-size: 128px;
  line-height: 0.9;
  color: var(--p5-white);
  text-shadow: 5px 5px 0 var(--p5-red);
}

/* 「已坚持 / 天」12px 灰字紧贴数字左下角 */
.days-label {
  display: block;
  margin-top: 4px;
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
  position: relative; /* 承载 D3 红斜杠下划线 */
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-red);
}

/* D3：hover 红斜杠下划线（首页菜单 .m-underline 同款参数）：
   4px 红条 rotate(-3deg)，0.25s 从左划出，悬停期间保持 */
.stats-link .m-underline {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -5px;
  height: 4px;
  background: var(--p5-red);
  transform: rotate(-3deg) scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s ease-out;
}

.stats-link:hover .m-underline {
  transform: rotate(-3deg) scaleX(1);
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
  position: relative; /* 承载 P1-1 左上角编号 */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-5) var(--sp-3);
  text-align: center;
  border: 1px dashed var(--p5-line);
}

/* P1-1：相邻已获得格交替旋转，制造贴纸节奏 */
.badge--earned:nth-child(odd) {
  transform: rotate(-4deg);
}

.badge--earned:nth-child(even) {
  transform: rotate(4deg);
}

/* P1-1：已获得底色压暗一档（0.16 → 0.08，更沉的黑红） */
.badge--earned {
  border: 1px solid var(--p5-line);
  background: rgba(230, 0, 18, 0.08);
}

/* P1-1：编号 01~06，10px 灰色等宽字 */
.badge-idx {
  position: absolute;
  top: 6px;
  left: 8px;
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
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
  width: 40px; /* P1-1：已获得菱形放大至 40px */
  height: 40px;
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
/* D1 等距保留：数字区 flex 统一 gap；P0-3 右半空档放迷你红柱，整体垂直居中 */
.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-6);
  padding: var(--sp-4) var(--sp-2);
  text-align: left;
  transition: background 0.2s;
}

.summary-bar:hover {
  background: var(--p5-red-soft);
}

.summary-nums {
  display: flex;
  gap: var(--sp-6); /* 组间距严格相等（D1） */
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

/* P0-2：三数字 60px Anton 白 + 红错位影 */
.summary-value {
  font-size: 60px;
  line-height: 1;
  color: var(--p5-white);
  text-shadow: 4px 4px 0 var(--p5-red);
}

.summary-label {
  font-size: var(--fs-caption); /* 12px */
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
}

/* ===== P0-3：迷你红柱（周一~周日里程） ===== */
.mini-chart {
  display: flex;
  align-items: flex-end;
  gap: 10px; /* 柱间距 10px */
  padding-right: var(--sp-2);
}

.mc-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 4px; /* 柱宽 4px（日字更宽，居中于柱位） */
}

.mc-bar {
  width: 4px;
  background: #8a1f24; /* 常规柱暗红 */
}

.mc-bar--peak {
  background: #e8323e; /* 最高柱亮红 */
}

.mc-day {
  font-size: 8px;
  line-height: 1;
  color: var(--p5-text-dim);
  transform: scale(0.9); /* 8px 视觉校正 */
  white-space: nowrap;
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

/* ===== P1-3：徽章卡右上星锚点（高度 0，星向上出血进卡片间隙，不碰文字） ===== */
.badge-star-anchor {
  position: relative;
  height: 0;
}

/* ===== P1-3/D6：页面底部星光留白带 =====
   独立 96px 空白区，星只落在带内（距上方设置卡文字 > 40px，宪法 C-3） */
.stars-zone {
  position: relative; /* <Stars> 的定位父级 */
  height: 96px;
}
</style>
