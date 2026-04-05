<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>编辑编码族</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>编码族</label>
          <input type="text" :value="form.familyCode" class="form-input" readonly />
        </div>
        <div class="form-group">
          <label>生产线</label>
          <input type="text" :value="form.lineCode" class="form-input" readonly />
        </div>
        <div class="form-group">
          <label>描述</label>
          <input type="text" v-model="form.description" class="form-input" />
        </div>
        <div class="form-group">
          <label>PF</label>
          <input type="text" v-model="form.pf" class="form-input" />
        </div>
        <div class="form-group">
          <label>编码规则</label>
          <input type="text" v-model="form.codingRule" class="form-input" />
        </div>
        <div class="form-group">
          <label>周期时间(秒)</label>
          <input type="number" step="0.01" v-model="form.cycleTime" class="form-input" />
        </div>
        <div class="form-group">
          <label>OEE(%)</label>
          <input type="number" step="0.01" v-model="form.oee" class="form-input" />
        </div>
        <div class="form-group">
          <label>人数</label>
          <input type="number" v-model="form.workerCount" class="form-input" />
        </div>
        <div class="modal-footer">
          <button type="button" class="btn" @click="$emit('close')">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: Boolean,
  family: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'confirm'])

const form = ref({
  familyCode: '',
  lineCode: '',
  description: '',
  pf: '',
  codingRule: '',
  cycleTime: '',
  oee: '',
  workerCount: ''
})

const isSubmitting = ref(false)

watch(() => props.family, (newFamily) => {
  if (newFamily) {
    form.value = {
      familyCode: newFamily.familyCode || '',
      lineCode: newFamily.lineCode || '',
      description: newFamily.description || '',
      pf: newFamily.pf || '',
      codingRule: newFamily.codingRule || '',
      cycleTime: newFamily.cycleTime || '',
      oee: newFamily.oee || '',
      workerCount: newFamily.workerCount || ''
    }
  }
}, { immediate: true })

const handleSubmit = async () => {
  isSubmitting.value = true
  try {
    emit('confirm', { ...form.value })
  } finally {
    isSubmitting.value = false
  }
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
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--muted-foreground);
}

.modal-close:hover {
  color: var(--foreground);
}

form {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input {
  width: 100%;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
