const API_BASE = '/api'

export function createApi(token) {
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

  const apiWithBody = async (path, options = {}) => {
    const headers = { 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    const res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.message || 'API Error')
    return data
  }

  return { api, apiWithBody }
}

export { API_BASE }
