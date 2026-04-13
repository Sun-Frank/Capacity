<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1 class="login-title">CAPICS</h1>
        <p class="login-subtitle">产能评估系统</p>
      </div>
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input type="text" class="form-input" v-model="username" placeholder="Enter username" required>
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input type="password" class="form-input" v-model="password" placeholder="Enter password" required>
        </div>
        <button type="submit" class="btn btn-primary" style="width: 100%;" :disabled="isSubmitting">
          {{ isSubmitting ? '登录中...' : '登录' }}
        </button>
        <p v-if="loginError" class="login-error">{{ loginError }}</p>
      </form>
      <div class="login-footer">
        Minimalist Monochrome Design · 2026
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { handleLogin: authLogin } = useAuth()
const { showToast } = useToast()

const username = ref('')
const password = ref('')
const isSubmitting = ref(false)
const loginError = ref('')

const handleLogin = async () => {
  if (isSubmitting.value) return
  isSubmitting.value = true
  loginError.value = ''
  try {
    await authLogin(username.value, password.value)
    showToast('登录成功', 'success')
    await router.replace('/')
    setTimeout(() => {
      if (window.location.pathname === '/login') {
        window.location.replace('/')
      }
    }, 100)
  } catch (err) {
    const message = err?.message || '登录失败，请检查账号密码'
    loginError.value = message
    showToast('登录失败: ' + message, 'error')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.login-error {
  margin-top: 0.75rem;
  color: #b91c1c;
  font-size: 0.9rem;
}
</style>
