import { ref } from 'vue'
import {
  getCreatedBys,
  getFileNamesByCreatedBy,
  getVersionsByCreatedByAndFileName,
  getPlansFiltered,
  getWeeklyReport
} from '@/api/mrp'

export function useMrpFilters(token) {
  const createdBys = ref([])
  const fileNames = ref([])
  const versions = ref([])

  const selectedCreatedBy = ref('')
  const selectedFileName = ref('')
  const selectedVersion = ref('')

  const weeklyCreatedBy = ref('')
  const weeklyFileNames = ref([])
  const weeklyFileName = ref('')
  const weeklyColumns = ref([])
  const weeklyReport = ref([])
  const weeklyColumnGroups = ref([])

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
    await loadFileNames(selectedCreatedBy.value)
  }

  const onFileNameChange = async () => {
    selectedVersion.value = ''
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
    if (weeklyCreatedBy.value) {
      await loadFileNames(weeklyCreatedBy.value)
      weeklyFileNames.value = fileNames.value
    }
  }

  const onWeeklyFileNameChange = () => {
    weeklyReport.value = []
    weeklyColumns.value = []
    weeklyColumnGroups.value = []
  }

  const loadWeeklyReportData = async () => {
    if (!weeklyCreatedBy.value || !weeklyFileName.value) {
      return null
    }
    try {
      const data = await getWeeklyReport(token.value, weeklyCreatedBy.value, weeklyFileName.value)
      const result = data.data && data.data[0]
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
    loadCreatedBys,
    loadFileNames,
    loadVersions,
    onCreatedByChange,
    onFileNameChange,
    loadMrpPlans,
    onWeeklyCreatedByChange,
    onWeeklyFileNameChange,
    loadWeeklyReportData,
    resetFilters
  }
}
