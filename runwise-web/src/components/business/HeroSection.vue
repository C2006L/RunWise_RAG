<script setup>
// 首页 Hero 区（V3 施工规格书 · 斜纹背景系统 + 纯文字菜单）
// 素材检测结论：hero-runner.png 为透明背景（alpha=0 采样确认）→ 中央黑色走廊设计，
// 色带不进入人物主体区域（两种背景情况均兼容）
// 层级（禁止改变）：z1 色带 → z1 装饰（DOM 后置，色带之上人物之下）→
//   z2 人物 + 飘带 → z3 左侧功能区 + 右侧纯文字菜单 → z4 RuN wIsE 签名位
// 删除清单（V3 第 1 部分）：「开始跑步」按钮 / 01 名牌 / 三块名牌盒子 /
//   SlashShape ×2 —— 全部不再出现
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import JaggedStar from '../../assets/shapes/JaggedStar.vue';
import * as statsApi from '../../api/stats';
import * as checkinApi from '../../api/checkin';
import { formatDate } from '../../composables/useFormatDate';
import heroRunnerUrl from '../../assets/images/hero-runner.png';

const router = useRouter();
const weekly = ref(null);
const todayRecord = ref(null);
const WEEK_TARGET_KM = 40;

onMounted(() => {
  statsApi
    .getWeeklyStats()
    .then((w) => {
      weekly.value = w;
    })
    .catch(() => {});
  checkinApi
    .getCheckinByDate(formatDate(new Date()))
    .then((r) => {
      todayRecord.value = r;
    })
    .catch(() => {});
});

const dataLine = computed(() =>
  weekly.value
    ? `本周 ${weekly.value.totalKm} / ${WEEK_TARGET_KM} KM ── 配速 7'07"/km ── 已打卡 ${weekly.value.checkinCount} 次`
    : `本周 -- / ${WEEK_TARGET_KM} KM ── 配速 --'--"/km ── 已打卡 -- 次`,
);

const todayLine = computed(() => {
  const r = todayRecord.value;
  if (!r) return '今日还未打卡 —— 去完成第一次奔跑';
  // r.pace 来自 formatPace，已含 /km 单位，禁止再拼接
  return `今日已打卡 ${r.distanceKm}km · ${r.durationMin}min · ${r.pace}`;
});

const weekPercent = computed(() => {
  if (!weekly.value) return 0;
  return Math.min(100, Math.round((weekly.value.totalKm / WEEK_TARGET_KM) * 100));
});

// ===== ransom-note 字母生成（菜单与主标题共用）=====
// 三分法轮转：invert 反白贴片（黑底白字+白描边）/ white-red 白字红影 / red-black 红字黑影
const ROTS = ['-5deg', '4deg', '-3deg', '5deg', '-4deg', '3deg'];
const SIZES = [1.1, 0.95, 1.05, 0.9, 1.0, 0.92];

function ransomLetters(word, seed = 0) {
  return word.split('').map((ch, i) => {
    if (ch === ' ') return { ch, gap: true };
    const k = (i + seed) % 3;
    return {
      ch,
      gap: false,
      kind: k === 0 ? 'invert' : k === 1 ? 'wr' : 'rb',
      rot: ROTS[(i + seed) % 6],
      size: SIZES[(i + seed) % 6],
    };
  });
}

// ===== 右侧纯文字菜单（第 6 部分）：无容器无边框，字母即图形 =====
const MENU = [
  { key: 'qa', word: 'SmArT Q&A', note: '智能问答', no: '02', to: '/qa', cls: 'm-1', base: '34px', rot: '-7deg' },
  { key: 'plan', word: 'TrAiN PlAn', note: '训练计划', no: '03', to: '/plan', cls: 'm-2', base: '40px', rot: '-11deg' },
  { key: 'injury', word: 'InJuRy CaRe', note: '伤病预防', no: '04', to: '/injury', cls: 'm-3', base: '34px', rot: '-6deg' },
].map((m, idx) => ({ ...m, letters: ransomLetters(m.word, idx * 2) }));

// 主标题 RuN wIsE（第 7 部分）
const TITLE = ransomLetters('RuNwIsE', 1).filter((l) => !l.gap);
// 在 RUN 与 WISE 间插入间隙（原词无空格，手动分组）
const TITLE_GAP_INDEX = 3;

// ===== 鞋印 ×8：沿中央走廊左下 → 右上，间距渐大 =====
const FOOTPRINTS = [
  { x: '30%', y: '78%', r: '12deg', c: 'white', o: 0.75, s: 34 },
  { x: '36%', y: '71%', r: '-10deg', c: 'red', o: 0.7, s: 36 },
  { x: '43%', y: '63%', r: '14deg', c: 'white', o: 0.65, s: 38 },
  { x: '51%', y: '55%', r: '-12deg', c: 'red', o: 0.6, s: 40 },
  { x: '59%', y: '46%', r: '10deg', c: 'white', o: 0.55, s: 42 },
  { x: '67%', y: '36%', r: '-14deg', c: 'red', o: 0.5, s: 44 },
  { x: '74%', y: '25%', r: '12deg', c: 'white', o: 0.45, s: 46 },
  { x: '80%', y: '14%', r: '-10deg', c: 'red', o: 0.4, s: 48 },
];

// ===== 里程牌 ×3（右上区域散布，允许出血；42 号置于菜单②下方空档，不遮字母）=====
const MILESTONES = [
  { km: '1', x: '58%', y: '8%', rot: '-6deg' },
  { km: '5', x: '72%', y: '20%', rot: '8deg' },
  { km: '42', x: '94%', y: '46%', rot: '-4deg' },
];

// ===== 散落小椭圆点 ×4（F4：漂浮 + 透明度呼吸，黑色区域空隙）=====
const DOTS = [
  { x: '18%', y: '30%', w: 10, h: 6, c: 'red', dur: '3.2s', delay: '0s' },
  { x: '48%', y: '26%', w: 8, h: 5, c: 'white', dur: '3.8s', delay: '1.1s' },
  { x: '70%', y: '74%', w: 12, h: 7, c: 'red', dur: '3.5s', delay: '0.6s' },
  { x: '90%', y: '58%', w: 7, h: 5, c: 'gray', dur: '4.1s', delay: '1.8s' },
];

// 菜单/大卡跳转均为路由直跳（锚点滚动方案已随卡片区移除）
function gotoCheckin() {
  router.push('/checkin');
}
</script>

<template>
  <section class="hero">
    <!-- ===== 第 3 部分 z1：斜纹背景系统（全部 -12° 平行） ===== -->
    <span class="band band-red-a" aria-hidden="true"></span>
    <span class="band band-white-1" aria-hidden="true"></span>
    <span class="band band-red-b" aria-hidden="true"></span>
    <span class="band band-white-2" aria-hidden="true"></span>
    <span class="band band-red-c" aria-hidden="true"></span>
    <!-- 黑色走廊内的白色网点（左下数据条后方） -->
    <span class="band band-ht-black" aria-hidden="true"></span>

    <!-- ===== 第 8 部分 z1（DOM 后置 → 色带之上、人物之下）：装饰 6 类 ===== -->
    <!-- 1. 鞋印 ×8 + 快乐体「RUN!」起点 -->
    <span class="fun fun-run">RUN!</span>
    <span
      v-for="(f, i) in FOOTPRINTS"
      :key="'fp' + i"
      class="footprint"
      :class="[f.c, i >= 5 ? 'fp--tail' : '']"
      :style="{
        left: f.x,
        top: f.y,
        width: f.s + 'px',
        opacity: f.o,
        transform: `rotate(${f.r})`,
      }"
      aria-hidden="true"
    >
      <svg viewBox="0 0 40 72" fill="currentColor">
        <path d="M20 4 C12 4 9 11 9 19 C9 27 12 33 20 35 C28 33 31 27 31 19 C31 11 28 4 20 4 Z" />
        <ellipse cx="20" cy="53" rx="8" ry="13" />
      </svg>
    </span>

    <!-- 2. 心率线 ×1（F5：波形横向循环滚动 + 红点沿波形跑动）+ 快乐体「别停！」末端 -->
    <span class="heartbeat" aria-hidden="true">
      <svg viewBox="0 0 1200 80" fill="none">
        <g class="hb-scroll">
          <path
            id="hb-path"
            d="M0,40 80,40 100,40 115,10 130,64 145,34 160,40 260,40 280,18 295,58 310,40 420,40 440,12 455,62 470,40 600,40"
            stroke="var(--p5-red)"
            stroke-width="3"
          />
          <!-- 三份拷贝铺满 0~1800：滚动 0~-600 全程屏内恒有波形，无缝循环 -->
          <use href="#hb-path" x="600" />
          <use href="#hb-path" x="1200" />
          <!-- 红点在滚动组内：与波形同一 translateX 坐标系 + 同为 10s 周期，
               屏幕上原地沿波形起伏骑行（修复点与波不同步） -->
          <circle r="5" fill="var(--p5-red)">
            <animateMotion
              dur="10s"
              repeatCount="indefinite"
              rotate="0"
              path="M600,40 680,40 700,40 715,10 730,64 745,34 760,40 860,40 880,18 895,58 910,40 1020,40 1040,12 1055,62 1070,40 1200,40"
            />
          </circle>
        </g>
      </svg>
    </span>
    <span class="fun fun-stop">别停！</span>

    <!-- 散落小椭圆点 ×4（F4 漂浮呼吸） -->
    <span
      v-for="(d, i) in DOTS"
      :key="'dot' + i"
      class="dot"
      :class="d.c"
      :style="{
        left: d.x,
        top: d.y,
        width: d.w + 'px',
        height: d.h + 'px',
        animationDuration: d.dur,
        animationDelay: d.delay,
      }"
      aria-hidden="true"
    ></span>

    <!-- 3. 里程牌 ×3（黑底白描边小方牌，右下红角标） -->
    <span
      v-for="(m, i) in MILESTONES"
      :key="'ms' + i"
      class="milestone"
      :style="{ left: m.x, top: m.y, transform: `rotate(${m.rot})` }"
      aria-hidden="true"
    >
      <span class="ms-km">KM</span>
      <span class="ms-num p5-num">{{ m.km }}</span>
      <span class="ms-flag"></span>
    </span>

    <!-- 4. 号码布 ×1（白底黑字 42，顶部两别针，悬浮大卡上方轻压卡顶） -->
    <span class="bib" aria-hidden="true">
      <span class="bib-pin bib-pin--l"></span>
      <span class="bib-pin bib-pin--r"></span>
      <span class="bib-num p5-num">42</span>
    </span>

    <!-- 5. 秒表 ×1（红描边，全页唯一正角 rotate(12deg)） -->
    <span class="stopwatch" aria-hidden="true">
      <svg viewBox="0 0 80 92" fill="none">
        <rect x="33" y="2" width="14" height="10" fill="var(--p5-red)" />
        <circle cx="40" cy="52" r="28" stroke="var(--p5-red)" stroke-width="4" />
        <line x1="40" y1="52" x2="40" y2="30" stroke="var(--p5-red)" stroke-width="4" />
        <line x1="40" y1="52" x2="58" y2="60" stroke="var(--p5-red)" stroke-width="3" />
      </svg>
    </span>

    <!-- 6. 星星 ×2（仅左下与右下黑色区域） -->
    <span class="deco-star deco-star--lb" aria-hidden="true">
      <JaggedStar color="red" :size="80" :seed="1" />
    </span>
    <span class="deco-star deco-star--rb" aria-hidden="true">
      <JaggedStar color="gray" :size="64" :seed="2" />
    </span>

    <!-- ===== 第 4 部分 z2：人物（透明底，居中，与黑走廊融合） ===== -->
    <img class="runner" :src="heroRunnerUrl" alt="奔跑的跑者" draggable="false" />

    <!-- ===== 第 9 部分 z2：底部飘带（C：4 份相同 seg，-25% 恰好一份宽，全程无缝） ===== -->
    <div class="ribbon" aria-hidden="true">
      <div class="ribbon-track">
        <span class="ribbon-seg">KEEP RUNNING · DON'T STOP · EVERY STEP COUNTS · RUN WISE ·&nbsp;</span>
        <span class="ribbon-seg">KEEP RUNNING · DON'T STOP · EVERY STEP COUNTS · RUN WISE ·&nbsp;</span>
        <span class="ribbon-seg">KEEP RUNNING · DON'T STOP · EVERY STEP COUNTS · RUN WISE ·&nbsp;</span>
        <span class="ribbon-seg">KEEP RUNNING · DON'T STOP · EVERY STEP COUNTS · RUN WISE ·&nbsp;</span>
      </div>
    </div>

    <!-- ===== 第 5 部分 z3：左侧功能区 ===== -->
    <!-- 5.1 副标题（页面最顶部，坐落 RED-A 上） -->
    <p class="sub-kicker">
      你的智能跑步训练中心 —— 打卡、答疑与数据统计，一站直达
    </p>

    <!-- 5.2 今日打卡大卡（A1：CHECK-IN 标签独立于卡外，骑压上边框不被 clip-path 裁切） -->
    <div class="ck-wrap">
      <div class="checkin-card">
        <h2 class="ck-title">今日打卡</h2>
        <p class="ck-today" :class="{ 'ck-today--done': todayRecord }">
          {{ todayLine }}
        </p>
        <div class="ck-progress">
          <div class="ck-progress-meta">
            <span>本周目标进度</span>
            <span class="p5-num">{{ weekPercent }}%</span>
          </div>
          <div class="ck-progress-track">
            <div class="ck-progress-fill" :style="{ width: weekPercent + '%' }"></div>
          </div>
        </div>
        <button class="ck-btn" type="button" @click="gotoCheckin">去打卡 →</button>
        <span class="ck-flag" aria-hidden="true"></span>
      </div>
      <!-- 标签在卡片兄弟层级、z 更高：边框线不得穿过标签 -->
      <span class="ck-tag">CHECK-IN</span>
    </div>

    <!-- 5.3 本周数据条（人物脚踩） + 快乐体「7 DAYS」上方 -->
    <span class="fun fun-days">7 DAYS</span>
    <button class="data-strip" type="button" @click="router.push('/stats')">
      <span class="ds-text">{{ dataLine }}</span>
    </button>

    <!-- ===== 第 6 部分 z3：右侧纯文字菜单（无容器无框，点击路由直跳） ===== -->
    <nav
      v-for="m in MENU"
      :key="m.key"
      class="menu-item"
      :class="m.cls"
      :style="{ '--m-base': m.base, transform: `rotate(${m.rot})` }"
      @click="router.push(m.to)"
    >
      <span class="m-no p5-num">{{ m.no }}</span>
      <span class="m-word" :aria-label="m.word">
        <template v-for="(l, i) in m.letters" :key="i">
          <span v-if="l.gap" class="m-gap"></span>
          <span
            v-else
            class="mlt"
            :class="'mlt--' + l.kind"
            :style="{ transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
            >{{ l.ch }}</span
          >
        </template>
      </span>
      <span class="m-note">{{ m.note }}</span>
      <!-- hover 红斜杠下划线 -->
      <span class="m-underline" aria-hidden="true"></span>
    </nav>

    <!-- ===== 第 7 部分 z4：RuN wIsE 签名位（右下） ===== -->
    <div class="hero-sign">
      <h1 class="ransom" aria-label="RUN WISE">
        <template v-for="(l, i) in TITLE" :key="i">
          <span
            class="mlt"
            :class="'mlt--' + l.kind"
            :style="{ transform: `rotate(${l.rot})`, fontSize: l.size + 'em' }"
            >{{ l.ch }}</span
          >
          <span v-if="i === TITLE_GAP_INDEX - 1" class="m-gap"></span>
        </template>
      </h1>
      <p class="sign-sub">SINCE 2024 · To Every Runner</p>
    </div>
  </section>
</template>

<style scoped>
/* ===== 第 2 部分 · Hero 容器 ===== */
.hero {
  position: relative;
  height: 100vh;
  min-height: 640px;
  overflow: hidden;
  background: var(--p5-black); /* BASE 整屏黑底 */
}

/* =====================================================================
   第 3 部分 · 斜纹背景系统：全部 -12° 平行（主轴，禁止杂角度）
   ===================================================================== */
.band {
  position: absolute;
  pointer-events: none;
  z-index: 1;
  transform: rotate(-12deg);
}

/* F1：红色带网点纹理缓慢向右上漂移（30s 一循环，background-position 不触发重排） */
.band-red-a,
.band-red-b,
.band-red-c {
  background-color: var(--p5-red);
  background-image: radial-gradient(
    circle,
    rgba(176, 13, 36, 0.5) 2.5px,
    transparent 2.6px
  );
  background-size: 10px 10px;
  animation: band-drift 30s linear infinite;
}

@keyframes band-drift {
  from {
    background-position: 0 0;
  }
  to {
    background-position: 300px -300px;
  }
}

/* RED-A 左上大带：覆盖左上约 30%，副标题坐落其上 */
.band-red-a {
  left: -220px;
  top: -110px;
  width: 780px;
  height: 300px;
}

/* WHITE-1 白细带：高 48px，平行 RED-A 下缘，从左缘延伸至 60% 宽 */
.band-white-1 {
  left: -160px;
  top: 218px;
  width: 720px;
  height: 48px;
  background: var(--p5-white);
}

/* RED-B 右上大带：覆盖右上约 35%，菜单①②坐落其上 */
.band-red-b {
  left: 560px;
  top: -180px;
  width: 900px;
  height: 380px;
}

/* WHITE-2 白细带：高 40px，右下区域，穿过 RuN wIsE 标题上方 */
.band-white-2 {
  left: 560px;
  top: 470px;
  width: 800px;
  height: 40px;
  background: var(--p5-white);
}

/* RED-C 左下小带：从左边缘 bottom 0~15% 进入，短带 */
.band-red-c {
  left: -120px;
  bottom: -40px;
  width: 320px;
  height: 120px;
}

/* 黑走廊白色网点 ×1：opacity 0.35，左下数据条后方 */
.band-ht-black {
  left: 40px;
  bottom: 60px;
  width: 360px;
  height: 180px;
  background-image: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.35) 1.6px,
    transparent 1.7px
  );
  background-size: 10px 10px;
}

/* =====================================================================
   第 8 部分 · 装饰（z1，DOM 后置于色带 → 色带之上、人物之下）
   ===================================================================== */

/* 快乐体点缀 ×3（ZCOOL KuaiLe，全站名额 3 处全在此） */
.fun {
  position: absolute;
  z-index: 1;
  font-family: var(--font-fun);
  pointer-events: none;
}

/* 「RUN!」鞋印起点（G2：上移避开数据条） */
.fun-run {
  left: 26%;
  top: 80%;
  font-size: 18px;
  color: var(--p5-red);
  transform: rotate(-4deg);
}

/* 「别停！」心率线末端（B2：换黄油体；F2：上下轻浮动） */
.fun-stop {
  right: 16%;
  bottom: 24%;
  font-family: var(--font-cn-display);
  font-size: 16px;
  color: var(--p5-white);
  transform: rotate(-6deg);
  animation: float-y 4.4s ease-in-out infinite;
  animation-delay: 1.3s;
}

/* 「7 DAYS」数据条上方（G1：改白色保证可读） */
.fun-days {
  left: 4%;
  bottom: calc(8% + 64px);
  font-size: 16px;
  color: var(--p5-white);
  transform: rotate(-3deg);
}

/* 散落小椭圆点（F4：漂浮 ±5px + 透明度呼吸 0.6~1） */
.dot {
  position: absolute;
  z-index: 1;
  border-radius: 50%;
  pointer-events: none;
  animation: dot-float 3.5s ease-in-out infinite;
}

.dot.red {
  background: var(--p5-red);
}

.dot.white {
  background: var(--p5-white);
}

.dot.gray {
  background: var(--p5-gray);
}

@keyframes dot-float {
  0%,
  100% {
    translate: 0 0;
    opacity: 0.6;
  }
  50% {
    translate: 5px -5px;
    opacity: 1;
  }
}

/* F2：常驻轻浮动（translate 独立属性，不覆盖既有 transform 旋转）；幅度 ±5px */
@keyframes float-y {
  0%,
  100% {
    translate: 0 0;
  }
  50% {
    translate: 0 -5px;
  }
}

/* F3：浮动 + 轻微摇摆；修复 5：加入小幅抖动步进（±1px），节奏更慢更轻 */
@keyframes float-sway {
  0%,
  100% {
    translate: 0 0;
    rotate: 0deg;
  }
  18% {
    translate: 1px -2px;
    rotate: 1deg;
  }
  36% {
    translate: -1px -3px;
    rotate: 1.5deg;
  }
  54% {
    translate: 1px -1px;
    rotate: 0.5deg;
  }
  72% {
    translate: -1px -2px;
    rotate: -1deg;
  }
  86% {
    translate: 0 -3px;
    rotate: -1.5deg;
  }
}

/* 鞋印 ×8 */
.footprint {
  position: absolute;
  z-index: 1;
  pointer-events: none;
}

.footprint.white {
  color: var(--p5-white);
}

.footprint.red {
  color: var(--p5-red);
}

/* 心率线：宽 55%，bottom 20%，rotate(-8°) 穿过走廊
   F5：波形横向循环滚动（10s 一屏）+ 红点 animateMotion 沿波形跑动 */
.heartbeat {
  position: absolute;
  left: 24%;
  bottom: 20%;
  width: 55%;
  z-index: 1;
  transform: rotate(-8deg);
  pointer-events: none;
}

.hb-scroll {
  animation: hb-scroll 10s linear infinite;
}

@keyframes hb-scroll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-600px);
  }
}

/* 里程牌 ×3：70×70 黑底白描边，Anton，右下红角标（修复 5：缓慢轻微抖动，节奏错开） */
.milestone {
  position: absolute;
  z-index: 1;
  width: 70px;
  height: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--p5-black);
  border: 2px solid var(--p5-white);
  pointer-events: none;
  animation: float-sway 6.5s ease-in-out infinite;
}

.milestone:nth-of-type(1) {
  animation-delay: 0.5s;
}

.milestone:nth-of-type(2) {
  animation-duration: 7.4s;
  animation-delay: 1.6s;
}

.milestone:nth-of-type(3) {
  animation-duration: 6.9s;
  animation-delay: 2.7s;
}

.ms-km {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.15em;
  color: var(--p5-text-dim);
}

.ms-num {
  font-size: 26px;
  line-height: 1;
  color: var(--p5-white);
}

.ms-flag {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 0 16px 16px;
  border-color: transparent transparent var(--p5-red) transparent;
}

/* 号码布 ×1：白底黑字 42，90×110，顶部两别针，rotate(-9°) 轻压卡顶（F3 浮动摇摆） */
.bib {
  position: absolute;
  left: 330px;
  top: 150px;
  width: 90px;
  height: 110px;
  z-index: 1;
  background: var(--p5-white);
  transform: rotate(-9deg);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  animation: float-sway 5.4s ease-in-out infinite;
  animation-delay: 0.9s;
}

.bib-pin {
  position: absolute;
  top: 8px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--p5-black);
}

.bib-pin--l {
  left: 14px;
}

.bib-pin--r {
  right: 14px;
}

.bib-num {
  font-size: 48px;
  color: var(--p5-black);
}

/* 秒表 ×1：红描边 80px，右上菜单上方，rotate(12deg) 全页唯一正角
   （C3：Hero 全出血后自页面顶开始，下移避开 fixed 导航） */
.stopwatch {
  position: absolute;
  right: 13%;
  top: 10%;
  width: 80px;
  z-index: 1;
  transform: rotate(12deg);
  pointer-events: none;
}

/* 星星 ×2：左下 / 右下黑色区域 */
.deco-star {
  position: absolute;
  z-index: 1;
  pointer-events: none;
}

.deco-star--lb {
  left: 3%;
  bottom: 32%;
}

.deco-star--rb {
  right: 3%;
  bottom: 30%;
}

/* ===== 第 4 部分 z2：人物 ===== */
.runner {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  height: 88%;
  width: auto;
  z-index: 2;
  user-select: none;
  pointer-events: none;
}

/* ===== 第 9 部分 z2：底部飘带 ===== */
.ribbon {
  position: absolute;
  left: -10%;
  width: 120%;
  bottom: -4px;
  height: 36px;
  z-index: 2;
  background: var(--p5-white);
  overflow: hidden;
  transform: rotate(-2.5deg);
  pointer-events: none;
}

.ribbon-track {
  display: flex;
  width: max-content;
  /* C2：4 份 seg，平移 -25% 恰好一份 seg 宽，任何时刻屏内都有 ≥1 份完整内容 */
  animation: ribbon-scroll 25s linear infinite;
}

.ribbon-seg {
  display: block;
  white-space: nowrap;
  line-height: 36px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 6px;
  color: var(--p5-black);
}

@keyframes ribbon-scroll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-25%);
  }
}

/* =====================================================================
   第 5 部分 z3：左侧功能区
   ===================================================================== */

/* 5.1 副标题：导航栏下方 32px（Hero 全出血自页面顶开始，需避让 fixed 导航） */
.sub-kicker {
  position: absolute;
  left: 4%;
  top: calc(var(--nav-h) + 32px);
  z-index: 3;
  max-width: 420px;
  padding-left: 14px;
  border-left: 4px solid var(--p5-white);
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--p5-white);
  transform: rotate(-2deg);
  white-space: nowrap; /* 横排一行（C3：禁止竖排折行） */
}

/* 5.2 今日打卡大卡（A1 重构）：ck-wrap 承接定位与旋转，标签为卡外兄弟元素 */
.ck-wrap {
  position: absolute;
  left: 4%;
  top: 300px;
  z-index: 3;
  width: 380px;
  transform: rotate(-2deg);
}

/* 卡片：纯黑底 + 白 2px 描边 + 双角斜切 + 右下红三角 */
.checkin-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px 28px;
  background: var(--p5-black);
  border: 2px solid var(--p5-white);
  clip-path: polygon(
    12px 0,
    100% 0,
    100% calc(100% - 12px),
    calc(100% - 12px) 100%,
    0 100%,
    0 12px
  );
}

/* CHECK-IN 红标签（A1：卡外层级，骑压上边框约一半在外，边框线不得穿过标签） */
.ck-tag {
  position: absolute;
  top: -15px;
  left: 26px;
  z-index: 2; /* 高于卡片（含边框线） */
  padding: 4px 12px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.12em;
  line-height: 1.2;
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(4px 0, 100% 0, calc(100% - 4px) 100%, 0 100%);
}

/* A3：中文标题换站酷庆科黄油体 */
.ck-title {
  font-family: var(--font-cn-display);
  font-size: 28px;
  font-weight: 400; /* 黄油体自带粗度，叠加 900 会假粗 */
  letter-spacing: 0.04em;
  color: var(--p5-white);
}

.ck-today {
  /* 字体统一为「去打卡」同款黄油体并增大字号（可读性优化） */
  font-family: var(--font-cn-display);
  font-size: 19px;
  font-weight: 400; /* 黄油体自带粗度，叠加 700 会假粗 */
  letter-spacing: 0.04em;
  color: var(--p5-text-dim);
}

.ck-today--done {
  color: var(--p5-cream);
}

.ck-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ck-progress-meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  /* 修复 4：「本周…」文本与「去打卡」按钮统一黄油体 */
  font-family: var(--font-cn-display);
  font-size: 14px;
  letter-spacing: 0.06em;
  color: var(--p5-text-dim);
}

.ck-progress-meta .p5-num {
  font-size: 18px;
  color: var(--p5-red);
}

.ck-progress-track {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  clip-path: polygon(3px 0, 100% 0, calc(100% - 3px) 100%, 0 100%);
}

.ck-progress-fill {
  height: 100%;
  background: var(--p5-red);
  clip-path: polygon(3px 0, 100% 0, calc(100% - 3px) 100%, 0 100%);
  transition: width 0.4s ease-out;
}

/* 「去打卡」全页唯一一级按钮（修复 4：标准按钮字号 22px，黄油体字距 6px；
   可读性优化：字号提升至 28px，高度同步 64px） */
.ck-btn {
  position: relative;
  z-index: 1; /* 高于红三角、错位影、斜切角等装饰 */
  height: 64px;
  margin-top: 4px;
  font-family: var(--font-cn-display);
  font-size: 28px;
  font-weight: 400;
  letter-spacing: 10px;
  text-indent: 10px; /* 抵消末字符字距，保证视觉居中 */
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(12px 0, 100% 0, calc(100% - 12px) 100%, 0 100%);
  cursor: pointer;
  transition: background 0.15s;
}

.ck-btn:hover {
  background: var(--p5-red-dark);
}

.ck-flag {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 0 22px 22px;
  border-color: transparent transparent var(--p5-red) transparent;
}

/* 5.3 数据条：红底白字白描边，两端斜切，rotate(-5°)
   可读性优化：字体统一为「去打卡」同款黄油体，宽度 46% 防止增大后裁字 */
.data-strip {
  position: absolute;
  left: 2%;
  bottom: 8%;
  width: 46%;
  height: 52px;
  z-index: 3;
  background: var(--p5-red);
  border: 2px solid var(--p5-white);
  color: var(--p5-white);
  font-family: var(--font-cn-display);
  font-size: 20px;
  font-weight: 400; /* 黄油体自带粗度，叠加 700 会假粗 */
  letter-spacing: 2px;
  text-align: center;
  white-space: nowrap;
  clip-path: polygon(3% 0, 100% 0, 97% 100%, 0 100%);
  transform: rotate(-5deg);
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.data-strip:hover {
  background: var(--p5-red-dark);
}

/* D：文字组每 8s 重新切入一次（0~0.6s 左侧带 skew 斜切入场，之后静止；
   底色红条静止，只有文字动；内容不变仅重播） */
.ds-text {
  display: inline-block;
  /* padding 避开 clip-path 两端 3% 斜切区，修复「本」「次」被裁 */
  padding: 0 26px;
  font-size: 20px;
  letter-spacing: 2px;
  animation: ds-cut 8s cubic-bezier(0.2, 0.9, 0.3, 1) infinite;
}

@keyframes ds-cut {
  0% {
    transform: translateX(-48px) skewX(-16deg);
    opacity: 0;
  }
  7.5% {
    /* 0.6s / 8s = 7.5% */
    transform: translateX(0) skewX(0deg);
    opacity: 1;
  }
  100% {
    transform: translateX(0) skewX(0deg);
    opacity: 1;
  }
}

/* =====================================================================
   第 6 部分 z3：右侧纯文字菜单（无容器 / 无边框 / 无底板）
   ===================================================================== */
.menu-item {
  position: absolute;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  cursor: pointer;
  pointer-events: auto;
  --m-base: 34px;
}

/* ① SmArT Q&A：right 6% top 16% rotate(-7°) */
.m-1 {
  right: 6%;
  top: 16%;
}

/* ② TrAiN PlAn：right 11% top 38% rotate(-11°) 字号 40px（最大） */
.m-2 {
  right: 11%;
  top: 38%;
}

/* ③ InJuRy CaRe：right 5% top 60% rotate(-6°) */
.m-3 {
  right: 5%;
  top: 60%;
}

/* 编号前缀：Anton 18px 红色，主字上方 */
.m-no {
  font-size: 18px;
  letter-spacing: 0.1em;
  color: var(--p5-red);
  margin-bottom: 2px;
}

/* 主字：Anton 基准字号（② 40px），字母拼贴（F2：上下轻浮动，周期错开） */
.m-word {
  display: flex;
  align-items: baseline;
  font-family: var(--font-display);
  font-size: var(--m-base);
  line-height: 1.05;
  animation: float-y 3.6s ease-in-out infinite;
}

.m-1 .m-word {
  animation-duration: 3.6s;
  animation-delay: 0.2s;
}

.m-2 .m-word {
  animation-duration: 4.5s;
  animation-delay: 1.4s;
}

.m-3 .m-word {
  animation-duration: 3.9s;
  animation-delay: 2.6s;
}

.m-gap {
  width: 0.35em;
}

.mlt {
  display: inline-block;
}

/* 三分法字母样式 */
/* 1/3 反白贴片：黑底白字小块（撕纸贴片，白描边保证黑底上可见） */
.mlt--invert {
  padding: 0 0.06em;
  color: var(--p5-white);
  background: var(--p5-black);
  border: 1.5px solid var(--p5-white);
}

/* 1/3 白字 + 红 3px 实心错位影 */
.mlt--wr {
  color: var(--p5-white);
  text-shadow: 3px 3px 0 var(--p5-red);
}

/* 1/3 红字 + 黑 3px 实心错位影 */
.mlt--rb {
  color: var(--p5-red);
  text-shadow: 3px 3px 0 var(--p5-black);
}

/* 中文小注（B2：站酷黄油体）：主字左下 8px，不参与浮动（F7 功能文字静止） */
.m-note {
  margin-top: 8px;
  font-family: var(--font-cn-display);
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.7);
}

/* hover 红斜杠下划线（F6：0.25s 快速划出，悬停期间保持） */
.m-underline {
  margin-top: 6px;
  width: 100%;
  height: 4px;
  background: var(--p5-red);
  transform: rotate(-3deg) scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s ease-out;
}

.menu-item:hover .m-underline {
  transform: rotate(-3deg) scaleX(1);
}

/* hover：字母抖动一次（交错延迟，各自 rotate/位移） */
.menu-item:hover .mlt {
  animation: mlt-jitter 0.3s ease-in-out;
}

.menu-item:hover .mlt:nth-child(2n) {
  animation-delay: 0.05s;
}

.menu-item:hover .mlt:nth-child(3n) {
  animation-delay: 0.1s;
}

@keyframes mlt-jitter {
  0% {
    translate: 0 0;
  }
  30% {
    translate: 1px -1px;
    rotate: 3deg;
  }
  60% {
    translate: -1px 1px;
    rotate: -3deg;
  }
  100% {
    translate: 0 0;
  }
}

/* =====================================================================
   第 7 部分 z4：RuN wIsE 签名位（右下）
   ===================================================================== */
.hero-sign {
  position: absolute;
  right: 5%;
  bottom: 10%;
  z-index: 4;
  transform: rotate(-3deg);
}

.ransom {
  display: flex;
  align-items: baseline;
  font-family: var(--font-display);
  font-size: clamp(64px, 9vw, 110px);
  line-height: 1;
  width: max-content;
  animation: float-y 4.8s ease-in-out infinite;
  animation-delay: 0.8s; /* 与菜单错拍（F9） */
}

.sign-sub {
  margin-top: 12px;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.25em;
  color: rgba(255, 255, 255, 0.6);
  text-align: right;
}

/* ===== 响应式 <768px（第 11 部分） ===== */
@media (max-width: 768px) {
  .hero {
    height: auto;
    min-height: 640px;
    display: flex;
    flex-direction: column;
    padding-top: 72px;
  }

  /* 色带角度不变，宽度按比例缩放 */
  .band-red-a {
    width: 560px;
    height: 240px;
  }

  .band-red-b {
    width: 640px;
    height: 300px;
  }

  .band-white-1 {
    width: 480px;
  }

  .band-white-2 {
    width: 520px;
  }

  .band-ht-black {
    display: none;
  }

  /* 人物 55% 高，left 55%（残影出血） */
  .runner {
    height: 55%;
    left: 55%;
    transform: none;
  }

  /* 装饰减法：鞋印留 5 个，秒表/号码布/里程牌隐藏 */
  .fp--tail,
  .stopwatch,
  .bib,
  .milestone {
    display: none;
  }

  .fun-run {
    left: 20%;
    top: auto;
    bottom: 34%;
  }

  .fun-stop {
    display: none;
  }

  .heartbeat {
    left: 10%;
    width: 70%;
    bottom: 30%;
  }

  .deco-star--lb {
    bottom: 42%;
  }

  .deco-star--rb {
    bottom: 40%;
  }

  /* 左侧功能区转文档流 */
  .sub-kicker {
    position: static;
    margin: 0 4% 24px;
  }

  /* 大卡 92% 居中（ck-wrap 承接定位） */
  .ck-wrap {
    position: static;
    width: 92%;
    margin: 0 auto 28px;
  }

  .checkin-card {
    width: 100%;
  }

  /* 数据条 95% */
  .fun-days {
    position: static;
    margin: 0 0 6px 5%;
    bottom: auto;
  }

  .data-strip {
    position: static;
    width: 95%;
    margin: 0 auto 32px;
    font-size: 15px;
    letter-spacing: 1px;
    height: 44px;
  }

  /* 菜单纵向堆叠人物下方，字号 24px */
  .menu-item {
    position: static;
    margin: 0 5% 22px;
    --m-base: 24px;
  }

  /* 标题 48px 底部居右 */
  .hero-sign {
    position: static;
    margin: 8% 5% 72px auto;
    width: max-content;
  }

  .ransom {
    font-size: 48px;
  }

  .ribbon {
    height: 28px;
  }

  .ribbon-track {
    line-height: 28px;
    font-size: 11px;
    letter-spacing: 4px;
  }
}
</style>
