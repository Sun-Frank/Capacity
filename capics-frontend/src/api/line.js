const API_BASE = '/api'

export function getLines(token) {
  return fetch(`${API_BASE}/lines`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getActiveLines(token) {
  return fetch(`${API_BASE}/lines/active`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getLineByCode(token, lineCode) {
  return fetch(`${API_BASE}/lines/${encodeURIComponent(lineCode)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function createLine(token, line) {
  return fetch(`${API_BASE}/lines`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(line)
  }).then(res => res.json())
}

export function updateLine(token, lineCode, line) {
  return fetch(`${API_BASE}/lines/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(line)
  }).then(res => res.json())
}
