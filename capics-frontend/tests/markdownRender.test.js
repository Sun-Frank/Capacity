import test from 'node:test'
import assert from 'node:assert/strict'

import { renderMarkdown } from '../src/utils/markdown.js'

test('renderMarkdown formats common markdown syntax and escapes unsafe html', () => {
  const html = renderMarkdown(`# 标题

**重点** 和 \`代码\`
- 列表1
- 列表2
> 引用
[链接](https://example.com)
<script>alert(1)</script>`)

  assert(html.includes('<h1>标题</h1>'))
  assert(html.includes('<strong>重点</strong>'))
  assert(html.includes('<code>代码</code>'))
  assert(html.includes('<ul>'))
  assert(html.includes('<li>列表1</li>'))
  assert(html.includes('<blockquote>引用</blockquote>'))
  assert(html.includes('<a href="https://example.com" target="_blank" rel="noopener noreferrer">链接</a>'))
  assert(!html.includes('<script>'))
  assert(html.includes('&lt;script&gt;alert(1)&lt;/script&gt;'))
})
