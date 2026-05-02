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

function clearAuthAndRedirect() {
  localStorage.removeItem('capics_token')
  localStorage.removeItem('capics_user')
  localStorage.removeItem('capics_username')
  localStorage.removeItem('capics_user_id')
  localStorage.removeItem('capics_role_codes')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

async function assertJsonResponse(res, fallbackMessage) {
  const json = await parseJsonSafe(res)
  if (res.status === 401 || res.status === 403) {
    clearAuthAndRedirect()
    throw new Error('登录已过期或无权限，请重新登录')
  }
  if (!res.ok) {
    throw new Error(json?.message || `${fallbackMessage} (HTTP ${res.status})`)
  }
  return json || { success: false, message: fallbackMessage }
}

export async function getCtLines(token) {
  const res = await fetchWithTimeout(`${API_BASE}/ct-lines`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  return assertJsonResponse(res, 'Load failed')
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

  return assertJsonResponse(res, 'Create failed')
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

  return assertJsonResponse(res, 'Import failed')
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

  return assertJsonResponse(res, 'Create import task failed')
}

export async function getCtLineImportTask(token, taskId) {
  const res = await fetchWithTimeout(
    `${API_BASE}/ct-lines/import-tasks/${encodeURIComponent(taskId)}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    },
    REQUEST_TIMEOUT_MS
  )

  return assertJsonResponse(res, 'Query import task failed')
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

  return assertJsonResponse(res, 'Save failed')
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
