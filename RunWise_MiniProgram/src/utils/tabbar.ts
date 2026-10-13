/**
 * custom-tab-bar 选中态同步（微信自定义 tabBar 官方模式）
 * 各 tab 页 onShow 里调用 setTabBarIndex(n)：
 * 通过 getCurrentPages() 取当前页实例的 getTabBar()，setData 同步选中索引。
 * 注意：自定义 tabbar 组件每个 tab 页各持一个实例，跨页共享 ref 不可靠，故用 setData。
 */
export function setTabBarIndex(index: number) {
  const pages = getCurrentPages()
  const page: any = pages[pages.length - 1]
  if (page && typeof page.getTabBar === 'function') {
    const bar = page.getTabBar()
    if (bar) {
      if (typeof bar.setData === 'function') bar.setData({ current: index })
      else bar.current = index
    }
  }
}
