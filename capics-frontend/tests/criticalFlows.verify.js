import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const BASE_URL = 'http://127.0.0.1:3000'
const OUTPUT_DIR = path.resolve('..', '.tmp', 'critical-flows')

function normalizeText(value) {
  return (value || '').replace(/\s+/g, ' ').trim()
}

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.locator('input').nth(0).fill('admin')
  await page.locator('input').nth(1).fill('admin123')
  await page.locator('button').last().click()
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(1200)
}

async function gotoRoute(page, route) {
  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(1200)
  assert(!page.url().endsWith('/login'), `${route} redirected to login`)
}

async function screenshot(page, name) {
  const filePath = path.join(OUTPUT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: false })
  return filePath
}

async function selectByIndex(select, index = 1) {
  const options = await select.locator('option').evaluateAll((nodes) =>
    nodes.map((node) => ({ value: node.value, disabled: node.disabled }))
  )
  const usable = options.filter((item) => item.value && !item.disabled)
  assert(usable.length >= index, 'Expected selectable options')
  await select.selectOption(usable[index - 1].value)
}

async function getUsableOptionValues(select) {
  const options = await select.locator('option').evaluateAll((nodes) =>
    nodes.map((node) => ({ value: node.value, disabled: node.disabled }))
  )
  return options.filter((item) => item.value && !item.disabled).map((item) => item.value)
}

async function waitForUsableOptions(select, minCount = 1, timeoutMs = 15000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const usable = await getUsableOptionValues(select)
    if (usable.length >= minCount) {
      return usable
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error('Expected selectable options')
}

async function fillMrpFilters(page, baseSelector = 'select', count = 3) {
  const selects = page.locator(baseSelector)
  await waitForUsableOptions(selects.nth(0))
  await selectByIndex(selects.nth(0))
  await page.waitForTimeout(300)
  await waitForUsableOptions(selects.nth(1))
  await selectByIndex(selects.nth(1))
  await page.waitForTimeout(300)
  await waitForUsableOptions(selects.nth(2))
  await selectByIndex(selects.nth(2))
  await page.waitForTimeout(300)
  if (count >= 4) {
    await waitForUsableOptions(selects.nth(3))
    await selectByIndex(selects.nth(3))
    await page.waitForTimeout(300)
  }
}

async function verifyDashboard(page) {
  await gotoRoute(page, '/')
  await fillMrpFilters(page, 'select', 3)
  const snapshotOptions = await getUsableOptionValues(page.locator('select').nth(3))
  if (snapshotOptions.length > 0) {
    await page.locator('select').nth(3).selectOption(snapshotOptions[0])
    await page.waitForTimeout(300)
    await page.getByRole('button', { name: /加载数据|鍔犺浇鏁版嵁/ }).click()
    await page.waitForTimeout(2500)
    const tableRows = page.locator('.main-table tbody tr')
    assert((await tableRows.count()) > 0, 'Dashboard should render data rows when snapshot exists')
  } else {
    assert(await page.locator('select').nth(3).isVisible(), 'Dashboard snapshot selector should be visible')
  }
  const toggleButtons = page.locator('.toggle-btn')
  await toggleButtons.nth(1).click()
  await page.waitForTimeout(600)
  return screenshot(page, 'dashboard-critical')
}

async function verifyProducts(page) {
  await gotoRoute(page, '/products')
  await page.locator('.toolbar .form-input').first().fill('SMT')
  await page.waitForTimeout(500)
  await page.locator('.toolbar .btn.btn-primary').nth(1).click()
  await page.waitForSelector('.modal-overlay', { timeout: 5000 })
  await page.locator('.modal-close').first().click()
  await page.waitForTimeout(300)
  const tabButtons = page.locator('button').filter({ hasText: /family-lines|products|缂栫爜|浜у搧/ })
  if (await tabButtons.count()) {
    await tabButtons.last().click().catch(() => {})
    await page.waitForTimeout(400)
  }
  return screenshot(page, 'products-critical')
}

async function verifyMrp(page) {
  await gotoRoute(page, '/mrp')
  await fillMrpFilters(page, 'select', 3)
  await page.getByRole('button', { name: /查询|鏌ヨ/ }).first().click()
  await page.waitForTimeout(2500)
  assert((await page.locator('table tbody tr').count()) > 0, 'MRP plan table should have rows')
  const tabButtons = page.locator('button').filter({ hasText: /weekly|weekly-description|monthly|description|周|月/ })
  if (await tabButtons.count()) {
    await tabButtons.nth(Math.min(1, (await tabButtons.count()) - 1)).click().catch(() => {})
    await page.waitForTimeout(400)
  }
  return screenshot(page, 'mrp-critical')
}

async function verifyMrpCompare(page) {
  await gotoRoute(page, '/mrp-compare')
  const selects = page.locator('select')
  await selectByIndex(selects.nth(0))
  await page.waitForTimeout(250)
  await selectByIndex(selects.nth(1))
  await page.waitForTimeout(250)
  await selectByIndex(selects.nth(2))
  await page.waitForTimeout(250)
  await selectByIndex(selects.nth(3))
  await page.waitForTimeout(250)
  await selectByIndex(selects.nth(4), 2)
  await page.waitForTimeout(250)
  await selectByIndex(selects.nth(5))
  await page.waitForTimeout(250)
  await page.getByRole('button', { name: /开始对比/ }).click()
  await page.waitForTimeout(3500)
  assert(await page.locator('.compare-matrix-table').isVisible(), 'MRP compare matrix should be visible')
  return screenshot(page, 'mrp-compare-critical')
}

async function verifyRouting(page) {
  await gotoRoute(page, '/routing')
  const firstGroup = page.locator('tr.group-header').first()
  assert(await firstGroup.isVisible(), 'Routing first group should be visible')
  await firstGroup.click()
  await page.waitForTimeout(300)
  assert((await page.locator('tr.child-row').count()) > 0, 'Routing child rows should expand')
  await page.locator('input.form-input').first().fill('')
  return screenshot(page, 'routing-critical')
}

async function verifyLines(page) {
  await gotoRoute(page, '/lines')
  await page.getByRole('button', { name: /添加产线/ }).click()
  await page.waitForSelector('.modal-overlay', { timeout: 5000 })
  await page.locator('.modal-close').first().click()
  await page.waitForTimeout(300)
  const editButton = page.getByRole('button', { name: /编辑/ }).first()
  if (await editButton.count()) {
    await editButton.click()
    await page.waitForTimeout(400)
    await page.locator('.modal-close').first().click()
  }
  return screenshot(page, 'lines-critical')
}

async function verifyProductLine(page) {
  await gotoRoute(page, '/product-line')
  await fillMrpFilters(page, 'select', 3)
  const snapshotOptions = await getUsableOptionValues(page.locator('select').nth(3))
  if (snapshotOptions.length > 0) {
    await page.locator('select').nth(3).selectOption(snapshotOptions[0])
    await page.waitForTimeout(300)
    await page.getByRole('button', { name: /加载数据/ }).click()
    await page.waitForTimeout(3500)
    assert(await page.locator('.heatmap-table').isVisible(), 'Product line heatmap should be visible')
    const monthTab = page.locator('.tab-chip').nth(1)
    await monthTab.click()
    await page.waitForTimeout(500)
  } else {
    assert(await page.locator('select').nth(3).isVisible(), 'Product line saved result selector should be visible')
  }
  return screenshot(page, 'product-line-critical')
}

async function verifyStaticCapacity(page, route, name) {
  await gotoRoute(page, route)
  await fillMrpFilters(page, 'select', 3)
  const snapshotOptions = await getUsableOptionValues(page.locator('select').nth(3))
  if (snapshotOptions.length > 0) {
    await page.locator('select').nth(3).selectOption(snapshotOptions[0])
    await page.waitForTimeout(300)
    await page.getByRole('button', { name: /加载结果|鍔犺浇缁撴灉/ }).click()
  } else {
    await page.getByRole('button', { name: /开始计算|寮€濮嬭绠?/ }).click()
  }
  await page.waitForTimeout(3500)
  assert(await page.locator('.main-table').isVisible(), `${name} table should be visible`)
  return screenshot(page, `${name}-critical`)
}

async function verifyRealtimeCapacity(page, route, name) {
  await gotoRoute(page, route)
  await fillMrpFilters(page, 'select', 3)
  await page.getByRole('button', { name: /加载数据|鍔犺浇鏁版嵁/ }).click()
  await page.waitForTimeout(3500)
  assert(await page.locator('.main-table').isVisible(), `${name} main table should be visible`)
  const showSummaryBtn = page.getByRole('button', { name: /显示汇总表|鏄剧ず姹囨€昏〃/ })
  if (await showSummaryBtn.count()) {
    await showSummaryBtn.click().catch(() => {})
    await page.waitForTimeout(500)
  }
  return screenshot(page, `${name}-critical`)
}

async function verifyUsers(page) {
  await gotoRoute(page, '/users')
  await page.getByRole('button', { name: /添加用户|娣诲姞/ }).click()
  await page.waitForSelector('.modal-overlay', { timeout: 5000 })
  await page.locator('.modal-close').first().click()
  await page.waitForTimeout(300)
  const passwordBtn = page.getByRole('button', { name: /改密码|鏀瑰瘑鐮?/ }).first()
  if (await passwordBtn.count()) {
    await passwordBtn.click()
    await page.waitForTimeout(400)
    await page.locator('.modal-close').first().click()
  }
  return screenshot(page, 'users-critical')
}

async function verifyAiConfig(page) {
  await gotoRoute(page, '/ai-config')
  const toggleBtn = page.getByRole('button', { name: /显示|隐藏|鏄剧ず|闅愯棌/ })
  if (await toggleBtn.count()) {
    await toggleBtn.first().click()
    await page.waitForTimeout(250)
  }
  await page.getByRole('button', { name: /API.*测试|API.*娴嬭瘯/ }).click()
  await page.waitForTimeout(2500)
  assert(await page.locator('.test-result').isVisible(), 'AI config test result should be visible')
  return screenshot(page, 'ai-config-critical')
}

async function verifyFusionWorkbench(page) {
  await gotoRoute(page, '/fusion-workbench')
  const tabs = page.locator('.tabs .btn')
  await tabs.nth(1).click()
  await page.waitForTimeout(300)
  assert(await page.locator('.card table').isVisible(), 'Fusion workbench table should be visible')
  await tabs.nth(0).click()
  await page.waitForTimeout(300)
  return screenshot(page, 'fusion-workbench-critical')
}

async function verifyCtLine(page) {
  await gotoRoute(page, '/ct-line')
  await page.getByRole('button', { name: /新增一条/ }).click()
  await page.waitForTimeout(300)
  await page.getByRole('button', { name: /取消/ }).first().click()
  await page.waitForTimeout(300)
  const editBtn = page.getByRole('button', { name: /编辑/ }).first()
  if (await editBtn.count()) {
    await editBtn.click()
    await page.waitForTimeout(300)
    await page.getByRole('button', { name: /取消/ }).first().click()
  }
  return screenshot(page, 'ct-line-critical')
}

async function runScenario(page, name, fn) {
  const consoleErrors = []
  const pageErrors = []
  const requestFailures = []

  const onConsole = (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text())
    }
  }
  const onPageError = (err) => pageErrors.push(err.message)
  const onRequestFailed = (req) => requestFailures.push(`${req.method()} ${req.url()} :: ${req.failure()?.errorText ?? 'unknown'}`)

  page.on('console', onConsole)
  page.on('pageerror', onPageError)
  page.on('requestfailed', onRequestFailed)

  try {
    const shot = await fn(page)
    return { name, ok: true, screenshot: shot, consoleErrors, pageErrors, requestFailures }
  } catch (error) {
    return {
      name,
      ok: false,
      error: error.message,
      consoleErrors,
      pageErrors,
      requestFailures,
      screenshot: await screenshot(page, `${name}-failed`).catch(() => null)
    }
  } finally {
    page.off('console', onConsole)
    page.off('pageerror', onPageError)
    page.off('requestfailed', onRequestFailed)
  }
}

async function main() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })

  try {
    await login(page)

    const scenarios = [
      ['dashboard', verifyDashboard],
      ['products', verifyProducts],
      ['mrp', verifyMrp],
      ['mrp-compare', verifyMrpCompare],
      ['routing', verifyRouting],
      ['lines', verifyLines],
      ['product-line', verifyProductLine],
      ['capacity-assessment-week', (p) => verifyStaticCapacity(p, '/capacity-assessment', 'capacity-assessment-week')],
      ['capacity-assessment-month', (p) => verifyStaticCapacity(p, '/capacity-assessment-monthly', 'capacity-assessment-month')],
      ['capacity-realtime-week', (p) => verifyRealtimeCapacity(p, '/capacity-realtime', 'capacity-realtime-week')],
      ['capacity-realtime-month', (p) => verifyRealtimeCapacity(p, '/capacity-realtime-monthly', 'capacity-realtime-month')],
      ['users', verifyUsers],
      ['ai-config', verifyAiConfig],
      ['fusion-workbench', verifyFusionWorkbench],
      ['ct-line', verifyCtLine]
    ]

    const results = []
    for (const [name, fn] of scenarios) {
      results.push(await runScenario(page, name, fn))
    }

    const failed = results.filter((item) => !item.ok)
    console.log(JSON.stringify({ results, failedCount: failed.length }, null, 2))
    assert.equal(failed.length, 0, `Critical flow verification found ${failed.length} failed scenarios`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
