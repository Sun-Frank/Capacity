const API_BASE = '/api'

export function getCapacityAssessment(token, createdBy, fileName, version) {
  return fetch(
    `${API_BASE}/capacity-assessment?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}&version=${encodeURIComponent(version)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}
