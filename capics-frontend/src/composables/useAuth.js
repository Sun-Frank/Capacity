import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getCurrentUser } from '@/api/auth'

const token = ref(localStorage.getItem('capics_token') || '')
const currentUser = ref(localStorage.getItem('capics_user') || '')
const currentUsername = ref(localStorage.getItem('capics_username') || '')
const userId = ref(Number(localStorage.getItem('capics_user_id') || 0) || null)
const roleCodes = ref(parseRoleCodes(localStorage.getItem('capics_role_codes')))
const isLoggedIn = computed(() => !!token.value)
const isAdmin = computed(() => roleCodes.value.includes('ADMIN'))

function parseRoleCodes(raw) {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((v) => String(v || '').trim().toUpperCase())
      .filter((v) => v)
  } catch (e) {
    return []
  }
}

export function useAuth() {
  const handleLogin = async (username, password) => {
    const data = await apiLogin(username, password)
    if (data.success) {
      token.value = data.data.token
      currentUser.value = data.data.realName || data.data.username
      currentUsername.value = data.data.username
      userId.value = data.data.id
      roleCodes.value = Array.isArray(data.data.roleCodes)
        ? data.data.roleCodes.map((v) => String(v || '').trim().toUpperCase()).filter((v) => v)
        : []
      localStorage.setItem('capics_token', token.value)
      localStorage.setItem('capics_user', currentUser.value)
      localStorage.setItem('capics_username', currentUsername.value)
      localStorage.setItem('capics_user_id', String(userId.value || ''))
      localStorage.setItem('capics_role_codes', JSON.stringify(roleCodes.value))
      return true
    }
    throw new Error(data.message || 'Login failed')
  }

  const handleLogout = async () => {
    try {
      if (token.value) {
        await apiLogout(token.value)
      }
    } catch (e) {
      console.log('Logout API call failed, continuing with local logout')
    }
    token.value = ''
    currentUser.value = ''
    currentUsername.value = ''
    userId.value = null
    roleCodes.value = []
    localStorage.removeItem('capics_token')
    localStorage.removeItem('capics_user')
    localStorage.removeItem('capics_username')
    localStorage.removeItem('capics_user_id')
    localStorage.removeItem('capics_role_codes')
  }

  const hasRole = (roleCode) => roleCodes.value.includes(String(roleCode || '').trim().toUpperCase())
  const hasAnyRole = (codes) => {
    if (!Array.isArray(codes)) return false
    return codes.some((code) => hasRole(code))
  }

  return {
    token,
    currentUser,
    currentUsername,
    userId,
    roleCodes,
    isAdmin,
    isLoggedIn,
    hasRole,
    hasAnyRole,
    handleLogin,
    handleLogout
  }
}
