<template>
  <div class="card">
    <div class="card-label">{{ label }}</div>
    <div class="card-value">{{ value }}</div>
    <div v-if="unit" class="card-unit">{{ unit }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  unit: String
})
</script>
