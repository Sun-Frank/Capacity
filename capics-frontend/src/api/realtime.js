const API_BASE = '/api'

export function calculateRealtime(token, version) {
  return fetch(`${API_BASE}/line-realtime/calculate?version=${encodeURIComponent(version)}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getRealtimeList(token, version) {
  return fetch(`${API_BASE}/line-realtime/list?version=${encodeURIComponent(version)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}
