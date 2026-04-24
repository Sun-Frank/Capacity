const API_BASE = '/api'

async function toApiResponse(res) {
  const text = await res.text()
  let parsed = null
  if (text) {
    try {
      parsed = JSON.parse(text)
    } catch (e) {
      parsed = null
    }
  }

  if (parsed && typeof parsed === 'object') {
    if (!res.ok && !parsed.message) {
      parsed.message = `Request failed (HTTP ${res.status})`
    }
    return parsed
  }

  const statusMessageMap = {
    401: 'Session expired, please login again',
    403: 'No permission to access this API',
    413: 'Payload too large, server rejected save (HTTP 413)',
    500: 'Server internal error (HTTP 500)'
  }

  return {
    success: false,
    message: statusMessageMap[res.status] || `API returned non-JSON (HTTP ${res.status})`
  }
}

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
  ).then(toApiResponse)
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
  ).then(toApiResponse)
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
  ).then(toApiResponse)
}