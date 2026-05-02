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

export function getNotebookNotes(token) {
  return fetch(`${API_BASE}/notebook`, { headers: headers(token) }).then(parse)
}

export function saveNotebookNote(token, note) {
  const hasId = !!note.id
  return fetch(`${API_BASE}/notebook${hasId ? `/${encodeURIComponent(note.id)}` : ''}`, {
    method: hasId ? 'PUT' : 'POST',
    headers: headers(token, true),
    body: JSON.stringify(note)
  }).then(parse)
}

export function deleteNotebookNote(token, id) {
  return fetch(`${API_BASE}/notebook/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: headers(token)
  }).then(parse)
}