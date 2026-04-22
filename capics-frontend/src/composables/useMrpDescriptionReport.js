function normalizeGroupName(row) {
  const description = typeof row?.description === 'string' ? row.description.trim() : ''
  const itemNumber = typeof row?.itemNumber === 'string' ? row.itemNumber.trim() : ''
  return description || itemNumber || '未维护描述'
}

function toNumber(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

export function aggregateReportByDescription(columns = [], rows = []) {
  const grouped = new Map()

  for (const row of rows) {
    const groupName = normalizeGroupName(row)
    const existing = grouped.get(groupName) || {
      descriptionGroup: groupName,
      itemCount: 0
    }

    existing.itemCount += 1

    for (const column of columns) {
      const key = column.key
      existing[key] = toNumber(existing[key]) + toNumber(row?.[key])
    }

    grouped.set(groupName, existing)
  }

  const aggregatedRows = Array.from(grouped.values())
    .sort((left, right) => left.descriptionGroup.localeCompare(right.descriptionGroup, 'zh-CN'))

  return {
    columns,
    rows: aggregatedRows
  }
}
