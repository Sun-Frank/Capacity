const API_BASE = '/api'

function headers(token, json = false) {
  const h = {}
  if (json) h['Content-Type'] = 'application/json'
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

async function parse(res) {
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new Error(data?.message || `API error (HTTP ${res.status})`)
  return data
}

export function getFeishuConfig(token) {
  return fetch(`${API_BASE}/system/feishu-config`, { headers: headers(token) }).then(parse)
}

export function saveFeishuConfig(token, config) {
  return fetch(`${API_BASE}/system/feishu-config`, {
    method: 'PUT',
    headers: headers(token, true),
    body: JSON.stringify(config)
  }).then(parse)
}

export function testFeishuConfig(token, config) {
  return fetch(`${API_BASE}/system/feishu-config/test`, {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify(config)
  }).then(parse)
}

export function sendFeishuMessage(token, payload) {
  return fetch(`${API_BASE}/system/feishu-config/send`, {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify({
      email: Array.isArray(payload.emails) ? payload.emails[0] : payload.email,
      text: payload.content || payload.text
    })
  }).then(parse)
}
