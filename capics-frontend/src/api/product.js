const API_BASE = '/api'

async function parseJsonSafe(res) {
  try {
    return await res.json()
  } catch (e) {
    return null
  }
}

async function postFormExpectApi(url, token, formData, fallbackMessage) {
  let res
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData
    })
  } catch (e) {
    const reason = e?.message || 'Network error'
    throw new Error(`网络请求失败（${reason}）。请确认前端可访问 /api，且后端服务已启动。`)
  }
  const json = await parseJsonSafe(res)
  if (!res.ok) {
    if (res.status === 413) {
      throw new Error('上传文件过大（HTTP 413），请减小文件或提高网关/后端上传大小限制。')
    }
    const msg = json?.message || json?.error || fallbackMessage
    throw new Error(msg)
  }
  return json || { success: false, message: fallbackMessage }
}

export function getFamilies(token) {
  return fetch(`${API_BASE}/products/families`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function searchFamilies(token, keyword) {
  const url = keyword
    ? `${API_BASE}/products/families/search?keyword=${encodeURIComponent(keyword)}`
    : `${API_BASE}/products/families`
  return fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getProducts(token) {
  return fetch(`${API_BASE}/products`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function searchProducts(token, keyword) {
  const url = keyword
    ? `${API_BASE}/products/search?keyword=${encodeURIComponent(keyword)}`
    : `${API_BASE}/products`
  return fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function updateProduct(token, itemNumber, lineCode, data, updatedBy) {
  return fetch(`${API_BASE}/products/${encodeURIComponent(itemNumber)}/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, updatedBy })
  }).then(res => res.json())
}

export function createProduct(token, data, createdBy) {
  return fetch(`${API_BASE}/products`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, createdBy, updatedBy: createdBy })
  }).then(res => res.json())
}

export function importFamilies(token, file, createdBy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('createdBy', createdBy)
  return postFormExpectApi(
    `${API_BASE}/products/families/import`,
    token,
    formData,
    '编码族导入失败，请检查模板与数据格式'
  )
}

export function checkFamilyImportDuplicates(token, file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch(`${API_BASE}/products/families/import/check`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(res => res.json())
}

export function importProducts(token, file, createdBy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('createdBy', createdBy)
  return postFormExpectApi(
    `${API_BASE}/products/import`,
    token,
    formData,
    '产品导入失败，请检查模板与数据格式'
  )
}

export function checkProductImportDuplicates(token, file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch(`${API_BASE}/products/import/check`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(res => res.json())
}

export function updateFamily(token, familyCode, lineCode, data, updatedBy) {
  return fetch(`${API_BASE}/products/families/${encodeURIComponent(familyCode)}/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, updatedBy })
  }).then(res => res.json())
}

export function createFamily(token, data, createdBy) {
  return fetch(`${API_BASE}/products/families`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, createdBy, updatedBy: createdBy })
  }).then(res => res.json())
}

// Family Lines API
export function getFamilyLines(token) {
  return fetch(`${API_BASE}/products/family-lines`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function searchFamilyLines(token, keyword) {
  const url = keyword
    ? `${API_BASE}/products/family-lines/search?keyword=${encodeURIComponent(keyword)}`
    : `${API_BASE}/products/family-lines`
  return fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function importFamilyLines(token, file, createdBy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('createdBy', createdBy)
  return postFormExpectApi(
    `${API_BASE}/products/family-lines/import`,
    token,
    formData,
    '编码族定线导入失败，请检查模板与数据格式'
  )
}

export function checkFamilyLineImportDuplicates(token, file) {
  const formData = new FormData()
  formData.append('file', file)
  return fetch(`${API_BASE}/products/family-lines/import/check`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(res => res.json())
}

export function updateFamilyLine(token, familyCode, lineCode, data, updatedBy) {
  return fetch(`${API_BASE}/products/family-lines/${encodeURIComponent(familyCode)}/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, updatedBy })
  }).then(res => res.json())
}

export function createFamilyLine(token, data, createdBy) {
  return fetch(`${API_BASE}/products/family-lines`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ...data, createdBy, updatedBy: createdBy })
  }).then(res => res.json())
}

export async function downloadFamilyTemplate(token) {
  const res = await fetch(`${API_BASE}/products/families/template`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error('模板下载失败')
  }
  return res.blob()
}

export async function downloadFamilyLineTemplate(token) {
  const res = await fetch(`${API_BASE}/products/family-lines/template`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error('模板下载失败')
  }
  return res.blob()
}

export async function downloadProductTemplate(token) {
  const res = await fetch(`${API_BASE}/products/template`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) {
    throw new Error('模板下载失败')
  }
  return res.blob()
}
