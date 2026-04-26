<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>物料号</th>
          <th>生产线</th>
          <th>编码族</th>
          <th>PF</th>
          <th>描述</th>
          <th>CT(秒)</th>
          <th>OEE(%)</th>
          <th>人数</th>
          <th v-if="canEdit">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in products" :key="rowKey(p)">
          <td>{{ p.itemNumber }}</td>
          <td>{{ p.lineCode }}</td>

          <template v-if="canEdit && isEditing(p)">
            <td><input class="table-input" v-model="editForm.familyCode" /></td>
            <td>{{ p.pf || '-' }}</td>
            <td><input class="table-input" v-model="editForm.description" /></td>
            <td><input class="table-input" v-model.number="editForm.cycleTime" type="number" step="0.01" /></td>
            <td><input class="table-input" v-model.number="editForm.oee" type="number" step="0.01" /></td>
            <td><input class="table-input" v-model.number="editForm.workerCount" type="number" step="1" /></td>
            <td>
              <button class="btn btn-small btn-primary" @click="saveEdit(p)">保存</button>
              <button class="btn btn-small" @click="cancelEdit">取消</button>
            </td>
          </template>

          <template v-else>
            <td>{{ p.familyCode }}</td>
            <td>{{ p.pf || '-' }}</td>
            <td>{{ p.description }}</td>
            <td>{{ p.cycleTime }}</td>
            <td>{{ formatOee(p.oee) }}</td>
            <td>{{ p.workerCount }}</td>
            <td v-if="canEdit">
              <button class="btn btn-small" @click="startEdit(p)">编辑</button>
            </td>
          </template>
        </tr>
        <tr v-if="products.length === 0">
          <td :colspan="canEdit ? 9 : 8" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  canEdit: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['save'])

const editingKey = ref('')
const editForm = ref({
  familyCode: '',
  description: '',
  cycleTime: null,
  oee: null,
  workerCount: null
})

watch(() => props.canEdit, (canEdit) => {
  if (!canEdit) {
    editingKey.value = ''
  }
})

const rowKey = (p) => `${p.itemNumber}::${p.lineCode}`

const isEditing = (p) => editingKey.value === rowKey(p)

const startEdit = (p) => {
  if (!props.canEdit) return
  editingKey.value = rowKey(p)
  editForm.value = {
    familyCode: p.familyCode || '',
    description: p.description || '',
    cycleTime: p.cycleTime ?? null,
    oee: p.oee ?? null,
    workerCount: p.workerCount ?? null,
    version: p.version || ''
  }
}

const cancelEdit = () => {
  editingKey.value = ''
}

const saveEdit = (p) => {
  if (!props.canEdit) return
  emit('save', {
    itemNumber: p.itemNumber,
    lineCode: p.lineCode,
    data: {
      ...p,
      familyCode: editForm.value.familyCode,
      description: editForm.value.description,
      cycleTime: editForm.value.cycleTime,
      oee: editForm.value.oee,
      workerCount: editForm.value.workerCount,
      version: editForm.value.version
    },
    done: cancelEdit
  })
}

const formatOee = (value) => {
  if (value === null || value === undefined) return '-'
  const num = parseFloat(value)
  if (Number.isNaN(num)) return '-'
  return `${num.toFixed(2)}%`
}
</script>

<style scoped>
.table-input {
  width: 100%;
  min-width: 80px;
  border: 1px solid var(--border, #dcdfe6);
  border-radius: 6px;
  padding: 0.25rem 0.4rem;
  font-size: 0.85rem;
}
</style>
