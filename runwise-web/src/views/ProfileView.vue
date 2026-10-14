<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RansomNote from '../components/common/RansomNote.vue'
import { useAuth } from '../composables/useAuth'
import * as checkinApi from '../api/checkin'
import runnerArt from '../assets/images/hero-runner.png'

const router = useRouter()
const { store, logout } = useAuth()
const records = ref([])
const loading = ref(true)
const loadError = ref('')
const loggingOut = ref(false)

const pad = (value) => String(value).padStart(2, '0')
const dateKey = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
const parseDate = (value) => new Date(`${value}T12:00:00`)
const addDays = (date, amount) => {
  const result = new Date(date)
  result.setDate(result.getDate() + amount)
  return result
}

onMounted(async () => {
  const [recordResult] = await Promise.allSettled([
    checkinApi.getAllCheckins(),
    store.isLoggedIn ? store.fetchProfile() : Promise.resolve(),
  ])

  if (recordResult.status === 'fulfilled') {
    records.value = Array.isArray(recordResult.value) ? recordResult.value : []
  } else {
    loadError.value = '训练档案暂时无法读取，请稍后刷新。'
  }
  loading.value = false
})

const summary = computed(() => {
  const byDate = new Map()
  records.value.forEach((record) => {
    if (!record?.date) return
    const current = byDate.get(record.date) || { distance: 0, count: 0 }
    current.distance += Number(record.distanceKm) || 0
    current.count += 1
    byDate.set(record.date, current)
  })

  const dates = [...byDate.keys()].sort()
  let longestStreak = 0
  let runningStreak = 0
  let previous = null
  dates.forEach((value) => {
    const current = parseDate(value)
    runningStreak = previous && dateKey(addDays(previous, 1)) === value ? runningStreak + 1 : 1
    longestStreak = Math.max(longestStreak, runningStreak)
    previous = current
  })

  const today = new Date()
  let cursor = byDate.has(dateKey(today)) ? today : addDays(today, -1)
  let currentStreak = 0
  while (byDate.has(dateKey(cursor))) {
    currentStreak += 1
    cursor = addDays(cursor, -1)
  }

  const monthGroups = new Map()
  dates.forEach((value) => {
    const date = parseDate(value)
    const month = value.slice(0, 7)
    if (!monthGroups.has(month)) monthGroups.set(month, new Set())
    monthGroups.get(month).add(date.getDate())
  })
  const fullMonthEarned = [...monthGroups.entries()].some(([month, days]) => {
    const [year, index] = month.split('-').map(Number)
    return days.size === new Date(year, index, 0).getDate()
  })

  const currentMonth = dateKey(today).slice(0, 7)
  const currentMonthCount = monthGroups.get(currentMonth)?.size || 0
  const currentMonthDays = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate()
  const weekStart = addDays(today, -6)
  const week = Array.from({ length: 7 }, (_, index) => {
    const date = addDays(weekStart, index)
    const key = dateKey(date)
    return {
      key,
      label: ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
      distance: byDate.get(key)?.distance || 0,
      checked: byDate.has(key),
    }
  })

  return {
    totalCount: records.value.length,
    totalDistance: records.value.reduce((sum, item) => sum + (Number(item.distanceKm) || 0), 0),
    longestStreak,
    currentStreak,
    weekendCount: dates.filter((value) => [0, 6].includes(parseDate(value).getDay())).length,
    fullMonthEarned,
    currentMonthCount,
    currentMonthDays,
    week,
    weekDistance: week.reduce((sum, item) => sum + item.distance, 0),
    weekCount: week.filter((item) => item.checked).length,
  }
})

// 与小程序端保持一致：每完成 5 次打卡提升 1 级。
const runnerLevel = computed(() => Math.max(1, Math.floor(summary.value.totalCount / 5) + 1))
const nickname = computed(() => store.userInfo?.nickname || store.userInfo?.username || '跑者')
const fileNumber = computed(() => {
  const id = store.userInfo?.id
  return id == null ? '---' : String(id).padStart(3, '0').slice(-3)
})

const achievements = computed(() => [
  { key: 'first', title: '首次打卡', target: 1, value: summary.value.totalCount, earned: summary.value.totalCount >= 1, note: '完成第一条训练记录' },
  { key: 'streak', title: '连续 7 天', target: 7, value: summary.value.longestStreak, earned: summary.value.longestStreak >= 7, note: `最长连续 ${summary.value.longestStreak} 天` },
  { key: 'distance', title: '累计百公里', target: 100, value: summary.value.totalDistance, earned: summary.value.totalDistance >= 100, note: `累计 ${formatDistance(summary.value.totalDistance)} km` },
  { key: 'weekend', title: '周末跑者', target: 10, value: summary.value.weekendCount, earned: summary.value.weekendCount >= 10, note: `已完成 ${summary.value.weekendCount} 次周末训练` },
  { key: 'night', title: '夜跑侠', measurable: false, earned: false, note: '训练记录尚未保存起跑时间' },
  { key: 'full-month', title: '全勤月', target: summary.value.currentMonthDays, value: summary.value.currentMonthCount, earned: summary.value.fullMonthEarned, note: summary.value.fullMonthEarned ? '已完成一次自然月全勤' : `本月 ${summary.value.currentMonthCount}/${summary.value.currentMonthDays} 天` },
])

const earnedCount = computed(() => achievements.value.filter((item) => item.earned).length)
const achievementRatio = (item) => item.measurable === false ? 0 : Math.min(1, item.value / item.target)
const nextAchievement = computed(() => achievements.value
  .filter((item) => !item.earned && item.measurable !== false)
  .sort((a, b) => achievementRatio(b) - achievementRatio(a))[0] || null)
const nextProgress = computed(() => Math.round(achievementRatio(nextAchievement.value || {}) * 100))
const nextRemaining = computed(() => {
  const item = nextAchievement.value
  if (!item) return '全部可统计荣誉已解锁'
  const remaining = Math.max(0, item.target - item.value)
  if (item.key === 'distance') return `还差 ${formatDistance(remaining)} km`
  if (item.key === 'full-month') return `还差 ${Math.ceil(remaining)} 天`
  return `还差 ${Math.ceil(remaining)} 次`
})

function formatDistance(value) {
  return Number(value || 0).toFixed(1).replace(/\.0$/, '')
}

function barHeight(value) {
  const max = Math.max(1, ...summary.value.week.map((item) => item.distance))
  return `${Math.max(value > 0 ? 12 : 3, Math.round((value / max) * 78))}px`
}

async function handleLogout() {
  loggingOut.value = true
  try {
    await logout()
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <main class="profile-page">
    <header class="profile-header">
      <div>
        <p class="eyebrow">MY RUNWISE · FILE {{ fileNumber }}</p>
        <h1><RansomNote text="跑者档案" :seed="55" /></h1>
      </div>
      <p class="header-code">RUNNER DOSSIER</p>
    </header>

    <p v-if="loadError" class="load-error">{{ loadError }}</p>

    <section class="dossier-grid" aria-label="跑者身份档案">
      <article class="identity-panel">
        <img class="runner-art" :src="runnerArt" alt="" aria-hidden="true" />
        <div class="identity-copy">
          <div class="identity-topline">
            <span>PHANTOM RUNNER</span>
            <strong>LV. {{ pad(runnerLevel) }}</strong>
          </div>
          <p class="identity-index">NO. {{ fileNumber }}</p>
          <h2>{{ nickname }}</h2>
          <p class="motto">坚持奔跑，记录每一步</p>
          <p class="streak-ribbon">CURRENT STREAK · {{ summary.currentStreak }} DAYS</p>
        </div>

        <div class="identity-stats" :class="{ 'is-loading': loading }">
          <div><strong>{{ loading ? '--' : formatDistance(summary.totalDistance) }}</strong><span>累计里程 · km</span></div>
          <div><strong>{{ loading ? '--' : summary.totalCount }}</strong><span>打卡次数 · 次</span></div>
          <div><strong>{{ loading ? '--' : summary.longestStreak }}</strong><span>最长连续 · 天</span></div>
        </div>
      </article>

      <aside class="status-panel">
        <p class="panel-kicker">CURRENT STATUS</p>
        <div class="level-block"><span>LEVEL</span><strong>{{ pad(runnerLevel) }}</strong></div>
        <div class="status-divider"></div>
        <div class="status-line"><span>当前连续</span><strong>{{ summary.currentStreak }} DAYS</strong></div>
        <div v-if="nextAchievement" class="next-target">
          <p>NEXT TARGET</p>
          <h3>{{ nextAchievement.title }}</h3>
          <div class="progress-track"><span :style="{ width: `${nextProgress}%` }"></span></div>
          <div class="progress-meta"><span>{{ nextProgress }}%</span><span>{{ nextRemaining }}</span></div>
        </div>
        <p v-else class="next-complete">ALL MEASURABLE MISSIONS COMPLETE!</p>
        <button type="button" class="text-link" @click="router.push('/stats')">查看完整统计 →</button>
      </aside>
    </section>

    <section class="achievements-section">
      <header class="section-heading">
        <div><p class="section-kicker">HONOR FILE</p><h2>ACHIEVEMENTS / 荣誉档案</h2></div>
        <strong>{{ earnedCount }} / {{ achievements.length }} UNLOCKED</strong>
      </header>
      <div class="achievement-grid">
        <article
          v-for="(item, index) in achievements"
          :key="item.key"
          class="achievement"
          :class="{ earned: item.earned, next: nextAchievement?.key === item.key, unavailable: item.measurable === false }"
        >
          <span class="achievement-index">{{ pad(index + 1) }}</span>
          <span class="achievement-diamond" aria-hidden="true"></span>
          <div><h3>{{ item.title }}</h3><p>{{ item.note }}</p></div>
          <span class="achievement-state">{{ item.earned ? 'UNLOCKED' : item.measurable === false ? 'NO DATA' : nextAchievement?.key === item.key ? 'NEXT' : 'LOCKED' }}</span>
        </article>
      </div>
    </section>

    <section class="lower-grid">
      <article class="week-panel">
        <header class="section-heading compact">
          <div><p class="section-kicker">LAST 7 DAYS</p><h2>THIS WEEK / 本周战绩</h2></div>
          <button type="button" class="text-link" @click="router.push('/stats')">查看统计 →</button>
        </header>
        <div class="week-content">
          <div class="week-summary"><strong>{{ formatDistance(summary.weekDistance) }}</strong><span>km · 近 7 日</span><b>{{ summary.weekCount }} 次训练</b></div>
          <div class="week-chart" aria-label="近七日里程图">
            <div v-for="day in summary.week" :key="day.key" class="week-day" :title="`${day.key} · ${formatDistance(day.distance)} km`">
              <span class="week-value">{{ day.distance ? formatDistance(day.distance) : '' }}</span>
              <i :style="{ height: barHeight(day.distance) }"></i>
              <span>{{ day.label }}</span>
            </div>
          </div>
        </div>
      </article>

      <aside class="account-panel">
        <p class="section-kicker">ACCOUNT</p>
        <h2>设置与账户</h2>
        <p>{{ store.userInfo?.username || nickname }}</p>
        <button type="button" :disabled="loggingOut" @click="handleLogout">{{ loggingOut ? '正在退出…' : '退出登录' }}<span>→</span></button>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.profile-page { width: min(1180px, calc(100% - 48px)); margin: 0 auto; padding: 30px 0 110px; color: var(--p5-white); }
.profile-header, .section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.profile-header { margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--p5-line); }
.eyebrow, .section-kicker, .panel-kicker { margin: 0 0 7px; color: var(--p5-red); font-size: 12px; font-weight: 900; letter-spacing: .16em; }
.profile-header h1 { margin: 0; font-size: clamp(42px, 5vw, 68px); line-height: .95; }
.header-code { color: var(--p5-text-dim); font-size: 15px; letter-spacing: .12em; }
.load-error { margin: 0 0 14px; padding: 10px 14px; color: #fff; background: rgba(230, 0, 18, .2); border-left: 4px solid var(--p5-red); }

.dossier-grid { display: grid; grid-template-columns: minmax(0, 1.82fr) minmax(290px, .85fr); gap: 18px; }
.identity-panel, .status-panel, .achievements-section, .week-panel, .account-panel { position: relative; background: #101012; border: 1px solid rgba(255,255,255,.18); overflow: hidden; }
.identity-panel { min-height: 500px; isolation: isolate; }
.identity-panel::before { content: ''; position: absolute; inset: 0; z-index: -2; background: linear-gradient(135deg, rgba(230,0,18,.15), transparent 43%), radial-gradient(circle at 25% 20%, rgba(255,255,255,.04) 0 1px, transparent 1.5px); background-size: auto, 18px 18px; }
.identity-panel::after { content: ''; position: absolute; inset: auto 0 0; z-index: 1; height: 42%; background: linear-gradient(to bottom, transparent, #101012 62%); pointer-events: none; }
.runner-art { position: absolute; z-index: 0; right: -42px; top: 15px; width: 52%; height: 485px; object-fit: contain; object-position: center right; opacity: .64; filter: saturate(.92) contrast(1.08); -webkit-mask-image: linear-gradient(to bottom, #000 0 68%, transparent 96%); mask-image: linear-gradient(to bottom, #000 0 68%, transparent 96%); }
.identity-copy { position: relative; z-index: 2; width: 64%; padding: 34px 34px 168px; }
.identity-topline { display: flex; justify-content: space-between; gap: 18px; color: #fff; font-weight: 900; letter-spacing: .08em; }
.identity-topline strong { color: var(--p5-red); white-space: nowrap; }
.identity-index { margin-top: 12px; color: var(--p5-text-dim); font: 700 12px ui-monospace, monospace; }
.identity-copy h2 { margin: 62px 0 7px; font-size: clamp(34px, 4vw, 56px); line-height: 1; text-shadow: 4px 4px 0 rgba(230,0,18,.72); }
.motto { color: #c9c9cf; }
.streak-ribbon { margin-top: 22px; padding: 9px 14px; width: min(410px, 100%); color: #fff; background: var(--p5-red); font-size: 14px; font-weight: 900; transform: skewX(-8deg); }
.identity-stats { position: absolute; z-index: 3; left: 28px; right: 28px; bottom: 24px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; padding-top: 19px; border-top: 1px solid rgba(255,255,255,.28); }
.identity-stats div { display: flex; flex-direction: column; text-align: center; }
.identity-stats strong { font-size: clamp(31px, 3vw, 46px); line-height: 1; }
.identity-stats span { margin-top: 7px; color: #aaaab3; font-size: 12px; }
.is-loading { opacity: .65; }

.status-panel { padding: 30px; background: linear-gradient(155deg, #17171a 0 62%, #2b1014 100%); }
.level-block { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 20px; }
.level-block span { color: #aaaab3; font-weight: 800; letter-spacing: .12em; }
.level-block strong { color: var(--p5-red); font-size: clamp(82px, 8vw, 116px); line-height: .78; text-shadow: 4px 4px 0 rgba(255,255,255,.13); }
.status-divider { height: 8px; margin: 28px -38px 24px; background: var(--p5-red); transform: rotate(-2deg); }
.status-line { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,.16); color: #aaaab3; }
.status-line strong { color: #fff; }
.next-target { margin-top: 28px; }
.next-target > p { color: var(--p5-red); font-size: 11px; font-weight: 900; letter-spacing: .15em; }
.next-target h3 { margin: 7px 0 18px; font-size: 25px; }
.progress-track { height: 8px; overflow: hidden; background: #36363d; transform: skewX(-12deg); }
.progress-track span { display: block; height: 100%; background: var(--p5-red); }
.progress-meta { display: flex; justify-content: space-between; margin-top: 9px; color: #aaaab3; font-size: 12px; }
.next-complete { margin-top: 30px; color: #d4a44f; font-weight: 900; }
.text-link { color: var(--p5-red); font-weight: 800; }
.status-panel > .text-link { position: absolute; left: 30px; bottom: 26px; }

.achievements-section { margin-top: 20px; padding: 26px; overflow: visible; }
.section-heading h2, .account-panel h2 { margin: 0; font-size: clamp(22px, 2.3vw, 31px); }
.section-heading > strong { color: var(--p5-red); font-size: 13px; white-space: nowrap; }
.achievement-grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 12px; margin-top: 22px; }
.achievement { position: relative; grid-column: span 4; min-height: 122px; display: flex; align-items: center; gap: 18px; padding: 24px 20px 17px; color: #8d8d98; background: #19191d; border: 1px solid #393941; }
.achievement:nth-child(even) { transform: translateY(5px); }
.achievement.earned { color: #fff; background: linear-gradient(135deg, #3b171b, #241316); border-color: #d4a44f; box-shadow: inset 0 0 22px rgba(212,164,79,.09); transform: rotate(-.7deg); }
.achievement.earned:nth-child(even) { transform: translateY(5px) rotate(.7deg); }
.achievement.next { grid-column: span 6; color: #fff; border-color: var(--p5-red); background: linear-gradient(125deg, rgba(230,0,18,.22), #19191d 62%); }
.achievement.unavailable { opacity: .66; }
.achievement-index { position: absolute; top: 8px; left: 10px; color: var(--p5-red); font: 800 10px ui-monospace, monospace; }
.achievement-diamond { flex: 0 0 30px; width: 30px; height: 30px; border: 2px solid #656570; transform: rotate(45deg); }
.achievement.earned .achievement-diamond { background: var(--p5-red); border-color: #efc86d; box-shadow: 0 0 15px rgba(212,164,79,.35); }
.achievement h3 { margin: 0 0 7px; font-size: 17px; color: inherit; }
.achievement p { margin: 0; font-size: 12px; line-height: 1.5; color: #aaaab3; }
.achievement-state { position: absolute; top: 9px; right: 10px; font-size: 9px; letter-spacing: .1em; color: #777782; }
.achievement.earned .achievement-state, .achievement.next .achievement-state { color: #efc86d; }

.lower-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(250px, .8fr); gap: 18px; margin-top: 20px; }
.week-panel, .account-panel { padding: 25px; }
.section-heading.compact { align-items: center; }
.week-content { display: grid; grid-template-columns: 190px 1fr; align-items: end; gap: 24px; margin-top: 18px; }
.week-summary { display: flex; flex-direction: column; }
.week-summary strong { font-size: 58px; line-height: .92; text-shadow: 4px 4px 0 var(--p5-red); }
.week-summary span, .week-summary b { margin-top: 8px; color: #aaaab3; font-size: 12px; }
.week-chart { height: 118px; display: grid; grid-template-columns: repeat(7, 1fr); align-items: end; gap: 10px; border-bottom: 1px solid #47474f; }
.week-day { height: 110px; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 5px; color: #8e8e98; font-size: 10px; }
.week-day i { width: min(24px, 70%); background: var(--p5-red); clip-path: polygon(10% 0,100% 0,85% 100%,0 100%); }
.week-value { min-height: 12px; color: #ddd; font-size: 9px; }
.account-panel { display: flex; flex-direction: column; }
.account-panel > p:not(.section-kicker) { margin-top: 15px; color: #8e8e98; }
.account-panel button { display: flex; justify-content: space-between; margin-top: auto; padding: 15px 0 4px; color: var(--p5-red); border-top: 1px solid #393941; font-weight: 900; }
.account-panel button:disabled { opacity: .5; }

@media (max-width: 1040px) {
  .profile-page { width: min(100% - 36px, 940px); }
  .dossier-grid { grid-template-columns: minmax(0, 1.55fr) minmax(270px, .8fr); }
  .runner-art { right: -65px; width: 57%; }
  .identity-copy { width: 69%; padding-left: 26px; }
  .achievement { grid-column: span 6; }
}
@media (max-width: 860px) {
  .dossier-grid, .lower-grid { grid-template-columns: 1fr; }
  .status-panel { min-height: 390px; }
  .achievement.next { grid-column: span 6; }
}
@media (max-width: 640px) {
  .profile-page { width: calc(100% - 24px); padding-top: 20px; }
  .header-code { display: none; }
  .identity-panel { min-height: 520px; }
  .runner-art { right: -85px; top: 48px; width: 78%; opacity: .48; }
  .identity-copy { width: 83%; padding: 24px 20px 180px; }
  .identity-copy h2 { margin-top: 56px; }
  .identity-stats { left: 14px; right: 14px; gap: 4px; }
  .identity-stats strong { font-size: 28px; }
  .identity-stats span { font-size: 10px; }
  .achievements-section, .week-panel, .account-panel { padding: 18px; }
  .section-heading { align-items: flex-start; }
  .achievement, .achievement.next { grid-column: span 12; }
  .achievement:nth-child(n) { transform: none; }
  .week-content { grid-template-columns: 1fr; }
  .week-summary strong { font-size: 48px; }
}
</style>
