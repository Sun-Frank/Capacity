const API_BASE = '/api'

export function getVersions(token) {
  return fetch(`${API_BASE}/mrp/versions`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getLatestMrpFile(token) {
  return fetch(`${API_BASE}/mrp/latest-file`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getAllPlans(token) {
  return fetch(`${API_BASE}/mrp/plans`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getPlansByVersion(token, version) {
  return fetch(`${API_BASE}/mrp/plans/version/${encodeURIComponent(version)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getPlansFiltered(token, createdBy, fileName, version) {
  return fetch(
    `${API_BASE}/mrp/plans/filtered?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}&version=${encodeURIComponent(version)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getCreatedBys(token) {
  return fetch(`${API_BASE}/mrp/filters/created-bys`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getFileNamesByCreatedBy(token, createdBy) {
  return fetch(`${API_BASE}/mrp/filters/${encodeURIComponent(createdBy)}/files`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getVersionsByCreatedByAndFileName(token, createdBy, fileName) {
  return fetch(
    `${API_BASE}/mrp/filters/${encodeURIComponent(createdBy)}/${encodeURIComponent(fileName)}/versions`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getWeeklyReport(token, createdBy, fileName) {
  return fetch(
    `${API_BASE}/mrp/reports/weekly/by-file?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getMonthlyReport(token, version) {
  return fetch(`${API_BASE}/mrp/reports/monthly?version=${encodeURIComponent(version)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getMonthlyReportByFile(token, createdBy, fileName) {
  return fetch(
    `${API_BASE}/mrp/reports/monthly/by-file?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getWeeklyDemandSingle(token, createdBy, fileName, version) {
  return fetch(
    `${API_BASE}/mrp/reports/weekly/single?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}&version=${encodeURIComponent(version)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function getMonthlyDemandSingle(token, createdBy, fileName, version) {
  return fetch(
    `${API_BASE}/mrp/reports/monthly/single?createdBy=${encodeURIComponent(createdBy)}&fileName=${encodeURIComponent(fileName)}&version=${encodeURIComponent(version)}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  ).then(res => res.json())
}

export function importMrpPlans(token, file, fileName, createdBy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('fileName', fileName)
  formData.append('createdBy', createdBy)

  return fetch(`${API_BASE}/mrp/plans/import`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    },
    body: formData
  }).then(async (res) => {
    const rawText = await res.text()
    const contentType = res.headers.get('content-type') || ''

    if (!rawText || !rawText.trim()) {
      if (res.status === 401 || res.status === 403) {
        throw new Error(`Import denied (HTTP ${res.status}). Please re-login and verify CORS origin whitelist.`)
      }
      throw new Error(`Import API returned empty response (HTTP ${res.status})`)
    }

    if (!contentType.includes('application/json')) {
      throw new Error(`Import API did not return JSON (HTTP ${res.status})`)
    }

    let data
    try {
      data = JSON.parse(rawText)
    } catch (error) {
      throw new Error(`Import API returned invalid JSON (HTTP ${res.status})`)
    }

    if (!res.ok) {
      throw new Error(data?.message || `Import failed (HTTP ${res.status})`)
    }

    return data
  })
}

export async function downloadMrpTemplate(token) {
  const res = await fetch(`${API_BASE}/mrp/plans/template`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error('Template download failed')
  }
  return res.blob()
}
