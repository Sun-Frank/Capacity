<template>
  <div class="page">
    <!-- MRP筛选条件 -->
    <div class="filters-row">
      <BaseSelect
        class="filter-select filter-select-created-by"
        v-model="selectedCreatedBy"
        :options="createdBys.map(c => ({ value: c, label: c }))"
        placeholder="选择导入人"
        @update:modelValue="onCreatedByChange"
      />
      <BaseSelect
        class="filter-select filter-select-file-name"
        v-model="selectedFileName"
        :options="fileNames.map(f => ({ value: f, label: f }))"
        placeholder="选择文件"
        :disabled="!selectedCreatedBy"
        @update:modelValue="onFileNameChange"
      />
      <BaseSelect
        class="filter-select filter-select-version"
        v-model="selectedVersion"
        :options="versions.map(v => ({ value: v, label: v }))"
        placeholder="选择版本"
        :disabled="!selectedFileName"
      />
      <button class="btn btn-primary" @click="loadCapacityAssessment" :disabled="loading">
        {{ loading ? '加载中...' : '加载数据' }}
      </button>
      <button
        v-if="selectedLine && editableData.length > 0"
        class="btn"
        @click="handleExportRealtimeWeekly"
      >
        数据导出
      </button>
      <button
        v-if="Object.keys(linesData).length > 0"
        class="btn btn-secondary"
        @click="showSnapshotModal = true"
      >
        保存快照
      </button>
    </div>

    <!-- 生产线筛选 -->
    <div v-if="availableLines.length > 0" class="line-filter-row">
      <span class="line-filter-label">选择生产线：</span>
      <BaseSelect
        v-model="selectedLine"
        :options="availableLines.map(l => ({ value: l, label: formatLineLabel(l) }))"
        placeholder="请选择生产线"
      />
      <button
        v-if="!showSummary && selectedLine && summaryData.length > 0"
        class="btn btn-secondary"
        @click="showSummary = true"
      >
        显示汇总表
      </button>
    </div>

    <!-- 警告信息 -->
    <div v-if="warnings.length > 0" class="warnings-container">
      <div class="warning-header">数据缺失提醒：</div>
      <ul class="warning-list">
        <li v-for="(warning, idx) in warnings" :key="idx">{{ warning }}</li>
      </ul>
    </div>

    <!-- 汇总表 -->
    <div
      v-if="selectedLine && summaryData.length > 0 && showSummary"
      class="summary-panel"
      :style="{ left: summaryPosition.x + 'px', top: summaryPosition.y + 'px' }"
      @mousedown="startDrag"
    >
      <div class="summary-title">
        <span>{{ formatLineLabel(selectedLine) }}</span>
        <button class="close-btn" @click="closeSummary">脳</button>
      </div>
      <div class="summary-scroll">
        <table class="summary-table">
          <thead>
            <tr>
              <th class="sticky-col">统计维度</th>
              <th v-for="(week, idx) in displayWeeks" :key="week">{{ getWeekDate(week) }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, idx) in summaryData"
              :key="idx"
              :class="{ 'total-row': row.dimension === '总计' }"
            >
              <td class="sticky-col">{{ row.dimension }}</td>
              <td v-for="(week, wIdx) in displayWeeks" :key="week">
                <span class="loading-value" :class="{ 'high-load': row.loadings[week] > 1 }">
                  {{ formatLoading(row.loadings[week]) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 表格容器 -->
    <div class="capacity-wrapper">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="Object.keys(linesData).length === 0" class="empty-state">
        请选择筛选条件并点击“加载数据”
      </div>
      <div v-else-if="!selectedLine" class="empty-state">
        请选择生产线查看产能评估表
      </div>
      <div v-else-if="selectedLineData.length === 0" class="empty-state">
        该生产线暂无数据
      </div>
      <div v-else class="table-wrapper">
        <div class="line-header">{{ formatLineLabel(selectedLine) }}</div>
        <div class="table-scroll" @click="closeAllEditing">
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
              <tr v-for="item in editableData" :key="item.itemNumber + '_' + item.componentNumber">
                <td class="sticky-col sticky-col-1">{{ item.itemNumber }}</td>
                <td class="sticky-col sticky-col-2">{{ item.description || '-' }}</td>
                <td class="sticky-col sticky-col-3">{{ item.componentNumber || '-' }}</td>
                <!-- 鐝骇閲?- 鑷姩璁＄畻锛屽彧璇?-->
                <td class="sticky-col sticky-col-4 data-cell">{{ formatShiftOutput(calcShiftOutput(item)) }}</td>
                <!-- 鐝汉鏁?- 鍙紪杈?-->
                <td
                  class="sticky-col sticky-col-5 editable-cell"
                  :class="{ 'is-editing': isCellEditing(item, 'shiftWorkers') }"
                  @dblclick="startEdit(item, 'shiftWorkers', $event)"
                  @contextmenu.prevent="startEdit(item, 'shiftWorkers', $event)"
                  @click.stop
                >
                  <template v-if="isCellEditing(item, 'shiftWorkers')">
                    <input
                      ref="editInputRef"
                      type="number"
                      v-model.number="item.shiftWorkers"
                      class="cell-input"
                      min="0"
                      @blur="finishEdit(item, 'shiftWorkers')"
                      @keydown.enter="finishEdit(item, 'shiftWorkers')"
                      @keydown.escape="cancelEdit(item, 'shiftWorkers')"
                    >
                  </template>
                  <template v-else>
                    <span class="cell-display">{{ item.shiftWorkers ?? '-' }}</span>
                  </template>
                </td>
                <!-- CT - 鍙紪杈?-->
                <td
                  class="sticky-col sticky-col-6 editable-cell"
                  :class="{ 'is-editing': isCellEditing(item, 'ct') }"
                  @dblclick="startEdit(item, 'ct', $event)"
                  @contextmenu.prevent="startEdit(item, 'ct', $event)"
                  @click.stop
                >
                  <template v-if="isCellEditing(item, 'ct')">
                    <input
                      ref="editInputRef"
                      type="number"
                      v-model.number="item.ct"
                      class="cell-input"
                      min="0"
                      step="0.01"
                      @blur="finishEdit(item, 'ct')"
                      @keydown.enter="finishEdit(item, 'ct')"
                      @keydown.escape="cancelEdit(item, 'ct')"
                    >
                  </template>
                  <template v-else>
                    <span class="cell-display">{{ item.ct ?? '-' }}</span>
                  </template>
                </td>
                <!-- OEE - 鍙紪杈?-->
                <td
                  class="sticky-col sticky-col-7 editable-cell"
                  :class="{ 'is-editing': isCellEditing(item, 'oee') }"
                  @dblclick="startEdit(item, 'oee', $event)"
                  @contextmenu.prevent="startEdit(item, 'oee', $event)"
                  @click.stop
                >
                  <template v-if="isCellEditing(item, 'oee')">
                    <input
                      ref="editInputRef"
                      type="number"
                      v-model.number="item.oee"
                      class="cell-input"
                      min="0"
                      max="100"
                      step="0.01"
                      @blur="finishEdit(item, 'oee')"
                      @keydown.enter="finishEdit(item, 'oee')"
                      @keydown.escape="cancelEdit(item, 'oee')"
                    >
                  </template>
                  <template v-else>
                    <span class="cell-display">{{ item.oee ? item.oee + '%' : '-' }}</span>
                  </template>
                </td>
                <!-- 闇€姹傞噺鍜孡OADING -->
                <template v-for="(week, idx) in weeks" :key="'data-' + idx">
                  <!-- 闇€姹傞噺 - 鍙紪杈?-->
                  <td
                    class="data-cell editable-cell"
                    :class="{ 'is-editing': isCellEditing(item, week + '_demand') }"
                    @dblclick="startEdit(item, week + '_demand', $event)"
                    @contextmenu.prevent="startEdit(item, week + '_demand', $event)"
                    @click.stop
                  >
                    <template v-if="isCellEditing(item, week + '_demand')">
                      <input
                        ref="editInputRef"
                        type="number"
                        v-model.number="item[week + '_demand']"
                        class="cell-input"
                        min="0"
                        @blur="finishEdit(item, week + '_demand')"
                        @keydown.enter="finishEdit(item, week + '_demand')"
                        @keydown.escape="cancelEdit(item, week + '_demand')"
                      >
                    </template>
                    <template v-else>
                      <span class="cell-display">{{ formatDemand(item[week + '_demand']) }}</span>
                    </template>
                  </td>
                  <!-- LOADING - 鍙 -->
                  <td
                    class="data-cell loading-cell"
                    :class="{ 'high-load': calcLoading(item, week) > 0.85 }"
                  >
                    {{ formatLoading(calcLoading(item, week)) }}
                  </td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 产线配置表格 -->
    <div v-if="selectedLine && selectedLineData.length > 0" class="line-config-section">
      <h3 class="section-title">产线配置</h3>
      <div class="config-table-wrapper">
        <table class="config-table">
          <thead>
            <tr>
              <th>生产线</th>
              <th>工作天数（天/周）</th>
              <th>班数（班/天）</th>
              <th>每班工作小时</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="line-name">{{ formatLineLabel(selectedLine) }}</td>
              <td
                class="editable-cell"
                :class="{ 'is-editing': editingConfig === 'workingDaysPerWeek' }"
                @dblclick="startConfigEdit('workingDaysPerWeek', $event)"
                @contextmenu.prevent="startConfigEdit('workingDaysPerWeek', $event)"
              >
                <template v-if="editingConfig === 'workingDaysPerWeek'">
                  <input
                    ref="configInputRef"
                    type="number"
                    v-model.number="lineConfigs[selectedLine].workingDaysPerWeek"
                    class="cell-input"
                    min="1"
                    max="7"
                    @blur="finishConfigEdit"
                    @keydown.enter="finishConfigEdit"
                    @keydown.escape="cancelConfigEdit"
                  >
                </template>
                <template v-else>
                  <span class="cell-display">{{ currentLineConfig.workingDaysPerWeek }}</span>
                </template>
              </td>
              <td
                class="editable-cell"
                :class="{ 'is-editing': editingConfig === 'shiftsPerDay' }"
                @dblclick="startConfigEdit('shiftsPerDay', $event)"
                @contextmenu.prevent="startConfigEdit('shiftsPerDay', $event)"
              >
                <template v-if="editingConfig === 'shiftsPerDay'">
                  <input
                    ref="configInputRef"
                    type="number"
                    v-model.number="lineConfigs[selectedLine].shiftsPerDay"
                    class="cell-input"
                    min="1"
                    max="5"
                    @blur="finishConfigEdit"
                    @keydown.enter="finishConfigEdit"
                    @keydown.escape="cancelConfigEdit"
                  >
                </template>
                <template v-else>
                  <span class="cell-display">{{ currentLineConfig.shiftsPerDay }}</span>
                </template>
              </td>
              <td
                class="editable-cell"
                :class="{ 'is-editing': editingConfig === 'hoursPerShift' }"
                @dblclick="startConfigEdit('hoursPerShift', $event)"
                @contextmenu.prevent="startConfigEdit('hoursPerShift', $event)"
              >
                <template v-if="editingConfig === 'hoursPerShift'">
                  <input
                    ref="configInputRef"
                    type="number"
                    v-model.number="lineConfigs[selectedLine].hoursPerShift"
                    class="cell-input"
                    min="0.1"
                    step="0.1"
                    @blur="finishConfigEdit"
                    @keydown.enter="finishConfigEdit"
                    @keydown.escape="cancelConfigEdit"
                  >
                </template>
                <template v-else>
                  <span class="cell-display">{{ currentLineConfig.hoursPerShift }}</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 保存快照弹窗 -->
    <div v-if="showSnapshotModal" class="modal-overlay" @click.self="showSnapshotModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>保存快照</h3>
          <button class="close-btn" @click="showSnapshotModal = false">脳</button>
        </div>
        <div class="modal-body">
          <label class="form-label">快照名称</label>
          <input
            type="text"
            v-model="snapshotName"
            placeholder="请输入快照名称"
            class="form-input"
          >
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showSnapshotModal = false">取消</button>
          <button class="btn btn-primary" @click="saveSnapshot" :disabled="!snapshotName">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/mrp'
import { getCapacityAssessment } from '@/api/capacityRealtime'
import { getLines } from '@/api/line'
import { saveSnapshot as saveSnapshotApi } from '@/api/simulationSnapshot'
import { downloadCsv } from '@/utils/export'
import BaseSelect from '@/components/common/BaseSelect.vue'

const { token } = useAuth()
const { showToast } = useToast()

// sessionStorage keys
const SESSION_KEY = 'capics_capacity_realtime'

// 淇濆瓨鐘舵€佸埌sessionStorage
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
      selectedLine: selectedLine.value,
      lineConfigs: lineConfigs.value
    }))
  } catch (e) {
    console.error('Save state error:', e)
  }
}

// 浠巗essionStorage鎭㈠鐘舵€?
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
      lineConfigs.value = state.lineConfigs || {}
      return true
    }
  } catch (e) {
    console.error('Restore state error:', e)
  }
  return false
}

// 姹囨€昏〃鎷栧姩鐘舵€?
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const summaryPosition = ref({ x: 926, y: 1 })
const showSummary = ref(true)

// 姹囨€昏〃鏄剧ず鐨勫懆鏁帮紙鍏ㄩ儴锛?
const displayWeeks = computed(() => {
  return weeks.value
})

// 鎷栧姩鐩稿叧鍑芥暟
const startDrag = (e) => {
  if (e.target.closest('.close-btn')) return
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - summaryPosition.value.x,
    y: e.clientY - summaryPosition.value.y
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  summaryPosition.value = {
    x: e.clientX - dragOffset.value.x,
    y: e.clientY - dragOffset.value.y
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const closeSummary = () => {
  showSummary.value = false
}

// MRP绛涢€夋潯浠?
const createdBys = ref([])
const fileNames = ref([])
const versions = ref([])
const selectedCreatedBy = ref('')
const selectedFileName = ref('')
const selectedVersion = ref('')

// 鐢熶骇绾跨瓫閫?
const selectedLine = ref('')
const linesData = ref({})
const weeks = ref([])
const weekDates = ref({})
const warnings = ref([])

// 浜х嚎閰嶇疆 - 鎸夌敓浜х嚎瀛樺偍
const lineConfigs = ref({})
const lineNameMap = ref({})

const loading = ref(false)
const error = ref('')

// 淇濆瓨蹇収鐘舵€?
const showSnapshotModal = ref(false)
const snapshotName = ref('')

// 淇濆瓨蹇収
const saveSnapshot = async () => {
  if (!snapshotName.value) {
    showToast('请输入快照名称', 'warning')
    return
  }
  try {
    // 娣辨嫹璐?linesData锛屽苟閲嶆柊璁＄畻鎵€鏈?loading 瀛楁
    const snapshotLinesData = JSON.parse(JSON.stringify(linesData.value))
    for (const lineCode of Object.keys(snapshotLinesData)) {
      for (const item of snapshotLinesData[lineCode]) {
        for (const week of weeks.value) {
          item[week + '_loading'] = calcLoading(item, week)
        }
      }
    }
    const data = {
      createdBy: selectedCreatedBy.value,
      fileName: selectedFileName.value,
      version: selectedVersion.value,
      snapshotName: snapshotName.value,
      source: 'dynamic',
      dimension: 'week',
      linesData: snapshotLinesData,
      dates: weeks.value,
      dateLabels: weekDates.value
    }
    const result = await saveSnapshotApi(token.value, data)
    if (result.success) {
      showToast('快照保存成功', 'success')
      showSnapshotModal.value = false
      snapshotName.value = ''
    } else {
      showToast('保存失败: ' + result.message, 'error')
    }
  } catch (err) {
    console.error('Save snapshot error:', err)
    showToast('保存失败', 'error')
  }
}

// 缂栬緫鐘舵€佽拷韪?
const editingCell = ref(null) // { item, field }
const editingConfig = ref(null)
const editInputRef = ref(null)
const configInputRef = ref(null)

// 鍙敤鐨勭敓浜х嚎鍒楄〃
const availableLines = computed(() => {
  return Object.keys(linesData.value).sort()
})

const formatLineLabel = (lineCode) => {
  if (!lineCode) return ''
  const lineName = lineNameMap.value[lineCode]
  return lineName ? `${lineCode} - ${lineName}` : lineCode
}

// 褰撳墠閫変腑鐨勭敓浜х嚎鏁版嵁
const selectedLineData = computed(() => {
  if (!selectedLine.value || !linesData.value[selectedLine.value]) {
    return []
  }
  return linesData.value[selectedLine.value]
})

// 鍙紪杈戠殑鏈湴鏁版嵁锛堢洿鎺ュ紩鐢╯electedLineData锛屼笉瑕佸壇鏈級
const editableData = computed(() => {
  return selectedLineData.value
})

// 褰撳墠閫変腑浜х嚎鐨勯厤缃?
const currentLineConfig = computed(() => {
  if (!selectedLine.value) return { workingDaysPerWeek: 5, shiftsPerDay: 2, hoursPerShift: 8 }
  return lineConfigs.value[selectedLine.value] || { workingDaysPerWeek: 5, shiftsPerDay: 2, hoursPerShift: 8 }
})

// 姹囨€绘暟鎹?
const summaryData = computed(() => {
  const items = selectedLineData.value
  const weeksVal = weeks.value

  // 1. 璁＄畻鐢熶骇绾挎€昏LOAD
  const totalRow = { dimension: '总计', loadings: {} }
  weeksVal.forEach(week => {
    let totalLoad = 0
    items.forEach(item => {
      totalLoad += calcLoading(item, week)
    })
    totalRow.loadings[week] = totalLoad
  })

  // 2. 鎸?PF 鍒嗙粍璁＄畻LOAD
  const pfGroups = {}
  items.forEach(item => {
    const pf = item.pf || '未分类'
    if (!pfGroups[pf]) {
      pfGroups[pf] = { dimension: pf, loadings: {} }
    }
    weeksVal.forEach(week => {
      if (!pfGroups[pf].loadings[week]) pfGroups[pf].loadings[week] = 0
      pfGroups[pf].loadings[week] += calcLoading(item, week)
    })
  })

  return [totalRow, ...Object.values(pfGroups)]
})

// 鍒ゆ柇鍗曞厓鏍兼槸鍚﹀浜庣紪杈戠姸鎬?
const isCellEditing = (item, field) => {
  if (!editingCell.value) return false
  const key = item.itemNumber + '_' + item.componentNumber
  const editKey = editingCell.value.item.itemNumber + '_' + editingCell.value.item.componentNumber
  return editKey === key && editingCell.value.field === field
}

// 寮€濮嬬紪杈戝崟鍏冩牸
const startEdit = async (item, field, event) => {
  editingCell.value = { item, field }
  await nextTick()
  if (editInputRef.value) {
    const input = Array.isArray(editInputRef.value) ? editInputRef.value[0] : editInputRef.value
    if (input) input.focus()
  }
}

// 瀹屾垚缂栬緫
const finishEdit = (item, field) => {
  editingCell.value = null
}

// 鍙栨秷缂栬緫
const cancelEdit = (item, field) => {
  editingCell.value = null
}

// 鍏抽棴鎵€鏈夌紪杈戠姸鎬?
const closeAllEditing = () => {
  editingCell.value = null
  editingConfig.value = null
}

// 寮€濮嬬紪杈戜骇绾块厤缃?
const startConfigEdit = async (field, event) => {
  editingConfig.value = field
  await nextTick()
  if (configInputRef.value) {
    const input = Array.isArray(configInputRef.value) ? configInputRef.value[0] : configInputRef.value
    if (input) input.focus()
  }
}

// 瀹屾垚/鍙栨秷浜х嚎閰嶇疆缂栬緫
const finishConfigEdit = () => {
  editingConfig.value = null
}

const cancelConfigEdit = () => {
  editingConfig.value = null
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
  lineConfigs.value = {}
  closeAllEditing()
}

// 鍔犺浇浜х嚎閰嶇疆
const loadLineConfigs = async (lineCodes) => {
  try {
    const response = await getLines(token.value)
    if (response.success && response.data) {
      const configs = {}
      const names = {}
      response.data.forEach(line => {
        if (lineCodes.includes(line.lineCode)) {
          names[line.lineCode] = line.lineName || ''
          configs[line.lineCode] = {
            workingDaysPerWeek: line.workingDaysPerWeek || 5,
            shiftsPerDay: line.shiftsPerDay || 2,
            hoursPerShift: line.hoursPerShift || 8
          }
        }
      })
      lineConfigs.value = configs
      lineNameMap.value = names
    }
  } catch (err) {
    console.error('Load line configs error:', err)
  }
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

        // 鑷姩閫変腑绗竴鏉＄敓浜х嚎
        const lineCodes = Object.keys(result.lines)
        if (lineCodes.length > 0) {
          selectedLine.value = lineCodes[0]
        }

        // 淇濆瓨鐘舵€?
        saveState()

        // 鍔犺浇鍚勪骇绾块厤缃?
        await loadLineConfigs(lineCodes)
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

// 璁＄畻鐝骇閲?= (3600 / CT) 脳 (OEE / 100) 脳 hoursPerShift
const calcShiftOutput = (item) => {
  const ct = parseFloat(item.ct) || 0
  const oee = parseFloat(item.oee) || 0
  const hours = parseFloat(currentLineConfig.value.hoursPerShift) || 0
  if (ct <= 0 || hours <= 0) return 0
  return (3600 / ct) * (oee / 100) * hours
}

// 璁＄畻LOAD = (闇€姹傞噺 脳 CT) / (宸ヤ綔澶╂暟 脳 鐝暟 脳 姣忕彮鏃堕暱 脳 OEE/100 脳 3600)
const calcLoading = (item, week) => {
  const demand = parseFloat(item[week + '_demand']) || 0
  const ct = parseFloat(item.ct) || 0
  const oee = parseFloat(item.oee) || 0
  const { workingDaysPerWeek, shiftsPerDay, hoursPerShift } = currentLineConfig.value
  if (demand <= 0 || ct <= 0 || hoursPerShift <= 0 || oee <= 0) return 0
  const denominator = workingDaysPerWeek * shiftsPerDay * hoursPerShift * (oee / 100) * 3600
  return (demand * ct) / denominator
}

const formatShiftOutput = (val) => {
  if (val === 0) return '-'
  return Math.round(val).toString()
}

const formatDemand = (demand) => {
  if (demand === null || demand === undefined || demand === '-' || demand === 0) return '-'
  const num = parseFloat(demand)
  if (isNaN(num)) return '-'
  return num.toFixed(0)
}

const formatLoading = (loading) => {
  if (loading === 0) return '-'
  return (loading * 100).toFixed(2) + '%'
}

const handleExportRealtimeWeekly = () => {
  if (!selectedLine.value || editableData.value.length === 0) {
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
  const rows = editableData.value.map(item => {
    const row = {
      lineCode: formatLineLabel(selectedLine.value),
      itemNumber: item.itemNumber || '',
      description: item.description || '',
      componentNumber: item.componentNumber || '',
      shiftOutput: Math.round(calcShiftOutput(item)) || '',
      shiftWorkers: item.shiftWorkers ?? '',
      ct: item.ct ?? '',
      oee: item.oee ?? ''
    }
    weeks.value.forEach(week => {
      row[`${week}_demand`] = item[`${week}_demand`] ?? ''
      row[`${week}_loading`] = (calcLoading(item, week) * 100).toFixed(2) + '%'
    })
    return row
  })
  downloadCsv(`动态产能模拟-周-${selectedLine.value || '全部'}.csv`, headers, rows)
  showToast('导出成功', 'success')
}

onMounted(() => {
  loadCreatedBys().then(() => {
    // 鎭㈠鐘舵€?    restoreState()
    const lineCodes = Object.keys(linesData.value || {})
    if (lineCodes.length > 0) {
      loadLineConfigs(lineCodes)
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

/* === Page Header === */
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

/* === Filters === */
.filters-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.5rem;
  flex-shrink: 0;
  flex-wrap: nowrap;
  overflow-x: auto;
}

.filters-row .btn {
  padding: 0.45rem 0.7rem;
}

.filter-select-created-by {
  width: 116px;
  min-width: 116px;
}

.filter-select-file-name {
  width: 136px;
  min-width: 136px;
}

.filter-select-version {
  width: 116px;
  min-width: 116px;
}

.line-filter-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.5rem;
  flex-shrink: 0;
}

.line-filter-label {
  font-weight: 500;
  color: var(--foreground);
}

/* === Summary Panel === */
.summary-panel {
  position: fixed;
  width: 1400px;
  max-height: 490px;
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: move;
  user-select: none;
}

.summary-title {
  padding: 0.5rem 0.75rem;
  font-weight: 600;
  font-size: 0.875rem;
  background: var(--muted);
  border-bottom: 1px solid var(--border-light);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  color: var(--primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
}

.close-btn {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: var(--muted-foreground);
  font-size: 1.2rem;
  line-height: 1;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.close-btn:hover {
  background: var(--border);
  color: var(--foreground);
}

.summary-scroll {
  flex: 1;
  overflow: auto;
  white-space: nowrap;
}

.summary-table {
  width: max-content;
  border-collapse: collapse;
  font-size: 0.75rem;
}

.summary-table th,
.summary-table td {
  border: 1px solid var(--border-light);
  padding: 0.375rem 0.5rem;
  text-align: center;
  white-space: nowrap;
}

.summary-table th {
  background: var(--muted);
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 20;
}

.summary-table .sticky-col {
  position: sticky;
  left: 0;
  background: white;
  z-index: 10;
  text-align: left;
  font-weight: 500;
  min-width: 80px;
}

.summary-table th.sticky-col {
  background: var(--muted);
  z-index: 30;
}

.summary-table .total-row {
  font-weight: 700;
  background: var(--muted);
}

.summary-table .total-row .sticky-col {
  background: var(--muted);
}

.summary-table .loading-value {
  font-weight: 600;
  color: var(--foreground);
}

.summary-table .loading-value.high-load {
  color: #DC2626;
}

.summary-scroll::-webkit-scrollbar {
  height: 6px;
  width: 6px;
}

.summary-scroll::-webkit-scrollbar-track {
  background: var(--muted);
  border-radius: 3px;
}

.summary-scroll::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}

.summary-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--muted-foreground);
}

/* === Editable Cell === */
.editable-cell {
  cursor: cell;
  position: relative;
}

.editable-cell:hover {
  background: #E8F4FD !important;
}

.editable-cell.is-editing {
  padding: 0 !important;
  background: #fff !important;
}

/* === Cell Input === */
.cell-input {
  width: 100%;
  height: 100%;
  min-height: 36px;
  border: 2px solid var(--primary);
  border-radius: 0;
  padding: 0 0.5rem;
  font-size: 0.875rem;
  text-align: center;
  background: #fff;
  color: var(--foreground);
  outline: none;
}

/* === Cell Display === */
.cell-display {
  display: inline-block;
  width: 100%;
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
  min-height: 300px;
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
  z-index: 20;
}

.header-row-1 th {
  top: 0;
  z-index: 22;
  background: white;
  color: var(--foreground);
}

.header-row-2 th {
  top: var(--header-row-height);
  z-index: 21;
  background: var(--foreground);
  color: white;
}

/* === Header sticky columns === */
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
tbody td.sticky-col-3 {
  background: #fff;
  font-weight: 500;
  text-align: left;
}

tbody td.sticky-col-4,
tbody td.sticky-col-5,
tbody td.sticky-col-6,
tbody td.sticky-col-7 {
  background: #fff;
  font-weight: 500;
  text-align: center;
}

.data-cell {
  background: var(--muted);
  color: var(--foreground);
  text-align: center;
}

.loading-cell {
  font-weight: 600;
}

.high-load {
  color: #DC2626;
  background: #FEE2E2 !important;
}

/* === Sticky Column Borders === */
.header-row-1 th.sticky-col-1, .header-row-2 th.sticky-col-1, tbody td.sticky-col-1 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-2, .header-row-2 th.sticky-col-2, tbody td.sticky-col-2 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-3, .header-row-2 th.sticky-col-3, tbody td.sticky-col-3 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-4, .header-row-2 th.sticky-col-4, tbody td.sticky-col-4 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-5, .header-row-2 th.sticky-col-5, tbody td.sticky-col-5 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-6, .header-row-2 th.sticky-col-6, tbody td.sticky-col-6 { border-right: 1px solid var(--border); }
.header-row-1 th.sticky-col-7, .header-row-2 th.sticky-col-7, tbody td.sticky-col-7 { border-right: 1px solid var(--border); }

/* === Line Config Section === */
.line-config-section {
  margin-top: 1.5rem;
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  padding: 1rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--foreground);
  margin-bottom: 1rem;
}

.config-table-wrapper {
  overflow-x: auto;
}

.config-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.config-table th,
.config-table td {
  border: 1px solid var(--border-light);
  padding: 0.75rem 1rem;
  text-align: center;
}

.config-table th {
  background: var(--muted);
  font-weight: 600;
  color: var(--foreground);
}

.config-table .line-name {
  font-weight: 600;
  color: var(--primary);
}

/* === Scrollbar - Apple Style === */
.table-scroll::-webkit-scrollbar,
.config-table-wrapper::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.table-scroll::-webkit-scrollbar-track,
.config-table-wrapper::-webkit-scrollbar-track {
  background: var(--muted);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb,
.config-table-wrapper::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.table-scroll::-webkit-scrollbar-thumb:hover,
.config-table-wrapper::-webkit-scrollbar-thumb:hover {
  background: var(--muted-foreground);
}

.table-scroll::-webkit-scrollbar-corner,
.config-table-wrapper::-webkit-scrollbar-corner {
  background: var(--muted);
}

/* Modal */
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

.modal-content {
  background: white;
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  min-width: 400px;
  max-width: 90%;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.modal-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
}

.modal-body {
  margin-bottom: 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
}
</style>

