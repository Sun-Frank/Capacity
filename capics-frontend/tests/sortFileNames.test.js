import test from 'node:test'
import assert from 'node:assert/strict'

import { sortFileNamesDesc, withSortedFileNames } from '../src/utils/sortFileNames.js'

test('sortFileNamesDesc sorts file names from Z to A', () => {
  assert.deepEqual(
    sortFileNamesDesc(['A-plan', 'Z-plan', 'M-plan', 'B-plan']),
    ['Z-plan', 'M-plan', 'B-plan', 'A-plan']
  )
})

test('withSortedFileNames sorts response data without mutating the original response', () => {
  const response = {
    success: true,
    data: ['file-2', 'file-10', 'file-1']
  }

  const sorted = withSortedFileNames(response)

  assert.deepEqual(sorted.data, ['file-10', 'file-2', 'file-1'])
  assert.deepEqual(response.data, ['file-2', 'file-10', 'file-1'])
})
