import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const BASE_URL = 'http://127.0.0.1:3000'
const OUTPUT_DIR = path.resolve('..', '.tmp', 'critical-flows')

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.locator('input').nth(0).fill('admin')
  await page.locator('input').nth(1).fill('admin123')
  await page.locator('button').last().click()
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(1200)
}

async function gotoRoute(page, route, options = {}) {
  const { allowHomeRedirect = false } = options
  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(1200)

  const currentUrl = page.url()
  assert(!currentUrl.endsWith('/login'), `${route} redirected to login`)

  const pathname = new URL(currentUrl).pathname
  if (allowHomeRedirect) {
    if (pathname === '/') {
      return { redirectedHome: true }
    }
    assert.equal(pathname, route, `${route} redirected to unexpected path: ${pathname}`)
    return { redirectedHome: false }
  }

  assert.equal(pathname, route, `${route} redirected to unexpected path: ${pathname}`)
  return { redirectedHome: false }
}

async function screenshot(page, name) {
  const filePath = path.join(OUTPUT_DIR, `${name}.png`)
  await page.screenshot({ path: filePath, fullPage: false })
  return filePath
}

async function clickIfExists(locator) {
  if (await locator.count()) {
    await locator.first().click()
    return true
  }
  return false
}

async function selectFirstUsable(select) {
  const values = await select.locator('option').evaluateAll((nodes) =>
    nodes
      .filter((node) => node.value && !node.disabled)
      .map((node) => node.value)
  )
  if (!values.length) return false
  await select.selectOption(values[0])
  return true
}

async function verifyDashboard(page) {
  await gotoRoute(page, '/')
  assert(await page.locator('body').isVisible(), 'Dashboard should render')
  return screenshot(page, 'dashboard-critical')
}

async function verifyProducts(page) {
  await gotoRoute(page, '/products')
  await page.locator('.toolbar .form-input').first().fill('SMT').catch(() => {})
  await page.waitForTimeout(300)

  const primaryButtons = page.locator('.toolbar .btn.btn-primary')
  if ((await primaryButtons.count()) >= 2) {
    await primaryButtons.nth(1).click()
    await page.waitForSelector('.modal-overlay', { timeout: 5000 })
    await page.locator('.modal-close').first().click()
  }

  assert(await page.locator('table, .table-wrapper').first().isVisible(), 'Products table should be visible')
  return screenshot(page, 'products-critical')
}

async function verifyMrp(page) {
  await gotoRoute(page, '/mrp')
  assert(await page.locator('body').isVisible(), 'MRP should render')
  return screenshot(page, 'mrp-critical')
}

async function verifyMrpCompare(page) {
  await gotoRoute(page, '/mrp-compare')
  const selects = page.locator('select')
  const count = await selects.count()
  for (let i = 0; i < count; i += 1) {
    await selectFirstUsable(selects.nth(i))
    await page.waitForTimeout(200)
  }
  const startBtn = page.getByRole('button', { name: /开始对比|寮€濮嬪姣?/ })
  if (await startBtn.count()) {
    await startBtn.first().click()
    await page.waitForTimeout(2000)
  }
  assert(await page.locator('body').isVisible(), 'MRP compare should render')
  return screenshot(page, 'mrp-compare-critical')
}

async function verifyRouting(page) {
  await gotoRoute(page, '/routing')
  const firstGroup = page.locator('tr.group-header').first()
  if (await firstGroup.count()) {
    await firstGroup.click()
    await page.waitForTimeout(300)
  }
  assert(await page.locator('table').isVisible(), 'Routing table should be visible')
  return screenshot(page, 'routing-critical')
}

async function verifyLines(page) {
  await gotoRoute(page, '/lines')
  const addBtn = page.getByRole('button', { name: /添加产线|娣诲姞浜х嚎/ })
  if (await addBtn.count()) {
    await addBtn.click()
    await page.waitForSelector('.modal-overlay', { timeout: 5000 })
    await page.locator('.modal-close').first().click()
  }
  assert(await page.locator('table').isVisible(), 'Lines table should be visible')
  return screenshot(page, 'lines-critical')
}

async function verifyProductLine(page) {
  await gotoRoute(page, '/product-line')
  assert(await page.locator('body').isVisible(), 'Product line should render')
  return screenshot(page, 'product-line-critical')
}

async function verifyStaticCapacity(page, route, name) {
  await gotoRoute(page, route)
  assert(await page.locator('body').isVisible(), `${name} should render`)
  return screenshot(page, `${name}-critical`)
}

async function verifyRealtimeCapacity(page, route, name) {
  await gotoRoute(page, route)
  assert(await page.locator('body').isVisible(), `${name} should render`)
  return screenshot(page, `${name}-critical`)
}

async function verifyUsers(page) {
  const state = await gotoRoute(page, '/users', { allowHomeRedirect: true })
  if (state.redirectedHome) {
    return screenshot(page, 'users-critical')
  }

  const addBtn = page.getByRole('button', { name: /添加用户|娣诲姞鐢ㄦ埛|濞ｈ濮?/ })
  if (await addBtn.count()) {
    await addBtn.click()
    await page.waitForSelector('.modal-overlay', { timeout: 5000 })
    await page.locator('.modal-close').first().click()
  }

  assert(await page.locator('table').isVisible(), 'Users table should be visible')
  return screenshot(page, 'users-critical')
}

async function verifyAiConfig(page) {
  const state = await gotoRoute(page, '/ai-config', { allowHomeRedirect: true })
  if (state.redirectedHome) {
    return screenshot(page, 'ai-config-critical')
  }

  const testBtn = page.getByRole('button', { name: /API.*测试|API.*娴嬭瘯|API.*濞村鐦?/ })
  if (await testBtn.count()) {
    await testBtn.first().click()
    await page.waitForTimeout(1500)
  }

  assert(await page.locator('body').isVisible(), 'AI config should render')
  return screenshot(page, 'ai-config-critical')
}

async function verifyFusionWorkbench(page) {
  await gotoRoute(page, '/fusion-workbench')
  assert(await page.locator('body').isVisible(), 'Fusion workbench should render')
  return screenshot(page, 'fusion-workbench-critical')
}

async function verifyCtLine(page) {
  await gotoRoute(page, '/ct-line')
  const addBtn = page.getByRole('button', { name: /新增一条|鏂板涓€鏉?/ })
  if (await addBtn.count()) {
    await addBtn.click()
    await page.waitForTimeout(300)
    await clickIfExists(page.getByRole('button', { name: /取消|鍙栨秷/ }))
  }
  assert(await page.locator('table').isVisible(), 'Ct-line table should be visible')
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
