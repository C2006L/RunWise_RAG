import {test} from 'node:test'
import assert from 'node:assert/strict'
import {createSessionGuard} from '../../shared/session-guard.mjs'
test('old refresh finalizer cannot discard the next identity refresh task',async()=>{
  const guard=createSessionGuard();let completeOld,completeNew;let calls=0
  const old=guard.refresh(()=>new Promise(resolve=>{completeOld=resolve}));await Promise.resolve()
  const original=guard.current();guard.invalidate();assert.equal(guard.isCurrent(original),false)
  const fresh=guard.refresh(()=>{calls++;return new Promise(resolve=>{completeNew=resolve})});await Promise.resolve()
  completeOld();await old
  assert.equal(guard.refresh(()=>{throw Error('must share current task')}),fresh)
  completeNew();await fresh;assert.equal(calls,1)
})
