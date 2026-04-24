const API_BASE = '/api'
const REQUEST_TIMEOUT_MS = 15000
const TASK_CREATE_TIMEOUT_MS = 120000
const IMPORT_TIMEOUT_MS = 10 * 60 * 1000

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
      throw new Error(`Request timed out (> ${Math.floor(timeoutMs / 1000)}s)`)
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
  }).then((res) => res.json())
}

export async function createCtLine(token, payload) {
  const res = await fetchWithTimeout(`${API_BASE}/ct-lines`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `Create failed (HTTP ${res.status})`)
  }
  return json || { success: false, message: 'Create failed' }
}

export async function importCtLines(token, file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetchWithTimeout(
    `${API_BASE}/ct-lines/import`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData
    },
    IMPORT_TIMEOUT_MS
  )

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `Import failed (HTTP ${res.status})`)
  }
  return json || { success: false, message: 'Import failed' }
}

export async function startCtLineImportTask(token, file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetchWithTimeout(
    `${API_BASE}/ct-lines/import-async`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData
    },
    TASK_CREATE_TIMEOUT_MS
  )

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `Create import task failed (HTTP ${res.status})`)
  }
  return json || { success: false, message: 'Create import task failed' }
}

export async function getCtLineImportTask(token, taskId) {
  const res = await fetchWithTimeout(
    `${API_BASE}/ct-lines/import-tasks/${encodeURIComponent(taskId)}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    },
    REQUEST_TIMEOUT_MS
  )

  const json = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(json?.message || `Query import task failed (HTTP ${res.status})`)
  }
  return json || { success: false, message: 'Query import task failed' }
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
    throw new Error(json?.message || `Save failed (HTTP ${res.status})`)
  }
  return json || { success: false, message: 'Save failed' }
}

export async function downloadCtLineTemplate(token) {
  const res = await fetchWithTimeout(`${API_BASE}/ct-lines/template`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error(`Template download failed (HTTP ${res.status})`)
  }
  return await res.blob()
}
