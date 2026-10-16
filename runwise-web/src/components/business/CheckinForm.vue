<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useCheckinStore } from '../../stores/checkin'
import { formatDate, formatDateCn, formatPace } from '../../composables/useFormatDate'

const emit = defineEmits(['editing-change'])

// 打卡表单（工程计划 4.4）：
// - 选中日期已有记录 → 只读状态卡，展示当日完整记录（距离 / 时长 / 配速 / 心情 / 备注）
// - 选中日期无记录（今天或过去）→ 可编辑表单，提交后红点与详情即时刷新
// - 打卡日期 = 当前选中日期（默认今天）；未来日期在日历层已禁选
const store = useCheckinStore()

const MOODS = [
  { value: 'great', label: '轻松' },
  { value: 'good', label: '适中' },
  { value: 'tired', label: '吃力' },
]
const moodLabel = (value) => MOODS.find((m) => m.value === value)?.label || value

const todayKey = formatDate(new Date())
const isToday = computed(() => store.selectedDate === todayKey)
const record = computed(() => store.selectedRecord)

const dateCn = computed(() => {
  const [y, m, d] = store.selectedDate.split('-').map(Number)
  return formatDateCn(new Date(y, m - 1, d))
})

const form = reactive({ distanceKm: '', durationMin: '', mood: 'good', note: '', imageUrl: '' })
const errorMsg = ref('')
const editing = ref(false)
const imageUploading = ref(false)
const imageInput = ref(null)

// 配速随输入实时预览（工程计划 4.4）
const pacePreview = computed(() => formatPace(form.distanceKm, form.durationMin))

// 切换选中日期 / 记录被更新 → 表单回填或重置
function fillForm(recordValue = store.selectedRecord) {
  const r = recordValue
  form.distanceKm = r ? String(r.distanceKm) : ''
  form.durationMin = r ? String(r.durationMin) : ''
  form.mood = r ? r.mood : 'good'
  form.note = r ? r.note || '' : ''
  form.imageUrl = r ? r.imageUrl || '' : ''
  errorMsg.value = ''
}

watch(
  () => store.selectedDate,
  () => {
    editing.value = false
    emit('editing-change', false)
    fillForm()
  },
  { immediate: true }
)

watch(
  () => store.selectedRecord,
  (nextRecord) => {
    if (!editing.value) fillForm(nextRecord)
  },
)

function startEdit() {
  fillForm(record.value)
  editing.value = true
  emit('editing-change', true)
}

function cancelEdit() {
  editing.value = false
  emit('editing-change', false)
  fillForm(record.value)
}

function chooseImage() {
  if (!imageUploading.value) imageInput.value?.click()
}

async function handleImageChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    errorMsg.value = '仅支持 JPG、PNG 或 WebP 图片'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errorMsg.value = '图片不能超过 5MB'
    return
  }
  imageUploading.value = true
  errorMsg.value = ''
  try {
    form.imageUrl = await store.uploadImage(file)
  } catch (error) {
    errorMsg.value = error?.message || '图片上传失败，请稍后重试'
  } finally {
    imageUploading.value = false
  }
}

async function handleSubmit() {
  if (store.submitting) return
  const distanceKm = Number(form.distanceKm)
  const durationMin = Number(form.durationMin)
  if (!distanceKm || distanceKm <= 0) {
    errorMsg.value = '请填写有效的跑步距离'
    return
  }
  if (!durationMin || durationMin <= 0) {
    errorMsg.value = '请填写有效的跑步时长'
    return
  }
  errorMsg.value = ''
  try {
    const payload = {
      date: store.selectedDate,
      distanceKm,
      durationMin,
      mood: form.mood,
      note: form.note,
      imageUrl: form.imageUrl,
    }
    if (editing.value && record.value) {
      await store.update(record.value.id, payload)
      editing.value = false
      emit('editing-change', false)
    } else {
      await store.submit(payload)
    }
  } catch {
    errorMsg.value = editing.value ? '保存失败，请稍后重试' : '提交失败，请稍后重试'
  }
}
</script>

<template>
  <div class="ck-form">
    <!-- 只读状态卡：选中日期已有记录 -->
    <template v-if="record && !editing">
      <p class="ck-kicker">{{ isToday ? 'TODAY · 已完成打卡' : 'RECORD · 当日记录' }}</p>
      <h2 class="ck-date">{{ dateCn }}</h2>

      <div class="ck-grid">
        <div class="ck-item">
          <span class="ck-label">距离</span>
          <span class="ck-value">{{ record.distanceKm }}<i>km</i></span>
        </div>
        <div class="ck-item">
          <span class="ck-label">时长</span>
          <span class="ck-value">{{ record.durationMin }}<i>min</i></span>
        </div>
        <div class="ck-item">
          <span class="ck-label">配速</span>
          <span class="ck-value">{{ record.pace }}</span>
        </div>
        <div class="ck-item">
          <span class="ck-label">体感</span>
          <span class="ck-value ck-value--text">{{ moodLabel(record.mood) }}</span>
        </div>
      </div>

      <div v-if="record.imageUrl" class="ck-photo ck-photo--detail">
        <img :src="record.imageUrl" alt="本次训练照片" />
      </div>

      <div class="ck-note">
        <span class="ck-label">备注</span>
        <p>{{ record.note || '无' }}</p>
      </div>

      <button class="ck-submit" type="button" @click="startEdit">
        编辑记录
      </button>
    </template>

    <!-- B2 空态：过去日期无记录（补录入口已收拢，今天无记录仍走下方表单） -->
    <template v-else-if="!isToday && !editing">
      <p class="ck-kicker">EMPTY · 暂无记录</p>
      <h2 class="ck-date">{{ dateCn }}</h2>
      <div class="ck-empty">
        <p class="ck-empty-text">
          这一天还没有记录 ·
          <RouterLink to="/" class="ck-empty-link">去首页打卡 →</RouterLink>
        </p>
      </div>
    </template>

    <!-- 可编辑表单：今天无记录（打卡主链路：首页「去打卡」→ 本页） -->
    <template v-else>
      <p class="ck-kicker">{{ editing ? 'EDIT · 编辑训练' : 'TODAY · 待打卡' }}</p>
      <h2 class="ck-date">{{ editing ? `${dateCn} · 编辑记录` : '今日打卡' }}</h2>

      <div class="field-row">
        <label class="field">
          <span class="field-label">距离（km）</span>
          <input
            v-model.trim="form.distanceKm"
            type="number"
            min="0"
            step="0.1"
            placeholder="如 5.2"
          />
        </label>
        <label class="field">
          <span class="field-label">时长（分钟）</span>
          <input
            v-model.trim="form.durationMin"
            type="number"
            min="0"
            step="1"
            placeholder="如 32"
          />
        </label>
      </div>

      <div class="field">
        <span class="field-label">体感</span>
        <div class="mood-chips">
          <button
            v-for="m in MOODS"
            :key="m.value"
            type="button"
            class="mood-chip"
            :class="{ 'is-active': form.mood === m.value }"
            @click="form.mood = m.value"
          >
            {{ m.label }}
          </button>
        </div>
      </div>

      <label class="field">
        <span class="field-label">备注（可选）</span>
        <textarea
          v-model="form.note"
          rows="2"
          maxlength="100"
          placeholder="一句话记录今天的跑步"
        ></textarea>
      </label>

      <div class="field">
        <span class="field-label">训练照片（可选）</span>
        <input
          ref="imageInput"
          class="ck-file-input"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="handleImageChange"
        />
        <div v-if="form.imageUrl" class="ck-photo">
          <img :src="form.imageUrl" alt="训练照片预览" />
          <div class="ck-photo-actions">
            <button type="button" @click="chooseImage">更换图片</button>
            <button type="button" class="is-danger" @click="form.imageUrl = ''">删除图片</button>
          </div>
        </div>
        <button v-else class="ck-upload" type="button" :disabled="imageUploading" @click="chooseImage">
          {{ imageUploading ? '上传中…' : '+ 上传训练照片' }}
          <small>JPG / PNG / WebP · 最大 5MB</small>
        </button>
      </div>

      <p class="pace-preview">
        预计配速 <strong>{{ pacePreview || '--' }}</strong>
      </p>

      <p v-if="errorMsg" class="ck-error">{{ errorMsg }}</p>

      <button
        class="ck-submit"
        type="button"
        :disabled="store.submitting || imageUploading"
        @click="handleSubmit"
      >
        {{ store.submitting ? '保存中…' : editing ? '保存修改' : '提交打卡' }}
      </button>
      <button v-if="editing" class="ck-cancel" type="button" @click="cancelEdit">取消</button>
    </template>
  </div>
</template>

<style scoped>
.ck-form {
  padding: 22px var(--sp-6) var(--sp-5);
  display: flex;
  flex-direction: column;
  /* 打卡页双栏等高拉伸：表单卡随日历卡高度填满（空白压缩） */
  height: 100%;
}

.ck-kicker {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--p5-red);
  margin-bottom: var(--sp-2);
}

.ck-date {
  font-size: var(--fs-h3);
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--p5-white);
  margin-bottom: 18px;
}

/* ===== 只读记录卡 ===== */
.ck-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.ck-item {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  padding: 11px 13px;
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  /* B4：红色左边框 2px——与训练计划 TODAY 行同款语言 */
  border-left: 2px solid var(--p5-red);
}

/* ===== B2 空态（过去日期无记录）===== */
.ck-empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: var(--sp-6) 0;
}

.ck-empty-text {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  letter-spacing: 0.04em;
}

.ck-empty-link {
  color: var(--p5-red);
  font-weight: 700;
  transition: color 0.2s;
}

.ck-empty-link:hover {
  color: var(--p5-red-dark);
  text-decoration: underline;
}

.ck-label {
  font-size: var(--fs-caption);
  color: var(--p5-text-dim);
  letter-spacing: 0.05em;
}

.ck-value {
  /* 展示字体 + 等宽数字（UI 精修 P1-3），中文单位 / 备注保持正文字体 */
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.2;
  color: var(--p5-white);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

/* 心情为中文文本：恢复正文字体，仅保留尺寸与颜色 */
.ck-value--text {
  font-family: var(--font-body);
  font-size: var(--fs-h3);
  font-weight: 700;
  letter-spacing: 0;
}

.ck-value i {
  font-style: normal;
  font-size: var(--fs-caption);
  font-weight: 400;
  color: var(--p5-text-dim);
  margin-left: 4px;
}

.ck-note {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, .12);
}

.ck-note p {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  line-height: 1.7;
}

/* ===== 表单 ===== */
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  margin-bottom: var(--sp-4);
}

.field-label {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  letter-spacing: 0.05em;
}

.field input,
.field textarea {
  padding: var(--sp-3) var(--sp-4);
  background: var(--p5-black);
  border: 1px solid var(--p5-line);
  color: var(--p5-white);
  font-size: var(--fs-body);
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus,
.field textarea:focus {
  border-color: var(--p5-red);
}

.field input::placeholder,
.field textarea::placeholder {
  color: #55555c;
}

.field textarea {
  resize: vertical;
}

.mood-chips {
  display: flex;
  gap: var(--sp-3);
}

.mood-chip {
  padding: var(--sp-2) var(--sp-4);
  /* 趣味点缀 3/3：ZCOOL KuaiLe 心情贴纸 */
  font-family: var(--font-fun);
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  border: 1px solid var(--p5-line);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
  transition: color 0.2s, border-color 0.2s, background-color 0.2s;
}

.mood-chip:hover {
  color: var(--p5-white);
  border-color: var(--p5-red);
}

.mood-chip.is-active {
  color: var(--p5-white);
  background: var(--p5-red);
  border-color: var(--p5-red);
  font-weight: 700;
}

.pace-preview {
  font-size: var(--fs-sub);
  color: var(--p5-text-dim);
  margin-bottom: var(--sp-4);
}

.pace-preview strong {
  color: var(--p5-white);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.03em;
}

.ck-error {
  font-size: var(--fs-sub);
  color: var(--p5-red);
  margin-bottom: var(--sp-3);
}

.ck-submit {
  margin-top: var(--sp-2);
  padding: var(--sp-3) var(--sp-4);
  background: var(--p5-red);
  color: var(--p5-white);
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.15em;
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition: background-color 0.2s;
}

.ck-submit:hover:not(:disabled) {
  background: var(--p5-red-dark);
}

.ck-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ck-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.ck-upload {
  min-height: 82px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--p5-white);
  border: 1px dashed rgba(255, 255, 255, .42);
  background: rgba(0, 0, 0, .28);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
  transition: border-color .2s, background-color .2s;
}

.ck-upload:hover:not(:disabled) {
  border-color: var(--p5-red);
  background: rgba(230, 0, 18, .08);
}

.ck-upload small {
  color: var(--p5-text-dim);
  font-size: 11px;
}

.ck-photo {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, .25);
  background: #09090a;
}

.ck-photo--detail {
  margin-bottom: 16px;
  display: block;
}

.ck-photo img {
  display: block;
  width: 100%;
  max-height: 150px;
  object-fit: cover;
}

.ck-photo-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ck-photo-actions button,
.ck-cancel {
  padding: 8px 12px;
  color: var(--p5-white);
  border: 1px solid rgba(255, 255, 255, .28);
  font-size: var(--fs-caption);
}

.ck-photo-actions button:hover,
.ck-cancel:hover {
  border-color: var(--p5-red);
}

.ck-photo-actions .is-danger {
  color: var(--p5-red);
}

.ck-cancel {
  margin-top: 10px;
  align-self: center;
  border: 0;
  color: var(--p5-text-dim);
}
</style>
