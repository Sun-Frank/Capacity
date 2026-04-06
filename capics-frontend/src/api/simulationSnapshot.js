const API_BASE = '/api'

export function saveSnapshot(token, data) {
  return fetch(
    `${API_BASE}/simulation-snapshots`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    }
  ).then(res => res.json())
}

export function getSnapshotNames(token, createdBy, fileName, version, source = 'dynamic', dimension = 'week') {
  const params = new URLSearchParams({
    createdBy,
    fileName,
    version,
    source,
    dimension
  })
  return fetch(
    `${API_BASE}/simulation-snapshots/names?${params.toString()}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getSnapshot(token, createdBy, fileName, version, snapshotName, source = 'dynamic', dimension = 'week') {
  const params = new URLSearchParams({
    createdBy,
    fileName,
    version,
    snapshotName,
    source,
    dimension
  })
  return fetch(
    `${API_BASE}/simulation-snapshots?${params.toString()}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}
