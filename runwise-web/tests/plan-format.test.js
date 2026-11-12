import test from 'node:test'
import assert from 'node:assert/strict'
import {planMarkdown} from '../../shared/plan-format.mjs'
test('copying a plan includes its full schedule and real context without breaking table cells',()=>{
  const text=planMarkdown({trainingContext:{periodStart:'2026-10-01',asOf:'2026-10-28',recordCount:8},plan:{explanation:'## 说明',days:[{date:'2026-10-29',type:'easy',targetKm:3,note:'轻松|恢复\n记录体感'}]}})
  assert.match(text,/2026-10-01—2026-10-28/);assert.match(text,/2026-10-29/);assert.match(text,/轻松\\\|恢复 记录体感/)
})
