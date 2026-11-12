// 服务器返回的区间是权威快照；先移除区间旧记录，避免删除后残留红点。
export function mergeCheckinRange(records,list,startDate,endDate) {
  const next=Object.fromEntries(Object.entries(records).filter(([date])=>date<startDate||date>endDate))
  for(const record of list) next[record.date]=record
  return next
}

export function replaceCheckin(records,record) {
  const next=Object.fromEntries(Object.entries(records).filter(([,item])=>String(item.id)!==String(record.id)))
  next[record.date]=record
  return next
}
