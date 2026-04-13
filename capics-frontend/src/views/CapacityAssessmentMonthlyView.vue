<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">静态产能核算（月）</h1>
      <p class="page-subtitle">融合版补齐模块：按月查看静态LOAD结果</p>
    </div>

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
      <button class="btn btn-primary" @click="loadData" :disabled="loading">
        {{ loading ? '计算中...' : '开始计算' }}
      </button>
    </div>

    <div v-if="warnings.length > 0" class="warnings-container">
      <div class="warning-header">数据提示</div>
      <ul class="warning-list">
        <li v-for="(warning, idx) in warnings" :key="idx">{{ warning }}</li>
      </ul>
    </div>

    <div v-if="availableLines.length > 0" class="line-filter-row">
      <span class="line-filter-label">选择生产线：</span>
      <BaseSelect
        v-model="selectedLine"
        :options="availableLines.map(l => ({ value: l, label: l }))"
        placeholder="请选择生产线"
      />
    </div>

    <div class="table-wrapper">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="!selectedLine" class="empty-state">请选择生产线查看数据</div>
      <div v-else class="table-scroll">
        <table class="main-table">
          <thead>
            <tr>
              <th class="sticky-col">成品编码</th>
              <th class="sticky-col">组件编码</th>
              <th class="sticky-col">PF</th>
              <th class="sticky-col">CT</th>
              <th class="sticky-col">OEE</th>
              <th v-for="month in months" :key="month" :colspan="2">{{ monthDates[month] || month }}</th>
            </tr>
            <tr>
              <th class="sticky-col"></th>
              <th class="sticky-col"></th>
              <th class="sticky-col"></th>
              <th class="sticky-col"></th>
              <th class="sticky-col"></th>
              <template v-for="month in months" :key="month + '-sub'">
                <th>需求</th>
                <th>LOAD</th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in selectedLineData" :key="item.itemNumber + '_' + item.componentNumber">
              <td class="sticky-col">{{ item.itemNumber }}</td>
              <td class="sticky-col">{{ item.componentNumber }}</td>
              <td class="sticky-col">{{ item.pf || '-' }}</td>
              <td class="sticky-col">{{ item.ct }}</td>
              <td class="sticky-col">{{ item.oee }}%</td>
              <template v-for="month in months" :key="month + '-v'">
                <td>{{ item[month + '_demand'] ?? 0 }}</td>
                <td :class="{ 'high-load': (item[month + '_loading'] || 0) > 1 }">{{ formatLoading(item[month + '_loading']) }}</td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import BaseSelect from '@/components/common/BaseSelect.vue'
import { useAuth } from '@/composables/useAuth'
import { getCreatedBys, getFileNamesByCreatedBy, getVersionsByCreatedByAndFileName } from '@/api/mrp'
import { getCapacityAssessmentMonthly } from '@/api/capacityMonthly'

const { token } = useAuth()

const createdBys = ref([])
const fileNames = ref([])
const versions = ref([])
const selectedCreatedBy = ref('')
const selectedFileName = ref('')
const selectedVersion = ref('')

const linesData = ref({})
const months = ref([])
const monthDates = ref({})
const warnings = ref([])
const selectedLine = ref('')

const loading = ref(false)
const error = ref('')

const availableLines = computed(() => Object.keys(linesData.value).sort())
const selectedLineData = computed(() => linesData.value[selectedLine.value] || [])

const formatLoading = (value) => {
  const n = Number(value || 0)
  return `${(n * 100).toFixed(1)}%`
}

const loadCreatedBys = async () => {
  const data = await getCreatedBys(token.value)
  createdBys.value = data.data || []
}

const onCreatedByChange = async () => {
  selectedFileName.value = ''
  selectedVersion.value = ''
  const data = await getFileNamesByCreatedBy(token.value, selectedCreatedBy.value)
  fileNames.value = data.data || []
  versions.value = []
}

const onFileNameChange = async () => {
  selectedVersion.value = ''
  const data = await getVersionsByCreatedByAndFileName(token.value, selectedCreatedBy.value, selectedFileName.value)
  versions.value = data.data || []
}

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await getCapacityAssessmentMonthly(token.value, selectedCreatedBy.value, selectedFileName.value, selectedVersion.value)
    if (!data.success) {
      throw new Error(data.message || '加载失败')
    }
    linesData.value = data.data.lines || {}
    months.value = data.data.months || []
    monthDates.value = data.data.monthDates || {}
    warnings.value = data.data.warnings || []
    selectedLine.value = availableLines.value[0] || ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(loadCreatedBys)
</script>

<style scoped>
.high-load { color: #c81e1e; font-weight: 700; }
</style>
