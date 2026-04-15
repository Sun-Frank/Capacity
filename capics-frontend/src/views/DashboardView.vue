<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">仪表盘</h1>
      <p class="page-subtitle">产能评估系统总览</p>
    </div>

    <!-- 筛选条件 -->
    <div class="filters-row">
      <!-- 数据源选择 -->
      <div class="source-toggle">
        <button
          class="toggle-btn"
          :class="{ active: dataSource === 'static' }"
          @click="setDataSource('static')"
        >
          静态产能核算
        </button>
        <button
          class="toggle-btn"
          :class="{ active: dataSource === 'dynamic' }"
          @click="setDataSource('dynamic')"
        >
          动态产能模拟
        </button>
      </div>

      <!-- 维度切换 -->
      <div class="source-toggle">
        <button
          class="toggle-btn"
          :class="{ active: dimension === 'week' }"
          @click="setDimension('week')"
        >
          周
        </button>
        <button
          class="toggle-btn"
          :class="{ active: dimension === 'month' }"
          @click="setDimension('month')"
        >
          月
        </button>
      </div>

      <!-- MRP版本选择 -->
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
      <!-- 动态模式下选择快照 -->
      <BaseSelect
        v-if="dataSource === 'dynamic'"
        v-model="selectedSnapshot"
        :options="snapshotNames.map(n => ({ value: n, label: n }))"
        placeholder="选择快照"
        :disabled="!selectedVersion"
      />
      <button class="btn btn-primary" @click="loadData" :disabled="loading">
        {{ loading ? '加载中...' : '加载数据' }}
      </button>
    </div>

    <!-- 警告信息 -->
    <div v-if="warnings.length > 0" class="warnings-container">
      <div class="warning-header">高负载预警：</div>
      <ul class="warning-list">
        <li v-for="(warning, idx) in warnings" :key="idx">{{ warning }}</li>
      </ul>
    </div>

    <!-- 产线LOAD汇总表 -->
    <div class="table-wrapper">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="rows.length === 0" class="empty-state">
        请选择筛选条件并点击"加载数据"
      </div>
      <div v-else class="table-scroll">
        <table class="main-table">
          <thead>
            <tr>
              <th class="sticky-col">生产线</th>
              <th v-for="date in dates" :key="date" class="date-header">
                {{ getDateLabel(date) }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in rows"
              :key="row.lineCode"
              :class="{ 'total-row': row.isTotal, 'group-row': row.group && !row.isTotal }"
            >
              <td class="sticky-col line-name" :class="{ 'total-name': row.isTotal }">
                {{ formatLineLabel(row.lineCode) }}
              </td>
              <td
                v-for="(loading, idx) in row.loadings"
                :key="dates[idx]"
                class="loading-cell"
                :class="{
                  'high-load': loading > row.threshold,
                  'warning-cell': row.isTotal && loading > row.threshold
                }"
              >
                {{ formatLoading(loading) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getLoadingMatrix, getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/dashboard'
import { getSnapshot, getSnapshotNames } from '@/api/simulationSnapshot'
import { getLines } from '@/api/line'
import BaseSelect from '@/components/common/BaseSelect.vue'

const { token } = useAuth()
const { showToast } = useToast()

// 数据源
const dataSource = ref('static')
const dimension = ref('week')

// MRP筛选条件
const createdBys = ref([])
const fileNames = ref([])
const versions = ref([])
const selectedCreatedBy = ref('')
const selectedFileName = ref('')
const selectedVersion = ref('')

// 快照
const snapshotNames = ref([])
const selectedSnapshot = ref('')

// 数据
const rows = ref([])
const dates = ref([])
const dateLabels = ref({})
const warnings = ref([])
const lineNameMap = ref({})

const loading = ref(false)
const error = ref('')

const setDataSource = (source) => {
  dataSource.value = source
  selectedSnapshot.value = ''
  if (source === 'dynamic') {
    loadSnapshotNames()
  }
}

const setDimension = (dim) => {
  dimension.value = dim
  selectedSnapshot.value = ''
  if (dataSource.value === 'dynamic' && selectedVersion.value) {
    loadSnapshotNames()
  }
}

const getDateLabel = (date) => {
  return dateLabels.value[date] || date
}

const formatLineLabel = (lineCode) => {
  if (!lineCode) return ''
  if (lineCode.includes('TOTAL')) return lineCode
  const lineName = lineNameMap.value[lineCode]
  return lineName ? `${lineCode} - ${lineName}` : lineCode
}

const formatLoading = (loading) => {
  if (loading === 0) return '-'
  return (loading * 100).toFixed(0) + '%'
}

const loadCreatedBys = async () => {
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
      data.data.forEach(line => {
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
  selectedSnapshot.value = ''
  snapshotNames.value = []
  if (selectedCreatedBy.value) {
    try {
      const data = await getFileNamesByCreatedBy(token.value, selectedCreatedBy.value)
      fileNames.value = data.data || []
    } catch (err) {
      console.error('Load fileNames error:', err)
    }
  }
}

const onFileNameChange = async () => {
  selectedVersion.value = ''
  versions.value = []
  selectedSnapshot.value = ''
  snapshotNames.value = []
  if (selectedCreatedBy.value && selectedFileName.value) {
    try {
      const data = await getVersionsByCreatedByAndFileName(token.value, selectedCreatedBy.value, selectedFileName.value)
      versions.value = data.data || []
    } catch (err) {
      console.error('Load versions error:', err)
    }
  }
}

const onVersionChange = () => {
  console.log('Version changed:', selectedVersion.value, 'dataSource:', dataSource.value)
  if (dataSource.value === 'dynamic' && selectedVersion.value) {
    loadSnapshotNames()
  }
}

const loadSnapshotNames = async () => {
  console.log('loadSnapshotNames called', {
    createdBy: selectedCreatedBy.value,
    fileName: selectedFileName.value,
    version: selectedVersion.value,
    source: dataSource.value,
    dimension: dimension.value
  })
  if (dataSource.value !== 'dynamic') {
    console.log('loadSnapshotNames early return: not dynamic')
    return
  }
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    console.log('loadSnapshotNames early return: missing params', {
      hasCreatedBy: !!selectedCreatedBy.value,
      hasFileName: !!selectedFileName.value,
      hasVersion: !!selectedVersion.value
    })
    return
  }
  try {
    console.log('loadSnapshotNames calling API...')
    const data = await getSnapshotNames(
      token.value,
      selectedCreatedBy.value,
      selectedFileName.value,
      selectedVersion.value,
      dataSource.value,
      dimension.value
    )
    console.log('loadSnapshotNames response:', data)
    snapshotNames.value = data.data || []
    console.log('snapshotNames set to:', snapshotNames.value)
  } catch (err) {
    console.error('Load snapshot names error:', err)
  }
}

// 监听版本变化，加载快照列表
watch(selectedVersion, () => {
  if (dataSource.value === 'dynamic' && selectedVersion.value) {
    loadSnapshotNames()
  }
})

const loadData = async () => {
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    showToast('请选择完整的筛选条件（导入人、文件、版本）', 'warning')
    return
  }

  // 动态模式必须选择快照
  if (dataSource.value === 'dynamic' && !selectedSnapshot.value) {
    showToast('请选择快照', 'warning')
    return
  }

  loading.value = true
  error.value = ''

  try {
    // 如果是动态模式且选择了快照，从快照加载
    if (dataSource.value === 'dynamic' && selectedSnapshot.value) {
      const snapshotData = await getSnapshot(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value,
        selectedSnapshot.value,
        dataSource.value,
        dimension.value
      )

      if (snapshotData.success) {
        const result = snapshotData.data
        // 转换快照数据为 Dashboard 需要的格式
        const linesData = result.linesData
        const datesList = result.dates
        const dateLabelsMap = result.dateLabels

        // 转换为 rows 格式
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

          newRows.push({
            lineCode,
            loadings: lineLoadings,
            isTotal: false,
            group,
            threshold: 1.0
          })

          if (group) {
            if (!groupLines[group]) {
              groupLines[group] = []
              groupLoadings[group] = new Array(datesList.length).fill(0)
            }
            groupLines[group].push(lineCode)
            for (let i = 0; i < lineLoadings.length; i++) {
              groupLoadings[group][i] += lineLoadings[i]
            }
          }
        }

        // 添加 TOTAL 行
        for (const [group, loadings] of Object.entries(groupLoadings)) {
          const lineCount = groupLines[group].length
          newRows.push({
            lineCode: group + ' TOTAL',
            loadings,
            isTotal: true,
            group,
            threshold: lineCount * 1.0
          })
        }

        // 排序：组内按 lineCode 降序，TOTAL 排最后，同组优先
        newRows.sort((a, b) => {
          const compGroupA = a.group || a.lineCode
          const compGroupB = b.group || b.lineCode
          const groupCompare = -compGroupA.localeCompare(compGroupB)
          if (groupCompare !== 0) return groupCompare
          if (a.isTotal && !b.isTotal) return 1
          if (!a.isTotal && b.isTotal) return -1
          return -a.lineCode.localeCompare(b.lineCode)
        })

        rows.value = newRows
        dates.value = datesList
        dateLabels.value = dateLabelsMap
        warnings.value = []
      } else {
        error.value = snapshotData.message || '加载快照失败'
        showToast('加载快照失败: ' + snapshotData.message, 'error')
      }
    } else {
      // 静态模式，正常调用 API
      const data = await getLoadingMatrix(
        token.value,
        dataSource.value,
        dimension.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value
      )

      if (data.success) {
        const result = data.data
        rows.value = result.rows || []
        dates.value = result.dates || []
        dateLabels.value = result.dateLabels || {}
        warnings.value = result.warnings || []
      } else {
        error.value = data.message || '加载数据失败'
        showToast('加载数据失败: ' + data.message, 'error')
      }
    }
  } catch (err) {
    console.error('Load dashboard data error:', err)
    error.value = '加载数据失败: ' + (err.message || '未知错误')
    showToast('加载数据失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCreatedBys()
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

/* Warnings */
.warnings-container {
  background: #FEE2E2;
  border: 1px solid #DC2626;
  border-radius: var(--radius-md);
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  max-height: 120px;
  overflow-y: auto;
}

.warning-header {
  font-weight: 600;
  color: #991B1B;
  margin-bottom: 0.5rem;
}

.warning-list {
  margin: 0;
  padding-left: 1.5rem;
  color: #7F1D1D;
  font-size: 0.875rem;
}

.warning-list li {
  margin-bottom: 0.25rem;
}

/* Table */
.table-wrapper {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: auto;
  min-height: 300px;
  max-height: calc(100vh - 300px);
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--muted-foreground);
  padding: 3rem;
}

.error-state {
  color: var(--error);
}

.table-scroll {
  overflow: auto;
}

.main-table {
  border-collapse: collapse;
  width: 100%;
  min-width: 800px;
}

.main-table th,
.main-table td {
  border: 1px solid var(--border-light);
  padding: 0.75rem 1rem;
  text-align: center;
  white-space: nowrap;
}

.main-table th {
  background: var(--muted);
  font-weight: 600;
  color: var(--foreground);
  position: sticky;
  top: 0;
  z-index: 10;
}

.sticky-col {
  position: sticky;
  left: 0;
  background: white;
  z-index: 5;
  text-align: left;
  font-weight: 500;
  min-width: 120px;
}

.main-table th.sticky-col {
  background: var(--muted);
  z-index: 15;
}

.line-name {
  font-weight: 500;
}

.total-row {
  background: var(--muted) !important;
  font-weight: 600;
}

.total-row .sticky-col {
  background: var(--muted);
}

.total-name {
  font-weight: 700;
  color: var(--primary);
}

.group-row .sticky-col {
  background: #F0F9FF;
}

.loading-cell {
  font-weight: 600;
}

.high-load {
  color: #DC2626;
  background: #FEE2E2 !important;
}

.warning-cell {
  background: #FEE2E2 !important;
}

/* Scrollbar */
.table-scroll::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.table-scroll::-webkit-scrollbar-track {
  background: var(--muted);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--muted-foreground);
}
</style>
