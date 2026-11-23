package org.vaadin.example.errors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Checks {@link DeclaredJsErrorsView} in a browser. Run with
 * {@code mvn -Pit verify}, which starts the application in production mode in
 * the pre-integration-test phase.
 * <p>
 * "Reported" means what Flow does for a failed {@code executeJs(String)}: it
 * throws the error again outside the call, so the page gets an uncaught
 * {@code error} event with the stack trace. An error that only rejects a
 * promise nobody handles shows up as an {@code unhandledrejection} event
 * instead, and Flow does not report it.
 * <p>
 * Run with {@code -Dit.csp=off} against an application started with
 * {@code -Dvaadin.csp=off} to check the {@code executeJs(String)} case too.
 */
class DeclaredJsErrorsIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final boolean CSP_OFF = "off"
            .equals(System.getProperty("it.csp"));

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();
    private final List<String> pageErrors = new ArrayList<>();

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
    void openView() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
            }
        });
        page.onPageError(error -> pageErrors.add(error));
        page.addInitScript("""
                window.__uncaught = [];
                window.__unhandledRejections = [];
                window.__cspViolations = [];
                window.addEventListener('error', (e) =>
                    window.__uncaught.push(String(e.error && e.error.message
                        ? e.error.message : e.message)));
                window.addEventListener('unhandledrejection', (e) =>
                    window.__unhandledRejections.push(String(e.reason
                        && e.reason.message ? e.reason.message : e.reason)));
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI));
                """);
        page.navigate(BASE_URL + "/declared-js-errors");
        assertThat(page.locator("#void-throws")).isAttached();
    }

    @AfterEach
    void close() {
        try {
            assertEquals(List.of(), page.evaluate("() => window.__cspViolations"),
                    "CSP violations");
        } finally {
            System.out.println("console errors: " + consoleErrors);
            System.out.println("page errors: " + pageErrors);
            context.close();
        }
    }

    @Test
    void declaredVoidMethodThrows_errorIsReported() {
        page.click("#void-throws");
        assertReported("void-throws");
    }

    @Test
    void declaredMethodThrowsAndResultIsNotUsed_errorIsReported() {
        page.click("#unsubscribed-throws");
        assertReported("unsubscribed-throws");
    }

    @Test
    void declaredVoidMethodRejectsLater_errorIsReported() {
        page.click("#async-rejects");
        assertReported("async-rejects");
    }

    @Test
    void declaredMethodThrowsAndResultIsUsed_serverGetsErrorAndNothingIsReported() {
        page.click("#subscribed-throws");
        assertThat(page.locator("#subscribed-result"))
                .hasText("error: Error: subscribed-throws");
        page.waitForTimeout(500);
        assertEquals(List.of(), uncaught(), "uncaught errors");
        assertEquals(List.of(), unhandledRejections(), "unhandled rejections");
    }

    @Test
    void declaredMethodSucceeds_nothingIsReported() {
        page.click("#succeeds");
        assertThat(page.locator("#succeeds-result")).hasText("value: 42");
        page.waitForTimeout(500);
        assertEquals(List.of(), uncaught(), "uncaught errors");
        assertEquals(List.of(), unhandledRejections(), "unhandled rejections");
    }

    @Test
    void stringExecuteJsThrows() {
        page.click("#string-throws");
        if (CSP_OFF) {
            assertThat(page.locator("#string-result")).hasText("sent");
            assertReported("string-throws");
        } else {
            assertThat(page.locator("#string-result"))
                    .hasText("refused: CspViolationException");
        }
    }

    private void assertReported(String message) {
        page.waitForFunction("() => window.__uncaught.length"
                + " + window.__unhandledRejections.length > 0");
        // A rejection is reported asynchronously, give the other event a
        // moment so that the lists are final
        page.waitForTimeout(500);
        assertEquals(List.of(), unhandledRejections(), "unhandled rejections");
        assertEquals(List.of(message), uncaught(), "uncaught errors");
    }

    @SuppressWarnings("unchecked")
    private List<String> uncaught() {
        return (List<String>) page.evaluate("() => window.__uncaught");
    }

    @SuppressWarnings("unchecked")
    private List<String> unhandledRejections() {
        return (List<String>) page.evaluate("() => window.__unhandledRejections");
    }
}
