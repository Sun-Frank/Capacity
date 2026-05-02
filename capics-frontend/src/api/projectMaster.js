const API_BASE = '/api'

function authHeaders(token, json = false) {
  const headers = {}
  if (json) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

async function parse(res) {
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new Error(data?.message || `API error (HTTP ${res.status})`)
  return data
}

export function getProjectMasters(token, keyword = '') {
  const qs = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  return fetch(`${API_BASE}/project-master${qs}`, { headers: authHeaders(token) }).then(parse)
}

export function createProjectMaster(token, data) {
  return fetch(`${API_BASE}/project-master`, {
    method: 'POST',
    headers: authHeaders(token, true),
    body: JSON.stringify(data)
  }).then(parse)
}

export function updateProjectMaster(token, id, data) {
  return fetch(`${API_BASE}/project-master/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: authHeaders(token, true),
    body: JSON.stringify(data)
  }).then(parse)
}

export function deleteProjectMaster(token, id) {
  return fetch(`${API_BASE}/project-master/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(token)
  }).then(parse)
}

export function importProjectMasters(token, file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch(`${API_BASE}/project-master/import`, {
    method: 'POST',
    headers: authHeaders(token),
    body: formData
  }).then(parse)
}

export async function downloadProjectMasterTemplate(token) {
  const res = await fetch(`${API_BASE}/project-master/template`, { headers: authHeaders(token) })
  if (!res.ok) throw new Error(`模板下载失败 (HTTP ${res.status})`)
  return res.blob()
}