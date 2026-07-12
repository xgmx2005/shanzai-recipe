const { chromium } = require('playwright')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const outputDir = path.join(root, 'docs', 'ui', 'screenshots')
const baseUrl = 'http://localhost:5173'
const apiUrl = 'http://localhost:8081/api/auth/login'

async function loginSession() {
  const response = await fetch(apiUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'user1', password: '123456' }),
  })
  const payload = await response.json()
  if (!payload.success) {
    throw new Error(payload.message || '登录失败')
  }
  const data = payload.data
  return {
    token: data.token,
    user: {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatarTheme: data.avatarTheme ?? 'leaf',
      avatarUrl: data.avatarUrl ?? '',
      role: data.role,
    },
  }
}

async function createBrowser() {
  for (const channel of ['chrome', 'msedge']) {
    try {
      return await chromium.launch({ channel, headless: true })
    } catch {}
  }
  return chromium.launch({ headless: true })
}

async function shot(page, route, name, wait = 1000) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' })
  if (route === '/user/recommend') {
    const restart = page.getByRole('button', { name: '重新开始' })
    if (await restart.isVisible().catch(() => false)) {
      await restart.click()
      await page.waitForLoadState('networkidle')
    }
  }
  await page.waitForTimeout(wait)
  await page.screenshot({
    path: path.join(outputDir, name),
    fullPage: false,
    animations: 'disabled',
  })
}

async function main() {
  const session = await loginSession()
  const browser = await createBrowser()
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  })

  const loginPage = await context.newPage()
  await shot(loginPage, '/login', '01-login.png')
  await loginPage.close()

  await context.addInitScript((auth) => {
    window.localStorage.setItem('shanzai-auth', JSON.stringify(auth))
  }, session)

  const page = await context.newPage()
  await shot(page, '/user/home', '02-user-home.png')
  await shot(page, '/user/profile', '03-profile.png')
  await shot(page, '/user/recommend', '04-recommend.png')
  await shot(page, '/user/shopping-lists', '05-shopping-list.png')
  await browser.close()
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
