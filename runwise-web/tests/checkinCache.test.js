import test from 'node:test'
import assert from 'node:assert/strict'
import {mergeCheckinRange,replaceCheckin} from '../src/utils/checkinCache.js'

test('refresh removes deleted rows without dropping other months',()=>{
  const old={'2026-09-01':{id:1},'2026-10-01':{id:2}}
  const result=mergeCheckinRange(old,[],'2026-10-01','2026-10-31')
  assert.deepEqual(result,{'2026-09-01':{id:1}})
  assert.ok(old['2026-10-01'])
})
test('editing a record date does not leave its old date cached',()=>{
  const result=replaceCheckin({'2026-10-01':{id:2,date:'2026-10-01'}},{id:2,date:'2026-10-02'})
  assert.equal(result['2026-10-01'],undefined)
  assert.equal(Object.keys(result).length,1)
  assert.equal(result['2026-10-02'].id,2)
})
