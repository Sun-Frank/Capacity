function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInline(value) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
}

function flushList(html, listItems, ordered) {
  if (!listItems.length) return
  html.push(`<${ordered ? 'ol' : 'ul'}>`)
  listItems.forEach((item) => html.push(`<li>${item}</li>`))
  html.push(`</${ordered ? 'ol' : 'ul'}>`)
  listItems.length = 0
}

export function renderMarkdown(markdown) {
  const html = []
  const unorderedItems = []
  const orderedItems = []
  const lines = String(markdown || '').replace(/\r\n/g, '\n').split('\n')

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      flushList(html, unorderedItems, false)
      flushList(html, orderedItems, true)
      return
    }

    const heading = trimmed.match(/^(#{1,3})\s+(.+)$/)
    if (heading) {
      flushList(html, unorderedItems, false)
      flushList(html, orderedItems, true)
      const level = heading[1].length
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      return
    }

    const unordered = trimmed.match(/^[-*]\s+(.+)$/)
    if (unordered) {
      flushList(html, orderedItems, true)
      unorderedItems.push(renderInline(unordered[1]))
      return
    }

    const ordered = trimmed.match(/^\d+\.\s+(.+)$/)
    if (ordered) {
      flushList(html, unorderedItems, false)
      orderedItems.push(renderInline(ordered[1]))
      return
    }

    const quote = trimmed.match(/^>\s+(.+)$/)
    if (quote) {
      flushList(html, unorderedItems, false)
      flushList(html, orderedItems, true)
      html.push(`<blockquote>${renderInline(quote[1])}</blockquote>`)
      return
    }

    flushList(html, unorderedItems, false)
    flushList(html, orderedItems, true)
    html.push(`<p>${renderInline(trimmed)}</p>`)
  })

  flushList(html, unorderedItems, false)
  flushList(html, orderedItems, true)

  return html.join('\n')
}
