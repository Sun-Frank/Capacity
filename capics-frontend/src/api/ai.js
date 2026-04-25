const API_BASE = '/api'

export function analyzeMrpCompare(token, payload) {
  return fetch(`${API_BASE}/ai/mrp-compare/analyze`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload || {})
  }).then(async (res) => {
    let data = null
    try {
      data = await res.json()
    } catch (error) {
      throw new Error(`AI接口返回异常 (HTTP ${res.status})`)
    }
    if (!res.ok || !data?.success) {
      throw new Error(data?.message || `AI分析失败 (HTTP ${res.status})`)
    }
    return data
  })
}
