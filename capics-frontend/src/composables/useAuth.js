import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getCurrentUser } from '@/api/auth'

const token = ref(localStorage.getItem('capics_token') || '')
const currentUser = ref(localStorage.getItem('capics_user') || '')
const currentUsername = ref(localStorage.getItem('capics_username') || '')
const userId = ref(null)
const isLoggedIn = computed(() => !!token.value)

export function useAuth() {
  const handleLogin = async (username, password) => {
    const data = await apiLogin(username, password)
    if (data.success) {
      token.value = data.data.token
      currentUser.value = data.data.realName || data.data.username
      currentUsername.value = data.data.username
      userId.value = data.data.id
      localStorage.setItem('capics_token', token.value)
      localStorage.setItem('capics_user', currentUser.value)
      localStorage.setItem('capics_username', currentUsername.value)
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
    localStorage.removeItem('capics_token')
    localStorage.removeItem('capics_user')
    localStorage.removeItem('capics_username')
  }

  return {
    token,
    currentUser,
    currentUsername,
    userId,
    isLoggedIn,
    handleLogin,
    handleLogout
  }
}
