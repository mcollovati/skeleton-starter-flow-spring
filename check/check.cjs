// Drives every case of the #26699 reproducer and prints the results as JSON.
// Usage: PLAYWRIGHT_CORE=<path to playwright-core> [CHROME=<chrome binary>] node check/check.cjs [baseUrl]
const { chromium } = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const base = process.argv[2] || 'http://localhost:8080';

(async () => {
  const browser = await chromium.launch({
    executablePath: process.env.CHROME || undefined,
    args: ['--no-sandbox'],
  });
  const results = {};
  const consoleErrors = [];
  const ctx = await browser.newContext({ viewport: { width: 1000, height: 700 } });
  const page = await ctx.newPage();
  page.on('console', (m) => { if (m.type() === 'error' || m.type() === 'warning') consoleErrors.push(`[${m.type()}] ${page.url()} ${m.text()}`); });
  page.on('pageerror', (e) => consoleErrors.push(`[pageerror] ${page.url()} ${e.message}`));

  async function login() {
    await page.goto(base + '/login');
    await page.waitForSelector('vaadin-login-form');
    await page.fill('input[name="username"]', 'user');
    await page.fill('input[name="password"]', 'user');
    await page.click('vaadin-button[slot="submit"], vaadin-login-form vaadin-button');
    await page.waitForURL((u) => !u.pathname.endsWith('/login'));
  }
  await login();

  // 1. resize
  await page.goto(base + '/resize');
  await page.waitForSelector('#size');
  await page.waitForTimeout(500);
  await page.setViewportSize({ width: 800, height: 600 });
  await page.waitForFunction(() => document.getElementById('size').textContent !== 'not resized yet', null, { timeout: 5000 }).catch(() => {});
  const expected = await page.evaluate(() => `${document.documentElement.clientWidth}x${document.documentElement.clientHeight}`);
  results.resize = { shown: await page.textContent('#size'), expected };

  // 2. flashClass
  await page.goto(base + '/flash');
  await page.waitForSelector('#flash');
  await page.click('#flash');
  const added = await page.waitForFunction(() => document.getElementById('box').classList.contains('flash'), null, { timeout: 3000 }).then(() => true, () => false);
  const removed = await page.waitForFunction(() => !document.getElementById('box').classList.contains('flash'), null, { timeout: 3000 }).then(() => true, () => false);
  results.flash = { classAdded: added, classRemovedAfterAnimation: removed };

  // 4. navigation
  await page.goto(base + '/navigation');
  await page.waitForSelector('#navigate');
  await page.click('#navigate');
  await page.waitForURL('**/target', { timeout: 5000 }).catch(() => {});
  const afterNavigate = new URL(page.url()).pathname;
  await page.goBack();
  await page.waitForTimeout(1000);
  const afterBack = new URL(page.url()).pathname;
  // Start from a distinct page, so Back after the forward shows which entry
  // the forward left in the history
  await page.goto(base + '/');
  await page.waitForTimeout(500);
  await page.goto(base + '/navigation');
  await page.waitForSelector('#navigate-forward');
  const histBefore = await page.evaluate(() => history.length);
  await page.click('#navigate-forward');
  await page.waitForTimeout(1500);
  const afterForward = new URL(page.url()).pathname;
  const histAfter = await page.evaluate(() => history.length);
  await page.goBack();
  await page.waitForTimeout(1000);
  const afterForwardBack = new URL(page.url()).pathname;
  await page.goto(base + '/navigation');
  await page.waitForSelector('#missing-link');
  const errorsBeforeMissing = consoleErrors.length;
  await page.click('#missing-link');
  await page.waitForTimeout(1500);
  results.navigation = {
    afterNavigate, afterBack, afterForward, historyGrowthOnForward: histAfter - histBefore, afterForwardBack,
    missingRouteUrl: new URL(page.url()).pathname,
    missingRouteBody: (await page.textContent('body')).replace(/\s+/g, ' ').slice(0, 120),
    consoleErrorsOnMissingRoute: consoleErrors.slice(errorsBeforeMissing),
  };

  // 3a. logout with WEBSOCKET push
  await page.goto(base + '/logout');
  await page.waitForSelector('#logout');
  await page.waitForTimeout(1000);
  const t1 = await page.textContent('#transport');
  await page.click('#logout');
  await page.waitForURL('**/login', { timeout: 8000 }).catch(() => {});
  results.logoutWebsocket = { transport: t1, endedOn: new URL(page.url()).pathname };

  // 3b. logout from a background thread with long polling
  await login();
  await page.goto(base + '/logout?transport=long-polling');
  await page.waitForSelector('#logout-background');
  await page.waitForTimeout(1500);
  const t2 = await page.textContent('#transport');
  await page.click('#logout-background');
  await page.waitForURL('**/login', { timeout: 8000 }).catch(() => {});
  results.logoutBackground = { transport: t2, endedOn: new URL(page.url()).pathname };

  results.allConsoleErrors = consoleErrors;
  console.log(JSON.stringify(results, null, 2));
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
