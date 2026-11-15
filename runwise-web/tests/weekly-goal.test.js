import { test } from 'node:test'
import assert from 'node:assert/strict'
import { currentPlanWeek, summarizePlanDays, summarizeActualWeek } from '../src/utils/weekly-goal.js'

test('Sunday and year boundary use the same Monday-Sunday week as the plan', () => {
  assert.deepEqual(currentPlanWeek(new Date(2026, 10, 15)), { start: '2026-11-09', end: '2026-11-15' })
  assert.deepEqual(currentPlanWeek(new Date(2027, 0, 1)), { start: '2026-12-28', end: '2027-01-03' })
  assert.equal(currentPlanWeek(new Date(2026, 10, 16)).start, '2026-11-16')
})
test('home and plan totals accept backend numeric values, ignore rest, and have no fixed fallback', () => {
  const days = [{ type: 'easy', targetKm: '5.2' }, { type: 'long', targetKm: 10.3 }, { type: 'rest', targetKm: 0 }]
  assert.deepEqual(summarizePlanDays(days), { totalKm: 15.5, displayKm: '15.5', trainingCount: 2, restCount: 1 })
  assert.equal(summarizePlanDays([]).totalKm, 0)
  assert.equal(summarizePlanDays([{ type: 'easy', targetKm: NaN }]).totalKm, 0)
})
test('actual distance and pace inputs exclude previous week and use natural week boundaries', () => {
  const records = [{ date: '2026-11-08', distanceKm: 100, durationMin: 500 },
    { date: '2026-11-09', distanceKm: 5, durationMin: 30 }, { date: '2026-11-15', distanceKm: 10, durationMin: 50 }]
  assert.deepEqual(summarizeActualWeek(records, currentPlanWeek(new Date(2026, 10, 15))),
    { totalKm: 15, totalMinutes: 80, checkinCount: 2 })
})
