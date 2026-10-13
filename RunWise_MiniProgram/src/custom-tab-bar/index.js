/**
 * P5R 底部导航（微信原生自定义 tabBar · Component 四件套）
 * 背景：uni-app vite 编译器不会编译 src/custom-tab-bar/index.vue（仅原样拷贝到产物根目录），
 * 微信找不到自定义 tabBar 组件导致整条导航不渲染；改为原生四件套，构建时原样进产物。
 * 选中态同步：各 tab 页 onShow 调 utils/tabbar.ts setTabBarIndex(n) → getTabBar().setData({ current })
 * 图标：自绘线性 SVG（PRD 13.2.7 ①：2px 线性描边、方头/斜切几何语言，白未选/红选中，禁 emoji）
 */
const svg = (body, color) =>
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="' +
      color +
      '" stroke-width="2" stroke-linecap="square" stroke-linejoin="miter">' +
      body +
      '</svg>'
  );

const WHITE = '#FFFFFF';
/* 选中态图标恒白：压在红切角块上（对齐 Web 定稿），红色会变"隐身" */

/* 首页：切角房子 */
const home = '<path d="M4 10.5 12 4l8 6.5V20h-5.5v-5h-5v5H4z"/>';
/* 打卡：日历 + 对勾 */
const checkin =
  '<path d="M5 5h14v15H5z"/><path d="M5 9h14"/><path d="M8.5 14l2.2 2.2 4.3-4.3"/>';
/* 答疑：切角对话气泡 */
const qa =
  '<path d="M4 5h16v11H10l-4 3.5V16H4z"/><path d="M9 9.5h6"/><path d="M9 12.5h4"/>';
/* 我的：人形 */
const mine =
  '<circle cx="12" cy="8" r="3.4"/><path d="M5.5 19.5c1.2-3.6 3.6-5.4 6.5-5.4s5.3 1.8 6.5 5.4"/>';

Component({
  data: {
    current: 0,
    tabList: [
      {
        pagePath: '/pages/index/index',
        text: '首页',
        icon: svg(home, WHITE),
        selIcon: svg(home, WHITE),
      },
      {
        pagePath: '/pages/checkin/checkin',
        text: '打卡',
        icon: svg(checkin, WHITE),
        selIcon: svg(checkin, WHITE),
      },
      {
        pagePath: '/pages/qa/qa',
        text: '答疑',
        icon: svg(qa, WHITE),
        selIcon: svg(qa, WHITE),
      },
      {
        pagePath: '/pages/mine/mine',
        text: '我的',
        icon: svg(mine, WHITE),
        selIcon: svg(mine, WHITE),
      },
    ],
  },
  methods: {
    onTabTap(e) {
      const ds = e.currentTarget.dataset;
      if (this.data.current === ds.index) return;
      this.setData({ current: ds.index });
      wx.switchTab({ url: ds.path });
    },
  },
});
