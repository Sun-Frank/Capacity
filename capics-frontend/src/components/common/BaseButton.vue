<template>
  <button :class="['btn', variantClass]" :disabled="disabled" @click="$emit('click')">
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'primary', 'secondary', 'yellow'].includes(v)
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])

const variantClass = computed(() => {
  if (props.variant === 'primary') return 'btn-primary'
  if (props.variant === 'secondary') return 'btn-secondary'
  if (props.variant === 'yellow') return 'btn-yellow'
  return ''
})
</script>
