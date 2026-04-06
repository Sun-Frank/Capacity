import { ref, watch } from 'vue'
import {
  getCreatedBys,
  getFileNamesByCreatedBy,
  getVersionsByCreatedByAndFileName,
  getPlansFiltered,
  getWeeklyReport
} from '@/api/mrp'

const SESSION_KEY_MRP = 'capics_mrp_filters'
const SESSION_KEY_WEEKLY = 'capics_weekly_filters'

// 保存筛选状态到sessionStorage
const saveMrpFilters = (state) => {
  try {
    sessionStorage.setItem(SESSION_KEY_MRP, JSON.stringify(state))
  } catch (e) {
    console.error('Save MRP filters error:', e)
  }
}

const loadMrpFilters = () => {
  try {
    const saved = sessionStorage.getItem(SESSION_KEY_MRP)
    return saved ? JSON.parse(saved) : null
  } catch (e) {
    console.error('Load MRP filters error:', e)
    return null
  }
}

const saveWeeklyFilters = (state) => {
  try {
    sessionStorage.setItem(SESSION_KEY_WEEKLY, JSON.stringify(state))
  } catch (e) {
    console.error('Save weekly filters error:', e)
  }
}

const loadWeeklyFilters = () => {
  try {
    const saved = sessionStorage.getItem(SESSION_KEY_WEEKLY)
    return saved ? JSON.parse(saved) : null
  } catch (e) {
    console.error('Load weekly filters error:', e)
    return null
  }
}

export function useMrpFilters(token) {
  // 从sessionStorage恢复初始状态
  const savedMrp = loadMrpFilters()
  const savedWeekly = loadWeeklyFilters()

  const createdBys = ref([])
  const fileNames = ref([])
  const versions = ref([])

  const selectedCreatedBy = ref(savedMrp?.selectedCreatedBy || '')
  const selectedFileName = ref(savedMrp?.selectedFileName || '')
  const selectedVersion = ref(savedMrp?.selectedVersion || '')

  const weeklyCreatedBy = ref(savedWeekly?.weeklyCreatedBy || '')
  const weeklyFileNames = ref([])
  const weeklyFileName = ref(savedWeekly?.weeklyFileName || '')
  const weeklyColumns = ref(savedWeekly?.weeklyColumns || [])
  const weeklyReport = ref(savedWeekly?.weeklyReport || [])
  const weeklyColumnGroups = ref(savedWeekly?.weeklyColumnGroups || [])

  // MRP数据缓存
  const mrpPlansCache = ref(savedMrp?.mrpPlansCache || null)

  // 周报数据缓存
  const weeklyReportCache = ref(savedWeekly?.weeklyReportCache || null)

  // 监听MRP筛选变化，自动保存
  watch([selectedCreatedBy, selectedFileName, selectedVersion], () => {
    saveMrpFilters({
      selectedCreatedBy: selectedCreatedBy.value,
      selectedFileName: selectedFileName.value,
      selectedVersion: selectedVersion.value,
      mrpPlansCache: mrpPlansCache.value
    })
  })

  // 监听周报筛选变化，自动保存
  watch([weeklyCreatedBy, weeklyFileName], () => {
    saveWeeklyFilters({
      weeklyCreatedBy: weeklyCreatedBy.value,
      weeklyFileName: weeklyFileName.value,
      weeklyReportCache: weeklyReportCache.value
    })
  })

  const loadCreatedBys = async () => {
    try {
      const data = await getCreatedBys(token.value)
      createdBys.value = data.data || []
    } catch (err) {
      console.error('Load createdBys error:', err)
    }
  }

  const loadFileNames = async (createdBy) => {
    if (!createdBy) {
      fileNames.value = []
      return
    }
    try {
      const data = await getFileNamesByCreatedBy(token.value, createdBy)
      fileNames.value = data.data || []
    } catch (err) {
      console.error('Load fileNames error:', err)
    }
  }

  const loadVersions = async (createdBy, fileName) => {
    if (!createdBy || !fileName) {
      versions.value = []
      return
    }
    try {
      const data = await getVersionsByCreatedByAndFileName(token.value, createdBy, fileName)
      versions.value = data.data || []
    } catch (err) {
      console.error('Load versions error:', err)
    }
  }

  const onCreatedByChange = async () => {
    selectedFileName.value = ''
    selectedVersion.value = ''
    versions.value = []
    mrpPlansCache.value = null
    await loadFileNames(selectedCreatedBy.value)
  }

  const onFileNameChange = async () => {
    selectedVersion.value = ''
    versions.value = []
    mrpPlansCache.value = null
    await loadVersions(selectedCreatedBy.value, selectedFileName.value)
  }

  const loadMrpPlans = async () => {
    if (!selectedCreatedBy.value || !selectedFileName.value || !selectedVersion.value) {
      return { data: [] }
    }
    try {
      const data = await getPlansFiltered(
        token.value,
        selectedCreatedBy.value,
        selectedFileName.value,
        selectedVersion.value
      )
      mrpPlansCache.value = data
      // 触发保存
      saveMrpFilters({
        selectedCreatedBy: selectedCreatedBy.value,
        selectedFileName: selectedFileName.value,
        selectedVersion: selectedVersion.value,
        mrpPlansCache: mrpPlansCache.value
      })
      return data
    } catch (err) {
      console.error('Load MRP plans error:', err)
      return { data: [] }
    }
  }

  // Weekly report filters
  const onWeeklyCreatedByChange = async () => {
    weeklyFileName.value = ''
    weeklyColumns.value = []
    weeklyReport.value = []
    weeklyColumnGroups.value = []
    weeklyReportCache.value = null
    if (weeklyCreatedBy.value) {
      await loadFileNames(weeklyCreatedBy.value)
      weeklyFileNames.value = fileNames.value
    }
  }

  const onWeeklyFileNameChange = () => {
    weeklyReport.value = []
    weeklyColumns.value = []
    weeklyColumnGroups.value = []
    weeklyReportCache.value = null
  }

  const loadWeeklyReportData = async () => {
    if (!weeklyCreatedBy.value || !weeklyFileName.value) {
      return null
    }
    try {
      const data = await getWeeklyReport(token.value, weeklyCreatedBy.value, weeklyFileName.value)
      weeklyReportCache.value = data
      const result = data?.data?.[0]
      if (result) {
        const cols = result.columns || []
        // Add versionIndex for alternating colors
        const versionGroups = {}
        let versionIndex = 0
        for (const col of cols) {
          const version = col.version
          if (!versionGroups[version]) {
            versionGroups[version] = versionIndex++
          }
          col.versionIndex = versionGroups[version]
        }
        weeklyColumns.value = cols
        weeklyReport.value = result.data || []

        // Generate column groups
        const groups = []
        let currentWeek = null
        let currentGroup = null
        for (const col of cols) {
          if (col.week !== currentWeek) {
            if (currentGroup) groups.push(currentGroup)
            currentGroup = { week: col.week, weekLabel: col.weekLabel, versions: [], versionIndices: [] }
            currentWeek = col.week
          }
          currentGroup.versions.push(col)
        }
        if (currentGroup) groups.push(currentGroup)
        weeklyColumnGroups.value = groups
      }
      // 触发保存
      saveWeeklyFilters({
        weeklyCreatedBy: weeklyCreatedBy.value,
        weeklyFileName: weeklyFileName.value,
        weeklyReportCache: weeklyReportCache.value
      })
      return data
    } catch (err) {
      console.error('Load weekly report error:', err)
      return null
    }
  }

  const resetFilters = () => {
    selectedCreatedBy.value = ''
    selectedFileName.value = ''
    selectedVersion.value = ''
    fileNames.value = []
    versions.value = []
    mrpPlansCache.value = null
    sessionStorage.removeItem(SESSION_KEY_MRP)
  }

  // 恢复MRP缓存数据
  const restoreMrpCache = () => {
    if (savedMrp?.mrpPlansCache) {
      mrpPlansCache.value = savedMrp.mrpPlansCache
    }
  }

  // 恢复周报缓存数据
  const restoreWeeklyCache = () => {
    if (savedWeekly?.weeklyReportCache) {
      weeklyReportCache.value = savedWeekly.weeklyReportCache
      const result = savedWeekly.weeklyReportCache?.data?.[0]
      if (result) {
        weeklyColumns.value = result.columns || []
        weeklyReport.value = result.data || []
        // 重建columnGroups
        const cols = weeklyColumns.value
        const groups = []
        let currentWeek = null
        let currentGroup = null
        for (const col of cols) {
          if (col.week !== currentWeek) {
            if (currentGroup) groups.push(currentGroup)
            currentGroup = { week: col.week, weekLabel: col.weekLabel, versions: [], versionIndices: [] }
            currentWeek = col.week
          }
          currentGroup.versions.push(col)
        }
        if (currentGroup) groups.push(currentGroup)
        weeklyColumnGroups.value = groups
      }
    }
  }

  return {
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
    loadFileNames,
    loadVersions,
    onCreatedByChange,
    onFileNameChange,
    loadMrpPlans,
    onWeeklyCreatedByChange,
    onWeeklyFileNameChange,
    loadWeeklyReportData,
    resetFilters,
    restoreMrpCache,
    restoreWeeklyCache
  }
}
