<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">仪表盘</h1>
      <p class="page-subtitle">产能评估系统总览</p>
    </div>

    <div class="filters-row">
      <div class="source-toggle">
        <button class="toggle-btn" :class="{ active: dataSource === 'static' }" @click="setDataSource('static')">静态产能核算</button>
        <button class="toggle-btn" :class="{ active: dataSource === 'dynamic' }" @click="setDataSource('dynamic')">动态产能模拟</button>
      </div>

      <div class="source-toggle">
        <button class="toggle-btn" :class="{ active: dimension === 'week' }" @click="setDimension('week')">周</button>
        <button class="toggle-btn" :class="{ active: dimension === 'month' }" @click="setDimension('month')">月</button>
      </div>

      <BaseSelect
        v-model="selectedCreatedBy"
        :options="createdBys.map(c => ({ value: c, label: c }))"
        placeholder="选择导入人"
        @update:modelValue="onCreatedByChange"
      />
      <BaseSelect
        v-model="selectedFileName"
        :options="fileNames.map(f => ({ value: f, label: f }))"
        placeholder="选择文件"
        :disabled="!selectedCreatedBy"
        @update:modelValue="onFileNameChange"
      />
      <BaseSelect
        v-model="selectedVersion"
        :options="versions.map(v => ({ value: v, label: v }))"
        placeholder="选择版本"
        :disabled="!selectedFileName"
        @update:modelValue="onVersionChange"
      />

      <BaseSelect
        v-if="dataSource === 'static'"
        v-model="selectedStaticSnapshot"
        :options="staticSnapshotNames.map(n => ({ value: n, label: n }))"
        placeholder="已计算结果版本"
        :disabled="!selectedVersion"
      />

      <BaseSelect
        v-if="dataSource === 'dynamic'"
        v-model="selectedDynamicSnapshot"
        :options="dynamicSnapshotNames.map(n => ({ value: n, label: n }))"
        placeholder="选择快照"
        :disabled="!selectedVersion"
      />

      <button class="btn btn-primary" @click="loadData" :disabled="loading">{{ loading ? '加载中...' : '加载数据' }}</button>
      <button class="btn" @click="handleExportDashboard" :disabled="rows.length === 0">数据导出</button>
    </div>

    <div v-if="warnings.length > 0" class="warnings-container">
      <div class="warning-header">高负载预警：</div>
      <ul class="warning-list">
        <li v-for="(warning, idx) in warnings" :key="idx">{{ warning }}</li>
      </ul>
    </div>

    <div class="table-wrapper">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="rows.length === 0" class="empty-state">请选择筛选条件并点击"加载数据"</div>
      <div v-else class="table-scroll">
        <table class="main-table">
          <thead>
            <tr>
              <th class="sticky-col">生产线</th>
              <th v-for="date in dates" :key="date" class="date-header">{{ getDateLabel(date) }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.lineCode" :class="{ 'total-row': row.isTotal, 'group-row': row.group && !row.isTotal }">
              <td class="sticky-col line-name" :class="{ 'total-name': row.isTotal }">{{ formatLineLabel(row.lineCode) }}</td>
              <td
                v-for="(loadingValue, idx) in row.loadings"
                :key="dates[idx]"
                class="loading-cell"
                :class="{ 'high-load': loadingValue > row.threshold, 'warning-cell': row.isTotal && loadingValue > row.threshold }"
              >
                {{ formatLoading(loadingValue) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { downloadCsv } from '@/utils/export'
import { getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/dashboard'
import { getSnapshot, getSnapshotNames } from '@/api/simulationSnapshot'
import { getLines } from '@/api/line'
import BaseSelect from '@/components/common/BaseSelect.vue'

const { token } = useAuth()
const { showToast } = useToast()

const dataSource = ref('static')
const dimension = ref('week')

const createdBys = ref([])
const fileNames = ref([])
const versions = ref([])
const selectedCreatedBy = ref('')
const selectedFileName = ref('')
const selectedVersion = ref('')

const staticSnapshotNames = ref([])
const selectedStaticSnapshot = ref('')
const dynamicSnapshotNames = ref([])
const selectedDynamicSnapshot = ref('')

const rows = ref([])
const dates = ref([])
const dateLabels = ref({})
const warnings = ref([])
const lineNameMap = ref({})

const loading = ref(false)
const error = ref('')

const setDataSource = async (source) => {
  dataSource.value = source
  selectedStaticSnapshot.value = ''
  selectedDynamicSnapshot.value = ''
  if (selectedVersion.value) {
    await loadSnapshotNames(source)
  }
}

const setDimension = async (dim) => {
  dimension.value = dim
  selectedStaticSnapshot.value = ''
  selectedDynamicSnapshot.value = ''
  if (selectedVersion.value) {
    await loadSnapshotNames(dataSource.value)
  }
}

const getDateLabel = (date) => dateLabels.value[date] || date

const formatLineLabel = (lineCode) => {
  if (!lineCode) return ''
  if (lineCode.includes('TOTAL')) return lineCode
  const lineName = lineNameMap.value[lineCode]
  return lineName ? `${lineCode} - ${lineName}` : lineCode
}

const formatLoading = (loadingValue) => {
  if (loadingValue === 0) return '-'
  return (loadingValue * 100).toFixed(0) + '%'
}

const loadCreatedByOptions = async () => {
  try {
    const data = await getCreatedBys(token.value)
    createdBys.value = data.data || []
  } catch (err) {
    console.error('Load createdBys error:', err)
  }
}

const loadLineNames = async () => {
  try {
    const data = await getLines(token.value)
    if (data.success && data.data) {
      const names = {}
      data.data.forEach((line) => {
        names[line.lineCode] = line.lineName || ''
      })
      lineNameMap.value = names
    }
  } catch (err) {
    console.error('Load line names error:', err)
  }
}

const onCreatedByChange = async () => {
  selectedFileName.value = ''
  selectedVersion.value = ''
  fileNames.value = []
  versions.value = []
  selectedStaticSnapshot.value = ''
  selectedDynamicSnapshot.value = ''
  staticSnapshotNames.value = []
  dynamicSnapshotNames.value = []
  if (!selectedCreatedBy.value) return

  try {
    const data = await getFileNamesByCreatedBy(token.value, selectedCreatedBy.value)
    fileNames.value = data.data || []
  } catch (err) {
    console.error('Load fileNames error:', err)
  }
}

const onFileNameChange = async () => {
  selectedVersion.value = ''
  versions.value = []
  selectedStaticSnapshot.value = ''
  selectedDynamicSnapshot.value = ''
  staticSnapshotNames.value = []
  dynamicSnapshotNames.value = []
  if (!selectedCreatedBy.value || !selectedFileName.value) return

  try {
    const data = await getVersionsByCreatedByAndFileName(token.value, selectedCreatedBy.value, selectedFileName.value)
    versions.value = data.data || []
  } catch (err) {
    console.error('Load versions error:', err)
  }
}

const onVersionChange = async () => {
  if (!selectedVersion.value) return
  await loadSnapshotNames(dataSource.value)
}

const loadSnapshotNames = async (source) => {
  const sourceType = source || dataSource.value
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) return

  try {
    const data = await getSnapshotNames(
      token.value,
      selectedCreatedBy.value,
      selectedFileName.value,
      selectedVersion.value,
      sourceType,
      dimension.value
    )
    if (sourceType === 'dynamic') {
      dynamicSnapshotNames.value = data.data || []
    } else {
      staticSnapshotNames.value = data.data || []
    }
  } catch (err) {
    console.error('Load snapshot names error:', err)
  }
}

watch(selectedVersion, async () => {
  if (selectedVersion.value) {
    await loadSnapshotNames(dataSource.value)
  }
})

const transformSnapshotToRows = (snapshot) => {
  const linesData = snapshot.linesData || {}
  const datesList = snapshot.dates || []
  const dateLabelsMap = snapshot.dateLabels || {}

  const newRows = []
  const groupLines = {}
  const groupLoadings = {}

  for (const [lineCode, items] of Object.entries(linesData)) {
    const group = lineCode.startsWith('SMT') ? 'SMT' : null
    const lineLoadings = []
    for (const date of datesList) {
      let totalLoad = 0
      for (const item of items) {
        const loadVal = item[date + '_loading']
        if (loadVal) totalLoad += typeof loadVal === 'number' ? loadVal : parseFloat(loadVal)
      }
      lineLoadings.push(totalLoad)
    }

    newRows.push({ lineCode, loadings: lineLoadings, isTotal: false, group, threshold: 1.0 })

    if (group) {
      if (!groupLines[group]) {
        groupLines[group] = []
        groupLoadings[group] = new Array(datesList.length).fill(0)
      }
      groupLines[group].push(lineCode)
      for (let i = 0; i < lineLoadings.length; i += 1) {
        groupLoadings[group][i] += lineLoadings[i]
      }
    }
  }

  for (const [group, loadings] of Object.entries(groupLoadings)) {
    newRows.push({
      lineCode: group + ' TOTAL',
      loadings,
      isTotal: true,
      group,
      threshold: groupLines[group].length * 1.0
    })
  }

  newRows.sort((a, b) => {
    const groupA = a.group || a.lineCode
    const groupB = b.group || b.lineCode
    const groupCompare = -groupA.localeCompare(groupB)
    if (groupCompare !== 0) return groupCompare
    if (a.isTotal && !b.isTotal) return 1
    if (!a.isTotal && b.isTotal) return -1
    return -a.lineCode.localeCompare(b.lineCode)
  })

  return { rows: newRows, dates: datesList, dateLabels: dateLabelsMap }
}

const loadData = async () => {
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    showToast('请选择完整的筛选条件（导入人、文件、版本）', 'warning')
    return
  }

  const currentSnapshotName = dataSource.value === 'dynamic' ? selectedDynamicSnapshot.value : selectedStaticSnapshot.value
  if (!currentSnapshotName) {
    showToast('请选择已计算结果版本', 'warning')
    return
  }

  loading.value = true
  error.value = ''

  try {
    const snapshotData = await getSnapshot(
      token.value,
      selectedCreatedBy.value,
      selectedFileName.value,
      selectedVersion.value,
      currentSnapshotName,
      dataSource.value,
      dimension.value
    )

    if (snapshotData.success) {
      const converted = transformSnapshotToRows(snapshotData.data || {})
      rows.value = converted.rows
      dates.value = converted.dates
      dateLabels.value = converted.dateLabels
      warnings.value = []
    } else {
      error.value = snapshotData.message || '加载快照失败'
      showToast('加载快照失败: ' + (snapshotData.message || ''), 'error')
    }
  } catch (err) {
    console.error('Load dashboard data error:', err)
    error.value = '加载数据失败: ' + (err.message || '未知错误')
    showToast('加载数据失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleExportDashboard = () => {
  if (!rows.value.length || !dates.value.length) {
    showToast('暂无可导出数据', 'warning')
    return
  }

  const headers = [{ key: 'lineCode', label: '生产线' }]
  dates.value.forEach((date) => {
    headers.push({
      key: date,
      label: getDateLabel(date)
    })
  })

  const exportRows = rows.value.map((row) => {
    const exportRow = {
      lineCode: formatLineLabel(row.lineCode)
    }
    dates.value.forEach((date, index) => {
      exportRow[date] = row.loadings?.[index] ?? 0
    })
    return exportRow
  })

  const sourceLabel = dataSource.value === 'static' ? '静态' : '动态'
  const dimensionLabel = dimension.value === 'week' ? '周' : '月'
  downloadCsv(`仪表盘-${sourceLabel}-${dimensionLabel}.csv`, headers, exportRows)
  showToast('导出成功', 'success')
}

onMounted(() => {
  loadCreatedByOptions()
  loadLineNames()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--foreground);
}

.page-subtitle {
  color: var(--muted-foreground);
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.filters-row {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.source-toggle {
  display: flex;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.toggle-btn {
  padding: 0.5rem 1rem;
  border: none;
  background: white;
  color: var(--muted-foreground);
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.toggle-btn:not(:last-child) {
  border-right: 1px solid var(--border);
}

.toggle-btn:hover {
  background: var(--muted);
}

.toggle-btn.active {
  background: var(--primary);
  color: white;
}

.warnings-container {
  background: #fee2e2;
  border: 1px solid #dc2626;
  border-radius: var(--radius-md);
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  max-height: 120px;
  overflow-y: auto;
}

.warning-header {
  font-weight: 600;
  color: #991b1b;
  margin-bottom: 0.5rem;
}

.warning-list {
  margin: 0;
  padding-left: 1.5rem;
  color: #7f1d1d;
  font-size: 0.875rem;
}

.table-wrapper {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: white;
}

.table-scroll {
  overflow: auto;
  max-height: calc(100vh - 280px);
}

.main-table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
}

.main-table th,
.main-table td {
  border: 1px solid var(--border);
  padding: 0.5rem 0.75rem;
  white-space: nowrap;
  text-align: center;
}

.main-table th {
  background: var(--muted);
  font-weight: 600;
}

.sticky-col {
  position: sticky;
  left: 0;
  z-index: 3;
  background: white;
  min-width: 160px;
  text-align: left;
}

.date-header {
  min-width: 100px;
}

.line-name {
  font-weight: 500;
}

.total-row td {
  background: #f8fafc;
  font-weight: 700;
}

.total-name {
  color: #0f172a;
}

.loading-cell.high-load {
  color: #b91c1c;
  font-weight: 700;
}

.loading-cell.warning-cell {
  background: #fee2e2;
}

.loading-state,
.error-state,
.empty-state {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--muted-foreground);
}

.error-state {
  color: var(--error);
}
</style>
