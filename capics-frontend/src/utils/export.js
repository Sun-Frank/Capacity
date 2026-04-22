function escapeCsvValue(value) {
  if (value === null || value === undefined) {
    return ''
  }
  const str = String(value)
  if (/[",\r\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

export function downloadCsv(fileName, headers, rows) {
  const headerLine = headers.map(h => escapeCsvValue(h.label)).join(',')
  const lines = rows.map(row => headers.map(h => escapeCsvValue(row[h.key])).join(','))
  const csv = [headerLine, ...lines].join('\r\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}
