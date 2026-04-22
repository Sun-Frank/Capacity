<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">MRP计划管理</h1>
      <p class="page-subtitle">物料需求计划</p>
    </div>

    <BaseTabs
      v-model="mrpTab"
      :tabs="[
        { label: '计划列表', value: 'plans' },
        { label: '周报表', value: 'weekly' },
        { label: '按描述分类汇总周报表', value: 'weekly-description' },
        { label: '月报表', value: 'monthly' },
        { label: '按描述分类汇总月报表', value: 'monthly-description' }
      ]"
    />

    <div v-if="mrpTab === 'plans'" class="tab-content">
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
        <button class="btn btn-primary" @click="loadMrpPlans">查询</button>
        <button class="btn" @click="showImportModal">导入MRP</button>
        <button class="btn" @click="handleDownloadMrpTemplate">模板下载</button>
      </div>
      <PlansTable :plans="mrpPlans" />
    </div>

    <div v-if="mrpTab === 'weekly'" class="tab-content tab-content-scrollable">
      <div class="filters-row">
        <BaseSelect
          v-model="weeklyCreatedBy"
          :options="createdBys.map(c => ({ value: c, label: c }))"
          placeholder="选择导入人"
          @update:modelValue="onWeeklyCreatedByChange"
        />
        <BaseSelect
          v-model="weeklyFileName"
          :options="weeklyFileNames.map(f => ({ value: f, label: f }))"
          placeholder="选择文件"
          :disabled="!weeklyCreatedBy"
          @update:modelValue="onWeeklyFileNameChange"
        />
        <button class="btn btn-primary" :disabled="!weeklyCreatedBy || !weeklyFileName" @click="loadWeeklyReportData">查询</button>
        <button class="btn" :disabled="!weeklyCreatedBy || !weeklyFileName" @click="handleExportWeeklyReport">导出周报表</button>
      </div>
      <WeeklyReportTable
        :columns="weeklyColumns"
        :report="weeklyReport"
        :createdBy="weeklyCreatedBy"
        :fileName="weeklyFileName"
      />
    </div>

    <div v-if="mrpTab === 'weekly-description'" class="tab-content tab-content-scrollable">
      <div class="filters-row">
        <BaseSelect
          v-model="weeklyCreatedBy"
          :options="createdBys.map(c => ({ value: c, label: c }))"
          placeholder="选择导入人"
          @update:modelValue="onWeeklyCreatedByChange"
        />
        <BaseSelect
          v-model="weeklyFileName"
          :options="weeklyFileNames.map(f => ({ value: f, label: f }))"
          placeholder="选择文件"
          :disabled="!weeklyCreatedBy"
          @update:modelValue="onWeeklyFileNameChange"
        />
        <button class="btn btn-primary" :disabled="!weeklyCreatedBy || !weeklyFileName" @click="loadWeeklyDescriptionReportData">查询</button>
      </div>
      <WeeklyDescriptionReportTable
        :columns="weeklyDescriptionColumns"
        :report="weeklyDescriptionReport"
        :createdBy="weeklyCreatedBy"
        :fileName="weeklyFileName"
      />
    </div>

    <div v-if="mrpTab === 'monthly'" class="tab-content tab-content-scrollable">
      <div class="filters-row">
        <BaseSelect
          v-model="monthlyCreatedBy"
          :options="createdBys.map(c => ({ value: c, label: c }))"
          placeholder="选择导入人"
          @update:modelValue="onMonthlyCreatedByChange"
        />
        <BaseSelect
          v-model="monthlyFileName"
          :options="monthlyFileNames.map(f => ({ value: f, label: f }))"
          placeholder="选择文件"
          :disabled="!monthlyCreatedBy"
          @update:modelValue="onMonthlyFileNameChange"
        />
        <button class="btn btn-primary" :disabled="!monthlyCreatedBy || !monthlyFileName" @click="loadMonthlyReportData">查询</button>
        <button class="btn" :disabled="!monthlyCreatedBy || !monthlyFileName" @click="handleExportMonthlyReport">导出月报表</button>
      </div>
      <MonthlyReportTable
        :columns="monthlyColumns"
        :report="monthlyReport"
        :createdBy="monthlyCreatedBy"
        :fileName="monthlyFileName"
      />
    </div>

    <div v-if="mrpTab === 'monthly-description'" class="tab-content tab-content-scrollable">
      <div class="filters-row">
        <BaseSelect
          v-model="monthlyCreatedBy"
          :options="createdBys.map(c => ({ value: c, label: c }))"
          placeholder="选择导入人"
          @update:modelValue="onMonthlyCreatedByChange"
        />
        <BaseSelect
          v-model="monthlyFileName"
          :options="monthlyFileNames.map(f => ({ value: f, label: f }))"
          placeholder="选择文件"
          :disabled="!monthlyCreatedBy"
          @update:modelValue="onMonthlyFileNameChange"
        />
        <button class="btn btn-primary" :disabled="!monthlyCreatedBy || !monthlyFileName" @click="loadMonthlyDescriptionReportData">查询</button>
      </div>
      <MonthlyDescriptionReportTable
        :columns="monthlyDescriptionColumns"
        :report="monthlyDescriptionReport"
        :createdBy="monthlyCreatedBy"
        :fileName="monthlyFileName"
      />
    </div>

    <ImportModal
      :show="showImport"
      type="mrp"
      :isImporting="isImporting"
      @close="showImport = false"
      @confirm="handleImport"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useMrpFilters } from '@/composables/useMrpFilters'
import { useToast } from '@/composables/useToast'
import { aggregateReportByDescription } from '@/composables/useMrpDescriptionReport'
import {
  downloadMrpTemplate,
  exportMonthlyReport,
  exportWeeklyReport,
  getFileNamesByCreatedBy,
  getMonthlyReportByFile,
  importMrpPlans
} from '@/api/mrp'
import BaseTabs from '@/components/common/BaseTabs.vue'
import BaseSelect from '@/components/common/BaseSelect.vue'
import ImportModal from '@/components/common/ImportModal.vue'
import PlansTable from '@/components/mrp/PlansTable.vue'
import WeeklyReportTable from '@/components/mrp/WeeklyReportTable.vue'
import MonthlyReportTable from '@/components/mrp/MonthlyReportTable.vue'
import WeeklyDescriptionReportTable from '@/components/mrp/WeeklyDescriptionReportTable.vue'
import MonthlyDescriptionReportTable from '@/components/mrp/MonthlyDescriptionReportTable.vue'

const { token, currentUser, currentUsername } = useAuth()
const {
  createdBys,
  fileNames,
  versions,
  selectedCreatedBy,
  selectedFileName,
  selectedVersion,
  weeklyCreatedBy,
  weeklyFileNames,
  weeklyFileName,
  weeklyColumns,
  weeklyReport,
  weeklyColumnGroups,
  mrpPlansCache,
  weeklyReportCache,
  loadCreatedBys,
  onCreatedByChange,
  onFileNameChange,
  loadVersions,
  loadMrpPlans: loadPlans,
  onWeeklyCreatedByChange,
  onWeeklyFileNameChange,
  loadWeeklyReportData: loadWeekly
} = useMrpFilters(token)

const { showToast } = useToast()

const mrpTab = ref('plans')
const mrpPlans = ref([])
const showImport = ref(false)
const isImporting = ref(false)

const monthlyCreatedBy = ref('')
const monthlyFileNames = ref([])
const monthlyFileName = ref('')
const monthlyColumns = ref([])
const monthlyReport = ref([])

const weeklyDescriptionColumns = computed(() => weeklyColumns.value)
const weeklyDescriptionReport = computed(() => aggregateReportByDescription(weeklyColumns.value, weeklyReport.value).rows)
const monthlyDescriptionColumns = computed(() => monthlyColumns.value)
const monthlyDescriptionReport = computed(() => aggregateReportByDescription(monthlyColumns.value, monthlyReport.value).rows)

const handleDownloadMrpTemplate = async () => {
  try {
    const blob = await downloadMrpTemplate(token.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'MRP导入模板-v2.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('模板下载成功', 'success')
  } catch (err) {
    showToast('模板下载失败: ' + (err.message || '未知错误'), 'error')
  }
}

const loadMrpPlans = async () => {
  if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
    showToast('请选择完整筛选条件（导入人、文件、版本）', 'warning')
    return
  }
  try {
    const data = await loadPlans()
    mrpPlans.value = data.data || []
  } catch {
    showToast('加载MRP计划失败', 'error')
  }
}

const loadWeeklyReportData = async () => {
  if (!weeklyCreatedBy.value || !weeklyFileName.value) {
    showToast('请选择导入人和文件', 'warning')
    return
  }
  mrpTab.value = 'weekly'
  await loadWeekly()
}

const loadWeeklyDescriptionReportData = async () => {
  if (!weeklyCreatedBy.value || !weeklyFileName.value) {
    showToast('请选择导入人和文件', 'warning')
    return
  }
  mrpTab.value = 'weekly-description'
  await loadWeekly()
}

const normalizeMonthlyColumns = (columns) => {
  const versionGroups = {}
  let versionIndex = 0
  for (const col of columns) {
    const version = col.version
    if (!versionGroups[version]) {
      versionGroups[version] = versionIndex++
    }
    col.versionIndex = versionGroups[version]
  }
}

const loadMonthlyReportData = async () => {
  if (!monthlyCreatedBy.value || !monthlyFileName.value) {
    showToast('请选择导入人和文件', 'warning')
    return
  }
  try {
    const data = await getMonthlyReportByFile(token.value, monthlyCreatedBy.value, monthlyFileName.value)
    const result = data.data && data.data[0]
    if (result) {
      const cols = result.columns || []
      normalizeMonthlyColumns(cols)
      monthlyColumns.value = cols
      monthlyReport.value = result.data || []
    } else {
      monthlyColumns.value = []
      monthlyReport.value = []
    }
  } catch (err) {
    console.error('Load monthly report error:', err)
    showToast('加载月报表失败', 'error')
  }
}

const loadMonthlyDescriptionReportData = async () => {
  if (!monthlyCreatedBy.value || !monthlyFileName.value) {
    showToast('请选择导入人和文件', 'warning')
    return
  }
  mrpTab.value = 'monthly-description'
  await loadMonthlyReportData()
}

const handleExportWeeklyReport = async () => {
  if (!weeklyCreatedBy.value || !weeklyFileName.value) {
    showToast('请先选择导入人和文件', 'warning')
    return
  }
  try {
    const blob = await exportWeeklyReport(token.value, weeklyCreatedBy.value, weeklyFileName.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'MRP周报表.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('周报表导出成功', 'success')
  } catch (err) {
    showToast(err?.message || '周报表导出失败', 'error')
  }
}

const handleExportMonthlyReport = async () => {
  if (!monthlyCreatedBy.value || !monthlyFileName.value) {
    showToast('请先选择导入人和文件', 'warning')
    return
  }
  try {
    const blob = await exportMonthlyReport(token.value, monthlyCreatedBy.value, monthlyFileName.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'MRP月报表.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    showToast('月报表导出成功', 'success')
  } catch (err) {
    showToast(err?.message || '月报表导出失败', 'error')
  }
}

const onMonthlyCreatedByChange = async () => {
  monthlyFileName.value = ''
  monthlyColumns.value = []
  monthlyReport.value = []
  if (monthlyCreatedBy.value) {
    const data = await getFileNamesByCreatedBy(token.value, monthlyCreatedBy.value)
    monthlyFileNames.value = data.data || []
  }
}

const onMonthlyFileNameChange = () => {
  monthlyReport.value = []
  monthlyColumns.value = []
}

const showImportModal = () => {
  showImport.value = true
}

const handleImport = async ({ file, fileName }) => {
  if (!file) {
    showToast('请选择文件', 'warning')
    return
  }
  if (!fileName) {
    showToast('请输入文件名称', 'warning')
    return
  }
  isImporting.value = true
  try {
    const createdBy = currentUsername.value || currentUser.value || 'admin'
    const result = await importMrpPlans(token.value, file, fileName, createdBy)
    if (result.success) {
      showToast('导入成功: ' + result.message, 'success')
      showImport.value = false
      await loadCreatedBys()
      selectedCreatedBy.value = createdBy
      await loadFileNames(selectedCreatedBy.value)
      selectedFileName.value = fileName
      await loadMrpPlans()
    } else {
      showToast('导入失败: ' + result.message, 'error')
    }
  } catch (err) {
    showToast('导入失败: ' + err.message, 'error')
  } finally {
    isImporting.value = false
  }
}

const loadFileNames = async (createdBy) => {
  if (!createdBy) {
    fileNames.value = []
    return
  }
  const data = await getFileNamesByCreatedBy(token.value, createdBy)
  fileNames.value = data.data || []
}

onMounted(() => {
  loadCreatedBys().then(async () => {
    if (mrpPlansCache.value) {
      mrpPlans.value = mrpPlansCache.value.data || []
    }

    if (selectedCreatedBy.value) {
      await loadFileNames(selectedCreatedBy.value)
      if (selectedFileName.value) {
        await loadVersions(selectedCreatedBy.value, selectedFileName.value)
      }
    }

    if (weeklyReportCache.value) {
      const result = weeklyReportCache.value?.data?.[0]
      if (result) {
        weeklyColumns.value = result.columns || []
        weeklyReport.value = result.data || []
        const cols = weeklyColumns.value
        const groups = []
        let currentWeek = null
        let currentGroup = null
        for (const col of cols) {
          if (col.week !== currentWeek) {
            if (currentGroup) groups.push(currentGroup)
            currentGroup = { week: col.week, weekLabel: col.weekLabel, versions: [] }
            currentWeek = col.week
          }
          currentGroup.versions.push(col)
        }
        if (currentGroup) groups.push(currentGroup)
        weeklyColumnGroups.value = groups
      }
    }

    if (weeklyCreatedBy.value) {
      await loadFileNames(weeklyCreatedBy.value)
      weeklyFileNames.value = fileNames.value
    }
  })
})
</script>

<style scoped>
.tab-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tab-content-scrollable {
  min-height: 0;
}

.filters-row {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
  flex-wrap: wrap;
}
</style>
