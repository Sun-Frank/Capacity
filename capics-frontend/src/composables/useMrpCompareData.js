function toNumber(value) {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function normalizeKey(value) {
  return String(value || '').trim().toUpperCase()
}

function getPeriodField(viewType) {
  return viewType === 'month' ? 'months' : 'weeks'
}

function getRangeField(viewType) {
  return viewType === 'month' ? 'monthDateRanges' : 'weekDateRanges'
}

function getWindowSize(viewType) {
  return viewType === 'month' ? 12 : 24
}

function formatRangeLabel(range, key) {
  if (!Array.isArray(range) || !range[0]) return key
  if (!range[1] || range[0] === range[1]) return String(range[0])
  return `${range[0]} ~ ${range[1]}`
}

function parseDate(dateText) {
  if (!dateText) return null
  const parsed = new Date(`${dateText}T00:00:00`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function buildDescriptionMap(products = []) {
  const descriptionMap = {}

  for (const product of products) {
    const key = normalizeKey(product?.itemNumber)
    if (!key) continue

    const description = String(product?.description || '').trim()
    if (description) {
      descriptionMap[key] = description
    }
  }

  return descriptionMap
}

function getDisplayName(row, descriptionMap) {
  const itemNumber = String(row?.itemNumber || '').trim()
  const description = descriptionMap[normalizeKey(itemNumber)]

  if (description) return description
  if (itemNumber) return itemNumber

  const fallbackDescription = String(row?.description || '').trim()
  return fallbackDescription || '未维护描述'
}

function buildPeriodWindow(payloadA, payloadB, viewType) {
  const rangeField = getRangeField(viewType)
  const rangesA = payloadA?.[rangeField] || {}
  const rangesB = payloadB?.[rangeField] || {}
  const mergedMap = new Map()

  for (const [key, range] of Object.entries(rangesA)) {
    mergedMap.set(key, { key, range, start: parseDate(Array.isArray(range) ? range[0] : ''), source: 'A' })
  }

  for (const [key, range] of Object.entries(rangesB)) {
    const existing = mergedMap.get(key)
    mergedMap.set(key, {
      key,
      range: existing?.range || range,
      start: existing?.start || parseDate(Array.isArray(range) ? range[0] : ''),
      source: existing?.source || 'B'
    })
  }

  const periods = Array.from(mergedMap.values()).sort((left, right) => {
    const leftTime = left.start?.getTime() ?? Number.POSITIVE_INFINITY
    const rightTime = right.start?.getTime() ?? Number.POSITIVE_INFINITY
    return leftTime - rightTime || left.key.localeCompare(right.key)
  })

  const firstPeriodA = Object.entries(rangesA)
    .map(([key, range]) => ({ key, start: parseDate(Array.isArray(range) ? range[0] : '') }))
    .sort((left, right) => (left.start?.getTime() ?? Number.POSITIVE_INFINITY) - (right.start?.getTime() ?? Number.POSITIVE_INFINITY))[0]

  const firstPeriodB = Object.entries(rangesB)
    .map(([key, range]) => ({ key, start: parseDate(Array.isArray(range) ? range[0] : '') }))
    .sort((left, right) => (left.start?.getTime() ?? Number.POSITIVE_INFINITY) - (right.start?.getTime() ?? Number.POSITIVE_INFINITY))[0]

  const startCandidates = [firstPeriodA?.start, firstPeriodB?.start].filter(Boolean)
  const compareStart = startCandidates.length
    ? new Date(Math.max(...startCandidates.map((item) => item.getTime())))
    : null

  const filteredPeriods = compareStart
    ? periods.filter((period) => period.start && period.start.getTime() >= compareStart.getTime())
    : periods

  return filteredPeriods.slice(0, getWindowSize(viewType)).map((period) => ({
    key: period.key,
    label: formatRangeLabel(period.range, period.key)
  }))
}

function aggregateByDisplayName(payload, descriptionMap, viewType, periodKeys) {
  const periodField = getPeriodField(viewType)
  const grouped = {}

  for (const row of payload?.data || []) {
    const name = getDisplayName(row, descriptionMap)
    const values = row?.[periodField] || {}

    if (!grouped[name]) {
      grouped[name] = Object.fromEntries(periodKeys.map((period) => [period, 0]))
    }

    for (const period of periodKeys) {
      grouped[name][period] += toNumber(values[period])
    }
  }

  return grouped
}

function buildMetricRow(type, label, values) {
  const total = values.reduce((sum, value) => sum + toNumber(value), 0)
  return {
    type,
    label,
    values,
    total
  }
}

function getPercentile(sortedValues, ratio) {
  if (!sortedValues.length) return 0
  const index = Math.min(sortedValues.length - 1, Math.max(0, Math.ceil(sortedValues.length * ratio) - 1))
  return sortedValues[index]
}

function buildDeltaThresholds(itemGroups) {
  const absValues = itemGroups
    .flatMap((group) => group.rows.find((row) => row.type === 'delta')?.values || [])
    .map((value) => Math.abs(toNumber(value)))
    .filter((value) => value > 0)
    .sort((left, right) => left - right)

  if (!absValues.length) {
    return {
      medium: 0,
      strong: 0
    }
  }

  const medium = getPercentile(absValues, 0.6)
  const strong = Math.max(medium, getPercentile(absValues, 0.85))

  return { medium, strong }
}

export function buildMrpCompareOverview({
  payloadA,
  payloadB,
  products,
  viewType,
  fileLabelA = '文件A',
  fileLabelB = '文件B'
}) {
  const descriptionMap = buildDescriptionMap(products)
  const periods = buildPeriodWindow(payloadA, payloadB, viewType)
  const periodKeys = periods.map((item) => item.key)
  const groupedA = aggregateByDisplayName(payloadA, descriptionMap, viewType, periodKeys)
  const groupedB = aggregateByDisplayName(payloadB, descriptionMap, viewType, periodKeys)
  const names = Array.from(new Set([...Object.keys(groupedA), ...Object.keys(groupedB)]))

  const itemGroups = names.map((name) => {
    const valuesA = periodKeys.map((period) => toNumber(groupedA[name]?.[period]))
    const valuesB = periodKeys.map((period) => toNumber(groupedB[name]?.[period]))
    const deltas = valuesB.map((value, index) => value - valuesA[index])
    const totalDelta = deltas.reduce((sum, value) => sum + value, 0)

    return {
      key: name,
      name,
      totalDelta,
      absTotalDelta: Math.abs(totalDelta),
      rows: [
        buildMetricRow('qtyA', `${fileLabelA}数量`, valuesA),
        buildMetricRow('qtyB', `${fileLabelB}数量`, valuesB),
        buildMetricRow('delta', '差异', deltas)
      ]
    }
  })
    .filter((group) => group.rows.some((row) => row.values.some((value) => value !== 0)))
    .sort((left, right) => right.absTotalDelta - left.absTotalDelta || left.name.localeCompare(right.name, 'zh-CN'))

  const totalValuesA = periodKeys.map((_, index) => itemGroups.reduce((sum, group) => sum + toNumber(group.rows[0].values[index]), 0))
  const totalValuesB = periodKeys.map((_, index) => itemGroups.reduce((sum, group) => sum + toNumber(group.rows[1].values[index]), 0))
  const totalDeltas = totalValuesB.map((value, index) => value - totalValuesA[index])

  const summaryGroup = {
    key: 'summary',
    name: '全部汇总',
    totalDelta: totalDeltas.reduce((sum, value) => sum + value, 0),
    rows: [
      buildMetricRow('qtyA', `${fileLabelA}数量`, totalValuesA),
      buildMetricRow('qtyB', `${fileLabelB}数量`, totalValuesB),
      buildMetricRow('delta', '差异', totalDeltas)
    ]
  }

  const deltaThresholds = buildDeltaThresholds(itemGroups)

  return {
    fileLabelA,
    fileLabelB,
    periods,
    groups: [summaryGroup, ...itemGroups],
    deltaThresholds,
    summary: {
      periodCount: periods.length,
      itemCount: itemGroups.length,
      totalQtyA: summaryGroup.rows[0].total,
      totalQtyB: summaryGroup.rows[1].total,
      totalDelta: summaryGroup.rows[2].total
    }
  }
}

export function buildMrpCompareMatrixExport(compareResult) {
  const periods = compareResult?.periods || []
  const groups = compareResult?.groups || []
  const headers = [
    { key: 'groupName', label: '产品描述 / Item Number' },
    { key: 'metricLabel', label: '指标' }
  ]

  periods.forEach((period) => {
    headers.push({
      key: period.key,
      label: `${period.key}${period.label ? ` (${period.label})` : ''}`
    })
  })

  headers.push({ key: 'total', label: '合计' })

  const rows = groups.flatMap((group) => group.rows.map((row) => {
    const exportRow = {
      groupName: group.name,
      metricLabel: row.label,
      total: row.total
    }

    periods.forEach((period, index) => {
      exportRow[period.key] = row.values[index] ?? ''
    })

    return exportRow
  }))

  return { headers, rows }
}
