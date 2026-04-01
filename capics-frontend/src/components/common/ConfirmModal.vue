<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="confirm-modal">
      <div class="confirm-header">
        <h3>{{ title }}</h3>
      </div>
      <div class="confirm-body">
        <p v-for="(item, index) in items" :key="index" class="confirm-item">
          {{ item }}
        </p>
      </div>
      <div class="confirm-footer">
        <button class="btn" @click="$emit('cancel')">取消</button>
        <button class="btn btn-primary" @click="$emit('confirm')">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认操作'
  },
  items: {
    type: Array,
    default: () => []
  }
})

defineEmits(['confirm', 'cancel'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-modal {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.confirm-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.confirm-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--foreground);
}

.confirm-body {
  padding: 1.5rem;
  max-height: 300px;
  overflow-y: auto;
}

.confirm-item {
  margin: 0.5rem 0;
  padding: 0.5rem;
  background: var(--background-secondary);
  border-radius: 4px;
  font-size: 0.9rem;
  word-break: break-all;
}

.confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-color);
}
</style>
