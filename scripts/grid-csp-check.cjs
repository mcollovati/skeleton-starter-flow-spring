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
  // Whether a cell with the text is rendered, optionally only in the grid with the given id
  const hasCell = async (text, gridId) => {
    try {
      await page.waitForFunction(
        ([t, id]) =>
          [...document.querySelectorAll(`${id ? '#' + id + ' > ' : ''}vaadin-grid-cell-content`)].some(
            (c) => c.innerText.trim() === t
          ),
        [text, gridId],
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

  // Replacing the details renderer unsets the previous one on the client. Details
  // that are already open stay empty, because Grid does not refresh their data for
  // the new renderer (existing bug, also without the CSP changes), so open another row.
  await page.locator("#replace-details").click();
  await page.waitForTimeout(500);
  await page.locator("#grid > vaadin-grid-cell-content", { hasText: /^Ada$/ }).click();
  check("replaced details renderer (unsetLitRenderer)", await hasCell("New details of Ada"));

  // Features grid: header and footer renderers, hidden column, tooltip
  const fg = 'features-grid';
  check('features grid renders', await hasCell('Person 000', fg));
  check('header component (setHeaderRenderer)', await hasCell('Name header', fg));
  check('footer text (setFooterRenderer)', await hasCell('Footer text', fg));
  check('footer component (setFooterRenderer)', await hasCell('Footer component', fg));
  const hiddenFlowId = await page.evaluate(
    () => [...document.querySelectorAll('#features-grid vaadin-grid-column')].find((c) => c.hidden)?._flowId
  );
  check('hidden column has _flowId (setColumnFlowId)', !!hiddenFlowId, String(hiddenFlowId));
  const tooltipGenerator = await page.evaluate(
    () => typeof document.querySelector('#features-grid > vaadin-tooltip[slot="tooltip"]')?.generator
  );
  check('tooltip generator (initTooltipGenerator)', tooltipGenerator === 'function', tooltipGenerator);

  // Sorting from the header, then from the server (setSorterDirections)
  const sorterDirection = () => page.evaluate(() => document.querySelector('#features-grid vaadin-grid-sorter')?.direction);
  const sorter = page.locator('#features-grid vaadin-grid-sorter').first();
  await sorter.click();
  await sorter.click();
  check('header sort click descending', await hasCell('Person 199', fg));
  await page.locator('#sort-asc').click();
  check('server sort ascending', (await hasCell('Person 000', fg)) && (await sorterDirection()) === 'asc', String(await sorterDirection()));
  await page.locator('#sort-desc').click();
  check('server sort descending', (await hasCell('Person 199', fg)) && (await sorterDirection()) === 'desc', String(await sorterDirection()));
  await page.locator('#sort-asc').click();
  await hasCell('Person 000', fg);

  // Editor (EditorJs.patchRenderer)
  await page.locator('#edit').click();
  let editorValue;
  try {
    await page.waitForFunction(() => document.querySelector('#name-editor')?.value === 'Person 000', null, { timeout: 5000 });
    editorValue = 'Person 000';
  } catch {
    editorValue = await page.evaluate(() => document.querySelector('#name-editor')?.value);
  }
  check('editor opens', editorValue === 'Person 000', String(editorValue));

  // Page size (resetConnector)
  await page.locator('#page-size').click();
  await page.waitForTimeout(500);
  const pageSize = await page.evaluate(() => document.querySelector('#features-grid').pageSize);
  check('page size (resetConnector)', pageSize === 20 && (await hasCell("Person 001", fg)), String(pageSize));

  // Scroll to end (scrollToEnd)
  await page.locator('#scroll-to-end').click();
  check('scroll to end', await hasCell('Person 199', fg));

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
