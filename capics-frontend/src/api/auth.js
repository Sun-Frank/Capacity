const API_BASE = '/api'

async function parseJsonSafe(res) {
  const text = await res.text()
  if (!text || !text.trim()) {
    return {}
  }
  try {
    return JSON.parse(text)
  } catch (e) {
    return {
      success: false,
      message: `Request failed (HTTP ${res.status})`
    }
  }
}

function buildHttpErrorMessage(status, fallback) {
  if (fallback) return fallback
  if (status === 401 || status === 403) {
    return `登录已失效或无权限 (HTTP ${status})，请重新登录`
  }
  return `Request failed (HTTP ${status})`
}

export function authApi(token) {
  const api = async (path, options = {}) => {
    const headers = { 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
    const data = await parseJsonSafe(res)
    if (!res.ok) throw new Error(data.message || 'API Error')
    return data
  }

  return {
    login: (username, password) =>
      api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      }),

    logout: () =>
      api('/auth/logout', { method: 'POST' }),

    getCurrentUser: () =>
      api('/auth/current')
  }
}

export function login(username, password) {
  return fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  }).then(async (res) => {
    const data = await parseJsonSafe(res)
    if (!res.ok) throw new Error(buildHttpErrorMessage(res.status, data.message))
    return data
  })
}

export function logout(token) {
  return fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(async (res) => {
    const data = await parseJsonSafe(res)
    if (!res.ok) throw new Error(buildHttpErrorMessage(res.status, data.message))
    return data
  })
}

export function getCurrentUser(token) {
  return fetch(`${API_BASE}/auth/current`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(async (res) => {
    const data = await parseJsonSafe(res)
    if (!res.ok) throw new Error(buildHttpErrorMessage(res.status, data.message))
    return data
  })
}
