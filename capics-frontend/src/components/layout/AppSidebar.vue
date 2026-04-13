<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="sidebar-title">CAPICS</div>
      <div class="sidebar-subtitle">产能评估系统</div>
    </div>
    <ul class="nav-menu">
      <li class="nav-section">生产管理</li>
      <li class="nav-item"><router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">仪表盘</router-link></li>
      <li class="nav-item"><router-link to="/products" class="nav-link" :class="{ active: $route.path === '/products' }">产品主数据</router-link></li>
      <li class="nav-item"><router-link to="/mrp" class="nav-link" :class="{ active: $route.path === '/mrp' }">MRP计划</router-link></li>
      <li class="nav-item"><router-link to="/routing" class="nav-link" :class="{ active: $route.path === '/routing' }">工艺路线</router-link></li>

      <li class="nav-section">生产线</li>
      <li class="nav-item"><router-link to="/lines" class="nav-link" :class="{ active: $route.path === '/lines' }">生产线配置</router-link></li>
      <li class="nav-item"><router-link to="/product-line" class="nav-link" :class="{ active: $route.path === '/product-line' }">产线一览</router-link></li>

      <li class="nav-section">分析</li>
      <li class="nav-item"><router-link to="/capacity-assessment" class="nav-link" :class="{ active: $route.path === '/capacity-assessment' }">静态产能核算（周）</router-link></li>
      <li class="nav-item"><router-link to="/capacity-assessment-monthly" class="nav-link" :class="{ active: $route.path === '/capacity-assessment-monthly' }">静态产能核算（月）</router-link></li>
      <li class="nav-item"><router-link to="/capacity-realtime" class="nav-link" :class="{ active: $route.path === '/capacity-realtime' }">动态产能模拟（周）</router-link></li>
      <li class="nav-item"><router-link to="/capacity-realtime-monthly" class="nav-link" :class="{ active: $route.path === '/capacity-realtime-monthly' }">动态产能模拟（月）</router-link></li>
      <li class="nav-item"><router-link to="/fusion-workbench" class="nav-link" :class="{ active: $route.path === '/fusion-workbench' }">融合工作台</router-link></li>

      <li class="nav-section">系统</li>
      <li class="nav-item"><router-link to="/users" class="nav-link" :class="{ active: $route.path === '/users' }">用户管理</router-link></li>
    </ul>
  </aside>
</template>

<script setup>
</script>
