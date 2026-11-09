// Headless check: node check/check.mjs <base-url> <path-to-playwright-core> [chrome]
// Opens three variants and prints the counter at 8 s and 20 s.
const [base, pw, chrome] = process.argv.slice(2);
const { chromium } = await import(pw + '/index.mjs');
const browser = await chromium.launch({ executablePath: chrome, args: ['--no-sandbox'] });
for (const query of ['', '?nodrop', '?transport=WEBSOCKET_XHR']) {
  const page = await browser.newPage();
  page.on('console', (m) => { if (m.type() === 'warning') console.log('   console:', m.text().slice(0, 90)); });
  await page.goto(base + query);
  const read = () => page.evaluate(() => Number(document.getElementById('counter')?.textContent));
  await page.waitForTimeout(8000);
  const at8 = await read();
  await page.waitForTimeout(12000);
  const at20 = await read();
  const state = await page.evaluate(() => window.Vaadin?.connectionState?.state);
  console.log(`${query || '(default: WEBSOCKET, one message dropped)'}: counter at 8 s = ${at8}, at 20 s = ${at20}, state = ${state}`
    + (at20 > at8 + 10 ? '  -> keeps going' : '  -> STOPPED'));
  await page.close();
}
await browser.close();
