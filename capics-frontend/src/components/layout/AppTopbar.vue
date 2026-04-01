<template>
  <div class="topbar">
    <div class="topbar-title">{{ pageTitle }}</div>
    <div class="topbar-user">
      <span>{{ currentUser }}</span>
      <button class="btn" @click="handleLogout">退出</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const { currentUser, handleLogout } = useAuth()

const pageTitles = {
  '/': '仪表盘',
  '/products': '产品主数据',
  '/mrp': 'MRP计划管理',
  '/routing': '工艺路线',
  '/lines': '生产线配置',
  '/product-line': '产品定线',
  '/capacity-assessment': '产能评估',
  '/users': '用户管理'
}

const pageTitle = computed(() => pageTitles[route.path] || '')
</script>
