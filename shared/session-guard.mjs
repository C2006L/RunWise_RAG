// An epoch represents an identity lifecycle, not an access-token rotation.
export function createSessionGuard() {
  let epoch = 0, pending = null
  return {
    current: () => epoch,
    isCurrent: value => value === epoch,
    invalidate() { pending = null; return ++epoch },
    refresh(run) {
      if (pending?.epoch === epoch) return pending.promise
      const task = { epoch, promise: null }
      task.promise = Promise.resolve().then(run).finally(() => { if (pending === task) pending = null })
      pending = task
      return task.promise
    },
  }
}
export function staleSessionError() {
  return Object.assign(new Error('登录状态已变化，请重新操作'), { code: 'SESSION_CHANGED' })
}
