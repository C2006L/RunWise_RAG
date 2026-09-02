<script setup>
import { onMounted } from "vue";
import { useAuth } from "../../composables/useAuth";

// 顶部斜切导航（工程计划 4.1 v2.0）：六项入口 + 红色斜块激活态（右上切角与 P5Card 统一）
// 数据统计移出导航（低频回访页，入口保留在首页数据条与个人中心）；/stats 路由保留
// M2：接入登录态（昵称 + 退出），onMounted 拉取用户信息（token 恢复 / 静默刷新链路入口）
const { store, logout } = useAuth();

const links = [
  { path: "/home", label: "首页" },
  { path: "/checkin", label: "打卡记录" },
  { path: "/plan", label: "训练计划" },
  { path: "/qa", label: "AI 答疑" },
  { path: "/injury", label: "伤病预防" },
  { path: "/profile", label: "个人中心" },
];

onMounted(() => {
  // 每次应用启动（整页加载 / 登录后首次进入业务页）校验会话并同步用户信息：
  // token 无效/过期时此请求触发 401 → 静默刷新 → 重放，整条链路用户无感知；
  // 双 token 均失效时由刷新队列统一处理（清登录态 + 踢回登录页 + toast）
  if (store.isLoggedIn) {
    store.fetchProfile().catch(() => {});
  }
});

async function handleLogout() {
  await logout();
}
</script>

<template>
  <header class="nav-bar">
    <div class="nav-inner">
      <router-link to="/home" class="nav-brand"> RUN<em>WISE</em> </router-link>

      <div class="nav-right">
        <nav class="nav-links">
          <router-link
            v-for="link in links"
            :key="link.path"
            :to="link.path"
            class="nav-link"
          >
            {{ link.label }}
          </router-link>
        </nav>

        <div v-if="store.userInfo" class="nav-user">
          <span class="nav-nickname">{{ store.userInfo.nickname }}</span>
          <button class="nav-logout" @click="handleLogout">退出</button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-nav);
  height: var(--nav-h);
  background: var(--p5-panel);
  border-bottom: 1px solid var(--p5-line);
}

.nav-inner {
  max-width: var(--page-max);
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--sp-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 品牌区：Anton 栈（Google Fonts 引入，Impact 兜底）+ 错位投影 */
.nav-brand {
  font-family: var(--font-display);
  font-size: var(--fs-h2);
  letter-spacing: 0.05em;
  color: var(--p5-white);
  text-shadow: 3px 3px 0 var(--p5-red); /* 轻微错位投影：白字红影，强化品牌感 */
}

.nav-brand em {
  font-style: normal;
  color: var(--p5-red);
  text-shadow: 3px 3px 0 var(--p5-shadow); /* 红字配深影，双色错位层次 */
}

.nav-right {
  display: flex;
  align-items: center;
  gap: var(--sp-5);
}

.nav-links {
  display: flex;
  gap: var(--sp-3);
}

/* 斜切链接底块：hover 红蒙层，激活态主红实底 */
.nav-link {
  display: inline-block;
  padding: var(--sp-2) var(--sp-4);
  font-size: var(--fs-sub);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
  clip-path: polygon(10px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  transition:
    color 0.2s,
    background-color 0.2s;
}

.nav-link:hover {
  color: var(--p5-white);
  background: var(--p5-red-soft);
}

/* 激活态：主红实底 + 右上角切角（尺寸同全局 --cut），与 P5Card 切角语言统一 */
.nav-link.router-link-active {
  color: var(--p5-white);
  background: var(--p5-red);
  font-weight: 700;
  clip-path: polygon(
    10px 0,
    calc(100% - var(--cut)) 0,
    100% var(--cut),
    100% 100%,
    calc(100% - 10px) 100%,
    0 100%
  );
}

/* ===== 用户区（M2）===== */
.nav-user {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding-left: var(--sp-4);
  border-left: 1px solid var(--p5-line);
}

.nav-nickname {
  font-size: var(--fs-sub);
  font-weight: 700;
  color: var(--p5-white);
}

.nav-logout {
  padding: var(--sp-1) var(--sp-3);
  font-size: var(--fs-caption);
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
  border: 1px solid var(--p5-line);
  transition:
    color 0.2s,
    border-color 0.2s;
}

.nav-logout:hover {
  color: var(--p5-red);
  border-color: var(--p5-red);
}
</style>
