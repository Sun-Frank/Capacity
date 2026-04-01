const API_BASE = '/api'

export function getUsers(token) {
  return fetch(`${API_BASE}/users`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getUserById(token, id) {
  return fetch(`${API_BASE}/users/${id}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function getUserByUsername(token, username) {
  return fetch(`${API_BASE}/users/username/${encodeURIComponent(username)}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function createUser(token, user) {
  return fetch(`${API_BASE}/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(user)
  }).then(res => res.json())
}

export function updateUser(token, id, user) {
  return fetch(`${API_BASE}/users/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(user)
  }).then(res => res.json())
}

export function deleteUser(token, id) {
  return fetch(`${API_BASE}/users/${id}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.json())
}

export function changePassword(token, id, password) {
  return fetch(`${API_BASE}/users/${id}/password`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ password })
  }).then(res => res.json())
}
