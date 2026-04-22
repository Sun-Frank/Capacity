import test from 'node:test'
import assert from 'node:assert/strict'

import { aggregateReportByDescription } from '../src/composables/useMrpDescriptionReport.js'

test('aggregateReportByDescription groups weekly rows by description and sums all column values', () => {
  const columns = [
    { key: 'w1_v1', week: '2026W18', weekLabel: '2026/04/27-2026/05/03', version: 'V1', versionIndex: 0 },
    { key: 'w1_v2', week: '2026W18', weekLabel: '2026/04/27-2026/05/03', version: 'V2', versionIndex: 1 },
    { key: 'w2_v1', week: '2026W19', weekLabel: '2026/05/04-2026/05/10', version: 'V1', versionIndex: 0 }
  ]
  const rows = [
    { itemNumber: 'ITEM-1', description: '成品A', w1_v1: 10, w1_v2: 12, w2_v1: 14 },
    { itemNumber: 'ITEM-2', description: '成品A', w1_v1: 5, w1_v2: 8, w2_v1: 6 },
    { itemNumber: 'ITEM-3', description: '成品B', w1_v1: 9, w1_v2: 4, w2_v1: 3 }
  ]

  const result = aggregateReportByDescription(columns, rows)

  assert.deepEqual(result.columns, columns)
  assert.equal(result.rows.length, 2)
  assert.equal(result.rows[0].descriptionGroup, '成品A')
  assert.equal(result.rows[0].itemCount, 2)
  assert.equal(result.rows[0].w1_v1, 15)
  assert.equal(result.rows[0].w1_v2, 20)
  assert.equal(result.rows[0].w2_v1, 20)
  assert.equal(result.rows[1].descriptionGroup, '成品B')
  assert.equal(result.rows[1].itemCount, 1)
})

test('aggregateReportByDescription falls back to item number when description is empty', () => {
  const columns = [
    { key: 'm1_v1', month: '2026-05', monthLabel: '2026/05', version: 'V1', versionIndex: 0 }
  ]
  const rows = [
    { itemNumber: 'ITEM-9', description: '', m1_v1: 7 },
    { itemNumber: 'ITEM-10', description: null, m1_v1: 3 }
  ]

  const result = aggregateReportByDescription(columns, rows)

  assert.equal(result.rows[0].descriptionGroup, 'ITEM-10')
  assert.equal(result.rows[0].itemCount, 1)
  assert.equal(result.rows[1].descriptionGroup, 'ITEM-9')
  assert.equal(result.rows[1].itemCount, 1)
})
