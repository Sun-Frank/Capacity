import test from 'node:test'
import assert from 'node:assert/strict'

import { buildMrpCompareMatrixExport, buildMrpCompareOverview } from '../src/composables/useMrpCompareData.js'

function formatDate(date) {
  return date.toISOString().slice(0, 10)
}

function createWeekRanges(startWeekNo, count, startDateText) {
  const startDate = new Date(`${startDateText}T00:00:00`)
  return Object.fromEntries(Array.from({ length: count }, (_, index) => {
    const weekNo = startWeekNo + index
    const weekKey = `2026W${String(weekNo).padStart(2, '0')}`
    const current = new Date(startDate)
    current.setDate(startDate.getDate() + index * 7)
    const end = new Date(current)
    end.setDate(current.getDate() + 6)
    return [weekKey, [formatDate(current), formatDate(end)]]
  }))
}

test('buildMrpCompareOverview aggregates by description into 3-row matrix with summary block', () => {
  const result = buildMrpCompareOverview({
    viewType: 'week',
    products: [
      { itemNumber: 'ITEM-1', description: '主板A' },
      { itemNumber: 'ITEM-2', description: '主板A' },
      { itemNumber: 'ITEM-3', description: '电源B' }
    ],
    payloadA: {
      data: [
        { itemNumber: 'ITEM-1', weeks: { '2026W18': 10, '2026W19': 20 } },
        { itemNumber: 'ITEM-2', weeks: { '2026W18': 5, '2026W19': 10 } },
        { itemNumber: 'ITEM-3', weeks: { '2026W18': 8, '2026W19': 8 } }
      ],
      weekDateRanges: {
        '2026W18': ['2026-04-27', '2026-05-03'],
        '2026W19': ['2026-05-04', '2026-05-10']
      }
    },
    payloadB: {
      data: [
        { itemNumber: 'ITEM-1', weeks: { '2026W18': 30, '2026W19': 10 } },
        { itemNumber: 'ITEM-2', weeks: { '2026W18': 10, '2026W19': 10 } },
        { itemNumber: 'ITEM-3', weeks: { '2026W18': 4, '2026W19': 8 } }
      ],
      weekDateRanges: {
        '2026W18': ['2026-04-27', '2026-05-03'],
        '2026W19': ['2026-05-04', '2026-05-10']
      }
    }
  })

  assert.deepEqual(result.periods.map((item) => item.key), ['2026W18', '2026W19'])
  assert.equal(result.summary.periodCount, 2)
  assert.equal(result.summary.itemCount, 2)
  assert.equal(result.summary.totalQtyA, 61)
  assert.equal(result.summary.totalQtyB, 72)
  assert.equal(result.summary.totalDelta, 11)
  assert.deepEqual(result.deltaThresholds, { medium: 10, strong: 25 })

  assert.equal(result.groups[0].name, '全部汇总')
  assert.deepEqual(result.groups[0].rows[0].values, [23, 38])
  assert.deepEqual(result.groups[0].rows[1].values, [44, 28])
  assert.deepEqual(result.groups[0].rows[2].values, [21, -10])

  assert.equal(result.groups[1].name, '主板A')
  assert.deepEqual(result.groups[1].rows[0].values, [15, 30])
  assert.deepEqual(result.groups[1].rows[1].values, [40, 20])
  assert.deepEqual(result.groups[1].rows[2].values, [25, -10])
})

test('buildMrpCompareOverview uses later start date and limits weeks to 24 periods', () => {
  const result = buildMrpCompareOverview({
    viewType: 'week',
    products: [{ itemNumber: 'ITEM-1', description: '成品A' }],
    payloadA: {
      data: [
        {
          itemNumber: 'ITEM-1',
          weeks: Object.fromEntries(Array.from({ length: 30 }, (_, index) => [`2026W${String(18 + index).padStart(2, '0')}`, index + 1]))
        }
      ],
      weekDateRanges: createWeekRanges(18, 30, '2026-04-27')
    },
    payloadB: {
      data: [
        {
          itemNumber: 'ITEM-1',
          weeks: Object.fromEntries(Array.from({ length: 28 }, (_, index) => [`2026W${String(20 + index).padStart(2, '0')}`, 100 + index]))
        }
      ],
      weekDateRanges: createWeekRanges(20, 28, '2026-05-11')
    }
  })

  assert.equal(result.periods.length, 24)
  assert.equal(result.periods[0].key, '2026W20')
  assert.equal(result.periods.at(-1).key, '2026W43')
  assert.equal(result.groups[1].rows[0].values[0], 3)
  assert.equal(result.groups[1].rows[1].values[0], 100)
  assert.equal(result.deltaThresholds.medium > 0, true)
  assert.equal(result.deltaThresholds.strong >= result.deltaThresholds.medium, true)
})

test('buildMrpCompareOverview falls back to item number when product description is missing', () => {
  const result = buildMrpCompareOverview({
    viewType: 'month',
    products: [],
    payloadA: {
      data: [
        { itemNumber: 'ITEM-9', months: { '2026-05': 12 } }
      ],
      monthDateRanges: { '2026-05': ['2026-05-01', '2026-05-31'] }
    },
    payloadB: {
      data: [
        { itemNumber: 'ITEM-9', months: { '2026-05': 8 } }
      ],
      monthDateRanges: { '2026-05': ['2026-05-01', '2026-05-31'] }
    }
  })

  assert.equal(result.groups[1].name, 'ITEM-9')
  assert.deepEqual(result.groups[1].rows[2].values, [-4])
})

test('buildMrpCompareMatrixExport flattens matrix groups into csv rows', () => {
  const compareResult = buildMrpCompareOverview({
    viewType: 'month',
    products: [{ itemNumber: 'ITEM-1', description: '成品A' }],
    payloadA: {
      data: [
        { itemNumber: 'ITEM-1', months: { '2026-05': 12, '2026-06': 10 } }
      ],
      monthDateRanges: {
        '2026-05': ['2026-05-01', '2026-05-31'],
        '2026-06': ['2026-06-01', '2026-06-30']
      }
    },
    payloadB: {
      data: [
        { itemNumber: 'ITEM-1', months: { '2026-05': 8, '2026-06': 15 } }
      ],
      monthDateRanges: {
        '2026-05': ['2026-05-01', '2026-05-31'],
        '2026-06': ['2026-06-01', '2026-06-30']
      }
    },
    fileLabelA: '文件A.xlsx / v1',
    fileLabelB: '文件B.xlsx / v2'
  })

  const exported = buildMrpCompareMatrixExport(compareResult)

  assert.deepEqual(exported.headers.map((item) => item.key), ['groupName', 'metricLabel', '2026-05', '2026-06', 'total'])
  assert.equal(exported.rows[0].groupName, '全部汇总')
  assert.equal(exported.rows[0].metricLabel, '文件A.xlsx / v1数量')
  assert.equal(exported.rows[0]['2026-05'], 12)
  assert.equal(exported.rows[1].metricLabel, '文件B.xlsx / v2数量')
  assert.equal(exported.rows[2].metricLabel, '差异')
  assert.equal(exported.rows.at(-1).groupName, '成品A')
})
