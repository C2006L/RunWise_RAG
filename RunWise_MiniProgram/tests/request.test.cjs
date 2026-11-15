const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const vm=require('node:vm')
const ts=require('typescript')
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject}}
const ok=data=>({statusCode:200,data:{code:200,data}})
const failure=(status,message)=>({statusCode:status,data:{code:status,message}})
async function fixture(handler){
  const guard=await import('../../shared/session-guard.mjs')
  const storage=new Map([['accessToken','old-access'],['refreshToken','old-refresh'],['userId',1]])
  const navigations=[];let refreshes=0
  const uni={getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v),removeStorageSync:k=>storage.delete(k),
    request:options=>{if(options.url.endsWith('/auth/refresh'))refreshes++;return handler(options)},showToast:()=>{},navigateTo:o=>navigations.push(o)}
  const source=fs.readFileSync(require.resolve('../src/utils/request.ts'),'utf8').replaceAll('import.meta.env','({})')
  const javascript=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText
  const exports={};vm.runInNewContext(javascript,{exports,require:()=>guard,uni,Promise,Error},{filename:'request.ts'})
  return {api:exports,storage,navigations,refreshes:()=>refreshes}
}
test('refresh success followed by a business error preserves new tokens',async()=>{
  let calls=0;const f=await fixture(o=>Promise.resolve(o.url.endsWith('/auth/refresh')?ok({accessToken:'fresh',refreshToken:'fresh-r'}):++calls===1?failure(401,'expired'):failure(409,'计划冲突')))
  await assert.rejects(f.api.request('/api/plan/item','POST',{}),/计划冲突/)
  assert.equal(f.storage.get('accessToken'),'fresh');assert.equal(f.navigations.length,0)
})
test('refresh outage does not log the user out',async()=>{
  const f=await fixture(o=>Promise.resolve(o.url.endsWith('/auth/refresh')?failure(503,'稍后重试'):failure(401,'expired')))
  await assert.rejects(f.api.get('/api/user/profile'),/稍后重试/)
  assert.equal(f.storage.get('refreshToken'),'old-refresh');assert.equal(f.navigations.length,0)
})
test('genuine refresh rejection clears identity and navigates once',async()=>{
  const f=await fixture(()=>Promise.resolve(failure(401,'expired')))
  await assert.rejects(f.api.get('/api/user/profile'),/登录已过期/)
  assert.equal(f.storage.has('accessToken'),false);assert.equal(f.navigations.length,1)
})
test('refresh response cannot resurrect a logged-out session',async()=>{
  const pending=deferred(),started=deferred()
  const f=await fixture(o=>{if(o.url.endsWith('/auth/refresh')){started.resolve();return pending.promise}return Promise.resolve(failure(401,'expired'))})
  const request=f.api.get('/api/user/profile');await started.promise;f.api.clearTokens()
  pending.resolve(ok({accessToken:'stale',refreshToken:'stale-r'}))
  await assert.rejects(request,/登录状态已变化/);assert.equal(f.storage.has('accessToken'),false)
})
test('old refresh failure cannot clear a newly logged-in account',async()=>{
  const pending=deferred(),started=deferred()
  const f=await fixture(o=>{if(o.url.endsWith('/auth/refresh')){started.resolve();return pending.promise}return Promise.resolve(failure(401,'expired'))})
  const request=f.api.get('/api/user/profile');await started.promise
  f.api.saveTokens('account-b','account-b-r');f.storage.set('userId',2)
  pending.resolve(failure(401,'expired'))
  await assert.rejects(request,/登录状态已变化/);assert.equal(f.storage.get('accessToken'),'account-b');assert.equal(f.navigations.length,0)
})
test('concurrent unauthorized requests share one refresh',async()=>{
  const pending=deferred(),started=deferred()
  const f=await fixture(o=>{if(o.url.endsWith('/auth/refresh')){started.resolve();return pending.promise}return Promise.resolve(o.header.Authorization==='Bearer fresh'?ok('result'):failure(401,'expired'))})
  const a=f.api.get('/api/a'),b=f.api.get('/api/b');await started.promise
  pending.resolve(ok({accessToken:'fresh',refreshToken:'fresh-r'}))
  assert.deepEqual(await Promise.all([a,b]),['result','result']);assert.equal(f.refreshes(),1)
})
test('late data response cannot expose a previous account after switching',async()=>{
  const pending=deferred(),f=await fixture(()=>pending.promise)
  const request=f.api.get('/api/checkin/list');f.api.saveTokens('account-b','account-b-r')
  pending.resolve(ok({records:['account-a-private-record']}))
  await assert.rejects(request,/登录状态已变化/)
})
