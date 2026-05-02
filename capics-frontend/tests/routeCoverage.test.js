import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

function extractRouterPaths() {
  const routerFile = fs.readFileSync(path.resolve('src/router/index.js'), 'utf8')
  const childBlock = routerFile.match(/children:\s*\[([\s\S]*?)\n\s*\]\n\s*}/)?.[1] || ''
  const paths = []
  for (const match of childBlock.matchAll(/path:\s*'([^']*)'/g)) {
    paths.push(match[1] ? `/${match[1]}` : '/')
  }
  return paths
}

function extractSmokePaths() {
  const smokeFile = fs.readFileSync(path.resolve('tests/pageSmoke.verify.js'), 'utf8')
  return Array.from(smokeFile.matchAll(/\{\s*path:\s*'([^']+)'/g), (match) => match[1])
}

test('page smoke verification covers every authenticated application route', () => {
  const routerPaths = extractRouterPaths()
  const smokePaths = extractSmokePaths()
  const missing = routerPaths.filter((routePath) => !smokePaths.includes(routePath))

  assert.deepEqual(missing, [])
})
