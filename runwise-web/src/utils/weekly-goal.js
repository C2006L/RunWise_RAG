// Natural week shared with the plan page: Monday through Sunday, local dates.
export function currentPlanWeek(now = new Date()) {
  const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  monday.setDate(monday.getDate() - (monday.getDay() || 7) + 1)
  const sunday = new Date(monday)
  sunday.setDate(sunday.getDate() + 6)
  const key = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return { start: key(monday), end: key(sunday) }
}

export function summarizePlanDays(days = []) {
  const training = days.filter((day) => day.type !== 'rest')
  const totalKm = Math.round(training.reduce((sum, day) => {
    const value = Number(day.targetKm)
    return sum + (Number.isFinite(value) && value > 0 ? value : 0)
  }, 0) * 100) / 100
  return { totalKm, displayKm: totalKm.toFixed(1), trainingCount: training.length, restCount: days.length - training.length }
}

export function summarizeActualWeek(records = [], range) {
  const rows = records.filter((row) => row.date >= range.start && row.date <= range.end)
  return { totalKm: Math.round(rows.reduce((sum, row) => sum + Number(row.distanceKm || 0), 0) * 100) / 100,
    totalMinutes: rows.reduce((sum, row) => sum + Number(row.durationMin || 0), 0), checkinCount: rows.length }
}
