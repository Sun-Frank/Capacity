<template>
  <div class="topbar">
    <div class="title-wrap">
      <div class="topbar-title">{{ pageTitle }}</div>
      <div v-if="pageSubtitle" class="topbar-subtitle">{{ pageSubtitle }}</div>
    </div>
    <div class="topbar-user">
      <span>{{ currentUser }}</span>
      <button class="btn" @click="onLogout">退出</button>
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

const onLogout = async () => {
  await handleLogout()
  await router.replace('/login')
}

const pageTitles = {
  '/': '仪表盘',
  '/products': '产品主数据',
  '/mrp': 'MRP计划管理',
  '/mrp-compare': 'MRP对比',
  '/routing': '工艺路线',
  '/ct-line': '产线-产品',
  '/lines': '生产线配置',
  '/product-line': '产线一览',
  '/capacity-assessment': '静态产能核算（周）',
  '/capacity-assessment-monthly': '静态产能核算（月）',
  '/capacity-realtime': '动态产能模拟（周）',
  '/capacity-realtime-monthly': '动态产能模拟（月）',
  '/fusion-workbench': '融合工作台',
  '/users': '用户管理',
  '/ai-config': 'AI后端环境配置'
}

const pageSubtitles = {
  '/': '产能评估总览',
  '/products': '基础主数据维护',
  '/mrp': 'MRP导入与版本管理',
  '/mrp-compare': '按产品描述汇总两个文件的数量差异',
  '/routing': '产品工艺路径管理',
  '/ct-line': '产线与产品参数映射',
  '/lines': '线体基础参数维护',
  '/product-line': '产线关联数据总览',
  '/capacity-assessment': '静态核算结果（周）',
  '/capacity-assessment-monthly': '静态核算结果（月）',
  '/capacity-realtime': '可编辑仿真结果（周）',
  '/capacity-realtime-monthly': '可编辑仿真结果（月）',
  '/fusion-workbench': '融合分析与协同',
  '/users': '系统账号与权限管理',
  '/ai-config': 'AI Agent 连接参数维护'
}

const pageTitle = computed(() => pageTitles[route.path] || '')
const pageSubtitle = computed(() => pageSubtitles[route.path] || '')
</script>

<style scoped>
.title-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.topbar-subtitle {
  font-size: 12px;
  line-height: 1.2;
  color: var(--muted-foreground);
  letter-spacing: 0.02em;
}
</style>
