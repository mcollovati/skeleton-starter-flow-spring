package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Checks, case by case, whether each feature of the ACME add-on works in the
 * application that runs with {@code vaadin.csp=strict}.
 * <p>
 * Every test asserts that its case WORKS: no {@code CspViolationException} in
 * Java, the effect the add-on promises is visible in the browser, and the page
 * reports no Content Security Policy violation. So the test report reads as
 * a list of what works (passed) and what does not (failed, with the reason in
 * the message). Failures are expected: they are the cases that still need a
 * fallback. Case 5 is the exception: a fallback does not support it, so its
 * test asserts that Flow refuses it and says so.
 * <p>
 * Run with {@code mvn -Pit verify}, which starts the application in
 * production mode (Flow sends the CSP header only there).
 */
class AddonCspIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();
    private final List<String> consoleLogs = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        // Uses the installed Chrome, so no browser is downloaded
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setChannel("chrome"));
        PlaywrightAssertions.setDefaultAssertionTimeout(5000);
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
            } else if ("log".equals(message.type())) {
                consoleLogs.add(message.text());
            }
        });
        page.onPageError(error -> consoleErrors.add("page error: " + error));
        page.addInitScript("""
                window.__cspViolations = [];
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI + ' ' + e.sourceFile + ':' + e.lineNumber));
                """);
    }

    @AfterEach
    void closePage() {
        try {
            Object violations = page.evaluate("() => window.__cspViolations");
            assertEquals(List.of(), violations,
                    "the browser reported CSP violations");
            assertEquals(List.of(), consoleErrors,
                    "the browser logged errors");
        } finally {
            context.close();
        }
    }

    @Test
    void header_strictPolicyIsSent() {
        String policy = page.navigate(BASE_URL + "/")
                .headerValue("content-security-policy");
        assertTrue(policy != null && policy.contains("'nonce-")
                && !policy.contains("unsafe-eval"),
                "expected a strict policy, got: " + policy
                        + ". Is the application in production mode?");
    }

    @Test
    @DisplayName("1. Element.executeJs in onAttach (AutoFocus)")
    void case01_autoFocus() {
        runCase(1);
        assertThat(page.locator("#area-1 input")).isFocused();
    }

    @Test
    @DisplayName("1b. AutoFocus in a view that does not catch anything (/plain)")
    void case01b_plainViewOpens() {
        page.navigate(BASE_URL + "/plain");
        Locator text = page.getByText("If you see this text, the view opened.");
        try {
            text.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            fail("the view did not open: "
                    + page.locator("body").innerText().strip());
        }
    }

    @Test
    @DisplayName("2. Element.executeJs, no result (ScrollPanel.scrollToBottom)")
    void case02_scrollToBottom() {
        runCase(2);
        waitFor("() => document.querySelector('#area-2 > div').scrollTop > 0",
                "the panel did not scroll");
    }

    @Test
    @DisplayName("3. Element.executeJs with result (ScrollPanel.getScrollTop)")
    void case03_scrollTopResult() {
        runCase(3);
        assertThat(page.locator("#area-3 > span"))
                .hasText(Pattern.compile("scrollTop=\\d"));
    }

    @Test
    @DisplayName("4. Page.executeJs with parameters (PageTitle)")
    void case04_pageTitle() {
        runCase(4);
        assertThat(page).hasTitle("Inbox (3)");
    }

    /**
     * The one case that asserts a refusal: a string built from values that
     * change can not have a fallback, so Flow refuses it and says why.
     */
    @Test
    @DisplayName("5. Concatenated string (Highlighter): refused, not supported")
    void case05_highlighter_refusedAsNotSupported() {
        page.navigate(BASE_URL + "/");
        page.locator("#run-5").click();
        assertThat(page.locator("#status-5"))
                .hasText(Pattern.compile("^REFUSED in Java"));
        assertThat(page.locator("#message-5")).containsText(
                "@JsStringFallback does not support strings like this");
    }

    @Test
    @DisplayName("6. Same text, varying parameter count (ConsoleLogger)")
    void case06_consoleLogger() {
        runCase(6);
        try {
            page.waitForCondition(() -> consoleLogs.stream()
                    .filter(line -> line.startsWith("[acme]")).count() == 2,
                    new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (PlaywrightException e) {
            fail("expected two [acme] console lines, got " + consoleLogs);
        }
        assertEquals(List.of("[acme] one", "[acme] one 2 true"), consoleLogs
                .stream().filter(line -> line.startsWith("[acme]")).toList());
    }

    @Test
    @DisplayName("7. setFilter with an expression (EscapeListener)")
    void case07_escapeFilter() {
        runCase(7);
        page.locator("#area-7 input").press("Escape");
        assertThat(page.locator("#area-7 span")).hasText("Escape pressed");
    }

    @Test
    @DisplayName("8. addEventData with an expression (CharCounter)")
    void case08_charCounter() {
        runCase(8);
        page.locator("#area-8 input").fill("  abc  ");
        assertThat(page.locator("#area-8 span")).hasText("trimmed length=3");
    }

    @Test
    @DisplayName("9. addJsInitializer(String) (ResizeWatcher)")
    void case09_resizeWatcher() {
        runCase(9);
        assertThat(page.locator("#area-9 > div"))
                .hasText(Pattern.compile("width: \\d+px"));
    }

    @Test
    @DisplayName("10. JsFunction.of (Tooltip)")
    void case10_tooltip() {
        runCase(10);
        assertThat(page.locator("#area-10 span")).hasAttribute("title",
                "from JsFunction.of");
    }

    @Test
    @DisplayName("11. Page.addDynamicImport(String) (LazyLoader)")
    void case11_lazyLoader() {
        runCase(11);
        waitFor("() => window.acmeLoaded === true",
                "the dynamic import did not run");
    }

    @Test
    @DisplayName("12. Inline onclick attribute (InlineHandler)")
    void case12_inlineHandler() {
        runCase(12);
        page.locator("#area-12 span").click();
        assertNoCspViolation();
        assertThat(page.locator("#area-12 span"))
                .hasText("inline onclick ran");
    }

    @Test
    @DisplayName("13. new Function in the add-on's web component (EvalWidget)")
    void case13_evalWidget() {
        runCase(13);
        assertNoCspViolation();
        assertThat(page.locator("#area-13 acme-eval-widget"))
                .hasText("eval result: 42");
    }

    @Test
    @DisplayName("14. Control: @DomEvent filter + @EventData expression (EnterField)")
    void case14_bundledEventAnnotations() {
        runCase(14);
        Locator input = page.locator("#area-14 input");
        input.fill("  abc  ");
        input.press("Enter");
        assertThat(page.locator("#area-14 span")).hasText("Enter: 'abc'");
    }

    /**
     * Opens the view, presses "Run" for a case and fails if Java refused the
     * add-on call.
     */
    private void runCase(int number) {
        page.navigate(BASE_URL + "/");
        page.locator("#run-" + number).click();
        Locator status = page.locator("#status-" + number);
        assertThat(status).not().hasText("not run");
        String text = status.innerText();
        if (text.startsWith("REFUSED")) {
            fail("refused in Java (" + text + ")");
        }
    }

    /**
     * Fails with what the browser blocked, for a case that only the browser
     * refuses. Waits briefly, since the violation event is dispatched after
     * the blocked script.
     */
    private void assertNoCspViolation() {
        page.waitForTimeout(500);
        Object violations = page.evaluate("() => window.__cspViolations");
        if (violations instanceof List<?> list && !list.isEmpty()) {
            page.evaluate("() => window.__cspViolations = []");
            consoleErrors.clear();
            fail("blocked by the browser: " + list);
        }
    }

    private void waitFor(String condition, String message) {
        try {
            page.waitForFunction(condition, null,
                    new Page.WaitForFunctionOptions().setTimeout(5000));
        } catch (PlaywrightException e) {
            fail(message + " within 5 s");
        }
    }
}
