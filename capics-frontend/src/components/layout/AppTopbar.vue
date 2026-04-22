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
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const { currentUser, handleLogout } = useAuth()

const pageTitles = {
  '/': '仪表盘',
  '/products': '产品主数据',
  '/mrp': 'MRP计划管理',
  '/routing': '工艺路线',
  '/ct-line': '产线-产品',
  '/lines': '生产线配置',
  '/product-line': '产品定线',
  '/capacity-assessment': '静态产能核算（周）',
  '/capacity-assessment-monthly': '静态产能核算（月）',
  '/capacity-realtime': '动态产能模拟（周）',
  '/capacity-realtime-monthly': '动态产能模拟（月）',
  '/fusion-workbench': '融合工作台',
  '/users': '用户管理'
}

const pageTitle = computed(() => pageTitles[route.path] || '')
</script>
