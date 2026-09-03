<script setup>
// ECharts 封装（工程计划 4.6 / 8.3③）：
// - 按需注册：仅 echarts/core + BarChart / LineChart + Grid / Tooltip + CanvasRenderer
//   （全量约 1MB，按需后构建增量控制在 400KB 内）
// - props.type 切换柱状 / 折线；数据 watch 重绘（同一实例 setOption，无残留）
// - 窗口 resize 自适应；组件卸载 dispose 防 WebView 内存泄漏
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts/core";
import { BarChart, LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps({
  type: {
    type: String,
    default: "bar", // 'bar' 周视图 | 'line' 月视图
  },
  labels: {
    type: Array,
    default: () => [],
  },
  values: {
    type: Array,
    default: () => [],
  },
  unit: {
    type: String,
    default: "km",
  },
  height: {
    type: String,
    default: "380px",
  },
});

const el = ref(null);
let chart = null;

// 红黑主题 option（核心三色取自 variables.css 冻结值，UI 精修 P1-5）
// 空柱占位（P3-4）：柱状图无打卡日在底部渲染暗红短横线，暗示「这天没跑」
function buildOption() {
  const isBar = props.type === "bar";
  const maxV = Math.max(...props.values, 1);
  // 零值占位高度：约为主刻度的 1.5%，视觉上为 2~4px 短横线
  const stubH = Math.max(maxV * 0.015, 0.05);

  return {
    backgroundColor: "transparent",
    tooltip: {
      trigger: "axis",
      backgroundColor: "#1b1b1e", // --p5-panel
      borderColor: "rgba(255,255,255,0.14)",
      textStyle: { color: "#ffffff", fontSize: 12 },
      // 自定义 formatter：只展示主系列，0 值显示「未打卡」
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params;
        if (!p) return "";
        const v = p.value;
        return `${p.name}<br/>${v > 0 ? `${v} ${props.unit}` : "未打卡"}`;
      },
    },
    // E5：y 轴名「单位 km」横向单行——name 默认横排在轴顶，grid.top 预留
    // 足够高度防裁切，nameTextStyle 限单行字号
    grid: { left: 56, right: 24, top: 52, bottom: 40 },
    xAxis: {
      type: "category",
      data: props.labels,
      axisLine: { lineStyle: { color: "rgba(255,255,255,0.24)" } },
      axisTick: { show: false },
      axisLabel: {
        color: "#9c9ca3",
        fontSize: 14,
        // 月视图为纯数字 → Anton 展示字体；周视图标签含中文保持正文字体
        fontFamily: isBar
          ? undefined
          : "'Anton', 'Impact', 'Arial Narrow', sans-serif",
      },
    },
    yAxis: {
      type: "value",
      name: `单位 ${props.unit}`,
      nameTextStyle: {
        color: "#9c9ca3",
        fontSize: 11,
        // E5：强制横向单行，禁止换行错位
        align: "left",
        verticalAlign: "middle",
        padding: [0, 0, 24, 0],
      },
      nameGap: 14,
      splitLine: { lineStyle: { color: "rgba(255,255,255,0.08)" } },
      axisLabel: {
        color: "#9c9ca3",
        fontSize: 13,
        fontFamily: "'Anton', 'Impact', 'Arial Narrow', sans-serif",
      },
    },
    series: isBar
      ? [
          {
            type: "bar",
            name: "mileage",
            data: props.values,
            barWidth: "42%",
            stack: "p5-bar",
            itemStyle: {
              // 主红 → 深红纵向渐变，贴打样图红色跑道弧线
              color: {
                type: "linear",
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: "#e60012" },
                  { offset: 1, color: "#8f000b" },
                ],
              },
            },
            emphasis: { itemStyle: { color: "#8f000b" } },
          },
          {
            // 零值占位系列：仅无打卡日渲染暗红短横线（silent 不响应鼠标）
            type: "bar",
            name: "stub",
            data: props.values.map((v) => (v > 0 ? 0 : stubH)),
            barWidth: "42%",
            stack: "p5-bar",
            silent: true,
            itemStyle: { color: "#8f000b", opacity: 0.55 },
          },
        ]
      : [
          {
            type: "line",
            data: props.values,
            smooth: true, // 平滑曲线
            // 隐藏所有数据点标记（消除密集圆点），悬停由 axis tooltip 呈现数值
            showSymbol: false,
            symbol: "circle",
            symbolSize: 8,
            lineStyle: { color: "#e60012", width: 2.5 },
            itemStyle: { color: "#e60012" },
            // 平滑面积填充（视觉精修指定值）
            areaStyle: { color: "rgba(232,17,45,0.15)" },
            // 悬停仅走 axis tooltip，不放大点亮数据点
            emphasis: { disabled: true },
          },
        ],
  };
}

function render() {
  if (!chart) return;
  chart.setOption(buildOption(), true); // notMerge：切换 bar/line 时完全替换
}

function handleResize() {
  chart?.resize();
}

onMounted(() => {
  chart = echarts.init(el.value);
  render();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chart?.dispose();
  chart = null;
});

watch(
  () => [props.type, props.labels, props.values],
  () => render(),
  { deep: true },
);
</script>

<template>
  <div ref="el" class="chart" :style="{ height }"></div>
</template>

<style scoped>
.chart {
  width: 100%;
}
</style>
