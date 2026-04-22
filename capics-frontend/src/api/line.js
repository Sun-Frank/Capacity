const API_BASE = '/api'

function buildAuthHeaders(token) {
  const t = String(token || '').trim()
  if (!t || t === 'undefined' || t === 'null') {
    return {}
  }
  return { Authorization: `Bearer ${t}` }
}

async function parseJsonSafe(res) {
  try {
    return await res.json()
  } catch (e) {
    return null
  }
}

export function getLines(token) {
  return fetch(`${API_BASE}/lines`, {
    headers: buildAuthHeaders(token)
  }).then(res => res.json())
}

export function getActiveLines(token) {
  return fetch(`${API_BASE}/lines/active`, {
    headers: buildAuthHeaders(token)
  }).then(res => res.json())
}

export function getLineByCode(token, lineCode) {
  return fetch(`${API_BASE}/lines/${encodeURIComponent(lineCode)}`, {
    headers: buildAuthHeaders(token)
  }).then(res => res.json())
}

export function createLine(token, line) {
  return fetch(`${API_BASE}/lines`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...buildAuthHeaders(token)
    },
    body: JSON.stringify(line)
  }).then(res => res.json())
}

export function updateLine(token, lineCode, line) {
  return fetch(`${API_BASE}/lines/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...buildAuthHeaders(token)
    },
    body: JSON.stringify(line)
  }).then(res => res.json())
}

export async function importLines(token, file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetch(`${API_BASE}/lines/import`, {
    method: 'POST',
    headers: buildAuthHeaders(token),
    body: formData
  })

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `生产线配置导入失败 (HTTP ${res.status})`)
  }
  return json || { success: false, message: '生产线配置导入失败' }
}

export async function downloadLineTemplate(token) {
  const res = await fetch(`${API_BASE}/lines/template`, {
    headers: buildAuthHeaders(token)
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`模板下载失败 (HTTP ${res.status})${text ? `: ${text}` : ''}`)
  }
  return res.blob()
}
