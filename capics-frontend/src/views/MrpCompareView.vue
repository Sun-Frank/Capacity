<template>
  <div class="page mrp-compare-page">
    <div class="page-header">
      <h1 class="page-title">MRP对比</h1>
      <p class="page-subtitle">按产品描述汇总后，对比两个 MRP 文件在统一时间窗口内的数量差异。</p>
    </div>

    <section class="table-wrapper compare-filter-panel">
      <div class="compare-grid">
        <div class="compare-file-card">
          <div class="compare-card-head">
            <span class="compare-card-tag">文件 A</span>
            <strong class="compare-card-title">{{ fileALabel }}</strong>
          </div>
          <div class="filters-stack">
            <BaseSelect
              v-model="fileA.createdBy"
              :options="createdBys.map((item) => ({ value: item, label: item }))"
              placeholder="选择导入人"
              @update:modelValue="onCreatedByChange(fileA)"
            />
            <BaseSelect
              v-model="fileA.fileName"
              :options="fileA.fileNames.map((item) => ({ value: item, label: item }))"
              placeholder="选择文件"
              :disabled="!fileA.createdBy"
              @update:modelValue="onFileNameChange(fileA)"
            />
            <BaseSelect
              v-model="fileA.version"
              :options="fileA.versions.map((item) => ({ value: item, label: item }))"
              placeholder="选择版本"
              :disabled="!fileA.fileName"
            />
          </div>
        </div>

        <div class="compare-file-card compare-file-card-secondary">
          <div class="compare-card-head">
            <span class="compare-card-tag">文件 B</span>
            <strong class="compare-card-title">{{ fileBLabel }}</strong>
          </div>
          <div class="filters-stack">
            <BaseSelect
              v-model="fileB.createdBy"
              :options="createdBys.map((item) => ({ value: item, label: item }))"
              placeholder="选择导入人"
              @update:modelValue="onCreatedByChange(fileB)"
            />
            <BaseSelect
              v-model="fileB.fileName"
              :options="fileB.fileNames.map((item) => ({ value: item, label: item }))"
              placeholder="选择文件"
              :disabled="!fileB.createdBy"
              @update:modelValue="onFileNameChange(fileB)"
            />
            <BaseSelect
              v-model="fileB.version"
              :options="fileB.versions.map((item) => ({ value: item, label: item }))"
              placeholder="选择版本"
              :disabled="!fileB.fileName"
            />
          </div>
        </div>
      </div>

      <div class="compare-actions">
        <div class="view-toggle">
          <button class="tab-chip" :class="{ active: viewType === 'week' }" @click="viewType = 'week'">周对比</button>
          <button class="tab-chip" :class="{ active: viewType === 'month' }" @click="viewType = 'month'">月对比</button>
        </div>
        <div class="compare-window-note">
          {{ viewType === 'week' ? '从两个文件较大的开始日期起，向后取 24 周。' : '从两个文件较大的开始日期起，向后取 12 个月。' }}
        </div>
        <button class="btn btn-primary" :disabled="loading" @click="loadCompareData">
          {{ loading ? '对比中...' : '开始对比' }}
        </button>
        <button class="btn" :disabled="loading || !compareResult" @click="handleAiAnalyze">
          {{ aiLoading ? 'AI分析中...' : 'AI分析差异' }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="state-panel">正在加载并汇总两个 MRP 文件...</div>
    <div v-else-if="pageError" class="state-panel error">{{ pageError }}</div>
    <div v-else-if="!compareResult" class="state-panel">请选择文件 A、文件 B 后开始对比。</div>
    <div v-else-if="!compareResult.periods.length" class="state-panel">根据较大的开始日期，未找到可用于对比的时间窗口。</div>
    <template v-else>
      <section class="stats-grid overview-stats">
        <article class="stat-card">
          <div class="stat-label">汇总项目数</div>
          <div class="stat-value">{{ compareResult.summary.itemCount }}</div>
          <div class="stat-unit">产品描述 / Item Number</div>
        </article>
        <article class="stat-card stat-card-neutral">
          <div class="stat-label">文件 A 总量</div>
          <div class="stat-value">{{ formatQty(compareResult.summary.totalQtyA) }}</div>
          <div class="stat-unit">{{ fileALabel }}</div>
        </article>
        <article class="stat-card stat-card-neutral">
          <div class="stat-label">文件 B 总量</div>
          <div class="stat-value">{{ formatQty(compareResult.summary.totalQtyB) }}</div>
          <div class="stat-unit">{{ fileBLabel }}</div>
        </article>
        <article class="stat-card" :class="compareResult.summary.totalDelta >= 0 ? 'stat-card-warm' : 'stat-card-cool'">
          <div class="stat-label">总差异</div>
          <div class="stat-value">{{ formatDelta(compareResult.summary.totalDelta) }}</div>
          <div class="stat-unit">文件 B - 文件 A</div>
        </article>
      </section>

      <section class="table-wrapper ai-analysis-panel">
        <div class="panel-header ai-panel-header">
          <div>
            <h2 class="panel-title">AI差异分析</h2>
            <p class="panel-copy">基于当前对比结果自动提炼差异重点、可能原因和建议动作。</p>
          </div>
          <div class="ai-meta" v-if="aiResult?.provider">
            <span class="ai-meta-tag">{{ aiResult.provider }}</span>
            <span class="ai-meta-model">{{ aiResult.model }}</span>
          </div>
        </div>
        <div v-if="aiLoading" class="state-panel">AI正在分析当前MRP差异...</div>
        <div v-else-if="aiError" class="state-panel error">{{ aiError }}</div>
        <div v-else-if="aiResult?.analysis" class="ai-analysis-content">
          <div class="ai-analysis-grid">
            <div>
              <pre class="ai-analysis-text">{{ aiResult.analysis }}</pre>
              <p v-if="aiResult.note" class="ai-analysis-note">{{ aiResult.note }}</p>
            </div>
            <aside class="ai-evidence-panel">
              <div class="evidence-block">
                <h3 class="evidence-title">互联网信号</h3>
                <ul v-if="aiExternalSignals.length" class="signal-list">
                  <li v-for="(signal, idx) in aiExternalSignals" :key="`${idx}-${signal.title || signal.note}`" class="signal-item">
                    <a
                      v-if="signal.link"
                      :href="signal.link"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="signal-title"
                    >
                      {{ signal.title || '未命名信号' }}
                    </a>
                    <div v-else class="signal-title">{{ signal.title || signal.note || '未命名信号' }}</div>
                    <div class="signal-meta">
                      <span v-if="signal.query">查询：{{ signal.query }}</span>
                      <span v-if="signal.publishedAt">时间：{{ signal.publishedAt }}</span>
                    </div>
                  </li>
                </ul>
                <div v-else class="signal-empty">本次未抓取到外部信号。</div>
              </div>

              <div class="evidence-block">
                <h3 class="evidence-title">系统内产能风险</h3>
                <div v-if="hasInternalRisk" class="risk-grid">
                  <div class="risk-item">
                    <div class="risk-label">产能风险</div>
                    <div class="risk-value">{{ aiInternalRisk.capacityRiskLevel || '--' }} / {{ formatRiskScore(aiInternalRisk.capacityRiskScore) }}</div>
                  </div>
                  <div class="risk-item">
                    <div class="risk-label">市场风险</div>
                    <div class="risk-value">{{ aiInternalRisk.marketRiskLevel || '--' }} / {{ formatRiskScore(aiInternalRisk.marketRiskScore) }}</div>
                  </div>
                  <div class="risk-item">
                    <div class="risk-label">供应链风险</div>
                    <div class="risk-value">{{ aiInternalRisk.supplyRiskLevel || '--' }} / {{ formatRiskScore(aiInternalRisk.supplyRiskScore) }}</div>
                  </div>
                  <div class="risk-item">
                    <div class="risk-label">冲击度</div>
                    <div class="risk-value">{{ formatRiskPercent(aiInternalRisk.periodShockPercent) }}</div>
                  </div>
                  <div class="risk-item">
                    <div class="risk-label">变化率</div>
                    <div class="risk-value">{{ formatRiskPercent(aiInternalRisk.totalDeltaRatePercent) }}</div>
                  </div>
                  <div class="risk-item">
                    <div class="risk-label">集中度</div>
                    <div class="risk-value">{{ formatRiskPercent(aiInternalRisk.top3ConcentrationPercent) }}</div>
                  </div>
                </div>
                <div v-else class="signal-empty">暂无系统内风险上下文。</div>
              </div>
            </aside>
          </div>
        </div>
        <div v-else class="state-panel">点击“AI分析差异”生成分析结论。</div>
      </section>

      <section class="table-wrapper compare-table-panel">
        <div class="panel-header compare-table-header">
          <div>
            <h2 class="panel-title">{{ viewType === 'week' ? '周差异矩阵' : '月差异矩阵' }}</h2>
            <p class="panel-copy">横轴按较大的开始日期向后取固定窗口，纵轴按产品描述汇总；未维护描述时直接显示 MRP 的 Item Number。</p>
          </div>
          <div class="compare-table-tools">
            <button class="btn" @click="handleExportCompareMatrix">导出差异矩阵</button>
            <div class="legend-row">
              <span class="legend-item"><i class="legend-dot dot-a"></i>文件 A 数量</span>
              <span class="legend-item"><i class="legend-dot dot-b"></i>文件 B 数量</span>
              <span class="legend-item"><i class="legend-dot dot-warm"></i>差异增加</span>
              <span class="legend-item"><i class="legend-dot dot-warm-strong"></i>差异增加（高）</span>
              <span class="legend-item"><i class="legend-dot dot-cool"></i>差异减少</span>
              <span class="legend-item"><i class="legend-dot dot-cool-strong"></i>差异减少（高）</span>
            </div>
          </div>
        </div>

        <div
          ref="topScrollbarRef"
          class="compare-top-scrollbar"
          @scroll="handleTopScrollbarScroll"
        >
          <div
            ref="topScrollbarInnerRef"
            class="compare-top-scrollbar-inner"
            :style="{ width: `${topScrollbarInnerWidth}px` }"
          ></div>
        </div>

        <div
          ref="tableScrollRef"
          class="table-scroll"
          @scroll="handleTableScroll"
        >
          <div ref="matrixContentRef" class="compare-matrix-content">
            <table ref="matrixTableRef" class="main-table compare-matrix-table">
              <thead>
                <tr>
                  <th class="sticky-left sticky-name">产品描述 / Item Number</th>
                  <th class="sticky-left sticky-metric">指标</th>
                  <th v-for="period in compareResult.periods" :key="period.key" class="period-head">
                    <strong>{{ period.key }}</strong>
                    <span>{{ period.label }}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                <template v-for="group in compareResult.groups" :key="group.key">
                  <tr
                    v-for="(row, rowIndex) in group.rows"
                  :key="`${group.key}-${row.type}`"
                  :class="[
                    'matrix-row',
                    `matrix-row-${row.type}`
                  ]"
                >
                    <th
                      v-if="rowIndex === 0"
                      :class="['sticky-left', 'sticky-name', 'group-name-cell', { 'group-name-summary': group.key === 'summary' }]"
                      :rowspan="group.rows.length"
                    >
                      <span class="group-name">{{ group.name }}</span>
                    </th>
                    <th
                      :class="['sticky-left', 'sticky-metric', 'metric-cell', { 'summary-metric-cell': group.key === 'summary' }]"
                    >
                      {{ row.label }}
                    </th>
                    <td
                      v-for="(value, valueIndex) in row.values"
                      :key="`${group.key}-${row.type}-${compareResult.periods[valueIndex].key}`"
                      :class="getCellClass(row.type, value, group.key === 'summary')"
                    >
                      {{ row.type === 'delta' ? formatDelta(value) : formatQty(value) }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import BaseSelect from '@/components/common/BaseSelect.vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import {
  getCreatedBys,
  getFileNamesByCreatedBy,
  getVersionsByCreatedByAndFileName,
  getWeeklyDemandSingle,
  getMonthlyDemandSingle
} from '@/api/mrp'
import { getProducts } from '@/api/product'
import { buildMrpCompareMatrixExport, buildMrpCompareOverview } from '@/composables/useMrpCompareData'
import { downloadCsv } from '@/utils/export'
import { analyzeMrpCompare } from '@/api/ai'

const { token } = useAuth()
const { showToast } = useToast()

const createdBys = ref([])
const loading = ref(false)
const pageError = ref('')
const viewType = ref('week')
const compareResult = ref(null)
const aiLoading = ref(false)
const aiError = ref('')
const aiResult = ref(null)
const topScrollbarRef = ref(null)
const topScrollbarInnerRef = ref(null)
const tableScrollRef = ref(null)
const matrixContentRef = ref(null)
const matrixTableRef = ref(null)
const topScrollbarInnerWidth = ref(0)
let syncingScroll = false
let resizeObserver = null

function createFileState() {
  return reactive({
    createdBy: '',
    fileName: '',
    version: '',
    fileNames: [],
    versions: []
  })
}

const fileA = createFileState()
const fileB = createFileState()

const fileALabel = computed(() => (fileA.fileName && fileA.version ? `${fileA.fileName} / ${fileA.version}` : '未选择文件'))
const fileBLabel = computed(() => (fileB.fileName && fileB.version ? `${fileB.fileName} / ${fileB.version}` : '未选择文件'))
const aiExternalSignals = computed(() => {
  const signals = aiResult.value?.externalSignals
  if (!Array.isArray(signals)) return []
  return signals.filter((item) => item && typeof item === 'object')
})
const aiInternalRisk = computed(() => {
  const risk = aiResult.value?.internalRisk
  if (!risk || typeof risk !== 'object') return {}
  return risk
})
const hasInternalRisk = computed(() => Object.keys(aiInternalRisk.value).length > 0)

function resetCompareResult() {
  compareResult.value = null
  pageError.value = ''
  aiError.value = ''
  aiResult.value = null
}

async function loadCreatedByOptions() {
  try {
    const data = await getCreatedBys(token.value)
    createdBys.value = data.data || []
  } catch (error) {
    console.error('Load createdBys error:', error)
  }
}

async function onCreatedByChange(state) {
  state.fileName = ''
  state.version = ''
  state.fileNames = []
  state.versions = []
  resetCompareResult()

  if (!state.createdBy) return

  try {
    const data = await getFileNamesByCreatedBy(token.value, state.createdBy)
    state.fileNames = data.data || []
  } catch (error) {
    console.error('Load fileNames error:', error)
    showToast('加载文件列表失败', 'error')
  }
}

async function onFileNameChange(state) {
  state.version = ''
  state.versions = []
  resetCompareResult()

  if (!state.createdBy || !state.fileName) return

  try {
    const data = await getVersionsByCreatedByAndFileName(token.value, state.createdBy, state.fileName)
    state.versions = data.data || []
  } catch (error) {
    console.error('Load versions error:', error)
    showToast('加载版本列表失败', 'error')
  }
}

function formatQty(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatDelta(value) {
  const numeric = Number(value || 0)
  if (numeric > 0) return `+${formatQty(numeric)}`
  return formatQty(numeric)
}

function formatRiskScore(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '--'
  return `${Math.round(num)}/100`
}

function formatRiskPercent(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '--'
  return `${num.toFixed(1)}%`
}

function getDeltaIntensityClass(value) {
  const numeric = Math.abs(Number(value || 0))
  const thresholds = compareResult.value?.deltaThresholds || { medium: 0, strong: 0 }

  if (!numeric) return 'cell-delta-equal'
  if (thresholds.strong && numeric >= thresholds.strong) {
    return value > 0 ? 'cell-delta-increase-strong' : 'cell-delta-decrease-strong'
  }
  if (thresholds.medium && numeric >= thresholds.medium) {
    return value > 0 ? 'cell-delta-increase-medium' : 'cell-delta-decrease-medium'
  }
  return value > 0 ? 'cell-delta-increase' : 'cell-delta-decrease'
}

function getCellClass(rowType, value, isSummary) {
  if (rowType === 'qtyA') return ['matrix-cell', 'cell-qty-a', isSummary ? 'cell-summary' : '']
  if (rowType === 'qtyB') return ['matrix-cell', 'cell-qty-b', isSummary ? 'cell-summary' : '']
  return ['matrix-cell', getDeltaIntensityClass(value), isSummary ? 'cell-summary' : '']
}

function syncHorizontalScroll(source, target) {
  if (!source || !target || syncingScroll) return
  syncingScroll = true
  target.scrollLeft = source.scrollLeft
  requestAnimationFrame(() => {
    syncingScroll = false
  })
}

function handleTopScrollbarScroll(event) {
  syncHorizontalScroll(event.target, tableScrollRef.value)
}

function handleTableScroll(event) {
  syncHorizontalScroll(event.target, topScrollbarRef.value)
}

function updateTopScrollbarWidth() {
  const inner = topScrollbarInnerRef.value
  const topScrollbar = topScrollbarRef.value
  const tableScroll = tableScrollRef.value
  const matrixContent = matrixContentRef.value
  const matrixTable = matrixTableRef.value
  if (!inner || !topScrollbar || !tableScroll || !matrixContent || !matrixTable) return

  const scrollWidth = Math.max(
    matrixTable.scrollWidth,
    matrixContent.scrollWidth,
    tableScroll.scrollWidth,
    tableScroll.clientWidth
  )
  topScrollbarInnerWidth.value = scrollWidth
  topScrollbar.scrollLeft = tableScroll.scrollLeft
  topScrollbar.style.visibility = scrollWidth > tableScroll.clientWidth ? 'visible' : 'hidden'
}

async function loadCompareData() {
  if (!fileA.createdBy || !fileA.fileName || !fileA.version || !fileB.createdBy || !fileB.fileName || !fileB.version) {
    showToast('请选择完整的文件 A 和文件 B 条件', 'warning')
    return
  }

  loading.value = true
  pageError.value = ''

  try {
    const demandApi = viewType.value === 'week' ? getWeeklyDemandSingle : getMonthlyDemandSingle
    const [resA, resB, productsRes] = await Promise.all([
      demandApi(token.value, fileA.createdBy, fileA.fileName, fileA.version),
      demandApi(token.value, fileB.createdBy, fileB.fileName, fileB.version),
      getProducts(token.value)
    ])

    if (!resA?.success) throw new Error(resA?.message || '文件 A 数据加载失败')
    if (!resB?.success) throw new Error(resB?.message || '文件 B 数据加载失败')
    if (!productsRes?.success) throw new Error(productsRes?.message || '产品主数据加载失败')

    compareResult.value = buildMrpCompareOverview({
      payloadA: resA.data,
      payloadB: resB.data,
      products: productsRes.data || [],
      viewType: viewType.value,
      fileLabelA: fileALabel.value,
      fileLabelB: fileBLabel.value
    })

    aiError.value = ''
    aiResult.value = null
    await nextTick()
    updateTopScrollbarWidth()

    showToast('MRP 对比结果已更新', 'success')
  } catch (error) {
    console.error('Load MRP compare error:', error)
    pageError.value = error.message || '加载 MRP 对比失败'
    compareResult.value = null
    showToast('加载 MRP 对比失败', 'error')
  } finally {
    loading.value = false
  }
}

function buildAiPayload() {
  if (!compareResult.value) return {}

  const groups = compareResult.value.groups || []
  const detailGroups = groups.filter((group) => group.key !== 'summary')

  const topItems = detailGroups.slice(0, 15).map((group) => {
    const rowA = group.rows.find((row) => row.type === 'qtyA')
    const rowB = group.rows.find((row) => row.type === 'qtyB')
    const rowDelta = group.rows.find((row) => row.type === 'delta')

    return {
      name: group.name,
      totalQtyA: rowA?.total || 0,
      totalQtyB: rowB?.total || 0,
      totalDelta: rowDelta?.total || 0,
      periodDeltas: (rowDelta?.values || []).slice(0, 24)
    }
  })

  return {
    viewType: viewType.value,
    fileLabelA: compareResult.value.fileLabelA,
    fileLabelB: compareResult.value.fileLabelB,
    periods: (compareResult.value.periods || []).map((period) => ({
      key: period.key,
      label: period.label
    })),
    summary: compareResult.value.summary || {},
    topItems
  }
}

async function handleAiAnalyze() {
  if (!compareResult.value) {
    showToast('请先完成文件对比', 'warning')
    return
  }

  aiLoading.value = true
  aiError.value = ''

  try {
    const res = await analyzeMrpCompare(token.value, buildAiPayload())
    aiResult.value = res.data || null
    if (!aiResult.value?.analysis) {
      aiError.value = 'AI未返回有效分析内容'
    } else {
      showToast('AI分析已完成', 'success')
    }
  } catch (error) {
    console.error('AI analyze failed:', error)
    aiError.value = error.message || 'AI分析失败'
    aiResult.value = null
    showToast('AI分析失败', 'error')
  } finally {
    aiLoading.value = false
  }
}

function handleExportCompareMatrix() {
  if (!compareResult.value?.groups?.length) {
    showToast('暂无可导出数据', 'warning')
    return
  }

  const exported = buildMrpCompareMatrixExport(compareResult.value)
  const suffix = viewType.value === 'week' ? '周' : '月'
  downloadCsv(`MRP对比-${suffix}-差异矩阵.csv`, exported.headers, exported.rows)
  showToast('导出成功', 'success')
}

watch(viewType, async () => {
  await nextTick()
  updateTopScrollbarWidth()
})

watch(compareResult, async () => {
  await nextTick()
  updateTopScrollbarWidth()
  requestAnimationFrame(() => {
    updateTopScrollbarWidth()
  })
})

onMounted(() => {
  window.addEventListener('resize', updateTopScrollbarWidth)
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      updateTopScrollbarWidth()
    })
    if (tableScrollRef.value) {
      resizeObserver.observe(tableScrollRef.value)
    }
    if (matrixContentRef.value) {
      resizeObserver.observe(matrixContentRef.value)
    }
    if (matrixTableRef.value) {
      resizeObserver.observe(matrixTableRef.value)
    }
  }
  nextTick(() => {
    updateTopScrollbarWidth()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateTopScrollbarWidth)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

loadCreatedByOptions()
</script>

<style scoped>
.mrp-compare-page {
  gap: 1rem;
  overflow: auto;
}

.compare-filter-panel,
.compare-table-panel,
.overview-stats,
.ai-analysis-panel {
  flex: 0 0 auto;
}

.compare-filter-panel {
  padding: 1rem 1.2rem;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.compare-file-card {
  padding: 1rem;
  border: 1px solid #d7e2f1;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f5f8fd 100%);
}

.compare-file-card-secondary {
  background: linear-gradient(180deg, #ffffff 0%, #fff6f1 100%);
}

.compare-card-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.85rem;
}

.compare-card-tag {
  width: fit-content;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: #e8f1ff;
  color: #255188;
  font-size: 0.8rem;
  font-weight: 700;
}

.compare-card-title {
  color: #17365d;
  line-height: 1.3;
}

.filters-stack {
  display: grid;
  gap: 0.75rem;
}

.compare-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.ai-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.ai-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ai-meta-tag {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: #eef2ff;
  color: #3344aa;
  font-size: 0.78rem;
  font-weight: 700;
}

.ai-meta-model {
  color: #5b6b7f;
  font-size: 0.82rem;
}

.ai-analysis-content {
  margin-top: 0.8rem;
}

.ai-analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 1fr);
  gap: 0.9rem;
  align-items: start;
}

.ai-analysis-text {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 0.92rem;
  color: #1d2735;
  background: #f8fbff;
  border: 1px solid #e1ecfa;
  border-radius: 8px;
  padding: 0.8rem;
}

.ai-analysis-note {
  margin-top: 0.55rem;
  color: #7a4b00;
  font-size: 0.84rem;
}

.ai-evidence-panel {
  border: 1px solid #e2ebf7;
  background: #f9fbfe;
  border-radius: 8px;
  padding: 0.75rem;
  display: grid;
  gap: 0.75rem;
}

.evidence-block {
  background: white;
  border: 1px solid #e8eef8;
  border-radius: 8px;
  padding: 0.65rem;
}

.evidence-title {
  margin: 0 0 0.5rem;
  font-size: 0.9rem;
  color: #224063;
}

.signal-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.55rem;
}

.signal-item {
  padding-bottom: 0.5rem;
  border-bottom: 1px dashed #d8e3f2;
}

.signal-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.signal-title {
  display: inline-block;
  color: #1f4d88;
  font-size: 0.86rem;
  text-decoration: none;
}

.signal-title:hover {
  text-decoration: underline;
}

.signal-meta {
  margin-top: 0.2rem;
  display: grid;
  gap: 0.1rem;
  color: #6c7d92;
  font-size: 0.78rem;
}

.signal-empty {
  color: #7a8ba0;
  font-size: 0.84rem;
}

.risk-grid {
  display: grid;
  gap: 0.45rem;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.4rem 0.5rem;
  border-radius: 6px;
  background: #f4f8fd;
}

.risk-label {
  color: #4f637d;
  font-size: 0.82rem;
}

.risk-value {
  color: #163f74;
  font-weight: 700;
  font-size: 0.83rem;
}

.view-toggle {
  display: flex;
  gap: 0.7rem;
  flex-wrap: wrap;
}

.compare-window-note {
  color: #60718b;
  font-size: 0.9rem;
}

.tab-chip {
  border: 1px solid #b8cae4;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #eef5ff 100%);
  color: #23456f;
  cursor: pointer;
  font-weight: 700;
  transition: 0.2s ease;
  padding: 0.62rem 1rem;
  min-width: 4.5rem;
}

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
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d7e2f1;
  border-radius: 8px;
  color: var(--muted-foreground);
}

.state-panel.error {
  color: var(--error);
}

.stat-card-neutral {
  background: linear-gradient(180deg, #f8fbff 0%, #eef4fc 100%);
}

.stat-card-warm {
  background: linear-gradient(180deg, #fff7f2 0%, #fff1e7 100%);
}

.stat-card-cool {
  background: linear-gradient(180deg, #f4f9ff 0%, #edf5ff 100%);
}

.compare-table-panel {
  padding: 1rem 1rem 1.1rem;
  position: relative;
  overflow: visible;
}

.compare-table-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 0.85rem;
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

.compare-table-tools {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.75rem;
}

.legend-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #60718b;
  font-size: 0.88rem;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  display: inline-block;
}

.dot-a {
  background: #dbeafe;
  border: 1px solid #93c5fd;
}

.dot-b {
  background: #fee2e2;
  border: 1px solid #fca5a5;
}

.dot-warm {
  background: #ef6b45;
}

.dot-warm-strong {
  background: #c73a16;
}

.dot-cool {
  background: #4f8fdd;
}

.dot-cool-strong {
  background: #124f9d;
}

.table-scroll {
  position: relative;
  overflow-x: auto;
  overflow-y: auto;
  max-height: calc(100vh - 280px);
  scrollbar-width: thin;
  scrollbar-color: #9eb6d8 #edf3fb;
  -ms-overflow-style: auto;
}

.table-scroll::-webkit-scrollbar {
  width: 12px;
  height: 0;
}

.table-scroll::-webkit-scrollbar-track {
  background: #edf3fb;
}

.table-scroll::-webkit-scrollbar-thumb {
  background: #9eb6d8;
  border-radius: 999px;
  border: 2px solid #edf3fb;
}

.compare-matrix-content {
  width: max-content;
  min-width: 100%;
}

.compare-top-scrollbar {
  position: sticky;
  top: 0;
  z-index: 7;
  overflow-x: auto;
  overflow-y: hidden;
  height: 22px;
  margin-bottom: 0.35rem;
  background: linear-gradient(180deg, #f8fbff 0%, #edf4fc 100%);
  border: 1px solid #d6e3f3;
  border-radius: 8px 8px 0 0;
  scrollbar-width: auto;
  scrollbar-color: #5e8fcb #dfeaf7;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.compare-top-scrollbar-inner {
  height: 1px;
}

.compare-top-scrollbar::-webkit-scrollbar {
  height: 14px;
}

.compare-top-scrollbar::-webkit-scrollbar-track {
  background: #dfeaf7;
  border-radius: 999px;
}

.compare-top-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #7ea9da 0%, #4f80bd 100%);
  border-radius: 999px;
  border: 2px solid #dfeaf7;
}

.compare-top-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #6e9cd2 0%, #3f6faa 100%);
}

.compare-matrix-table {
  width: max-content;
  min-width: 1080px;
  border-collapse: separate;
  border-spacing: 0;
}

.compare-matrix-table th,
.compare-matrix-table td {
  padding: 0.7rem 0.8rem;
  border-bottom: 1px solid #e3ebf7;
  border-right: 1px solid #eef3fa;
  text-align: center;
  vertical-align: middle;
}

.compare-matrix-table thead th {
  position: sticky;
  top: 22px;
  z-index: 20;
  background: #f7faff;
  color: #28466f;
  font-weight: 700;
}

.sticky-left {
  position: sticky;
  left: 0;
  z-index: 3;
  background: #fff;
}

.sticky-metric {
  left: 220px;
  z-index: 3;
  background: #fff;
}

.compare-matrix-table thead .sticky-left {
  z-index: 24;
  background: #f7faff;
}

.sticky-name {
  min-width: 220px;
  text-align: left;
}

.sticky-metric {
  min-width: 140px;
  text-align: left;
}

.period-head {
  min-width: 118px;
}

.period-head strong,
.period-head span {
  display: block;
}

.period-head span {
  margin-top: 0.2rem;
  color: #6a7b93;
  font-size: 0.78rem;
  font-weight: 500;
}

.group-name-cell {
  background: #fbfdff;
}

.group-name {
  display: block;
  color: #17365d;
  font-weight: 700;
  line-height: 1.4;
}

.metric-cell {
  color: #48627f;
  font-weight: 700;
}

.matrix-row-qtyA .metric-cell {
  background: #f5f9ff;
}

.matrix-row-qtyB .metric-cell {
  background: #fff6f3;
}

.matrix-row-delta .metric-cell {
  background: #f8fafc;
}

.group-name-summary,
.summary-metric-cell {
  background: #fff8e8;
}

.matrix-cell {
  font-variant-numeric: tabular-nums;
}

.cell-qty-a {
  background: #f7fbff;
}

.cell-qty-b {
  background: #fff9f6;
}

.cell-delta-increase {
  background: rgba(239, 107, 69, 0.14);
  color: #bc4c2c;
  font-weight: 700;
}

.cell-delta-increase-medium {
  background: rgba(239, 107, 69, 0.22);
  color: #a53d20;
  font-weight: 700;
}

.cell-delta-increase-strong {
  background: rgba(199, 58, 22, 0.28);
  color: #85260f;
  font-weight: 800;
}

.cell-delta-decrease {
  background: rgba(79, 143, 221, 0.14);
  color: #245ea8;
  font-weight: 700;
}

.cell-delta-decrease-medium {
  background: rgba(79, 143, 221, 0.22);
  color: #1e4f8d;
  font-weight: 700;
}

.cell-delta-decrease-strong {
  background: rgba(18, 79, 157, 0.28);
  color: #103f7b;
  font-weight: 800;
}

.cell-delta-equal {
  background: #f8fafc;
  color: #6b7b8f;
}

.cell-summary,
.group-name-summary {
  box-shadow: inset 0 0 0 9999px rgba(255, 248, 225, 0.18);
}

@media (max-width: 980px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }

  .ai-analysis-grid {
    grid-template-columns: 1fr;
  }

  .compare-table-header {
    flex-direction: column;
  }

  .compare-table-tools {
    align-items: flex-start;
  }

  .sticky-name {
    min-width: 180px;
  }

  .sticky-metric {
    left: 180px;
    min-width: 120px;
  }
}
</style>
