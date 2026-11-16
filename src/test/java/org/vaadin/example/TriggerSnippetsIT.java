package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Checks the views of this reproducer in a browser while the application runs
 * in production mode with {@code vaadin.csp=strict}, so Vaadin sends a
 * Content-Security-Policy without {@code unsafe-eval}. Run with
 * {@code mvn -Pit verify}.
 * <p>
 * After each test, the page must have reported no Content Security Policy
 * violation and no console error.
 */
class TriggerSnippetsIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        // Uses the installed Chrome, so no browser is downloaded
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setChannel("chrome"));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void newContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setAcceptDownloads(true));
        context.grantPermissions(
                List.of("clipboard-read", "clipboard-write"),
                new BrowserContext.GrantPermissionsOptions()
                        .setOrigin(BASE_URL));
        page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
            }
        });
        page.onPageError(error -> consoleErrors.add("page error: " + error));
        // Runs before the page and outside its policy. Chrome on Linux has no
        // navigator.share, so S1 records what the declared snippet passes.
        page.addInitScript("""
                window.__cspViolations = [];
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI + ' ' + e.sourceFile + ':' + e.lineNumber));
                if (!navigator.share) {
                    navigator.share = (data) => {
                        window.__shared = data;
                        return Promise.resolve();
                    };
                }
                """);
    }

    @AfterEach
    void noCspViolationsOrConsoleErrors() {
        try {
            Object violations = page.evaluate("() => window.__cspViolations");
            assertEquals(List.of(), violations, "CSP violations");
            assertEquals(List.of(), consoleErrors, "console errors");
        } finally {
            context.close();
        }
    }

    private void open(String route) {
        page.navigate(BASE_URL + "/" + route);
        assertThat(page.locator("#" + route)).isAttached();
    }

    @Test
    void cspHeaderIsStrict() {
        String policy = page.navigate(BASE_URL + "/trigger-snippets")
                .headerValue("content-security-policy");
        assertThat(page.locator("#trigger-snippets")).isAttached();
        assertEquals(true, policy != null && !policy.contains("unsafe-eval"),
                "policy without unsafe-eval: " + policy);
    }

    @Test
    void w1_writeLiteral_r1_readItBack() {
        open("trigger-snippets");
        page.locator("#w1").click();
        assertThat(page.locator("#w1-result"))
                .hasText("copied=" + TriggerSnippetsView.COPY_TEXT);
        page.locator("#r1").click();
        assertThat(page.locator("#r1-result"))
                .hasText("read=" + TriggerSnippetsView.COPY_TEXT);
    }

    @Test
    void w2_writeInputValue() {
        open("trigger-snippets");
        page.locator("#w2-source").fill("typed value");
        page.locator("#w2").click();
        assertThat(page.locator("#w2-result")).hasText("copied=typed value");
        assertEquals("typed value",
                page.evaluate("() => navigator.clipboard.readText()"));
    }

    @Test
    void d1_downloadUrlWithFileName() throws Exception {
        open("trigger-snippets");
        Download download = page.waitForDownload(
                () -> page.locator("#d1").click());
        assertEquals(TriggerSnippetsView.DOWNLOAD_NAME,
                download.suggestedFilename());
        assertEquals("sample file for the download and new tab cases\n",
                Files.readString(download.path(), StandardCharsets.UTF_8));
    }

    @Test
    void d2_downloadFromHandler() throws Exception {
        open("trigger-snippets");
        Download download = page.waitForDownload(
                () -> page.locator("#d2").click());
        assertEquals("handler.txt", download.suggestedFilename());
        assertEquals(TriggerSnippetsView.HANDLER_BODY,
                Files.readString(download.path(), StandardCharsets.UTF_8));
    }

    @Test
    void n1_openUrlInNewTab() {
        open("trigger-snippets");
        Page popup = page.waitForPopup(() -> page.locator("#n1").click());
        popup.waitForLoadState();
        assertEquals(BASE_URL + "/sample.txt", popup.url());
    }

    @Test
    void n2_javascriptUrlIsNotOpened() {
        open("trigger-snippets");
        List<Page> popups = new ArrayList<>();
        context.onPage(popups::add);
        page.locator("#n2").click();
        // Wait for a round trip, so a popup would have been opened by now
        page.locator("#t1").click();
        assertThat(page.locator("#t1-result")).hasText("fired");
        assertEquals(List.of(), popups);
    }

    @Test
    void f1_requestFullscreen() {
        open("trigger-snippets");
        page.locator("#f1").click();
        assertThat(page.locator("#f1-result")).hasText("entered");
        // The panel is moved into a wrapper, which is what goes fullscreen
        assertEquals(true, page.evaluate("() => document.fullscreenElement"
                + "?.contains(document.getElementById('f1-panel')) === true"));
    }

    @Test
    void s1_shareTitleAndUrl() {
        open("trigger-snippets");
        page.locator("#s1").click();
        assertThat(page.locator("#s1-result")).hasText("shared");
        assertEquals(Map.of("title", "Title", "url", "/sample.txt"),
                page.evaluate("() => window.__shared"));
    }

    @Test
    void z1_sizeTrigger() {
        open("trigger-snippets");
        assertThat(page.locator("#z1-width-result")).hasText("120");
        assertThat(page.locator("#z1-height-result")).hasText("60");
        page.evaluate("() => document.getElementById('z1-panel')"
                + ".style.width = '200px'");
        assertThat(page.locator("#z1-width-result")).hasText("200");
    }

    @Test
    void t1_triggerAfter() {
        open("trigger-snippets");
        page.locator("#t1").click();
        assertThat(page.locator("#t1-result")).hasText("fired");
    }

    @Test
    void e1_domEventTriggerSetsSignal() {
        open("trigger-snippets");
        page.locator("#e1-source").pressSequentially("abc");
        assertThat(page.locator("#e1-result")).hasText("abc");
    }

    @Test
    void e2_doubleClickTriggerSetsProperty() {
        open("trigger-snippets");
        page.locator("#e2").dblclick();
        assertThat(page.locator("#e2-result")).hasText("double");
    }

    private void paste(String selector, String script) {
        page.locator(selector).focus();
        page.evaluate("(selector) => { const dt = new DataTransfer(); "
                + script + " document.querySelector(selector).dispatchEvent("
                + "new ClipboardEvent('paste', {clipboardData: dt, "
                + "bubbles: true, cancelable: true})); }", selector);
    }

    @Test
    void p1_pasteTextAndHtml() {
        open("paste-snippets");
        paste("#p1-target", "dt.setData('text/plain', 'plain');"
                + " dt.setData('text/html', '<b>bold</b>');");
        assertThat(page.locator("#p1-result"))
                .hasText("text=plain;html=<b>bold</b>");
    }

    @Test
    void p2_pasteInEditableFieldIsSkipped() {
        open("paste-snippets");
        paste("#p2-input", "dt.setData('text/plain', 'in input');");
        paste("#p2-target", "dt.setData('text/plain', 'in div');");
        assertThat(page.locator("#p2-result"))
                .hasText("target=p2-target;text=in div");
    }

    @Test
    void p3_pasteFileIsUploaded() {
        open("paste-snippets");
        paste("#p3-target", "dt.items.add(new File(['hello'], 'pasted.txt',"
                + " {type: 'text/plain'}));");
        assertThat(page.locator("#p3-result"))
                .hasText("files=1;last=pasted.txt");
    }

    @Test
    void i1_snippetInitializerAndCleanup() {
        open("initializer-snippets");
        page.locator("#i1-register").click();
        assertThat(page.locator("#i1-init")).hasText("1");
        assertThat(page.locator("#i1-cleanup")).hasText("0");
        page.locator("#i1-detach").click();
        assertThat(page.locator("#i1-cleanup")).hasText("1");
        page.locator("#i1-reattach").click();
        assertThat(page.locator("#i1-init")).hasText("2");
        page.locator("#i1-remove").click();
        assertThat(page.locator("#i1-cleanup")).hasText("2");
        assertNull(page.evaluate("() => document.getElementById('i1-target')"
                + ".dataset.x"));
    }

    @Test
    void refusedForms() {
        open("initializer-snippets");
        assertThat(page.locator("#i2")).hasAttribute("data-outcome",
                "IllegalArgumentException");
        assertThat(page.locator("#i3")).hasAttribute("data-outcome",
                "CspViolationException");
        assertThat(page.locator("#i4")).hasAttribute("data-outcome",
                "NullPointerException");
        assertThat(page.locator("#i5")).hasAttribute("data-outcome",
                "CspViolationException");
    }
}
