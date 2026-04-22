const API_BASE = '/api'
const REQUEST_TIMEOUT_MS = 15000

async function fetchWithTimeout(url, options = {}, timeoutMs = REQUEST_TIMEOUT_MS) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal
    })
  } catch (e) {
    if (e?.name === 'AbortError') {
      throw new Error(`请求超时（>${Math.floor(timeoutMs / 1000)}秒）`)
    }
    throw e
  } finally {
    clearTimeout(timeout)
  }
}

async function parseJsonSafe(res) {
  try {
    return await res.json()
  } catch (e) {
    return null
  }
}

export function getCtLines(token) {
  return fetchWithTimeout(`${API_BASE}/ct-lines`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}

export async function importCtLines(token, file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetchWithTimeout(`${API_BASE}/ct-lines/import`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData
  }, 30000)

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `导入失败 (HTTP ${res.status})`)
  }
  return json || { success: false, message: '导入失败' }
}

export async function updateCtLine(token, id, payload) {
  const res = await fetchWithTimeout(`${API_BASE}/ct-lines/${id}`, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `保存失败 (HTTP ${res.status})`)
  }
  return json || { success: false, message: '保存失败' }
}

export async function downloadCtLineTemplate(token) {
  const res = await fetchWithTimeout(`${API_BASE}/ct-lines/template`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error(`模板下载失败 (HTTP ${res.status})`)
  }
  return await res.blob()
}
