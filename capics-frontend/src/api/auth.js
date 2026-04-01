const API_BASE = '/api'

export function authApi(token) {
  const api = async (path, options = {}) => {
    const headers = { 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
    const data = await res.json()
    if (!res.ok) throw new Error(data.message || 'API Error')
    return data
  }

  return {
    login: (username, password) =>
      api('/auth/login', {
        method: 'POST',
        body: { username, password }
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
  }).then(res => res.json())
}

export function logout(token) {
  return fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getCurrentUser(token) {
  return fetch(`${API_BASE}/auth/current`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}
