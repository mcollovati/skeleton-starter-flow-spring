package org.vaadin.example;

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
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Checks {@link EventDataElementView} in a browser while the application runs
 * with {@code vaadin.csp=strict}. Run with {@code mvn -Pit verify}, which
 * starts the application in the pre-integration-test phase.
 * <p>
 * Every test opens the view in a new page. After each test, the page must
 * have reported no Content Security Policy violation and no console error.
 */
class EventDataElementIT {

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
        page.navigate(BASE_URL + "/event-data-element");
        assertThat(page.locator("#rows")).isAttached();
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

    // X1
    @Test
    void shadowRoot_originalTargetAndDocumentLevelTarget() {
        page.locator("#shadow-inner").click();
        assertThat(result("shadow")).hasText("originalTarget=shadow-inner"
                + " | eventTarget=shadow-host"
                + " | documentLevelTarget=shadow-host");
    }

    // X2
    @Test
    void closestRow_declaredReplacementOfRefusedString() {
        page.locator("#cell-2").click();
        assertThat(result("row")).hasText(
                "closestRow=row-2 | getEventData=row-2 | targetId=cell-2");
    }

    // X3, X4
    @Test
    void capturedSelectorAndElementOutsideSource() {
        page.locator("#query-target").click();
        assertThat(result("capture"))
                .hasText("query=query-target | outside=outside-target");
    }

    // X5
    @Test
    void nullAndNumber_noElement() {
        page.locator("#no-element").click();
        assertThat(result("no-element"))
                .hasText("nothing=none | notANode=none | getEventData=null");
    }

    // X6, X7
    @Test
    void invisibleNotReported_unmappedNodeGivesClosestParent() {
        page.locator("#hide-invisible").click();
        // The browser still has the hidden element, so only the server check
        // keeps it out
        assertThat(page.locator("#invisible-target[hidden]")).isAttached();
        page.locator("#invisible-source > span").first().click();
        assertThat(result("invisible"))
                .hasText("invisible=none | rawChild=raw-parent");
    }

    private Locator result(String id) {
        return page.locator("#result-" + id);
    }
}
