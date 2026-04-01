const API_BASE = '/api'

export function getRoutings(token) {
  return fetch(`${API_BASE}/routings`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getRoutingsFull(token) {
  return fetch(`${API_BASE}/routings/full`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getRoutingById(token, id) {
  return fetch(`${API_BASE}/routings/${id}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getRoutingByProductNumber(token, productNumber) {
  return fetch(`${API_BASE}/routings/product/${encodeURIComponent(productNumber)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function importRoutings(token, file, createdBy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('createdBy', createdBy)
  return fetch(`${API_BASE}/routings/import`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(res => res.json())
}

export function checkRoutingImportDuplicates(token, file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch(`${API_BASE}/routings/import/check`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(res => res.json())
}

export function getRoutingItemsGrouped(token) {
  return fetch(`${API_BASE}/routings/items/grouped`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function updateRoutingItemLine(token, id, lineCode, updatedBy) {
  return fetch(`${API_BASE}/routings/items/${id}/line`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ lineCode, updatedBy })
  }).then(res => res.json())
}
