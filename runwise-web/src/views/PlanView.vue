<script setup>
// 训练计划页（工程计划 4.7；Phase B 功能态）：
// - B1 类型标签引用 constants/trainingTypes.js（组件零硬编码颜色）
// - B2 「今天在哪」：按系统星期定位本表行（红左边框 + TODAY/REST 贴纸，全表唯一）
// - B3 计划管理：+ 添加训练 / 行内编辑删除，数据经 services/planService 收口
// - B4 行 hover 背景 #16161A；B5 距离数字 Anton 16px 白 + 灰色小单位
// - 课表表格保持零倾斜（3.4.3 硬规则）
import { computed, onMounted, ref } from 'vue'
import P5Card from '../components/common/P5Card.vue'
import PlanEditModal from '../components/business/PlanEditModal.vue'
import * as planService from '../services/planService'
import { resolveTrainingType, WEEKDAY_OPTIONS } from '../constants/trainingTypes'

const weeks = ref([])
const currentWeek = ref(1)
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)

// ===== 弹窗状态（add / edit / confirm-remove 三态复用一个遮罩） =====
const modal = ref(null) // { mode: 'add' } | { mode: 'edit', day } | { mode: 'confirm', day }

onMounted(async () => {
  try {
    weeks.value = (await planService.getWeeks()) || []
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

function selectWeek(weekNo) {
  currentWeek.value = weekNo
}

const currentWeekData = computed(() => weeks.value.find((w) => w.weekNo === currentWeek.value))

// ===== C3：周合计页脚（由表格行自动求和，禁止手写死数值） =====
const weekSummary = computed(() => {
  const days = currentWeekData.value?.days || []
  const totalKm = days.reduce((sum, d) => sum + (d.targetKm || 0), 0)
  return {
    totalKm: totalKm.toFixed(1),
    trainCount: days.filter((d) => d.type !== 'rest').length,
    restCount: days.filter((d) => d.type === 'rest').length,
  }
})

// ===== B2：今天定位（仅第 1 周含今天；同日刷新稳定） =====
const todayWeekday = computed(() => {
  const idx = (new Date().getDay() + 6) % 7 // 周一 = 0
  return WEEKDAY_OPTIONS[idx]
})

function isToday(day) {
  return currentWeek.value === 1 && day.weekday === todayWeekday.value
}

// ===== B1：类型标签（映射表统一出 label 与配色类） =====
function tagOf(day) {
  return resolveTrainingType(day.type)
}

// ===== B3：增删改（全部经 planService，组件不碰数据源） =====
function openAdd() {
  modal.value = { mode: 'add' }
}

function openEdit(day) {
  modal.value = { mode: 'edit', day }
}

function openConfirm(day) {
  modal.value = { mode: 'confirm', day }
}

function closeModal() {
  modal.value = null
}

async function reload() {
  weeks.value = (await planService.getWeeks()) || []
}

async function onModalSubmit(payload) {
  saving.value = true
  try {
    const m = modal.value
    if (m.mode === 'add') {
      await planService.add({ weekNo: currentWeek.value, ...payload })
    } else if (m.mode === 'edit') {
      await planService.update(m.day.id, payload)
    } else if (m.mode === 'confirm') {
      await planService.remove(m.day.id)
    }
    await reload()
    closeModal()
  } catch {
    loadError.value = true
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="plan-page">
    <header class="page-head p5-page-header">
      <p class="page-kicker">RUNWISE WEB</p>
      <h1 class="page-title p5-page-title">训练计划</h1>
      <p class="page-desc">4 周课表安排 —— 轻松跑、节奏跑、间歇与长距离的科学配比。</p>
    </header>

    <div class="p5-divider" aria-hidden="true"></div>

    <!-- 红框工艺（frame="red"）= AI 答疑页三卡同款：白 2px 描边 + 左上/右下
         12px 斜切 + 22px 红角标 + 6px 深红错位实心影 + 红底白字贴纸 -->
    <P5Card
      tag="WEEKLY PLAN"
      tag-rotate="-4deg"
      tag-top="-16px"
      tag-left="28px"
      frame="red"
    >
      <div class="plan-body">
        <!-- 周切换 + 添加训练 -->
        <div class="plan-toolbar">
          <div class="week-tabs">
            <button
              v-for="w in weeks"
              :key="w.weekNo"
              class="week-tab"
              type="button"
              :class="{ 'week-tab--active': w.weekNo === currentWeek }"
              @click="selectWeek(w.weekNo)"
            >
              第 {{ w.weekNo }} 周
            </button>
          </div>
          <!-- B3.1：红平行四边形添加按钮 -->
          <button class="add-btn" type="button" @click="openAdd">+ 添加训练</button>
        </div>

        <!-- 课表表格 -->
        <div v-if="loading" class="plan-status">课表加载中…</div>
        <div v-else-if="loadError" class="plan-status">
          课表加载失败，请刷新重试
        </div>
        <table v-else-if="currentWeekData" class="plan-table">
          <thead>
            <tr>
              <th>星期</th>
              <th>类型</th>
              <th class="col-num">目标距离</th>
              <th>目标配速</th>
              <th class="col-status">状态</th>
              <th class="col-actions" aria-label="操作"></th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="d in currentWeekData.days"
              :key="d.id || d.date"
              :class="{
                'row--rest': d.type === 'rest',
                'row--today': isToday(d),
              }"
            >
              <td class="cell-weekday">
                {{ d.weekday }}
                <!-- B2：TODAY / REST 贴纸（全表有且仅有一行） -->
                <span v-if="isToday(d)" class="today-sticker" :class="{ 'sticker--rest': d.type === 'rest' }">
                  {{ d.type === 'rest' ? 'REST' : 'TODAY' }}
                </span>
              </td>
              <td>
                <span class="tt-tag" :class="tagOf(d).cls" :title="d.note || ''">{{ tagOf(d).label }}</span>
              </td>
              <!-- B5：Anton 16px 白数字 + 灰色小单位 -->
              <td class="cell-num">
                <template v-if="d.targetKm > 0">
                  <span class="km-num p5-num">{{ d.targetKm.toFixed(1) }}</span>
                  <span class="km-unit p5-unit">km</span>
                </template>
                <span v-else class="cell-dash">—</span>
              </td>
              <td class="cell-pace">{{ d.paceRange || '—' }}</td>
              <td class="cell-status">
                <span
                  class="status-dot"
                  :class="{
                    'dot--rest': d.type === 'rest',
                    'dot--live': isToday(d) && d.type !== 'rest',
                  }"
                ></span>
                <!-- C5：今天的训练行状态升级「进行中」（今天的休息行仍为 REST/休息日，不冲突） -->
                <span>{{
                  d.type === 'rest' ? '休息日' : isToday(d) ? '进行中' : '待执行'
                }}</span>
              </td>
              <!-- B3.3：行 hover 显示编辑/删除 -->
              <td class="cell-actions">
                <button class="row-act" type="button" @click="openEdit(d)">编辑</button>
                <button
                  v-if="d.type !== 'rest'"
                  class="row-act row-act--danger"
                  type="button"
                  @click="openConfirm(d)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
          <!-- C3：周合计页脚行——数据由表格行自动求和 -->
          <tfoot>
            <tr>
              <td colspan="6" class="plan-foot">
                <span class="foot-label">本周合计</span>
                <span class="foot-num p5-num">{{ weekSummary.totalKm }}</span>
                <span class="foot-unit">km</span>
                <span class="foot-sep">·</span>
                <span class="foot-num p5-num">{{ weekSummary.trainCount }}</span>
                <span class="foot-label">训</span>
                <span class="foot-sep">·</span>
                <span class="foot-num p5-num">{{ weekSummary.restCount }}</span>
                <span class="foot-label">休</span>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </P5Card>

    <!-- B3.2：编辑弹窗（add / edit / 删除二次确认三态） -->
    <PlanEditModal
      :visible="!!modal"
      :mode="modal?.mode || 'add'"
      :week-no="currentWeek"
      :initial="modal?.day || null"
      @close="closeModal"
      @submit="onModalSubmit"
    />
  </div>
</template>

<style scoped>
.plan-page {
  display: flex;
  flex-direction: column;
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

.plan-body {
  padding: var(--sp-5) var(--sp-6) var(--sp-6);
}

/* ===== 工具行：周切换 + 添加 ===== */
.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
  margin-bottom: var(--sp-5);
}

.week-tabs {
  display: flex;
  gap: var(--sp-3);
}

.week-tab {
  position: relative; /* 承载 C4 斜杠下划线 */
  padding: 8px 24px;
  font-size: var(--fs-sub);
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
  background: transparent;
  border: 1px solid var(--p5-line);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}

.week-tab:hover {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.week-tab--active {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
  font-weight: 700;
}

/* C4：非当前周 hover 红色斜杠下划线（0.25s 滑入）；
   按钮有 clip-path，伪元素须留在按钮内边界以内 */
.week-tab:not(.week-tab--active)::after {
  content: "";
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 3px;
  height: 2px;
  background: var(--p5-red);
  transform: skewX(-24deg) scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s;
}

.week-tab:not(.week-tab--active):hover::after {
  transform: skewX(-24deg) scaleX(1);
}

/* B3.1：红平行四边形 + 站酷酷黑 14px */
.add-btn {
  padding: 9px 22px;
  font-family: var(--font-cn-display);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.12em;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition: background 0.15s;
}

.add-btn:hover {
  background: var(--p5-red-dark);
}

/* ===== 课表表格（零倾斜，3.4.3 硬规则） ===== */
.plan-table {
  width: 100%;
  border-collapse: collapse;
}

.plan-table th {
  padding: var(--sp-3) var(--sp-3);
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-align: left;
  color: var(--p5-text-dim);
  border-bottom: 1px solid var(--p5-line);
}

.plan-table td {
  padding: var(--sp-4) var(--sp-3);
  font-size: var(--fs-sub);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.plan-table tr:last-child td {
  border-bottom: none;
}

/* B4：行 hover 微亮（红留给卡片，行不用红） */
.plan-table tbody tr {
  transition: background 0.15s;
}

.plan-table tbody tr:hover {
  background: #16161a;
}

/* C2：休息日弱化——整行背景再暗一档 */
.row--rest {
  background: rgba(0, 0, 0, 0.25);
}

.row--rest td {
  color: var(--p5-text-dim);
}

/* C2：类型列「休息」降为 50% 透明度斜体 */
.row--rest .tt-tag {
  opacity: 0.5;
  font-style: italic;
}

/* B2：今日行——行首 4px 红实线左边框 */
tr.row--today td {
  border-top: 1px solid var(--p5-line);
  border-bottom: 1px solid var(--p5-line);
}

tr.row--today td:first-child {
  border-left: 4px solid var(--p5-red);
  padding-left: calc(var(--sp-3) - 4px);
}

.cell-weekday {
  font-weight: 700;
  color: var(--p5-white);
  white-space: nowrap;
}

/* B2：TODAY / REST 贴纸（Anton 11px，-3°） */
.today-sticker {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 7px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.12em;
  line-height: 1.4;
  color: var(--p5-white);
  background: var(--p5-red);
  transform: rotate(-3deg);
  vertical-align: middle;
}

.today-sticker.sticker--rest {
  color: var(--p5-black);
  background: var(--p5-gray);
}

.col-num,
.cell-num {
  text-align: right;
  white-space: nowrap;
}

/* B5：距离数字 Anton 16px 白 + 灰色小单位 */
.cell-num {
  font-size: 16px;
  color: var(--p5-white);
}

.km-num {
  font-size: 16px;
  color: var(--p5-white);
}

.cell-dash {
  color: var(--p5-text-dim);
}

.row--rest .cell-num {
  font-size: var(--fs-sub);
}

.col-status,
.cell-status {
  text-align: right;
  white-space: nowrap;
}

.cell-pace {
  color: var(--p5-text-dim);
  white-space: nowrap;
}

/* ===== C1：行尾操作按钮（非 hover 一律隐藏 + 右侧滑入 0.2s） =====
   修复说明：原实现附 :focus-within——点击编辑/关闭弹窗后焦点留在按钮上，
   操作列便永久可见（截图里周一/周三「编辑」常驻的根因）。现仅由行 hover 驱动 */
.col-actions,
.cell-actions {
  text-align: right;
  white-space: nowrap;
}

.cell-actions {
  opacity: 0;
  transform: translateX(12px); /* 从右侧滑入 */
  transition:
    opacity 0.2s,
    transform 0.2s;
  pointer-events: none; /* 隐藏态不响应点击，避免误触 */
}

.plan-table tbody tr:hover .cell-actions {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}

.row-act {
  padding: 2px 4px;
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s;
}

.row-act:hover {
  color: var(--p5-red);
}

.row-act--danger:hover {
  color: var(--p5-red);
  text-decoration: underline;
}

/* 状态点：训练日菱形红 / 休息日圆点灰 */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 8px;
  background: var(--p5-red);
  transform: rotate(45deg);
}

.status-dot.dot--rest {
  background: var(--p5-text-dim);
  transform: none;
  border-radius: 50%;
}

/* C5：今日训练行「进行中」——红点呼吸（仅透明度，避开菱形 rotate 冲突） */
.status-dot.dot--live {
  animation: dot-breath 1.6s ease-in-out infinite;
}

@keyframes dot-breath {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

@media (prefers-reduced-motion: reduce) {
  .status-dot.dot--live {
    animation: none;
  }
}

/* ===== C3：周合计页脚行（数字 Anton 20px 白，标签灰） ===== */
/* 提高特异性以压过 .plan-table td 的默认 padding */
.plan-table td.plan-foot {
  padding: var(--sp-4) var(--sp-3);
  text-align: right;
  border-top: 1px solid var(--p5-line);
  border-bottom: none;
}

.foot-num {
  font-size: 20px;
  line-height: 1;
  color: var(--p5-white);
}

.foot-label,
.foot-unit,
.foot-sep {
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
}

.foot-label {
  margin: 0 4px 0 6px;
}

.foot-unit {
  margin-left: 4px;
}

.foot-sep {
  margin: 0 10px;
}

.plan-status {
  padding: var(--sp-6) 0;
  text-align: center;
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
}

/* 删除二次确认文案（弹窗插槽） */
.confirm-text {
  font-size: var(--fs-body);
  line-height: 1.8;
  color: var(--p5-white);
}

@media (max-width: 760px) {
  .cell-pace,
  .plan-table th:nth-child(4) {
    display: none;
  }

  .plan-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
