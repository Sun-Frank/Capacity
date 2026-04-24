<template>
  <div class="page product-line-page">
    <div class="page-header">
      <h1 class="page-title">产线一览</h1>
      <p class="page-subtitle">按工艺段查看周、月产能利用率趋势和产线分布</p>
    </div>

    <section class="table-wrapper filter-panel">
      <div class="filters-row">
        <BaseSelect
          v-model="selectedCreatedBy"
          :options="createdBys.map((item) => ({ value: item, label: item }))"
          placeholder="选择导入人"
          @update:modelValue="onCreatedByChange"
        />
        <BaseSelect
          v-model="selectedFileName"
          :options="fileNames.map((item) => ({ value: item, label: item }))"
          placeholder="选择文件"
          :disabled="!selectedCreatedBy"
          @update:modelValue="onFileNameChange"
        />
        <BaseSelect
          v-model="selectedVersion"
          :options="versions.map((item) => ({ value: item, label: item }))"
          placeholder="选择版本"
          :disabled="!selectedFileName"
          @update:modelValue="onVersionChange"
        />
        <BaseSelect
          v-model="selectedSavedResultVersion"
          :options="savedResultVersions.map((item) => ({ value: item, label: item }))"
          placeholder="已计算结果版本"
          :disabled="!selectedVersion"
        />
        <button class="btn btn-primary" :disabled="loading" @click="loadOverview">
          {{ loading ? '加载中...' : '加载数据' }}
        </button>
      </div>
    </section>

    <section v-if="hasAnyData" class="table-wrapper process-panel">
      <div class="process-chips">
        <button
          v-for="processCode in processOptions"
          :key="processCode"
          class="process-chip"
          :class="{ active: selectedProcess === processCode }"
          @click="selectedProcess = processCode"
        >
          {{ getProcessDisplay(processCode) }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="state-panel">正在加载周、月产能数据...</div>
    <div v-else-if="pageError" class="state-panel error">{{ pageError }}</div>
    <div v-else-if="!hasAnyData" class="state-panel">
      请选择导入人、文件、版本后加载产线一览。
    </div>
    <template v-else>
      <section class="table-wrapper tab-panel">
        <button class="tab-chip" :class="{ active: activeTab === 'week' }" @click="activeTab = 'week'">周</button>
        <button class="tab-chip" :class="{ active: activeTab === 'month' }" @click="activeTab = 'month'">月</button>
        <span class="tab-note">{{ activeWindowNote }}</span>
      </section>

      <section class="stats-grid overview-stats">
        <article class="stat-card">
          <div class="stat-label">{{ activeTabLabel }}平均利用率</div>
          <div class="stat-value">{{ formatRate(activeSummary.avgLoading) }}</div>
          <div class="stat-unit">{{ processDisplay }}</div>
        </article>
        <article class="stat-card">
          <div class="stat-label">{{ activeTabLabel }}峰值利用率</div>
          <div class="stat-value">{{ formatRate(activeSummary.peakLoading) }}</div>
          <div class="stat-unit">高峰负荷</div>
        </article>
        <article class="stat-card">
          <div class="stat-label">产线数</div>
          <div class="stat-value">{{ activeSummary.lineCount || 0 }}</div>
          <div class="stat-unit">{{ processDisplay }}</div>
        </article>
        <article class="stat-card">
          <div class="stat-label">超载产线数</div>
          <div class="stat-value">{{ activeSummary.overloadedLineCount || 0 }}</div>
          <div class="stat-unit">利用率 &gt; 100%</div>
        </article>
      </section>

      <section v-if="activeSummary.lineCount" class="charts-grid">
        <article class="chart-panel chart-panel-wide">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">{{ activeTabLabel }}利用率趋势</h2>
              <p class="panel-copy">柱线组合图，柱表示总需求量，线表示平均利用率。</p>
            </div>
          </div>
          <v-chart class="chart-box" autoresize :option="trendOption" />
        </article>

        <article class="chart-panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">{{ activeTabLabel }}产线排名</h2>
              <p class="panel-copy">按平均利用率排序，定位当前工艺段高负荷产线。</p>
            </div>
          </div>
          <v-chart class="chart-box" autoresize :option="rankOption" />
        </article>

        <article class="chart-panel chart-panel-full">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">{{ activeTabLabel }}热力矩阵</h2>
              <p class="panel-copy">横轴为时间，纵轴为产线；默认大于100%显示红色，FAL1007N、ICP1001N 大于200%才显示红色，橙色表示超100但未到特殊红线阈值。</p>
            </div>
            <button class="btn" @click="handleExportHeatmap">导出热力矩阵</button>
          </div>
          <div class="heatmap-table-scroll">
            <table class="heatmap-table">
              <thead>
                <tr>
                  <th class="heatmap-line-head">产线</th>
                  <th
                    v-for="column in heatmapMatrix.columns"
                    :key="column.key"
                    class="heatmap-period-head"
                  >
                    {{ column.label }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in heatmapMatrix.rows" :key="row.lineCode">
                  <th class="heatmap-line-cell">{{ row.displayName }}</th>
                  <td
                    v-for="cell in row.cells"
                    :key="`${row.lineCode}-${cell.key}`"
                    :class="['heatmap-value-cell', getHeatmapCellClass(cell.value)]"
                    :style="{ backgroundColor: cell.color, color: getHeatmapTextColor(cell.value) }"
                    @mouseenter="showHeatmapTooltip($event, row, cell)"
                    @mousemove="moveHeatmapTooltip($event)"
                    @mouseleave="hideHeatmapTooltip"
                  >
                    {{ cell.value }}%
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div
            v-if="heatmapTooltip.visible"
            class="heatmap-tooltip"
            :style="{ left: `${heatmapTooltip.left}px`, top: `${heatmapTooltip.top}px` }"
          >
            <div class="heatmap-tooltip-title">{{ heatmapTooltip.lineLabel }}</div>
            <div>{{ heatmapTooltip.periodLabel }}</div>
            <div>利用率：{{ heatmapTooltip.value }}%</div>
          </div>
          <div class="heatmap-legend-note">
            <span class="legend-chip legend-blue"></span>
            <span>蓝色：100%及以下</span>
            <span class="legend-chip legend-orange"></span>
            <span>橙色：超100但未到特殊红线阈值</span>
            <span class="legend-chip legend-red"></span>
            <span>红色：达到红线阈值</span>
          </div>
        </article>
      </section>
      <div v-else class="state-panel">当前工艺段暂无可展示的{{ activeTabLabel }}数据。</div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import BaseSelect from '@/components/common/BaseSelect.vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { downloadCsv } from '@/utils/export'
import { getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/mrp'
import { getLines } from '@/api/line'
import { getSnapshot, getSnapshotNames } from '@/api/simulationSnapshot'
import {
  ALL_PROCESS,
  buildProductLineOverview,
  formatRate,
  getHeatmapCellColor,
  getHeatmapTrendWindow,
  getProcessDisplay
} from '@/composables/useProductLineOverview'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent
])

const { token } = useAuth()
const { showToast } = useToast()

const createdBys = ref([])
const fileNames = ref([])
const versions = ref([])

const selectedCreatedBy = ref('')
const selectedFileName = ref('')
const selectedVersion = ref('')
const selectedSavedResultVersion = ref('')
const savedResultVersions = ref([])

const loading = ref(false)
const pageError = ref('')
const activeTab = ref('week')
const selectedProcess = ref(ALL_PROCESS)
const heatmapTooltip = ref({
  visible: false,
  left: 0,
  top: 0,
  lineLabel: '',
  periodLabel: '',
  value: ''
})

const overviewData = ref({
  processOptions: [ALL_PROCESS],
  weekly: {},
  monthly: {}
})

const processOptions = computed(() => overviewData.value.processOptions || [ALL_PROCESS])

const hasAnyData = computed(() => {
  return Object.keys(overviewData.value.weekly || {}).length > 0 || Object.keys(overviewData.value.monthly || {}).length > 0
})

const activeSummary = computed(() => {
  const summaryMap = activeTab.value === 'week' ? overviewData.value.weekly : overviewData.value.monthly
  return summaryMap[selectedProcess.value] || {
    lineCount: 0,
    overloadedLineCount: 0,
    avgLoading: 0,
    peakLoading: 0,
    trend: [],
    lines: [],
    heatmapRows: []
  }
})

const processDisplay = computed(() => getProcessDisplay(selectedProcess.value))
const activeTabLabel = computed(() => (activeTab.value === 'week' ? '周' : '月'))
const activeWindowNote = computed(() => (
  activeTab.value === 'week'
    ? '仅统计并展示开始日期后的前24周'
    : '仅统计并展示开始日期后的前12个月'
))

const trendOption = computed(() => {
  const trend = activeSummary.value.trend || []
  return {
    color: ['#0a69c0', '#f05d23'],
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value) => `${Number(value).toFixed(1)}`
    },
    legend: {
      top: 0,
      textStyle: { color: '#254164' }
    },
    grid: {
      left: 56,
      right: 28,
      top: 44,
      bottom: 44
    },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.label),
      axisLabel: { color: '#4f6485' }
    },
    yAxis: [
      {
        type: 'value',
        name: '需求量',
        axisLabel: { color: '#4f6485' },
        splitLine: { lineStyle: { color: '#e4ecf8' } }
      },
      {
        type: 'value',
        name: '利用率',
        min: 0,
        axisLabel: {
          color: '#4f6485',
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '总需求量',
        type: 'bar',
        barMaxWidth: 28,
        data: trend.map((item) => Number(item.totalDemand.toFixed(0)))
      },
      {
        name: '平均利用率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        data: trend.map((item) => Number((item.avgLoading * 100).toFixed(1)))
      }
    ]
  }
})

const rankOption = computed(() => {
  const lines = [...(activeSummary.value.lines || [])].slice(0, 12).reverse()
  return {
    color: ['#0091db'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => `${Number(value).toFixed(1)}%`
    },
    grid: {
      left: 110,
      right: 18,
      top: 18,
      bottom: 18
    },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: '{value}%', color: '#4f6485' },
      splitLine: { lineStyle: { color: '#e4ecf8' } }
    },
    yAxis: {
      type: 'category',
      data: lines.map((line) => line.displayName || line.lineCode),
      axisLabel: { color: '#4f6485' }
    },
    series: [
      {
        type: 'bar',
        data: lines.map((line) => Number((line.avgLoading * 100).toFixed(1))),
        barWidth: 18,
        label: {
          show: true,
          position: 'right',
          color: '#254164',
          formatter: '{c}%'
        }
      }
    ]
  }
})

const heatmapMatrix = computed(() => {
  const rows = activeSummary.value.heatmapRows || []
  const trend = getHeatmapTrendWindow(activeSummary.value.trend || [], activeTab.value)
  return {
    columns: trend.map((item) => ({
      key: item.key,
      label: item.label
    })),
    rows: rows.map((line) => ({
      lineCode: line.lineCode,
      displayName: line.displayName || line.lineCode,
      fullLabel: line.label || line.displayName || line.lineCode,
      cells: trend.map((item) => {
        const value = Number(((line.loadingByDate?.[item.key] || 0) * 100).toFixed(1))
        return {
          key: item.key,
          label: item.label,
          value,
          color: getHeatmapCellColor(line.lineCode, value)
        }
      })
    }))
  }
})

function getHeatmapTextColor(value) {
  if (value > 100) return '#ffffff'
  if (value > 80) return '#ffffff'
  return '#17365d'
}

function getHeatmapCellClass(value) {
  if (value > 150) return 'heatmap-cell-critical'
  if (value > 100) return 'heatmap-cell-hot'
  if (value > 80) return 'heatmap-cell-strong'
  return ''
}

function showHeatmapTooltip(event, row, cell) {
  heatmapTooltip.value = {
    visible: true,
    left: event.clientX + 14,
    top: event.clientY + 14,
    lineLabel: row.fullLabel,
    periodLabel: cell.label || cell.key,
    value: cell.value
  }
}

function moveHeatmapTooltip(event) {
  if (!heatmapTooltip.value.visible) return
  heatmapTooltip.value.left = event.clientX + 14
  heatmapTooltip.value.top = event.clientY + 14
}

function hideHeatmapTooltip() {
  heatmapTooltip.value.visible = false
}

function handleExportHeatmap() {
  if (!heatmapMatrix.value.rows.length || !heatmapMatrix.value.columns.length) {
    showToast('暂无可导出热力矩阵数据', 'warning')
    return
  }

  const headers = [
    { key: 'lineLabel', label: '产线' }
  ]

  heatmapMatrix.value.columns.forEach((column) => {
    headers.push({ key: column.key, label: column.label })
  })

  const rows = heatmapMatrix.value.rows.map((row) => {
    const exportRow = { lineLabel: row.fullLabel }
    row.cells.forEach((cell) => {
      exportRow[cell.key] = `${cell.value}%`
    })
    return exportRow
  })

  downloadCsv(`产线一览-${activeTabLabel.value}-热力矩阵-${selectedProcess.value}.csv`, headers, rows)
  showToast('热力矩阵导出成功', 'success')
}

async function loadCreatedByOptions() {
  try {
    const data = await getCreatedBys(token.value)
    createdBys.value = data.data || []
  } catch (error) {
    console.error('Load createdBys error:', error)
  }
}

async function onCreatedByChange() {
  selectedFileName.value = ''
  selectedVersion.value = ''
  selectedSavedResultVersion.value = ''
  savedResultVersions.value = []
  versions.value = []
  overviewData.value = { processOptions: [ALL_PROCESS], weekly: {}, monthly: {} }
  pageError.value = ''
  if (!selectedCreatedBy.value) {
    fileNames.value = []
    return
  }
  try {
    const data = await getFileNamesByCreatedBy(token.value, selectedCreatedBy.value)
    fileNames.value = data.data || []
  } catch (error) {
    console.error('Load fileNames error:', error)
    showToast('加载文件列表失败', 'error')
  }
}

async function onFileNameChange() {
  selectedVersion.value = ''
  selectedSavedResultVersion.value = ''
  savedResultVersions.value = []
  overviewData.value = { processOptions: [ALL_PROCESS], weekly: {}, monthly: {} }
  pageError.value = ''
  if (!selectedCreatedBy.value || !selectedFileName.value) {
    versions.value = []
    return
  }
  try {
    const data = await getVersionsByCreatedByAndFileName(token.value, selectedCreatedBy.value, selectedFileName.value)
    versions.value = data.data || []
  } catch (error) {
    console.error('Load versions error:', error)
    showToast('加载版本列表失败', 'error')
  }
}

async function onVersionChange() {
  selectedSavedResultVersion.value = ''
  savedResultVersions.value = []
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    return
  }
  try {
    const [weekSnapshots, monthSnapshots] = await Promise.all([
      getSnapshotNames(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value,
        'static',
        'week'
      ),
      getSnapshotNames(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value,
        'static',
        'month'
      )
    ])
    const weekNames = weekSnapshots?.success ? (weekSnapshots.data || []) : []
    const monthNames = monthSnapshots?.success ? (monthSnapshots.data || []) : []
    const monthSet = new Set(monthNames)
    savedResultVersions.value = weekNames.filter((name) => monthSet.has(name))
  } catch (error) {
    console.error('Load saved result versions error:', error)
    showToast('加载已计算结果版本失败', 'error')
  }
}

async function loadOverview() {
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    showToast('请选择完整的导入人、文件和版本', 'warning')
    return
  }
  if (!selectedSavedResultVersion.value) {
    showToast('请选择已计算结果版本', 'warning')
    return
  }

  loading.value = true
  pageError.value = ''

  try {
    const [weeklyRes, monthlyRes, linesRes] = await Promise.all([
      getSnapshot(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value,
        selectedSavedResultVersion.value,
        'static',
        'week'
      ),
      getSnapshot(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value,
        selectedSavedResultVersion.value,
        'static',
        'month'
      ),
      getLines(token.value)
    ])

    if (!weeklyRes?.success) {
      throw new Error(weeklyRes?.message || '周数据加载失败')
    }
    if (!monthlyRes?.success) {
      throw new Error(monthlyRes?.message || '月数据加载失败')
    }
    if (!linesRes?.success) {
      throw new Error(linesRes?.message || '产线数据加载失败')
    }

    const lineNameMap = {}
    ;(linesRes.data || []).forEach((line) => {
      lineNameMap[line.lineCode] = line.lineName || ''
    })

    const weeklyPayload = {
      lines: weeklyRes.data?.linesData || {},
      weeks: weeklyRes.data?.dates || [],
      weekDates: weeklyRes.data?.dateLabels || {}
    }
    const monthlyPayload = {
      lines: monthlyRes.data?.linesData || {},
      months: monthlyRes.data?.dates || [],
      monthDates: monthlyRes.data?.dateLabels || {}
    }
    overviewData.value = buildProductLineOverview(weeklyPayload, monthlyPayload, lineNameMap)
    selectedProcess.value = overviewData.value.processOptions.includes(selectedProcess.value) ? selectedProcess.value : ALL_PROCESS
    showToast('产线一览已更新', 'success')
  } catch (error) {
    console.error('Load product line overview error:', error)
    pageError.value = error.message || '加载产线一览失败'
    overviewData.value = { processOptions: [ALL_PROCESS], weekly: {}, monthly: {} }
    showToast('加载产线一览失败', 'error')
  } finally {
    loading.value = false
  }
}

loadCreatedByOptions()
</script>

<style scoped>
.product-line-page {
  gap: 1rem;
  overflow: auto;
}

.filter-panel,
.process-panel,
.tab-panel,
.charts-grid,
.overview-stats {
  flex: 0 0 auto;
}

.filter-panel,
.process-panel,
.tab-panel {
  padding: 1rem 1.2rem;
}

.filters-row {
  display: flex;
  gap: 0.85rem;
  align-items: center;
  flex-wrap: wrap;
}

.process-chips,
.tab-panel {
  display: flex;
  gap: 0.7rem;
  flex-wrap: wrap;
  align-items: center;
}

.process-chip,
.tab-chip {
  border: 1px solid #b8cae4;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #eef5ff 100%);
  color: #23456f;
  cursor: pointer;
  font-weight: 700;
  transition: 0.2s ease;
}

.process-chip {
  padding: 0.55rem 0.85rem;
}

.tab-chip {
  padding: 0.62rem 1rem;
  min-width: 4rem;
}

.tab-note {
  margin-left: auto;
  color: #60718b;
  font-size: 0.9rem;
  white-space: nowrap;
}

.process-chip.active,
.tab-chip.active {
  background: linear-gradient(135deg, #0a69c0 0%, #0091db 100%);
  color: white;
  border-color: #0a69c0;
  box-shadow: 0 10px 20px rgba(10, 105, 192, 0.2);
}

.state-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 16rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #d7e2f1;
  border-radius: 8px;
  color: var(--muted-foreground);
}

.state-panel.error {
  color: var(--error);
}

.overview-stats {
  margin-bottom: 0;
}

.charts-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 1rem;
}

.chart-panel {
  min-width: 0;
  padding: 1rem 1rem 0.8rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #d7e2f1;
  border-radius: 8px;
  box-shadow: 0 10px 22px rgba(8, 34, 72, 0.08);
}

.chart-panel-wide,
.chart-panel-full {
  grid-column: span 1;
}

.chart-panel-full {
  grid-column: 1 / -1;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.panel-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.15rem;
  color: #163153;
}

.panel-copy {
  margin: 0.25rem 0 0;
  color: #60718b;
  font-size: 0.9rem;
}

.chart-box {
  width: 100%;
  height: 340px;
}

.chart-box-heatmap {
  height: 440px;
}

.heatmap-table-scroll {
  overflow: auto;
  border: 1px solid #e3ebf7;
  border-radius: 8px;
}

.heatmap-table {
  width: 100%;
  min-width: 960px;
  border-collapse: separate;
  border-spacing: 0;
}

.heatmap-table th,
.heatmap-table td {
  padding: 0.75rem 0.6rem;
  border-right: 1px solid #eef3fa;
  border-bottom: 1px solid #eef3fa;
  text-align: center;
  white-space: nowrap;
}

.heatmap-table thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #f7faff;
  color: #28466f;
  font-weight: 700;
}

.heatmap-line-head,
.heatmap-line-cell {
  position: sticky;
  left: 0;
  z-index: 1;
  background: #fbfdff;
  text-align: left;
  min-width: 140px;
  color: #28466f;
}

.heatmap-line-head {
  z-index: 3;
}

.heatmap-period-head {
  min-width: 86px;
}

.heatmap-value-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.heatmap-value-cell:hover {
  transform: scale(1.02);
  box-shadow: inset 0 0 0 2px rgba(19, 54, 93, 0.22);
}

.heatmap-cell-strong {
  text-shadow: 0 1px 1px rgba(255, 255, 255, 0.18);
}

.heatmap-cell-hot {
  text-shadow: 0 1px 2px rgba(8, 20, 36, 0.45);
  letter-spacing: 0.01em;
}

.heatmap-cell-critical {
  text-shadow: 0 1px 2px rgba(8, 20, 36, 0.55);
  letter-spacing: 0.01em;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.heatmap-tooltip {
  position: fixed;
  z-index: 40;
  pointer-events: none;
  min-width: 160px;
  max-width: 260px;
  padding: 0.65rem 0.8rem;
  border-radius: 8px;
  background: rgba(16, 28, 44, 0.94);
  color: #ffffff;
  box-shadow: 0 10px 26px rgba(8, 20, 36, 0.28);
  font-size: 0.85rem;
  line-height: 1.45;
}

.heatmap-tooltip-title {
  font-weight: 700;
  margin-bottom: 0.2rem;
}

.heatmap-legend-note {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem 0.8rem;
  align-items: center;
  margin-top: 0.6rem;
  color: #60718b;
  font-size: 0.88rem;
}

.legend-chip {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.legend-blue {
  background: #0b74b8;
}

.legend-orange {
  background: #f2a65a;
}

.legend-red {
  background: #d93f2f;
}

@media (max-width: 1100px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .tab-note {
    margin-left: 0;
    width: 100%;
  }

  .chart-box {
    height: 300px;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

