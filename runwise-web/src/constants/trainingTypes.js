// 训练类型映射表（Phase B1，单一数据源）：
// - 训练计划页【类型标签】渲染必须引用此文件，组件内禁止硬编码颜色
// - 将来数据库 type 字段值与此表键一致，颜色自动生效
// - 样式类 tt-* 定义于全局 p5-effects.css（Phase B 区块），任何组件可直接复用
export const TRAINING_TYPES = {
  interval: { label: '间歇跑', cls: 'tt-interval' },
  tempo: { label: '节奏跑', cls: 'tt-tempo' },
  long: { label: '长距离', cls: 'tt-long' },
  easy: { label: '轻松跑', cls: 'tt-easy' },
  rest: { label: '休息', cls: 'tt-rest' },
}

// 兜底：未知类型 → 灰描边灰字
export const TRAINING_TYPE_DEFAULT = { label: '训练', cls: 'tt-default' }

// 按类型取映射（含兜底），下拉选项等固定顺序场景用 TRAINING_TYPES 本体
export function resolveTrainingType(type) {
  return TRAINING_TYPES[type] || TRAINING_TYPE_DEFAULT
}

// 下拉框选项（仅五个固定值，禁止自由文本）
export const TRAINING_TYPE_OPTIONS = Object.entries(TRAINING_TYPES).map(
  ([value, t]) => ({ value, label: t.label }),
)

export const WEEKDAY_OPTIONS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
