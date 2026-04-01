<template>
  <div v-if="toast.show" :class="['toast', toast.type]">
    {{ toast.message }}
  </div>
</template>

<script setup>
import { useToast } from '@/composables/useToast'
const { toast } = useToast()
</script>
