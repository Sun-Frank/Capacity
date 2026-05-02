<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>{{ line ? '编辑生产线' : '添加生产线' }}</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      <form @submit.prevent="handleSubmit">
        <div class="form-group"><label>生产线编码</label><input type="text" v-model="form.lineCode" class="form-input" required :disabled="!!line" /></div>
        <div class="form-group"><label>生产线名称</label><input type="text" v-model="form.lineName" class="form-input" /></div>
        <div class="form-group"><label>工艺段</label><input type="text" v-model="form.processSegment" class="form-input" placeholder="FA / PCBA / TEST" /></div>
        <div class="form-group"><label>每周工作天数</label><input type="number" v-model="form.workingDaysPerWeek" class="form-input" min="1" max="7" required /></div>
        <div class="form-group"><label>每天班次</label><input type="number" v-model="form.shiftsPerDay" class="form-input" min="1" max="10" required /></div>
        <div class="form-group"><label>每班时长(小时)</label><input type="number" step="0.1" v-model="form.hoursPerShift" class="form-input" min="0.1" max="24" required /></div>
        <div class="form-group"><label>状态</label><select v-model="form.isActive" class="form-input"><option :value="true">启用</option><option :value="false">禁用</option></select></div>
        <div class="modal-footer"><button type="button" class="btn" @click="$emit('close')">取消</button><button type="submit" class="btn btn-primary" :disabled="isSubmitting">{{ isSubmitting ? '保存中...' : '保存' }}</button></div>
      </form>
    </div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ show: Boolean, line: { type: Object, default: null } })
const emit = defineEmits(['close', 'confirm'])
const emptyForm = () => ({ lineCode: '', lineName: '', processSegment: '', workingDaysPerWeek: 5, shiftsPerDay: 2, hoursPerShift: 8.0, isActive: true })
const form = ref(emptyForm())
const isSubmitting = ref(false)
watch(() => props.line, (newLine) => { form.value = newLine ? { ...emptyForm(), ...newLine } : emptyForm() }, { immediate: true })
const handleSubmit = async () => { isSubmitting.value = true; try { emit('confirm', { ...form.value }) } finally { isSubmitting.value = false } }
</script>
<style scoped>.modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center;z-index:1000}.modal{background:#fff;border:1px solid var(--border-color);border-radius:12px;width:90%;max-width:520px;max-height:90vh;overflow-y:auto;box-shadow:0 20px 60px rgba(15,23,42,.24)}.modal-header{display:flex;justify-content:space-between;align-items:center;padding:1rem 1.5rem;border-bottom:1px solid var(--border-color)}.modal-header h3{margin:0;font-size:1.25rem}.modal-close{background:none;border:0;font-size:1.5rem;cursor:pointer;color:var(--muted-foreground)}form{padding:1.5rem}.form-group{margin-bottom:1rem}.form-group label{display:block;margin-bottom:.5rem;font-weight:700}.form-group input,.form-group select{width:100%}.modal-footer{display:flex;justify-content:flex-end;gap:1rem;margin-top:1.5rem;padding-top:1rem;border-top:1px solid var(--border-color)}</style>