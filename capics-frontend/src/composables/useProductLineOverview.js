const ALL_PROCESS = 'ALL'
const WEEK_HEATMAP_LIMIT = 24
const MONTH_HEATMAP_LIMIT = 12
const EXCLUDED_LINE_CODES = new Set(['FAL1005N'])
const SPECIAL_RED_THRESHOLD_LINE_CODES = new Set(['FAL1007N', 'ICP1001N'])

function toNumber(value) {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function normalizeLineCode(lineCode) {
  return String(lineCode || '').trim().toUpperCase()
}

function getProcessCode(lineCode) {
  return normalizeLineCode(lineCode).slice(0, 3) || 'OTHER'
}

function shouldIncludeLine(lineCode) {
  return !EXCLUDED_LINE_CODES.has(normalizeLineCode(lineCode))
}

function getDimensionDates(dates, dimension) {
  const limit = dimension === 'month' ? MONTH_HEATMAP_LIMIT : WEEK_HEATMAP_LIMIT
  return Array.isArray(dates) ? dates.slice(0, limit) : []
}

function getLineDisplayName(lineCode, lineName) {
  if (getProcessCode(lineCode) === 'FAL' && lineName) {
    return `${lineCode} ${lineName}`
  }
  return lineCode
}

function getHeatmapRedThreshold(lineCode) {
  return SPECIAL_RED_THRESHOLD_LINE_CODES.has(normalizeLineCode(lineCode)) ? 200 : 100
}

export function getHeatmapCellBucket(lineCode, value) {
  const numericValue = toNumber(value)
  const redThreshold = getHeatmapRedThreshold(lineCode)

  if (numericValue <= 60) return 0
  if (numericValue <= 80) return 1
  if (numericValue <= 100) return 2
  if (numericValue > redThreshold) return 4
  return 3
}

function buildDimensionOverview(dates, dateLabels, rawLines, lineNameMap, dimension) {
  const scopedDates = getDimensionDates(dates, dimension)

  const normalizedLines = Object.entries(rawLines || {})
    .filter(([lineCode]) => shouldIncludeLine(lineCode))
    .map(([lineCode, rows]) => {
      const demandByDate = {}
      const loadingByDate = {}
      const lineName = lineNameMap[lineCode] || ''

      scopedDates.forEach((dateKey) => {
        demandByDate[dateKey] = 0
        loadingByDate[dateKey] = 0
      })

      ;(rows || []).forEach((row) => {
        scopedDates.forEach((dateKey) => {
          demandByDate[dateKey] += toNumber(row[`${dateKey}_demand`])
          loadingByDate[dateKey] += toNumber(row[`${dateKey}_loading`])
        })
      })

      const processCode = getProcessCode(lineCode)
      const avgLoading = scopedDates.length
        ? scopedDates.reduce((sum, dateKey) => sum + loadingByDate[dateKey], 0) / scopedDates.length
        : 0
      const peakLoading = scopedDates.reduce((max, dateKey) => Math.max(max, loadingByDate[dateKey]), 0)

      return {
        lineCode,
        lineName,
        displayName: getLineDisplayName(lineCode, lineName),
        label: lineName ? `${lineCode} - ${lineName}` : lineCode,
        processCode,
        demandByDate,
        loadingByDate,
        avgLoading,
        peakLoading,
        overloaded: peakLoading > 1
      }
    })

  const processCodes = Array.from(new Set(normalizedLines.map((line) => line.processCode))).sort()
  const groupedByProcess = { [ALL_PROCESS]: normalizedLines }

  processCodes.forEach((processCode) => {
    groupedByProcess[processCode] = normalizedLines.filter((line) => line.processCode === processCode)
  })

  const processSummaries = {}

  Object.entries(groupedByProcess).forEach(([processCode, lines]) => {
    const totalDemandByDate = {}
    const avgLoadingByDate = {}

    scopedDates.forEach((dateKey) => {
      const totalDemand = lines.reduce((sum, line) => sum + line.demandByDate[dateKey], 0)
      const totalLoading = lines.reduce((sum, line) => sum + line.loadingByDate[dateKey], 0)

      totalDemandByDate[dateKey] = totalDemand
      avgLoadingByDate[dateKey] = lines.length ? totalLoading / lines.length : 0
    })

    const avgLoading = scopedDates.length
      ? scopedDates.reduce((sum, dateKey) => sum + avgLoadingByDate[dateKey], 0) / scopedDates.length
      : 0
    const peakLoading = scopedDates.reduce((max, dateKey) => Math.max(max, avgLoadingByDate[dateKey]), 0)

    processSummaries[processCode] = {
      processCode,
      lineCount: lines.length,
      overloadedLineCount: lines.filter((line) => line.overloaded).length,
      avgLoading,
      peakLoading,
      dates: scopedDates,
      dateLabels,
      lines: [...lines].sort((a, b) => b.avgLoading - a.avgLoading || a.lineCode.localeCompare(b.lineCode)),
      trend: scopedDates.map((dateKey) => ({
        key: dateKey,
        label: dateLabels[dateKey] || dateKey,
        totalDemand: totalDemandByDate[dateKey],
        avgLoading: avgLoadingByDate[dateKey]
      })),
      heatmapRows: [...lines].sort((a, b) => a.lineCode.localeCompare(b.lineCode)),
      totalDemandByDate,
      avgLoadingByDate
    }
  })

  return {
    processCodes,
    processSummaries
  }
}

export function buildProductLineOverview(weeklyPayload, monthlyPayload, lineNameMap = {}) {
  const weekly = buildDimensionOverview(
    weeklyPayload?.weeks || [],
    weeklyPayload?.weekDates || {},
    weeklyPayload?.lines || {},
    lineNameMap,
    'week'
  )

  const monthly = buildDimensionOverview(
    monthlyPayload?.months || [],
    monthlyPayload?.monthDates || {},
    monthlyPayload?.lines || {},
    lineNameMap,
    'month'
  )

  const processCodes = Array.from(new Set([
    ...weekly.processCodes,
    ...monthly.processCodes
  ])).sort()

  return {
    processOptions: [ALL_PROCESS, ...processCodes],
    weekly: weekly.processSummaries,
    monthly: monthly.processSummaries
  }
}

export function formatRate(value) {
  return `${(toNumber(value) * 100).toFixed(1)}%`
}

export function getHeatmapTrendWindow(trend, dimension) {
  return getDimensionDates(trend, dimension)
}

export function getHeatmapCellColor(lineCode, value) {
  const bucket = getHeatmapCellBucket(lineCode, value)
  if (bucket === 0) return '#dff3ff'
  if (bucket === 1) return '#87c5ea'
  if (bucket === 2) return '#0b74b8'
  if (bucket === 3) return '#f2a65a'
  return '#d93f2f'
}

export function getProcessDisplay(processCode) {
  return processCode === ALL_PROCESS ? '全部工艺段' : processCode
}

export { ALL_PROCESS }
