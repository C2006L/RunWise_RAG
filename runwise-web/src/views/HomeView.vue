<script setup>
// 首页（V3 终版）：Hero（含数据条/大卡/纯文字菜单，菜单路由直跳）+ 简洁页脚
// - 下方三卡详情区已删除（功能入口由 Hero 菜单与顶部导航承担）
// - 页脚（E 组去重）：只留 RUN WISE 小字标 + 版权行；导航链接与顶部重复已删；
//   「To Every Runner」全页仅 Hero 签名处出现 1 次
import HeroSection from "../components/business/HeroSection.vue";
</script>

<template>
  <div class="home-page">
    <HeroSection />
    <footer class="home-footer">
      <span class="footer-band" aria-hidden="true"></span>
      <div class="footer-inner">
        <span class="footer-logo">RUN WISE</span>
        <span class="footer-copy">© 2024 RunWise</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  /* Hero 全出血 + 多处旋转元素：裁掉旋转包围盒造成的亚像素横向溢出 */
  overflow-x: clip;
}

/* ===== 页脚（≤120px 不抢戏） ===== */
.home-footer {
  position: relative;
  background: var(--p5-black);
  padding-top: 26px; /* 白带之下的内容起点 */
}

/* 顶部 -12° 白色细带：延续 Hero 色带语言（rotate 包围盒用溢出裁切兜住） */
.footer-band {
  position: absolute;
  top: 0;
  left: -4%;
  width: 108%;
  height: 10px;
  background: var(--p5-white);
  transform: rotate(-1.2deg); /* 视觉 -12° 微倾（页脚极窄，全角度会出界） */
  transform-origin: left center;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-5);
  max-width: var(--content-max);
  min-height: 60px;
  margin: 0 auto;
  padding: 0 var(--sp-6) 18px;
}

.footer-logo {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0.1em;
  color: var(--p5-white);
  text-shadow: 3px 3px 0 var(--p5-red);
}

.footer-copy {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--p5-text-dim);
}

@media (max-width: 768px) {
  .footer-inner {
    flex-direction: column;
    gap: var(--sp-3);
    padding-bottom: 14px;
  }
}
</style>
