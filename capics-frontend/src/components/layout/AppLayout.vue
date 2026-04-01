<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="main-content">
      <AppTopbar />
      <div class="page-content">
        <router-view />
      </div>
    </div>
    <ToastNotification />
  </div>
</template>

<script setup>
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'
import ToastNotification from '@/components/common/ToastNotification.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-left: var(--sidebar-width);
  height: 100vh;
}

.page-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
