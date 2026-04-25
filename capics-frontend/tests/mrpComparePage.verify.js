import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const BASE_URL = 'http://127.0.0.1:3000'
const OUTPUT_DIR = path.resolve('..', '.tmp')
const OUTPUT_FILE = path.join(OUTPUT_DIR, 'mrp-compare-verify.png')

function approxEqual(a, b, tolerance = 1.5) {
  return Math.abs(a - b) <= tolerance
}

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.locator('input').nth(0).fill('admin')
  await page.locator('input').nth(1).fill('admin123')
  await page.locator('button').last().click()
  await page.waitForTimeout(1000)
}

async function loadCompareMatrix(page) {
  await page.goto(`${BASE_URL}/mrp-compare`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(1200)

  const selects = page.locator('select')
  await selects.nth(0).selectOption('admin')
  await page.waitForTimeout(300)

  const fileAOptions = await selects.nth(1).locator('option').evaluateAll((nodes) => nodes.map((n) => n.value))
  await selects.nth(1).selectOption(fileAOptions[1])
  await page.waitForTimeout(300)

  const fileAVersions = await selects.nth(2).locator('option').evaluateAll((nodes) => nodes.map((n) => n.value))
  await selects.nth(2).selectOption(fileAVersions[1])
  await page.waitForTimeout(300)

  await selects.nth(3).selectOption('admin')
  await page.waitForTimeout(300)

  const fileBOptions = await selects.nth(4).locator('option').evaluateAll((nodes) => nodes.map((n) => n.value))
  await selects.nth(4).selectOption(fileBOptions[2])
  await page.waitForTimeout(300)

  const fileBVersions = await selects.nth(5).locator('option').evaluateAll((nodes) => nodes.map((n) => n.value))
  await selects.nth(5).selectOption(fileBVersions[1])
  await page.waitForTimeout(300)

  await page.locator('button.btn.btn-primary').click()
  await page.waitForTimeout(3000)
  await page.locator('.compare-table-panel').scrollIntoViewIfNeeded()
  await page.waitForTimeout(300)
}

async function collectMetrics(page, label) {
  return page.evaluate((tag) => {
    const topBar = document.querySelector('.compare-top-scrollbar')
    const table = document.querySelector('.table-scroll')
    const headerMetric = document.querySelectorAll('thead th')[1]
    const firstPeriod = document.querySelectorAll('.period-head')[0]
    const summaryName = document.querySelector('tbody tr th.sticky-name')
    const tableRect = table.getBoundingClientRect()
    const stickyNames = Array.from(document.querySelectorAll('tbody th.sticky-name'))
    const visibleStickyName = stickyNames.find((node) => {
      const rect = node.getBoundingClientRect()
      return rect.bottom > tableRect.top + 12 && rect.top < tableRect.bottom - 12
    }) || summaryName

    const rect = (node) => {
      if (!node) return null
      const box = node.getBoundingClientRect()
      return {
        top: box.top,
        left: box.left,
        right: box.right,
        bottom: box.bottom,
        width: box.width,
        height: box.height
      }
    }

    const headerRect = rect(headerMetric)
    const visibleStickyRect = rect(visibleStickyName)
    const rawHeaderPoint = headerRect
      ? document.elementFromPoint(headerRect.left + headerRect.width / 2, headerRect.top + headerRect.height / 2)
      : null
    const rawStickyPoint = visibleStickyRect
      ? document.elementFromPoint(
          visibleStickyRect.left + 20,
          Math.max(
            visibleStickyRect.top + 12,
            Math.min(visibleStickyRect.bottom - 12, tableRect.bottom - 12)
          )
        )
      : null
    const headerPoint = rawHeaderPoint?.closest('th,td') || rawHeaderPoint
    const stickyPoint = rawStickyPoint?.closest('th,td') || rawStickyPoint

    return {
      label: tag,
      topBar: {
        scrollLeft: topBar.scrollLeft,
        scrollWidth: topBar.scrollWidth,
        clientWidth: topBar.clientWidth
      },
      table: {
        scrollTop: table.scrollTop,
        scrollLeft: table.scrollLeft,
        scrollHeight: table.scrollHeight,
        clientHeight: table.clientHeight
      },
      headerMetric: headerRect,
      firstPeriod: rect(firstPeriod),
      summaryName: rect(summaryName),
      visibleStickyName: visibleStickyRect,
      topElementAtHeader: headerPoint ? {
        tag: headerPoint.tagName,
        className: headerPoint.className,
        text: headerPoint.textContent.trim().slice(0, 40)
      } : null,
      topElementAtStickyName: stickyPoint ? {
        tag: stickyPoint.tagName,
        className: stickyPoint.className,
        text: stickyPoint.textContent.trim().slice(0, 40)
      } : null
    }
  }, label)
}

async function main() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })

  try {
    await login(page)
    await loadCompareMatrix(page)

    const before = await collectMetrics(page, 'before')

    assert(before.topBar.scrollWidth > before.topBar.clientWidth, 'Top scrollbar should be horizontally scrollable')

    await page.evaluate(() => {
      const topBar = document.querySelector('.compare-top-scrollbar')
      topBar.scrollLeft = 700
      topBar.dispatchEvent(new Event('scroll', { bubbles: true }))
    })
    await page.waitForTimeout(200)
    const afterHorizontal = await collectMetrics(page, 'afterHorizontal')

    assert.equal(afterHorizontal.topBar.scrollLeft, 700, 'Top scrollbar scrollLeft should update')
    assert.equal(afterHorizontal.table.scrollLeft, 700, 'Top scrollbar should sync to table horizontal scroll')
    assert(approxEqual(afterHorizontal.headerMetric.left, before.headerMetric.left), 'Frozen metric header should stay fixed horizontally')
    assert(afterHorizontal.firstPeriod.left < before.firstPeriod.left, 'Period columns should move left after horizontal scroll')

    await page.evaluate(() => {
      const table = document.querySelector('.table-scroll')
      table.scrollTop = 900
    })
    await page.waitForTimeout(200)
    const afterVertical = await collectMetrics(page, 'afterVertical')

    assert(afterVertical.table.scrollTop >= 900, 'Table vertical scroll should move')
    assert(approxEqual(afterVertical.headerMetric.top, before.headerMetric.top), 'Header row should stay fixed during vertical scroll')
    assert(afterVertical.summaryName.top < before.summaryName.top, 'Summary row should scroll away instead of sticking')
    assert(
      afterVertical.topElementAtHeader?.className?.includes('sticky-metric') ||
      afterVertical.topElementAtHeader?.className?.includes('period-head'),
      'Header point should still be covered by header cells'
    )
    assert(
      afterVertical.topElementAtStickyName?.className?.includes('sticky-name'),
      'Visible left frozen column should stay on top after combined scrolling'
    )

    await page.evaluate(() => {
      const topBar = document.querySelector('.compare-top-scrollbar')
      topBar.scrollLeft = 1100
      topBar.dispatchEvent(new Event('scroll', { bubbles: true }))
      const table = document.querySelector('.table-scroll')
      table.scrollTop = 1500
    })
    await page.waitForTimeout(200)
    const stress = await collectMetrics(page, 'stress')

    assert.equal(stress.table.scrollLeft, 1100, 'Stress horizontal scroll should sync')
    assert(stress.table.scrollTop >= 1500, 'Stress vertical scroll should sync')
    assert(
      stress.topElementAtStickyName?.className?.includes('sticky-name'),
      'Left frozen column should remain topmost during stress scroll'
    )

    await page.screenshot({ path: OUTPUT_FILE, fullPage: false })

    console.log('MRP compare page verification passed.')
    console.log(JSON.stringify({ before, afterHorizontal, afterVertical, stress, screenshot: OUTPUT_FILE }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
