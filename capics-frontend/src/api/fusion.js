const API_BASE = '/api'

export function getLineProfiles(token) {
  return fetch(`${API_BASE}/fusion/line-profiles`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}

export function saveLineProfile(token, lineCode, payload) {
  return fetch(`${API_BASE}/fusion/line-profiles/${encodeURIComponent(lineCode)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  }).then(res => res.json())
}

export function getManpowerPlans(token, lineClass = '') {
  const q = lineClass ? `?lineClass=${encodeURIComponent(lineClass)}` : ''
  return fetch(`${API_BASE}/fusion/manpower-plans${q}`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}

export function createManpowerPlan(token, payload) {
  return fetch(`${API_BASE}/fusion/manpower-plans`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  }).then(res => res.json())
}

export function deleteManpowerPlan(token, id) {
  return fetch(`${API_BASE}/fusion/manpower-plans/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}

export function getMeetingMinutes(token, mpsVersion = '') {
  const q = mpsVersion ? `?mpsVersion=${encodeURIComponent(mpsVersion)}` : ''
  return fetch(`${API_BASE}/fusion/meeting-minutes${q}`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}

export function createMeetingMinutes(token, payload) {
  return fetch(`${API_BASE}/fusion/meeting-minutes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  }).then(res => res.json())
}

export function deleteMeetingMinutes(token, id) {
  return fetch(`${API_BASE}/fusion/meeting-minutes/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` }
  }).then(res => res.json())
}
