<template>

  <div class="page">

    <div class="page-header">

      <h1 class="page-title">静态产能核算</h1>

      <p class="page-subtitle">静态产能核算（周）模块</p>

    </div>



    <!-- MRP筛选条件 -->

    <div class="filters-row">

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

      />

      <button class="btn btn-primary" @click="loadCapacityAssessment" :disabled="loading">

        {{ loading ? '计算中...' : '开始计算' }}

      </button>
      <button class="btn" @click="handleExportCapacity" :disabled="!selectedLine || selectedLineData.length === 0">数据导出</button>

    </div>



    <!-- 生产线筛选（数据加载后显示） -->

    <div v-if="availableLines.length > 0" class="line-filter-row">

      <span class="line-filter-label">选择生产线：</span>

      <BaseSelect

        v-model="selectedLine"

        :options="availableLines.map(l => ({ value: l, label: formatLineLabel(l) }))"

        placeholder="请选择生产线"

      />

    </div>



    <!-- 警告信息 -->

    <div v-if="warnings.length > 0" class="warnings-container">

      <div class="warning-header">数据缺失提醒：</div>

      <ul class="warning-list">

        <li v-for="(warning, idx) in warnings" :key="idx">{{ warning }}</li>

      </ul>

    </div>



    <!-- 表格容器 -->

    <div class="capacity-wrapper">

      <div v-if="loading" class="loading-state">加载中...</div>

      <div v-else-if="error" class="error-state">{{ error }}</div>

      <div v-else-if="Object.keys(linesData).length === 0" class="empty-state">

        请选择筛选条件并点击"开始计算"

      </div>

      <div v-else-if="!selectedLine" class="empty-state">

        请选择生产线查看产能评估表

      </div>

      <div v-else-if="selectedLineData.length === 0" class="empty-state">

        该生产线暂无数据

      </div>

      <div v-else class="table-wrapper">

        <div class="line-header">{{ formatLineLabel(selectedLine) }}</div>

        <div class="table-scroll">

          <table class="main-table">

            <thead>

              <tr class="header-row-1">

                <th class="sticky-col sticky-col-1">Item Number</th>

                <th class="sticky-col sticky-col-2">Description</th>

                <th class="sticky-col sticky-col-3">Component Code</th>

                <th class="sticky-col sticky-col-4">班产量</th>

                <th class="sticky-col sticky-col-5">班人数</th>

                <th class="sticky-col sticky-col-6">CT</th>

                <th class="sticky-col sticky-col-7">OEE</th>

                <th

                  v-for="(week, idx) in weeks"

                  :key="'week-' + idx"

                  :colspan="2"

                  class="week-header"

                >

                  {{ getWeekDate(week) }}

                </th>

              </tr>

              <tr class="header-row-2">

                <th class="sticky-col sticky-col-1"></th>

                <th class="sticky-col sticky-col-2"></th>

                <th class="sticky-col sticky-col-3"></th>

                <th class="sticky-col sticky-col-4"></th>

                <th class="sticky-col sticky-col-5"></th>

                <th class="sticky-col sticky-col-6"></th>

                <th class="sticky-col sticky-col-7"></th>

                <template v-for="(week, idx) in weeks" :key="'sub-' + idx">

                  <th class="sub-header">需求量</th>

                  <th class="sub-header">LOADING</th>

                </template>

              </tr>

            </thead>

            <tbody>

              <tr v-for="item in selectedLineData" :key="item.itemNumber + '_' + item.componentNumber">

                <td class="sticky-col sticky-col-1">{{ item.itemNumber }}</td>

                <td class="sticky-col sticky-col-2">{{ item.description || '-' }}</td>

                <td class="sticky-col sticky-col-3">{{ item.componentNumber || '-' }}</td>

                <td class="sticky-col sticky-col-4">{{ item.shiftOutput ?? '-' }}</td>

                <td class="sticky-col sticky-col-5">{{ item.shiftWorkers ?? '-' }}</td>

                <td class="sticky-col sticky-col-6">{{ item.ct ?? '-' }}</td>

                <td class="sticky-col sticky-col-7">{{ item.oee ? item.oee + '%' : '-' }}</td>

                <template v-for="(week, idx) in weeks" :key="'data-' + idx">

                  <td class="data-cell">

                    {{ formatDemand(item[week + '_demand']) }}

                  </td>

                  <td class="data-cell">

                    {{ formatLoading(item[week + '_loading']) }}

                  </td>

                </template>

              </tr>

            </tbody>

          </table>

        </div>

      </div>

    </div>

  </div>

</template>



<script setup>

import { ref, computed, onMounted } from 'vue'

import { useAuth } from '@/composables/useAuth'

import { useToast } from '@/composables/useToast'

import { getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/mrp'

import { getCapacityAssessment } from '@/api/capacity'
import { getLines } from '@/api/line'
import { downloadCsv } from '@/utils/export'

import BaseSelect from '@/components/common/BaseSelect.vue'



const { token } = useAuth()

const { showToast } = useToast()

// sessionStorage keys
const SESSION_KEY = 'capics_capacity_static'

// 保存状态到sessionStorage
const saveState = () => {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({
      selectedCreatedBy: selectedCreatedBy.value,
      selectedFileName: selectedFileName.value,
      selectedVersion: selectedVersion.value,
      linesData: linesData.value,
      weeks: weeks.value,
      weekDates: weekDates.value,
      warnings: warnings.value,
      selectedLine: selectedLine.value
    }))
  } catch (e) {
    console.error('Save state error:', e)
  }
}

// 从sessionStorage恢复状态
const restoreState = () => {
  try {
    const saved = sessionStorage.getItem(SESSION_KEY)
    if (saved) {
      const state = JSON.parse(saved)
      selectedCreatedBy.value = state.selectedCreatedBy || ''
      selectedFileName.value = state.selectedFileName || ''
      selectedVersion.value = state.selectedVersion || ''
      linesData.value = state.linesData || {}
      weeks.value = state.weeks || []
      weekDates.value = state.weekDates || {}
      warnings.value = state.warnings || []
      selectedLine.value = state.selectedLine || ''
      return true
    }
  } catch (e) {
    console.error('Restore state error:', e)
  }
  return false
}



// MRP筛选条件

const createdBys = ref([])

const fileNames = ref([])

const versions = ref([])

const selectedCreatedBy = ref('')

const selectedFileName = ref('')

const selectedVersion = ref('')
const lineNameMap = ref({})



// 生产线筛选

const selectedLine = ref('')

const linesData = ref({})

const weeks = ref([])

const weekDates = ref({})

const warnings = ref([])



const loading = ref(false)

const error = ref('')



// 可用的生产线列表

const availableLines = computed(() => {

  return Object.keys(linesData.value).sort()

})

const formatLineLabel = (lineCode) => {
  if (!lineCode) return ''
  const lineName = lineNameMap.value[lineCode]
  return lineName ? `${lineCode} - ${lineName}` : lineCode
}



// 当前选中的生产线数据

const selectedLineData = computed(() => {

  if (!selectedLine.value || !linesData.value[selectedLine.value]) {

    return []

  }

  return linesData.value[selectedLine.value]

})

const loadLineNames = async (lineCodes) => {
  try {
    const response = await getLines(token.value)
    if (response.success && response.data) {
      const names = {}
      response.data.forEach(line => {
        if (!lineCodes || lineCodes.length === 0 || lineCodes.includes(line.lineCode)) {
          names[line.lineCode] = line.lineName || ''
        }
      })
      lineNameMap.value = names
    }
  } catch (err) {
    console.error('Load line names error:', err)
  }
}



const loadCreatedBys = async () => {

  try {

    const data = await getCreatedBys(token.value)

    createdBys.value = data.data || []

  } catch (err) {

    console.error('Load createdBys error:', err)

  }

}



const onCreatedByChange = async () => {

  selectedFileName.value = ''

  selectedVersion.value = ''

  fileNames.value = []

  versions.value = []

  resetData()

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

  resetData()

  if (selectedCreatedBy.value && selectedFileName.value) {

    try {

      const data = await getVersionsByCreatedByAndFileName(token.value, selectedCreatedBy.value, selectedFileName.value)

      versions.value = data.data || []

    } catch (err) {

      console.error('Load versions error:', err)

    }

  }

}



const resetData = () => {

  linesData.value = {}

  weeks.value = []

  weekDates.value = {}

  warnings.value = []

  selectedLine.value = ''

}



const loadCapacityAssessment = async () => {

  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {

    showToast('请选择完整的筛选条件（导入人、文件、版本）', 'warning')

    return

  }



  loading.value = true

  error.value = ''

  resetData()



  try {

    const data = await getCapacityAssessment(

      token.value,

      selectedCreatedBy.value,

      selectedFileName.value,

      selectedVersion.value

    )



    if (data.success) {

      const result = data.data

      if (result && result.lines) {

        linesData.value = result.lines

        weeks.value = result.weeks || []

        weekDates.value = result.weekDates || {}

        warnings.value = result.warnings || []



        // 自动选中第一条生产线

        const lineCodes = Object.keys(result.lines)

        if (lineCodes.length > 0) {

          selectedLine.value = lineCodes[0]

        }



        // 保存状态

        saveState()
        await loadLineNames(lineCodes)

      } else {

        linesData.value = {}

      }

    } else {

      error.value = data.message || '加载数据失败'

      showToast('加载产能评估数据失败: ' + data.message, 'error')

    }

  } catch (err) {

    console.error('Load capacity assessment error:', err)

    error.value = '加载数据失败: ' + (err.message || '未知错误')

    showToast('加载产能评估数据失败', 'error')

  } finally {

    loading.value = false

  }

}



const getWeekDate = (week) => {

  return weekDates.value[week] || week

}



const formatDemand = (demand) => {

  if (demand === null || demand === undefined || demand === '-') return '-'

  const num = parseFloat(demand)

  if (isNaN(num)) return '-'

  return num.toFixed(0)

}



const formatLoading = (loading) => {

  if (loading === null || loading === undefined || loading === '-') return '-'

  const num = parseFloat(loading)

  if (isNaN(num)) return '-'

  return num.toFixed(2)

}

const handleExportCapacity = () => {
  if (!selectedLine.value || selectedLineData.value.length === 0) {
    showToast('暂无可导出数据', 'warning')
    return
  }
  const headers = [
    { key: 'lineCode', label: '生产线' },
    { key: 'itemNumber', label: 'Item Number' },
    { key: 'description', label: 'Description' },
    { key: 'componentNumber', label: 'Component Code' },
    { key: 'shiftOutput', label: '班产量' },
    { key: 'shiftWorkers', label: '班人数' },
    { key: 'ct', label: 'CT' },
    { key: 'oee', label: 'OEE(%)' }
  ]
  weeks.value.forEach(week => {
    headers.push({ key: `${week}_demand`, label: `${getWeekDate(week)}_需求量` })
    headers.push({ key: `${week}_loading`, label: `${getWeekDate(week)}_LOADING` })
  })
  const rows = selectedLineData.value.map(item => {
    const row = {
      lineCode: formatLineLabel(selectedLine.value),
      itemNumber: item.itemNumber || '',
      description: item.description || '',
      componentNumber: item.componentNumber || '',
      shiftOutput: item.shiftOutput ?? '',
      shiftWorkers: item.shiftWorkers ?? '',
      ct: item.ct ?? '',
      oee: item.oee ?? ''
    }
    weeks.value.forEach(week => {
      row[`${week}_demand`] = item[`${week}_demand`] ?? ''
      row[`${week}_loading`] = item[`${week}_loading`] ?? ''
    })
    return row
  })
  downloadCsv(`静态产能核算-周-${selectedLine.value || '全部'}.csv`, headers, rows)
  showToast('导出成功', 'success')
}



onMounted(() => {

  loadCreatedBys().then(async () => {
    // 恢复状态
    restoreState()
    const lineCodes = Object.keys(linesData.value || {})
    if (lineCodes.length > 0) {
      await loadLineNames(lineCodes)
    }
  })

})

</script>



<style scoped>

/* === Apple Style Variables === */

.page {

  --col-1-width: 170px;

  --col-2-width: 220px;

  --col-3-width: 220px;

  --col-4-width: 100px;

  --col-5-width: 80px;

  --col-6-width: 80px;

  --col-7-width: 80px;

  --header-row-height: 40px;

}



/* === Filters === */

.filters-row {

  display: flex;

  gap: 1rem;

  align-items: center;

  margin-bottom: 1rem;

  flex-shrink: 0;

  flex-wrap: wrap;

}



.line-filter-row {

  display: flex;

  gap: 1rem;

  align-items: center;

  margin-bottom: 1rem;

  flex-shrink: 0;

}



.line-filter-label {

  font-weight: 500;

  color: var(--foreground);

}



/* === Warnings === */

.warnings-container {

  background: #FEF3C7;

  border: 1px solid #F59E0B;

  border-radius: var(--radius-md);

  padding: 0.75rem 1rem;

  margin-bottom: 1rem;

  max-height: 120px;

  overflow-y: auto;

}



.warning-header {

  font-weight: 600;

  color: #92400E;

  margin-bottom: 0.5rem;

}



.warning-list {

  margin: 0;

  padding-left: 1.5rem;

  color: #78350F;

  font-size: 0.875rem;

}



.warning-list li {

  margin-bottom: 0.25rem;

}



/* === Table Container === */

.capacity-wrapper {

  flex: 1;

  overflow: hidden;

  display: flex;

  flex-direction: column;

  min-height: 0;

}



.loading-state,

.error-state,

.empty-state {

  display: flex;

  align-items: center;

  justify-content: center;

  flex: 1;

  color: var(--muted-foreground);

  background: white;

  border-radius: var(--radius-lg);

  border: 1px solid var(--border-light);

}



.error-state {

  color: var(--error);

}



.table-wrapper {

  flex: 1;

  overflow: hidden;

  display: flex;

  flex-direction: column;

  background: white;

  border-radius: var(--radius-lg);

  border: 1px solid var(--border-light);

}



.line-header {

  font-size: 1rem;

  font-weight: 600;

  color: var(--foreground);

  padding: 0.75rem 1rem;

  border-bottom: 1px solid var(--border-light);

  background: var(--muted);

  flex-shrink: 0;

  border-radius: var(--radius-lg) var(--radius-lg) 0 0;

}



.table-scroll {

  flex: 1;

  overflow: auto;

}



/* === Table Base === */

.main-table {

  border-collapse: separate;

  border-spacing: 0;

  width: max-content;

  min-width: 100%;

}



/* === Sticky Columns === */

.sticky-col {

  position: sticky;

  background: #fff;

}



.sticky-col-1 {

  left: 0;

  width: var(--col-1-width);

  min-width: var(--col-1-width);

  max-width: var(--col-1-width);

  z-index: 6;

}



.sticky-col-2 {

  left: var(--col-1-width);

  width: var(--col-2-width);

  min-width: var(--col-2-width);

  max-width: var(--col-2-width);

  z-index: 5;

  overflow: hidden;

  text-overflow: ellipsis;

}



.sticky-col-3 {

  left: calc(var(--col-1-width) + var(--col-2-width));

  width: var(--col-3-width);

  min-width: var(--col-3-width);

  max-width: var(--col-3-width);

  z-index: 4;

}



.sticky-col-4 {

  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width));

  width: var(--col-4-width);

  min-width: var(--col-4-width);

  max-width: var(--col-4-width);

  z-index: 3;

}



.sticky-col-5 {

  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width));

  width: var(--col-5-width);

  min-width: var(--col-5-width);

  max-width: var(--col-5-width);

  z-index: 2;

}



.sticky-col-6 {

  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width) + var(--col-5-width));

  width: var(--col-6-width);

  min-width: var(--col-6-width);

  max-width: var(--col-6-width);

  z-index: 1;

}



.sticky-col-7 {

  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width) + var(--col-5-width) + var(--col-6-width));

  width: var(--col-7-width);

  min-width: var(--col-7-width);

  max-width: var(--col-7-width);

  z-index: 0;

}



/* === Header Cells === */

thead th {

  position: sticky;

  border: 1px solid var(--border-light);

  font-weight: 600;

  text-align: center;

  padding: 0.5rem;

  white-space: nowrap;

  box-sizing: border-box;

  height: var(--header-row-height);

  background: var(--muted);

  color: var(--foreground);

}



.header-row-1 th {

  top: 0;

  z-index: 8;

  background: white;

  color: var(--foreground);

}



.header-row-2 th {

  top: var(--header-row-height);

  z-index: 7;

  background: var(--foreground);

  color: white;

}



/* === Sticky Columns (Body 层级) === */
.sticky-col {
  position: sticky;
  background: #fff;
}

/* 冻结列的层级设为 11~17，确保高于普通的 tbody 单元格(0) */
.sticky-col-1 {
  left: 0;
  width: var(--col-1-width);
  min-width: var(--col-1-width);
  max-width: var(--col-1-width);
  z-index: 17;
}

.sticky-col-2 {
  left: var(--col-1-width);
  width: var(--col-2-width);
  min-width: var(--col-2-width);
  max-width: var(--col-2-width);
  z-index: 16;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sticky-col-3 {
  left: calc(var(--col-1-width) + var(--col-2-width));
  width: var(--col-3-width);
  min-width: var(--col-3-width);
  max-width: var(--col-3-width);
  z-index: 15;
}

.sticky-col-4 {
  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width));
  width: var(--col-4-width);
  min-width: var(--col-4-width);
  max-width: var(--col-4-width);
  z-index: 14;
}

.sticky-col-5 {
  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width));
  width: var(--col-5-width);
  min-width: var(--col-5-width);
  max-width: var(--col-5-width);
  z-index: 13;
}

.sticky-col-6 {
  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width) + var(--col-5-width));
  width: var(--col-6-width);
  min-width: var(--col-6-width);
  max-width: var(--col-6-width);
  z-index: 12;
}

.sticky-col-7 {
  left: calc(var(--col-1-width) + var(--col-2-width) + var(--col-3-width) + var(--col-4-width) + var(--col-5-width) + var(--col-6-width));
  width: var(--col-7-width);
  min-width: var(--col-7-width);
  max-width: var(--col-7-width);
  z-index: 11;
}

/* === Header Cells (表头基础层级) === */
thead th {
  position: sticky;
  border: 1px solid var(--border-light);
  font-weight: 600;
  text-align: center;
  padding: 0.5rem;
  white-space: nowrap;
  box-sizing: border-box;
  height: var(--header-row-height);
  background: var(--muted);
  color: var(--foreground);
  z-index: 20; /* 提升基础表头层级，确保向下滚动时不被 body 的冻结列覆盖 */
}

.header-row-1 th {
  top: 0;
  z-index: 22; /* 第一行表头层级略高 */
  background: white;
  color: var(--foreground);
}

.header-row-2 th {
  top: var(--header-row-height);
  z-index: 21; /* 第二行表头层级次高 */
  background: var(--foreground);
  color: white;
}

/* 第二行前7列（空白占位）改为白底，与第一行冻结列保持一致 */
.header-row-2 th.sticky-col-1,
.header-row-2 th.sticky-col-2,
.header-row-2 th.sticky-col-3,
.header-row-2 th.sticky-col-4,
.header-row-2 th.sticky-col-5,
.header-row-2 th.sticky-col-6,
.header-row-2 th.sticky-col-7 {
  background: #fff;
  color: var(--foreground);
}

/* === Header sticky columns - highest z-index (表头冻结交叉区) === */
/* 这里是左上角的核心区域，层级必须是最高的 (> 22)，这样向右滚动时才压得住普通表头 */
.header-row-1 th.sticky-col-1, .header-row-2 th.sticky-col-1 { z-index: 37 !important; }
.header-row-1 th.sticky-col-2, .header-row-2 th.sticky-col-2 { z-index: 36 !important; }
.header-row-1 th.sticky-col-3, .header-row-2 th.sticky-col-3 { z-index: 35 !important; }
.header-row-1 th.sticky-col-4, .header-row-2 th.sticky-col-4 { z-index: 34 !important; }
.header-row-1 th.sticky-col-5, .header-row-2 th.sticky-col-5 { z-index: 33 !important; }
.header-row-1 th.sticky-col-6, .header-row-2 th.sticky-col-6 { z-index: 32 !important; }
.header-row-1 th.sticky-col-7, .header-row-2 th.sticky-col-7 { z-index: 31 !important; }



/* === Week Header === */

.week-header {

  background: white !important;

  color: var(--foreground) !important;

  border-left: none;

  border-right: none;

  min-width: 100px;

  width: 100px;

}



.sub-header {

  background: var(--muted) !important;

  color: var(--foreground) !important;

  min-width: 80px;

  width: 80px;

}



/* === Body Cells === */

tbody td {

  border: 1px solid var(--border-light);

  padding: 0.5rem;

  text-align: center;

  white-space: nowrap;

  box-sizing: border-box;

  height: var(--header-row-height);

  background: white;

  color: var(--foreground);

}



tbody td.sticky-col-1,

tbody td.sticky-col-2,

tbody td.sticky-col-3,

tbody td.sticky-col-4,

tbody td.sticky-col-5,

tbody td.sticky-col-6,

tbody td.sticky-col-7 {

  background: #fff;

  font-weight: 500;

  text-align: left;

}



.data-cell {

  background: var(--muted);

  color: var(--foreground);

  text-align: right;

}



/* === Sticky Column Borders === */

.header-row-1 th.sticky-col-1,

.header-row-2 th.sticky-col-1,

tbody td.sticky-col-1 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-2,

.header-row-2 th.sticky-col-2,

tbody td.sticky-col-2 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-3,

.header-row-2 th.sticky-col-3,

tbody td.sticky-col-3 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-4,

.header-row-2 th.sticky-col-4,

tbody td.sticky-col-4 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-5,

.header-row-2 th.sticky-col-5,

tbody td.sticky-col-5 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-6,

.header-row-2 th.sticky-col-6,

tbody td.sticky-col-6 {

  border-right: 1px solid var(--border);

}



.header-row-1 th.sticky-col-7,

.header-row-2 th.sticky-col-7,

tbody td.sticky-col-7 {

  border-right: 1px solid var(--border);

}



/* === Scrollbar - Apple Style === */

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



.table-scroll::-webkit-scrollbar-corner {

  background: var(--muted);

}

</style>
