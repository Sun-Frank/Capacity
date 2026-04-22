import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildProductLineOverview,
  getHeatmapCellBucket,
  getHeatmapCellColor,
  getHeatmapTrendWindow
} from '../src/composables/useProductLineOverview.js'

test('weekly heatmap only keeps the first 24 periods', () => {
  const trend = Array.from({ length: 30 }, (_, index) => ({ key: `W${index + 1}` }))

  const result = getHeatmapTrendWindow(trend, 'week')

  assert.equal(result.length, 24)
  assert.deepEqual(result.map((item) => item.key), trend.slice(0, 24).map((item) => item.key))
})

test('monthly heatmap only keeps the first 12 periods', () => {
  const trend = Array.from({ length: 18 }, (_, index) => ({ key: `M${index + 1}` }))

  const result = getHeatmapTrendWindow(trend, 'month')

  assert.equal(result.length, 12)
  assert.deepEqual(result.map((item) => item.key), trend.slice(0, 12).map((item) => item.key))
})

test('all weekly and monthly summaries are scoped to the first 24 weeks and 12 months', () => {
  const weeklyKeys = Array.from({ length: 30 }, (_, index) => `W${index + 1}`)
  const monthlyKeys = Array.from({ length: 15 }, (_, index) => `M${index + 1}`)

  const weeklyRow = {}
  weeklyKeys.forEach((key, index) => {
    weeklyRow[`${key}_demand`] = index + 1
    weeklyRow[`${key}_loading`] = 0.5 + index * 0.01
  })

  const monthlyRow = {}
  monthlyKeys.forEach((key, index) => {
    monthlyRow[`${key}_demand`] = index + 1
    monthlyRow[`${key}_loading`] = 0.6 + index * 0.01
  })

  const result = buildProductLineOverview(
    {
      weeks: weeklyKeys,
      weekDates: Object.fromEntries(weeklyKeys.map((key) => [key, key])),
      lines: { FAL1001: [weeklyRow] }
    },
    {
      months: monthlyKeys,
      monthDates: Object.fromEntries(monthlyKeys.map((key) => [key, key])),
      lines: { FAL1001: [monthlyRow] }
    },
    { FAL1001: '装配一线' }
  )

  assert.equal(result.weekly.ALL.trend.length, 24)
  assert.equal(result.weekly.ALL.dates.length, 24)
  assert.equal(result.monthly.ALL.trend.length, 12)
  assert.equal(result.monthly.ALL.dates.length, 12)
})

test('FAL lines use code plus line name as display name', () => {
  const result = buildProductLineOverview(
    {
      weeks: ['W1'],
      weekDates: { W1: 'W1' },
      lines: {
        FAL1001: [{ W1_demand: 1, W1_loading: 0.8 }],
        SMT1001: [{ W1_demand: 1, W1_loading: 0.7 }]
      }
    },
    {
      months: ['M1'],
      monthDates: { M1: 'M1' },
      lines: {
        FAL1001: [{ M1_demand: 1, M1_loading: 0.8 }],
        SMT1001: [{ M1_demand: 1, M1_loading: 0.7 }]
      }
    },
    { FAL1001: '装配一线', SMT1001: '贴片一线' }
  )

  const falLine = result.weekly.ALL.lines.find((line) => line.lineCode === 'FAL1001')
  const smtLine = result.weekly.ALL.lines.find((line) => line.lineCode === 'SMT1001')

  assert.equal(falLine.displayName, 'FAL1001 装配一线')
  assert.equal(smtLine.displayName, 'SMT1001')
})

test('FAL1005N is excluded from display and statistics', () => {
  const result = buildProductLineOverview(
    {
      weeks: ['W1'],
      weekDates: { W1: 'W1' },
      lines: {
        FAL1005N: [{ W1_demand: 10, W1_loading: 0.9 }],
        FAL1001: [{ W1_demand: 5, W1_loading: 0.8 }]
      }
    },
    {
      months: ['M1'],
      monthDates: { M1: 'M1' },
      lines: {
        FAL1005N: [{ M1_demand: 10, M1_loading: 0.9 }],
        FAL1001: [{ M1_demand: 5, M1_loading: 0.8 }]
      }
    },
    { FAL1005N: '不参与产线', FAL1001: '装配一线' }
  )

  assert.equal(result.weekly.ALL.lineCount, 1)
  assert.equal(result.monthly.ALL.lineCount, 1)
  assert.equal(result.weekly.ALL.lines.some((line) => line.lineCode === 'FAL1005N'), false)
  assert.equal(result.monthly.ALL.lines.some((line) => line.lineCode === 'FAL1005N'), false)
})

test('special heatmap lines only turn red above 200 percent', () => {
  assert.equal(getHeatmapCellBucket('FAL1001', 50), 0)
  assert.equal(getHeatmapCellBucket('FAL1001', 75), 1)
  assert.equal(getHeatmapCellBucket('FAL1001', 95), 2)
  assert.equal(getHeatmapCellBucket('FAL1001', 120), 4)
  assert.equal(getHeatmapCellBucket('FAL1007N', 150), 3)
  assert.equal(getHeatmapCellBucket('FAL1007N', 210), 4)
  assert.equal(getHeatmapCellColor('FAL1001', 120), '#d93f2f')
  assert.equal(getHeatmapCellColor('FAL1007N', 150), '#f2a65a')
  assert.equal(getHeatmapCellColor('FAL1007N', 210), '#d93f2f')
  assert.equal(getHeatmapCellColor('ICP1001N', 180), '#f2a65a')
  assert.equal(getHeatmapCellColor('ICP1001N', 220), '#d93f2f')
})
