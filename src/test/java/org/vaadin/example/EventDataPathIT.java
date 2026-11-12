package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import com.microsoft.playwright.Response;

/**
 * Checks {@link EventDataPathView} in a browser while the application runs
 * with {@code vaadin.csp=strict}. Run with {@code mvn -Pit verify}, which
 * starts the application in the pre-integration-test phase.
 * <p>
 * Every test opens the view in a new page. After each test, the page must
 * have reported no Content Security Policy violation and no console error.
 */
class EventDataPathIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static final String IAE = "IllegalArgumentException";
    private static final String CSP = "CspViolationException";

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private Response response;
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
    void openView() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
            }
        });
        page.onPageError(error -> consoleErrors.add("page error: " + error));
        page.addInitScript("""
                window.__cspViolations = [];
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI + ' ' + e.sourceFile + ':' + e.lineNumber));
                """);
        response = page.navigate(BASE_URL + "/event-data-path");
        assertThat(page.locator("#setup")).isAttached();
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

    @Test
    void strictCspHeader_noUnsafeEval() {
        String csp = response.headers().get("content-security-policy");
        assertNotNull(csp, "Content-Security-Policy header");
        assertTrue(csp.contains("'strict-dynamic'"), csp);
        assertFalse(csp.contains("unsafe-eval"), csp);
    }

    // P1
    @Test
    void addEventDataPath_valuesReadWithPathAsKey() {
        page.locator("#paths").click();
        assertThat(result("paths")).hasText("event.type=\"click\""
                + " | event.detail=1" + " | event.target.id=\"paths\""
                + " | element.dataset.case=\"paths\""
                // ?. stops at null and gives undefined, which JSON leaves out
                + " | event.relatedTarget?.id=missing");
    }

    // P2
    @Test
    void addEventDataElementPath_elementsMapped() {
        page.locator("#elements-child").click();
        assertThat(result("elements")).hasText(
                "event.target.parentElement=elements-parent"
                        + " | event.target=elements-child"
                        + " | element.parentElement=elements-container");
    }

    // R1
    @Test
    void notPaths_refusedByBothMethods() {
        for (int i = 0; i < EventDataPathView.NOT_PATHS.length; i++) {
            assertOutcome("not-path-" + i, IAE);
            assertOutcome("not-element-path-" + i, IAE);
        }
        assertOutcome("null-path", "NullPointerException");
        assertOutcome("null-element-path", "NullPointerException");
    }

    // R1
    @Test
    void edgeCasePaths_acceptedByBothMethods() {
        for (int i = 0; i < EventDataPathView.PATHS.length; i++) {
            assertOutcome("path-" + i, "OK");
            assertOutcome("element-path-" + i, "OK");
        }
    }

    // R2
    @Test
    void refusedString_addsNothing() {
        assertOutcome("rejected-probe-path", IAE);
        assertOutcome("rejected-probe-element-path", IAE);
        page.locator("#rejected-probe").click();
        assertThat(result("rejected-probe"))
                .hasText("{\"element.id\":\"rejected-probe\"}");
    }

    // B1
    @Test
    void addEventDataClass_validBean_decoded() {
        page.locator("#bean").click();
        assertThat(result("bean"))
                .hasText(Pattern.compile("^type=click \\| detail=1 \\| "));
    }

    // B2, B3
    @Test
    void beanWithNonPathProperty_compiled_refusedByStrictMode() {
        // Since 1992c6915a and da29a7ba02 the bean methods do not check their
        // paths: a non-path is compiled, so strict mode refuses it, before
        // anything is added
        assertOutcome("bean-top-level-type", CSP);
        assertOutcome("bean-non-ascii-detail", CSP);
        page.locator("#bean-probe").click();
        assertThat(result("bean-probe"))
                .hasText("{\"element.id\":\"bean-probe\"}");
    }

    // D1, D2
    @Test
    void deprecatedMethods_pathsStillWork() {
        page.locator("#deprecated").click();
        assertThat(result("deprecated")).hasText("event.type=click"
                + " | event.target.parentElement=deprecated-parent");
    }

    // D3, D4
    @Test
    void deprecatedMethods_expressionsRefusedByStrictMode() {
        assertOutcome("deprecated-expression", CSP);
        assertOutcome("deprecated-element-expression", CSP);
    }

    // E1
    @Test
    void domEventPathEventData_works() {
        page.locator("#dom-event").click();
        assertThat(result("dom-event")).hasText("detail=1 | id=dom-event");
    }

    // E2
    @Test
    void domEventExpressionEventData_refusedByStrictMode() {
        assertOutcome("dom-event-expression", CSP);
    }

    // T1
    @Test
    void preventDefault_linkNotFollowed() {
        page.locator("#prevent-link").click();
        assertThat(result("prevent")).hasText("clicked");
        assertFalse(page.url().endsWith("#prevented"), page.url());
    }

    // T2
    @Test
    void stopPropagation_outerNotReached() {
        page.locator("#propagation-inner").click();
        assertThat(result("propagation")).hasText("inner");
        // A click on the outer element still reaches its listener
        page.locator("#propagation-outer").click(
                new com.microsoft.playwright.Locator.ClickOptions()
                        .setPosition(1, 1));
        assertThat(result("propagation")).hasText("inner outer");
    }

    // T3
    @Test
    void synchronizeProperty_valueSynchronized() {
        page.locator("#sync-input").fill("hello");
        page.locator("#sync-input").press("Tab");
        assertThat(result("sync")).hasText("value=hello");
    }

    // W1
    @Test
    void windowResizeListener_sizeReceived() {
        page.setViewportSize(700, 500);
        assertThat(result("resize")).hasText("700x500");
    }

    private void assertOutcome(String id, String expected) {
        assertThat(page.locator("#setup-" + id))
                .hasAttribute("data-outcome", expected);
    }

    private com.microsoft.playwright.Locator result(String id) {
        return page.locator("#result-" + id);
    }
}
