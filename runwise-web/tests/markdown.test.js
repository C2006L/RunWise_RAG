import test from 'node:test'
import assert from 'node:assert/strict'
import MarkdownIt from 'markdown-it'
import {createAnswerRenderer} from '../../shared/markdown-policy.mjs'
const renderer=createAnswerRenderer(MarkdownIt)
test('renders titles, lists, emphasis and fenced code',()=>{
  const html=renderer.render('## 训练建议\n\n- **轻松跑**\n- 恢复\n\n```text\n<test>\n```')
  assert.match(html,/<h2/);assert.match(html,/<strong>轻松跑<\/strong>/);assert.match(html,/<ul/);assert.match(html,/&lt;test&gt;/)
})
test('raw HTML, images and unsafe links are not executable',()=>{
  const html=renderer.render('<script>alert(1)</script>\n<img src=x onerror=alert(1)>\n[x](javascript:alert(1))\n![image](https://example.com/a.png)\n[x](data:text/html,hello)')
  assert.doesNotMatch(html,/<script|<img|href="(?:javascript|data):/i)
})
test('table is isolated from surrounding paragraphs for native horizontal scrolling',()=>{
  const blocks=renderer.blocks('正文\n\n| 日期 | 距离 |\n| --- | --- |\n| 周一 | 5km |\n\n结束')
  assert.deepEqual(blocks.map(block=>block.table),[false,true,false]);assert.match(blocks[1].html,/<table/)
})
test('external links carry isolation attributes and relative links cannot navigate',()=>{
  const html=renderer.render('[链接](https://example.com) [其他](//other.example)')
  assert.match(html,/rel="noopener noreferrer"/);assert.match(html,/href="#"/)
})
