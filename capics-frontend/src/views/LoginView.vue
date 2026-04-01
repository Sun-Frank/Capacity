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
        <button type="submit" class="btn btn-primary" style="width: 100%;">登录</button>
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

const handleLogin = async () => {
  try {
    await authLogin(username.value, password.value)
    showToast('登录成功', 'success')
    router.push('/')
  } catch (err) {
    showToast('登录失败: ' + err.message, 'error')
  }
}
</script>
