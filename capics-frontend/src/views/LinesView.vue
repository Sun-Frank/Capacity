<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">生产线配置</h1>
      <p class="page-subtitle">生产线基础参数</p>
    </div>

    <div class="table-wrapper">
      <div style="margin-bottom: 1rem; display: flex; gap: 1rem; align-items: center;">
        <button class="btn btn-primary" @click="showAddModal">添加产线</button>
        <button class="btn btn-primary" @click="showImportModal">批量导入</button>
        <button class="btn" @click="handleDownloadTemplate">模板下载</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>生产线编码</th>
            <th>生产线名称</th>
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
            <td>{{ line.lineName || '-' }}</td>
            <td>{{ line.workingDaysPerWeek }}</td>
            <td>{{ line.shiftsPerDay }}</td>
            <td>{{ line.hoursPerShift }}</td>
            <td>{{ line.isActive ? '启用' : '禁用' }}</td>
            <td>
              <button class="btn btn-small" @click="editLine(line)">编辑</button>
            </td>
          </tr>
          <tr v-if="lines.length === 0">
            <td colspan="7" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
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

    <ImportModal
      :show="showImport"
      type="line"
      :isImporting="isImporting"
      @close="showImport = false"
      @confirm="handleImport"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getLines, createLine, updateLine, importLines, downloadLineTemplate } from '@/api/line'
import EditLineModal from '@/components/lines/EditLineModal.vue'
import ImportModal from '@/components/common/ImportModal.vue'

const { token, currentUser } = useAuth()
const { showToast } = useToast()
const lines = ref([])
const showModal = ref(false)
const selectedLine = ref(null)
const showImport = ref(false)
const isImporting = ref(false)

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

const showImportModal = () => {
  showImport.value = true
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
    showToast('保存失败', 'error')
  }
}

const handleImport = async ({ file }) => {
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }

  isImporting.value = true
  try {
    const result = await importLines(token.value, file)
    if (result?.success) {
      showToast(result.message || '导入成功', 'success')
      showImport.value = false
      await loadLines()
    } else {
      showToast(result?.message || '导入失败', 'error')
    }
  } catch (err) {
    showToast(err?.message || '导入失败', 'error')
  } finally {
    isImporting.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const blob = await downloadLineTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '生产线配置导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast(err?.message || '模板下载失败', 'error')
  }
}

onMounted(() => {
  loadLines()
})
</script>
