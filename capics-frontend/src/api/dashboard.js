const API_BASE = '/api'

export function getLoadingMatrix(token, type = 'static', dimension = 'week', createdBy = '', fileName = '', version = '') {
  const params = new URLSearchParams({
    type,
    dimension,
    createdBy,
    fileName,
    version
  })
  return fetch(
    `${API_BASE}/dashboard/loading?${params.toString()}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getCreatedBys(token) {
  return fetch(
    `${API_BASE}/mrp/filters/created-bys`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getFileNamesByCreatedBy(token, createdBy) {
  return fetch(
    `${API_BASE}/mrp/filters/${encodeURIComponent(createdBy)}/files`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getVersionsByCreatedByAndFileName(token, createdBy, fileName) {
  return fetch(
    `${API_BASE}/mrp/filters/${encodeURIComponent(createdBy)}/${encodeURIComponent(fileName)}/versions`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}
