<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>编辑生产线</h3>
        <button class="btn-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <div style="margin-bottom: 1rem;">
          <label class="form-label">成品物料号</label>
          <div class="form-value">{{ item?.productNumber }}</div>
        </div>
        <div style="margin-bottom: 1rem;">
          <label class="form-label">组件物料号</label>
          <div class="form-value">{{ item?.componentNumber }}</div>
        </div>
        <div style="margin-bottom: 1rem;">
          <label class="form-label">当前生产线</label>
          <div class="form-value">{{ item?.lineCode }}</div>
        </div>
        <div style="margin-bottom: 1rem;">
          <label class="form-label">新生产线</label>
          <BaseSelect
            v-model="selectedLine"
            :options="lines"
            placeholder="请选择生产线"
          />
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="$emit('close')">取消</button>
        <button class="btn btn-primary" @click="handleConfirm" :disabled="!selectedLine">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseSelect from '@/components/common/BaseSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  item: {
    type: Object,
    default: null
  },
  lines: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'confirm'])

const selectedLine = ref('')

watch(() => props.show, (newVal) => {
  if (newVal) {
    selectedLine.value = ''
  }
})

watch(() => props.item, (newItem) => {
  if (newItem) {
    selectedLine.value = ''
  }
})

const handleConfirm = () => {
  if (!selectedLine.value) return
  emit('confirm', {
    id: props.item?.id,
    lineCode: selectedLine.value
  })
}
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

.modal {
  background: var(--background);
  border-radius: 8px;
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.125rem;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--muted-foreground);
}

.btn-close:hover {
  color: var(--foreground);
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border);
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: var(--muted-foreground);
}

.form-value {
  padding: 0.5rem 0;
  color: var(--foreground);
}
</style>
