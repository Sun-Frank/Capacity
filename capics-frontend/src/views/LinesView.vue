<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">生产线配置</h1>
      <p class="page-subtitle">生产线基础参数</p>
    </div>
    <div class="table-wrapper">
      <div style="margin-bottom: 1rem; display: flex; gap: 1rem; align-items: center;">
        <button class="btn btn-primary" @click="showAddModal">添加产线</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>生产线编码</th>
            <th>每周工作天数</th>
            <th>每天班次</th>
            <th>每班时长(小时)</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="line in lines" :key="line.lineCode">
            <td>{{ line.lineCode }}</td>
            <td>{{ line.workingDaysPerWeek }}</td>
            <td>{{ line.shiftsPerDay }}</td>
            <td>{{ line.hoursPerShift }}</td>
            <td>{{ line.isActive ? '启用' : '禁用' }}</td>
            <td>
              <button class="btn btn-small" @click="editLine(line)">编辑</button>
            </td>
          </tr>
          <tr v-if="lines.length === 0">
            <td colspan="6" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <EditLineModal
      :show="showModal"
      :line="selectedLine"
      @close="closeModal"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { getLines, createLine, updateLine } from '@/api/line'
import EditLineModal from '@/components/lines/EditLineModal.vue'

const { token, currentUser } = useAuth()
const lines = ref([])
const showModal = ref(false)
const selectedLine = ref(null)

const loadLines = async () => {
  try {
    const data = await getLines(token.value)
    lines.value = data.data || []
  } catch (err) {
    console.error('Load lines error:', err)
  }
}

const showAddModal = () => {
  selectedLine.value = null
  showModal.value = true
}

const editLine = (line) => {
  selectedLine.value = line
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  selectedLine.value = null
}

const handleConfirm = async (formData) => {
  try {
    const username = currentUser.value || 'system'
    const lineData = {
      ...formData,
      updatedBy: username
    }

    if (selectedLine.value) {
      await updateLine(token.value, selectedLine.value.lineCode, lineData)
    } else {
      await createLine(token.value, lineData)
    }
    closeModal()
    loadLines()
  } catch (err) {
    console.error('Save line error:', err)
    alert('保存失败')
  }
}

onMounted(() => {
  loadLines()
})
</script>
