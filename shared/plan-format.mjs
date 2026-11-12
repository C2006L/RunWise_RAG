const labels={easy:'轻松跑',tempo:'节奏跑',interval:'间歇跑',long:'长距离',rest:'休息'}
const cell=value=>String(value??'').replace(/\|/g,'\\|').replace(/[\r\n]+/g,' ')
export function planMarkdown(draft){
  const plan=draft.plan,context=draft.trainingContext
  const lines=[plan.explanation,'',`参考记录：${context.periodStart}—${context.asOf}，${context.recordCount} 次训练。`,'',
    '| 日期 | 训练 | 距离/km | 说明 |','| --- | --- | --- | --- |']
  for(const day of plan.days) lines.push(`| ${cell(day.date)} | ${cell(labels[day.type]||day.type)} | ${cell(day.targetKm)} | ${cell(day.note)} |`)
  lines.push('','以上为基于已记录训练的保守草稿，确认前请核对当前体感与时间安排。')
  return lines.join('\n')
}
