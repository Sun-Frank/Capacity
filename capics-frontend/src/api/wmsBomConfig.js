const API_BASE = '/api'

async function parseJson(res, fallbackMessage) {
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(data?.message || `${fallbackMessage} (HTTP ${res.status})`)
  }
  return data
}

export function getWmsBomConfig(token) {
  return fetch(`${API_BASE}/system/wms-bom-config`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then((res) => parseJson(res, 'Load WMS BOM config failed'))
}

export function saveWmsBomConfig(token, payload) {
  return fetch(`${API_BASE}/system/wms-bom-config`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  }).then((res) => parseJson(res, 'Save WMS BOM config failed'))
}

export function testWmsBomConfig(token, payload) {
  return fetch(`${API_BASE}/system/wms-bom-config/test`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  }).then((res) => parseJson(res, 'Test WMS BOM config failed'))
}
