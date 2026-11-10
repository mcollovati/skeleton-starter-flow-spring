// Checks /grid-csp under vaadin.csp=strict in a real browser.
const { chromium } = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');

const results = [];
const check = (name, ok, detail = '') => results.push(`${ok ? 'PASS' : 'FAIL'} ${name}${detail ? ' - ' + detail : ''}`);

(async () => {
  const browser = await chromium.launch({ executablePath: '/usr/bin/google-chrome', headless: true });
  const page = await browser.newPage();
  const consoleErrors = [];
  page.on('console', (m) => m.type() === 'error' && consoleErrors.push(m.text()));
  page.on('pageerror', (e) => consoleErrors.push('pageerror: ' + e.message));
  await page.addInitScript(() => {
    window.__cspViolations = [];
    document.addEventListener('securitypolicyviolation', (e) =>
      window.__cspViolations.push(`${e.violatedDirective} ${e.blockedURI} ${e.sourceFile}:${e.lineNumber}`)
    );
  });

  const response = await page.goto('http://localhost:8080/grid-csp');
  const csp = response.headers()['content-security-policy'] || '';
  check('strict CSP header without unsafe-eval', csp.includes("'strict-dynamic'") && !csp.includes('unsafe-eval'));

  const cells = () => page.locator('vaadin-grid-cell-content').allInnerTexts();
  const hasCell = async (text) => {
    try {
      await page.waitForFunction(
        (t) => [...document.querySelectorAll('vaadin-grid-cell-content')].some((c) => c.innerText.trim() === t),
        text,
        { timeout: 5000 }
      );
      return true;
    } catch {
      return false;
    }
  };
  const log = () => page.locator('#log').innerText();

  for (const [name, text] of [
    ['path column', 'Ada'],
    ['component column', 'C:Ada'],
    ['TextRenderer', 'T:Ada'],
    ['NumberRenderer', '36 years'],
    ['LocalDateRenderer', '1990-01-10'],
    ['NativeButtonRenderer', 'Click'],
    ['app template with capture', 'Status: active'],
    ['hierarchy column', 'Parent 1'],
    ['component hierarchy column', '[Folder]'],
  ]) {
    check(name, await hasCell(text));
  }

  // withFunction from a built-in template
  await page.locator('vaadin-grid-cell-content button', { hasText: 'Click' }).first().click();
  await page.waitForTimeout(500);
  check('button click reaches server', (await log()) === 'button Ada', await log());

  // withFunction from an application template (functions.select)
  await page.locator('vaadin-grid-cell-content .status').first().click();
  await page.waitForTimeout(500);
  check('app template function reaches server', (await log()) === 'select Ada', await log());

  // Item details, opened by clicking a row
  await page.locator('vaadin-grid-cell-content', { hasText: /^Linus$/ }).click();
  check('item details (ComponentRenderer)', await hasCell('Details of Linus'));

  // Hierarchy column expands through functions.onClick
  await page.locator('vaadin-grid-tree-toggle', { hasText: 'Parent 1' }).click();
  check('hierarchy column expands', await hasCell('Parent 1 / Child 1'));

  // Component hierarchy column expands through the canceled click
  await page.locator('vaadin-grid-tree-toggle', { hasText: '[Folder]' }).click();
  check('component hierarchy column expands', await hasCell('[File]'));

  // A string template is refused on the server
  await page.locator('#string-template').click();
  await page.waitForTimeout(500);
  check('string template refused', (await log()) === 'refused: CspViolationException', await log());

  const violations = await page.evaluate(() => window.__cspViolations);
  check('no CSP violations in the browser', violations.length === 0, violations.join(' | '));
  check('no console errors', consoleErrors.length === 0, consoleErrors.join(' | '));

  console.log(results.join('\n'));
  await page.screenshot({ path: 'target/grid-csp.png', fullPage: true });
  await browser.close();
})().catch((e) => {
  console.log(results.join('\n'));
  console.error(e);
  process.exit(1);
});
