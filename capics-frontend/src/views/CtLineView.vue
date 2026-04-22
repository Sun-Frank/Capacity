<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">产线-产品</h1>
      <p class="page-subtitle">导入并维护 CT-line 文件指定列数据（B/C/D/F/I/P/W/X）</p>
    </div>

    <div class="table-wrapper">
      <div class="ctline-toolbar">
        <input
          v-model.trim="searchKeyword"
          class="form-input search-input"
          placeholder="搜索生产线 / 物料号 / 主备线 / 修改人"
        />
        <button class="btn btn-primary" @click="showImport = true">导入数据</button>
        <button class="btn" @click="handleDownloadTemplate">模板下载</button>
        <button class="btn" @click="handleExportCtLines">数据导出</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>生产线</th>
            <th>物料号</th>
            <th>主备线</th>
            <th>CT(秒)</th>
            <th>OEE</th>
            <th>人数</th>
            <th>最后修改日期</th>
            <th>最后修改人</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in filteredRows" :key="row.id">
            <template v-if="editingId === row.id">
              <td><input v-model.trim="editForm.colB" class="form-input" /></td>
              <td><input v-model.trim="editForm.colC" class="form-input" /></td>
              <td><input v-model.trim="editForm.colD" class="form-input" /></td>
              <td><input v-model.trim="editForm.colF" class="form-input" /></td>
              <td><input v-model.trim="editForm.colI" class="form-input" /></td>
              <td><input v-model.trim="editForm.colP" class="form-input" /></td>
              <td><input v-model.trim="editForm.colW" class="form-input" /></td>
              <td><input v-model.trim="editForm.colX" class="form-input" /></td>
              <td style="white-space: nowrap;">
                <button class="btn btn-small btn-primary" :disabled="isSaving" @click="saveEdit(row.id)">
                  {{ isSaving ? '保存中...' : '保存' }}
                </button>
                <button class="btn btn-small" :disabled="isSaving" @click="cancelEdit">取消</button>
              </td>
            </template>
            <template v-else>
              <td>{{ row.colB }}</td>
              <td>{{ row.colC }}</td>
              <td>{{ row.colD }}</td>
              <td>{{ row.colF }}</td>
              <td>{{ row.colI }}</td>
              <td>{{ row.colP }}</td>
              <td>{{ row.colW || '-' }}</td>
              <td>{{ row.colX || '-' }}</td>
              <td>
                <button class="btn btn-small" @click="startEdit(row)">编辑</button>
              </td>
            </template>
          </tr>
          <tr v-if="filteredRows.length === 0">
            <td colspan="9" style="text-align: center; color: var(--muted-foreground);">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <ImportModal
      :show="showImport"
      type="ct-line"
      :isImporting="isImporting"
      @close="showImport = false"
      @confirm="handleImport"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import ImportModal from '@/components/common/ImportModal.vue'
import { downloadCtLineTemplate, getCtLines, importCtLines, updateCtLine } from '@/api/ctLine'
import { downloadCsv } from '@/utils/export'

const { token } = useAuth()
const { showToast } = useToast()

const rows = ref([])
const showImport = ref(false)
const isImporting = ref(false)
const searchKeyword = ref('')

const editingId = ref(null)
const isSaving = ref(false)
const editForm = ref({
  colB: '',
  colC: '',
  colD: '',
  colF: '',
  colI: '',
  colP: '',
  colW: '',
  colX: ''
})

const filteredRows = computed(() => {
  const keyword = (searchKeyword.value || '').trim().toLowerCase()
  if (!keyword) {
    return rows.value
  }
  return (rows.value || []).filter((row) => {
    const targets = [row.colB, row.colC, row.colD, row.colX]
    return targets.some((v) => String(v || '').toLowerCase().includes(keyword))
  })
})

const loadRows = async () => {
  try {
    const res = await getCtLines(token.value)
    rows.value = res?.data?.rows || []
  } catch (err) {
    showToast('加载数据失败', 'error')
  }
}

const startEdit = (row) => {
  editingId.value = row.id
  editForm.value = {
    colB: row.colB || '',
    colC: row.colC || '',
    colD: row.colD || '',
    colF: row.colF || '',
    colI: row.colI || '',
    colP: row.colP || '',
    colW: row.colW || '',
    colX: row.colX || ''
  }
}

const cancelEdit = () => {
  editingId.value = null
}

const saveEdit = async (id) => {
  if (!editForm.value.colB || !editForm.value.colC || !editForm.value.colD || !editForm.value.colF || !editForm.value.colI || !editForm.value.colP) {
    showToast('生产线、物料号、主备线、CT(秒)、OEE、人数不能为空', 'warning')
    return
  }

  isSaving.value = true
  try {
    const res = await updateCtLine(token.value, id, editForm.value)
    if (res.success) {
      const idx = rows.value.findIndex((r) => r.id === id)
      if (idx >= 0 && res.data) {
        rows.value[idx] = { ...rows.value[idx], ...res.data }
      } else {
        await loadRows()
      }
      editingId.value = null
      showToast(res.message || '保存成功', 'success')
    } else {
      showToast(res.message || '保存失败', 'error')
    }
  } catch (err) {
    showToast(err?.message || '保存失败', 'error')
  } finally {
    isSaving.value = false
  }
}

const handleImport = async ({ file }) => {
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }

  isImporting.value = true
  try {
    const res = await importCtLines(token.value, file)
    if (res.success) {
      showToast(res.message || '导入成功', 'success')
      showImport.value = false
      await loadRows()
    } else {
      showToast(res.message || '导入失败', 'error')
    }
  } catch (err) {
    showToast(err?.message || '导入失败', 'error')
  } finally {
    isImporting.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const blob = await downloadCtLineTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '产线-产品导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast(err?.message || '模板下载失败', 'error')
  }
}

const handleExportCtLines = () => {
  const headers = [
    { key: 'colB', label: '生产线' },
    { key: 'colC', label: '物料号' },
    { key: 'colD', label: '主备线' },
    { key: 'colF', label: 'CT(秒)' },
    { key: 'colI', label: 'OEE' },
    { key: 'colP', label: '人数' },
    { key: 'colW', label: '最后修改日期' },
    { key: 'colX', label: '最后修改人' }
  ]
  const exportRows = (rows.value || []).map((r) => ({
    colB: r.colB || '',
    colC: r.colC || '',
    colD: r.colD || '',
    colF: r.colF || '',
    colI: r.colI || '',
    colP: r.colP || '',
    colW: r.colW || '',
    colX: r.colX || ''
  }))
  downloadCsv('产线-产品.csv', headers, exportRows)
  showToast('导出成功', 'success')
}

onMounted(loadRows)
</script>

<style scoped>
.ctline-toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 1rem;
  display: flex;
  gap: 0.75rem;
  align-items: center;
  padding: 0.5rem 0;
  background: var(--surface, #fff);
}

.search-input {
  width: 320px;
  max-width: 40vw;
}

.table-wrapper thead th {
  top: 56px;
}
</style>
