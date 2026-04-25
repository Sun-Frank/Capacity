const API_BASE = '/api'

export function getAiBackendConfig(token) {
  return fetch(`${API_BASE}/system/ai-config`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(async (res) => {
    const data = await res.json()
    if (!res.ok || !data?.success) {
      throw new Error(data?.message || `加载AI配置失败 (HTTP ${res.status})`)
    }
    return data
  })
}

export function saveAiBackendConfig(token, payload) {
  return fetch(`${API_BASE}/system/ai-config`, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload || {})
  }).then(async (res) => {
    const data = await res.json()
    if (!res.ok || !data?.success) {
      throw new Error(data?.message || `保存AI配置失败 (HTTP ${res.status})`)
    }
    return data
  })
}

export function testAiBackendConfig(token, payload) {
  return fetch(`${API_BASE}/system/ai-config/test`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload || {})
  }).then(async (res) => {
    const data = await res.json()
    if (!res.ok || !data?.success) {
      throw new Error(data?.message || `测试AI配置失败 (HTTP ${res.status})`)
    }
    return data
  })
}
