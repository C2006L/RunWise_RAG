<script setup>
// 计划编辑弹窗（Phase B3.2，P5 风：黑底 / 白描边 / 右下斜切角 / 红角标）
// 三态：add（新增）/ edit（编辑）/ confirm（删除二次确认）
// 字段约束：星期固定周一~周日；类型仅五个固定值（constants 表），禁止自由文本
import { computed, reactive, watch } from 'vue'
import {
  TRAINING_TYPE_OPTIONS,
  WEEKDAY_OPTIONS,
  resolveTrainingType,
} from '../../constants/trainingTypes'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'add' }, // 'add' | 'edit' | 'confirm'
  weekNo: { type: Number, default: 1 },
  initial: { type: Object, default: null },
})

const emit = defineEmits(['close', 'submit'])

const form = reactive({
  weekday: '周一',
  type: 'easy',
  targetKm: 5,
  note: '',
})

const isConfirm = computed(() => props.mode === 'confirm')

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    if (props.mode === 'edit' && props.initial) {
      form.weekday = props.initial.weekday
      form.type = props.initial.type
      form.targetKm = props.initial.targetKm || 0
      form.note = props.initial.note || ''
    } else if (!isConfirm.value) {
      form.weekday = '周一'
      form.type = 'easy'
      form.targetKm = 5
      form.note = ''
    }
  },
)

const isRest = computed(() => form.type === 'rest')
const typeLabel = computed(() => resolveTrainingType(form.type).label)

function onSubmit() {
  if (isConfirm.value) {
    emit('submit', null)
    return
  }
  emit('submit', {
    weekday: form.weekday,
    weekdayIndex: WEEKDAY_OPTIONS.indexOf(form.weekday),
    type: form.type,
    targetKm: isRest.value ? 0 : Number(form.targetKm) || 0,
    note: form.note,
  })
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="pm-mask" @click.self="emit('close')">
      <div class="pm-dialog" role="dialog" aria-modal="true">
        <span class="pm-flag" aria-hidden="true"></span>
        <p class="pm-kicker p5-num">
          {{ isConfirm ? 'CONFIRM' : mode === 'edit' ? 'EDIT PLAN' : 'NEW PLAN' }}
        </p>
        <h3 class="pm-title">
          {{ isConfirm ? '删除训练' : mode === 'edit' ? '编辑训练' : '添加训练' }}
          <span class="pm-week p5-num">W{{ weekNo }}</span>
        </h3>

        <!-- 删除二次确认视图 -->
        <template v-if="isConfirm">
          <p class="pm-confirm-text">
            确认删除「{{ initial?.weekday }} · {{ initial?.label }}」的训练安排？<br />
            删除后该日将恢复为休息日。
          </p>
        </template>

        <!-- 新增 / 编辑表单 -->
        <template v-else>
          <div class="pm-field">
            <label class="pm-label">星期</label>
            <select v-model="form.weekday" class="pm-input">
              <option v-for="w in WEEKDAY_OPTIONS" :key="w" :value="w">{{ w }}</option>
            </select>
          </div>

          <div class="pm-field">
            <label class="pm-label">类型</label>
            <select v-model="form.type" class="pm-input">
              <option v-for="t in TRAINING_TYPE_OPTIONS" :key="t.value" :value="t.value">
                {{ t.label }}
              </option>
            </select>
          </div>

          <div v-if="!isRest" class="pm-field">
            <label class="pm-label">目标距离（km）</label>
            <input
              v-model.number="form.targetKm"
              class="pm-input"
              type="number"
              min="0"
              max="60"
              step="0.5"
            />
          </div>

          <div class="pm-field">
            <label class="pm-label">备注（可空）</label>
            <input
              v-model="form.note"
              class="pm-input"
              type="text"
              maxlength="40"
              placeholder="选填，如：配合力量课"
            />
          </div>
        </template>

        <div class="pm-actions">
          <button class="pm-btn pm-btn--ghost" type="button" @click="emit('close')">取消</button>
          <button class="pm-btn pm-btn--red" type="button" @click="onSubmit">
            {{
              isConfirm
                ? '确认删除'
                : isRest
                  ? `设为休息（${form.weekday}）`
                  : `保存（${typeLabel} · ${form.weekday}）`
            }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.pm-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
}

/* 黑底 + 白描边 + 右下斜切角 */
.pm-dialog {
  position: relative;
  width: min(420px, 92vw);
  padding: var(--sp-6);
  background: var(--p5-black);
  border: 2px solid var(--p5-white);
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 16px), calc(100% - 16px) 100%, 0 100%);
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

/* 红角标（右上斜切小三角） */
.pm-flag {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-top: 22px solid var(--p5-red);
  border-left: 22px solid transparent;
}

.pm-kicker {
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--p5-red);
}

.pm-title {
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
  font-size: var(--fs-h2);
  font-weight: 900;
  color: var(--p5-white);
}

.pm-week {
  font-size: 14px;
  color: var(--p5-text-dim);
}

.pm-field {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.pm-label {
  font-size: var(--fs-caption);
  letter-spacing: 0.1em;
  color: var(--p5-text-dim);
}

/* 深底白字表单控件：直角、focus 红边（全站硬朗刀切风） */
.pm-input {
  height: 44px;
  padding: 0 var(--sp-3);
  font-size: var(--fs-body);
  color: var(--p5-white);
  background: var(--p5-panel);
  border: 1px solid var(--p5-line);
  border-radius: 0;
  outline: none;
  transition: border-color 0.15s;
}

.pm-input:focus {
  border-color: var(--p5-red);
}

select.pm-input {
  appearance: none;
}

.pm-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-3);
  margin-top: var(--sp-2);
}

.pm-confirm-text {
  font-size: var(--fs-body);
  line-height: 1.8;
  color: var(--p5-white);
}

.pm-btn {
  height: 44px;
  padding: 0 var(--sp-5);
  font-size: var(--fs-sub);
  font-weight: 700;
  letter-spacing: 0.08em;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.pm-btn--ghost {
  color: var(--p5-text-dim);
  background: transparent;
  border-color: var(--p5-line);
}

.pm-btn--ghost:hover {
  color: var(--p5-white);
  border-color: var(--p5-white);
}

/* 红色直角按钮 + hover 深红 */
.pm-btn--red {
  color: var(--p5-white);
  background: var(--p5-red);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
}

.pm-btn--red:hover {
  background: var(--p5-red-dark);
}
</style>
