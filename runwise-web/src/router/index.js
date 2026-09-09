import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "../stores/user";

// 路由表（工程计划第 7 章）：meta.title 供 afterEach 设置标题，requiresAuth 供守卫使用
// 登录守卫（工程计划 7.1，M2 实装）：
// - 未登录访问 requiresAuth 页面 → 踢回 /login 并携带 redirect 参数
// - 已登录访问 /login → 直接进 /home
const routes = [
  {
    path: "/",
    redirect: "/home",
  },
  {
    path: "/login",
    name: "login",
    component: () => import("../views/LoginView.vue"),
    meta: { requiresAuth: false, title: "登录", hideNav: true },
  },
  {
    path: "/home",
    name: "home",
    component: () => import("../views/HomeView.vue"),
    // fullBleed：首页 Hero 全出血（100vh 从页面顶开始，main 不加通用 padding）
    meta: { requiresAuth: true, title: "首页", fullBleed: true },
  },
  {
    path: "/checkin",
    name: "checkin",
    component: () => import("../views/CheckinView.vue"),
    // marquee：底部跑马灯（S2，五内页统一；AI 答疑页高度锁死不挂）
    meta: { requiresAuth: true, title: "打卡记录", marquee: true },
  },
  {
    path: "/plan",
    name: "plan",
    component: () => import("../views/PlanView.vue"),
    meta: { requiresAuth: true, title: "训练计划", marquee: true },
  },
  {
    path: "/qa",
    name: "qa",
    component: () => import("../views/QaView.vue"),
    meta: { requiresAuth: true, title: "AI 答疑" },
  },
  {
    path: "/injury",
    name: "injury",
    component: () => import("../views/InjuryView.vue"),
    meta: { requiresAuth: true, title: "伤病预防", marquee: true },
  },
  {
    path: "/profile",
    name: "profile",
    component: () => import("../views/ProfileView.vue"),
    meta: { requiresAuth: true, title: "个人中心", marquee: true },
  },
  {
    path: "/stats",
    name: "stats",
    component: () => import("../views/StatsView.vue"),
    meta: { requiresAuth: true, title: "数据统计", marquee: true },
  },
  {
    path: "/shapes-preview",
    name: "shapes-preview",
    component: () => import("../assets/shapes/ShapePreview.vue"),
    meta: { requiresAuth: true, title: "素材预览" },
  },
];

const router = createRouter({
  // BASE_URL 跟随 Vite base（GitHub Pages 项目站点为 /RunWise_RAG/，本地 dev 为 /）
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

// 全局守卫：isLoggedIn 只校验双 token 存在性，有效性由 api 层（401 → 静默刷新）负责
router.beforeEach((to) => {
  const { isLoggedIn } = useUserStore();
  if (to.meta.requiresAuth && !isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && isLoggedIn) {
    return { path: "/home" };
  }
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · RunWise` : "RunWise";
});

export default router;
