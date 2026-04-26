import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const BASE_URL = 'http://127.0.0.1:3000'
const OUTPUT_DIR = path.resolve('..', '.tmp', 'page-smoke')

const ROUTES = [
  { path: '/', name: 'Dashboard' },
  { path: '/products', name: 'Products' },
  { path: '/mrp', name: 'Mrp' },
  { path: '/mrp-compare', name: 'MrpCompare' },
  { path: '/routing', name: 'Routing' },
  { path: '/lines', name: 'Lines' },
  { path: '/product-line', name: 'ProductLine' },
  { path: '/capacity-assessment', name: 'CapacityAssessment' },
  { path: '/capacity-assessment-monthly', name: 'CapacityAssessmentMonthly' },
  { path: '/capacity-realtime', name: 'CapacityRealtime' },
  { path: '/capacity-realtime-monthly', name: 'CapacityRealtimeMonthly' },
  { path: '/users', name: 'Users', allowHomeRedirect: true },
  { path: '/ai-config', name: 'AiConfig', allowHomeRedirect: true },
  { path: '/fusion-workbench', name: 'FusionWorkbench' },
  { path: '/ct-line', name: 'CtLine' }
]

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.locator('input').nth(0).fill('admin')
  await page.locator('input').nth(1).fill('admin123')
  await page.locator('button').last().click()
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(1200)
}

function isIgnorableConsoleMessage(type, text) {
  const lowered = text.toLowerCase()
  if (type === 'warning') return true
  if (lowered.includes('favicon')) return true
  if (lowered.includes('sourcemap')) return true
  return false
}

function isIgnorableRequestFailure(request, errorText) {
  const url = request.url()
  const method = request.method()
  if (errorText?.includes('ERR_ABORTED') && method === 'GET' && url.includes('/api/')) {
    return true
  }
  return false
}

async function verifyRoute(page, route) {
  const consoleMessages = []
  const pageErrors = []
  const requestFailures = []

  const onConsole = (message) => {
    if (message.type() === 'error' && !isIgnorableConsoleMessage(message.type(), message.text())) {
      consoleMessages.push(message.text())
    }
  }
  const onPageError = (error) => {
    pageErrors.push(error.message)
  }
  const onRequestFailed = (request) => {
    const failure = request.failure()
    const errorText = failure?.errorText ?? 'unknown'
    if (!isIgnorableRequestFailure(request, errorText)) {
      requestFailures.push(`${request.method()} ${request.url()} :: ${errorText}`)
    }
  }

  page.on('console', onConsole)
  page.on('pageerror', onPageError)
  page.on('requestfailed', onRequestFailed)

  try {
    await page.goto(`${BASE_URL}${route.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
    await page.waitForTimeout(2000)

    const currentUrl = page.url()
    const currentPath = new URL(currentUrl).pathname
    assert(!currentUrl.endsWith('/login'), `${route.name} redirected to login`)

    if (route.allowHomeRedirect) {
      const isAllowed = currentPath === route.path
      const isBlockedToHome = currentPath === '/'
      assert(isAllowed || isBlockedToHome, `${route.name} unexpected redirect: ${currentPath}`)
    } else {
      assert.equal(currentPath, route.path, `${route.name} unexpected redirect: ${currentPath}`)
    }

    const mainVisible = await page.locator('main, .app-layout, .page-container, .content-area, body').first().isVisible()
    assert(mainVisible, `${route.name} main container is not visible`)

    const blockingError = page.locator('.error, .error-panel, .el-error, .alert-error, .result-error')
    const blockingErrorCount = await blockingError.count()

    const screenshotPath = path.join(OUTPUT_DIR, `${route.name}.png`)
    await page.screenshot({ path: screenshotPath, fullPage: false })

    return {
      route,
      ok: consoleMessages.length === 0 && pageErrors.length === 0 && requestFailures.length === 0 && blockingErrorCount === 0,
      currentUrl,
      currentPath,
      consoleMessages,
      pageErrors,
      requestFailures,
      blockingErrorCount,
      screenshotPath
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

    const results = []
    for (const route of ROUTES) {
      results.push(await verifyRoute(page, route))
    }

    const failed = results.filter((item) => !item.ok)
    console.log(JSON.stringify({ results, failedCount: failed.length }, null, 2))

    assert.equal(failed.length, 0, `Smoke test found ${failed.length} failed routes`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
