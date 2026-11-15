import {test} from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import axios from 'axios'
import {createSessionGuard,staleSessionError} from '../../shared/session-guard.mjs'
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject}}
const result=(id=1)=>({userId:id,accessToken:`access-${id}`,refreshToken:`refresh-${id}`})
function fixture(){
  const sessionGuard=createSessionGuard(),memory=new Map(),routes=[]
  const authApi={login:async()=>result(),register:async()=>result(),refreshToken:async()=>result(),logout:async()=>{},getProfile:async()=>result()}
  const localStorage={getItem:k=>memory.get(k),setItem:(k,v)=>memory.set(k,v),removeItem:k=>memory.delete(k)}
  const context={sessionGuard,staleSessionError,authApi,localStorage,axios,Promise,
    useCheckinStore:()=>({$reset(){}}),
    defineStore:(_,o)=>{const s=o.state();for(const [k,f]of Object.entries(o.actions))s[k]=f.bind(s);for(const [k,f]of Object.entries(o.getters))Object.defineProperty(s,k,{get:()=>f(s)});return ()=>s},
    router:{currentRoute:{value:{path:'/qa',fullPath:'/qa'}},push:r=>routes.push(r)},
    document:{getElementById:()=>({style:{},textContent:''})},requestAnimationFrame:f=>f(),setTimeout:()=>1,clearTimeout:()=>{}}
  vm.createContext(context)
  let source=fs.readFileSync(new URL('../src/stores/user.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('export const useUserStore','const useUserStore')
  vm.runInContext(source+'\nglobalThis.store=useUserStore()',context)
  context.useUserStore=()=>context.store;context.store.setSession(result())
  source=fs.readFileSync(new URL('../src/api/request.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('import.meta.env','({})').replace('export async function','async function').replace('export { instance }','')
  vm.runInContext(source+'\nglobalThis.api=instance',context)
  return {...context,routes}
}
function response(config,status,data){return {config,status,statusText:'',headers:{},data}}
function denied(config,status=401){return new axios.AxiosError('request failed','ERR_BAD_REQUEST',config,null,response(config,status,{message:'failed'}))}
test('logout clears immediately and cannot clear a later successful login',async()=>{
  const f=fixture(),pending=deferred();f.authApi.logout=()=>pending.promise
  const logout=f.store.logout();assert.equal(f.store.isLoggedIn,false)
  f.authApi.login=async()=>result(2);await f.store.login('b','password');pending.resolve();await logout
  assert.equal(f.store.userInfo.id,'2');assert.equal(f.store.accessToken,'access-2')
})
test('boot restores valid persisted identity before treating it as verified',async()=>{
 const f=fixture();f.store.sessionVerified=false;f.authApi.getProfile=async()=>({...result(),nickname:'小明'})
 assert.equal(await f.store.restoreSession(),true);assert.equal(f.store.sessionVerified,true);assert.equal(f.store.userInfo.nickname,'小明')
})
test('boot rejects obsolete credentials quietly without a misleading expired toast or navigation',async()=>{
 const f=fixture();f.store.sessionVerified=false
 f.authApi.getProfile=()=>f.api.get('/user/profile',{__bootstrapAuth:true}).then(r=>r.data.data)
 f.api.defaults.adapter=async c=>{throw denied(c)}
 f.authApi.refreshToken=async()=>{throw new axios.AxiosError('expired','ERR_BAD_REQUEST',{},null,{status:401})}
 assert.equal(await f.store.restoreSession(),false);assert.equal(f.store.isLoggedIn,false);assert.equal(f.routes.length,0)
})
test('boot service outage keeps credentials without marking identity verified',async()=>{
 const f=fixture();f.store.sessionVerified=false;f.authApi.getProfile=async()=>{throw new Error('Network Error')}
 await assert.rejects(f.store.restoreSession(),/Network/);assert.equal(f.store.isLoggedIn,true);assert.equal(f.store.sessionVerified,false)
})
test('request identity is captured before an immediate account switch',async()=>{
  const f=fixture(),pending=deferred();let sent
  f.api.defaults.adapter=c=>{sent=c.headers.Authorization;return pending.promise.then(()=>response(c,200,{code:200,data:'old account data'}))}
  const request=f.api.post('/checkin',{remark:'account-a'})
  f.store.setSession(result(2));pending.resolve()
  await assert.rejects(request,/登录状态已变化/);assert.equal(sent,'Bearer access-1')
})
test('late login and profile responses cannot restore a logged-out identity',async()=>{
  const f=fixture(),pending=deferred();f.authApi.login=()=>pending.promise
  const login=f.store.login('a','password');f.store.reset();pending.resolve(result())
  await assert.rejects(login,/登录状态已变化/);assert.equal(f.store.isLoggedIn,false)
  const profile=deferred();f.authApi.getProfile=()=>profile.promise
  const fetch=f.store.fetchProfile();f.store.reset();profile.resolve(result())
  await assert.rejects(fetch,/登录状态已变化/);assert.equal(f.store.userInfo,null)
})
test('old refresh response cannot restore state after logout',async()=>{
  const f=fixture(),pending=deferred(),started=deferred()
  f.api.defaults.adapter=async c=>{throw denied(c)}
  f.authApi.refreshToken=()=>{started.resolve();return pending.promise}
  const request=f.api.get('/private');await started.promise;f.store.reset();pending.resolve(result())
  await assert.rejects(request,/登录状态已变化/);assert.equal(f.store.isLoggedIn,false)
})
test('old refresh rejection does not clear a different logged-in account',async()=>{
  const f=fixture(),pending=deferred(),started=deferred()
  f.api.defaults.adapter=async c=>{throw denied(c)}
  f.authApi.refreshToken=()=>{started.resolve();return pending.promise}
  const request=f.api.get('/private');await started.promise;f.store.setSession(result(2))
  pending.reject(new axios.AxiosError('expired','ERR_BAD_REQUEST',{},null,{status:401}))
  await assert.rejects(request,/登录状态已变化/);assert.equal(f.store.userInfo.id,'2');assert.equal(f.routes.length,0)
})
test('refresh service failure preserves current session',async()=>{
  const f=fixture();f.api.defaults.adapter=async c=>{throw denied(c)}
  f.authApi.refreshToken=async()=>{throw new axios.AxiosError('unavailable','ERR_BAD_RESPONSE',{},null,{status:503})}
  await assert.rejects(f.api.get('/private'));assert.equal(f.store.isLoggedIn,true);assert.equal(f.routes.length,0)
})
test('concurrent unauthorized requests share refresh and replay with new token',async()=>{
  const f=fixture(),pending=deferred(),started=deferred();let refreshes=0
  f.authApi.refreshToken=()=>{refreshes++;started.resolve();return pending.promise}
  f.api.defaults.adapter=async c=>{if(c.headers.Authorization!=='Bearer fresh')throw denied(c);return response(c,200,{code:200,data:'ok'})}
  const a=f.api.get('/a'),b=f.api.get('/b');await started.promise
  pending.resolve({...result(),accessToken:'fresh',refreshToken:'fresh-r'})
  await Promise.all([a,b]);assert.equal(refreshes,1);assert.equal(f.store.accessToken,'fresh')
})
