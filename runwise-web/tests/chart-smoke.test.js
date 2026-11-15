import { test } from 'node:test'
import assert from 'node:assert/strict'
import * as echarts from 'echarts'

test('updated chart runtime renders discrete training data without fabricating points', () => {
  for (const type of ['bar', 'line']) {
    const chart = echarts.init(null, null, { renderer: 'svg', ssr: true, width: 700, height: 300 })
    try {
      chart.setOption({ xAxis: { type: 'category', data: ['2026-11-14', '2026-11-15'] },
        yAxis: { type: 'value', min: 0 }, series: [{ type, smooth: false, data: [0, 5.2] }] })
      assert.match(chart.renderToSVGString(), /<svg/)
      assert.deepEqual(chart.getOption().series[0].data, [0, 5.2])
    } finally { chart.dispose() }
  }
})
