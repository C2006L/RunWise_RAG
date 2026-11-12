// 两端共用策略：原始 HTML 与图片均禁用，不执行模型输出的 HTML。
export function createAnswerRenderer(MarkdownIt) {
  const md = new MarkdownIt({ html: false, breaks: true, linkify: false, typographer: false })
  md.disable('image')
  const link = md.renderer.rules.link_open || ((tokens, i, options, env, self) => self.renderToken(tokens, i, options))
  md.renderer.rules.link_open = (tokens, i, options, env, self) => {
    const token = tokens[i]
    if (!/^https?:\/\//i.test(token.attrGet('href') || '')) token.attrSet('href', '#')
    token.attrSet('target', '_blank'); token.attrSet('rel', 'noopener noreferrer')
    return link(tokens, i, options, env, self)
  }
  const styles = {
    paragraph_open: 'margin:0 0 12px;line-height:1.85;overflow-wrap:anywhere;',
    heading_open: 'margin:18px 0 10px;line-height:1.5;font-weight:800;font-size:18px;',
    bullet_list_open: 'padding-left:22px;margin:8px 0 14px;',
    ordered_list_open: 'padding-left:24px;margin:8px 0 14px;',
    blockquote_open: 'margin:12px 0;padding:4px 12px;border-left:3px solid #f20d1a;color:#c8c6cc;',
    table_open: 'border-collapse:collapse;min-width:420px;width:100%;font-size:14px;',
    th_open: 'border:1px solid #68666c;padding:9px;text-align:left;font-weight:700;background:#252528;',
    td_open: 'border:1px solid #56545a;padding:9px;line-height:1.7;',
  }
  for (const [rule, style] of Object.entries(styles)) md.renderer.rules[rule] = (tokens, i, options, env, self) => {
    tokens[i].attrSet('style', style); return self.renderToken(tokens, i, options)
  }
  const fence = md.renderer.rules.fence
  md.renderer.rules.fence = (tokens, i, options, env, self) => `<div style="overflow-x:auto;max-width:100%;padding:12px;background:#202023;">${fence(tokens, i, options, env, self)}</div>`
  return {
    render(text) { return md.render(String(text || '')) },
    blocks(text) {
      const tokens = md.parse(String(text || ''), {}), blocks = []
      let normal = []
      for (let i = 0; i < tokens.length; i++) {
        if (tokens[i].type !== 'table_open') { normal.push(tokens[i]); continue }
        if (normal.length) { blocks.push({ table: false, html: md.renderer.render(normal, md.options, {}) }); normal = [] }
        const table = []
        do { table.push(tokens[i++]) } while (i < tokens.length && tokens[i - 1].type !== 'table_close')
        i--; blocks.push({ table: true, html: md.renderer.render(table, md.options, {}) })
      }
      if (normal.length) blocks.push({ table: false, html: md.renderer.render(normal, md.options, {}) })
      return blocks
    },
  }
}
