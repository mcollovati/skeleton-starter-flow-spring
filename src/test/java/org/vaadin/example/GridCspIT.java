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
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

/**
 * Checks {@link GridCspView} in a browser while the application runs with
 * {@code vaadin.csp=strict}. Run with {@code mvn -Pit verify}, which starts
 * the application in the pre-integration-test phase.
 * <p>
 * Every test opens the view in a new page. After each test, the page must
 * have reported no Content Security Policy violation and no console error.
 */
class GridCspIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static final String FEATURES_GRID = "features-grid";

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
        response = page.navigate(BASE_URL + "/grid-csp");
        assertThat(cell("grid", "Ada")).isAttached();
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

    @Test
    void rendererColumns_render() {
        for (String text : List.of("Ada", "C:Ada", "T:Ada", "36 years",
                "1990-01-10", "Click", "Status: active")) {
            assertThat(cell("grid", text)).isAttached();
        }
    }

    @Test
    void nativeButton_click_reachesServer() {
        page.locator("#grid > vaadin-grid-cell-content button")
                .filter(new Locator.FilterOptions().setHasText("Click"))
                .first().click();
        assertThat(page.locator("#log")).hasText("button Ada");
    }

    @Test
    void appTemplateFunction_click_reachesServer() {
        page.locator("#grid > vaadin-grid-cell-content .status").first()
                .click();
        assertThat(page.locator("#log")).hasText("select Ada");
    }

    @Test
    void itemDetails_rowClicked_detailsRendered() {
        cell("grid", "Linus").click();
        assertThat(cell("grid", "Details of Linus")).isAttached();
    }

    @Test
    void itemDetailsRenderer_replaced_newRendererUsed() {
        cell("grid", "Linus").click();
        assertThat(cell("grid", "Details of Linus")).isAttached();

        page.locator("#replace-details").click();
        assertThat(page.locator("#log")).hasText("details replaced");

        // Details that are already open stay empty after the renderer is
        // replaced (vaadin/flow-components#10653, also without the CSP
        // changes), so another row is opened
        cell("grid", "Ada").click();
        assertThat(cell("grid", "New details of Ada")).isAttached();
    }

    @Test
    void headerAndFooter_rendered() {
        for (String text : List.of("Name header", "Footer text",
                "Footer component")) {
            assertThat(cell(FEATURES_GRID, text)).isAttached();
        }
    }

    @Test
    void hiddenColumn_hasFlowId() {
        page.waitForFunction("""
                () => [...document.querySelectorAll('#features-grid vaadin-grid-column')]
                    .some((column) => column.hidden && column._flowId)
                """);
    }

    @Test
    void tooltipGenerator_set() {
        page.waitForFunction("""
                () => typeof document.querySelector(
                    '#features-grid > vaadin-tooltip[slot="tooltip"]')?.generator === 'function'
                """);
    }

    @Test
    void headerSorter_clickedTwice_sortedDescending() {
        Locator sorter = page.locator("#features-grid vaadin-grid-sorter")
                .first();
        sorter.click();
        sorter.click();
        assertThat(cell(FEATURES_GRID, "Person 199")).isAttached();
    }

    @Test
    void serverSort_sorterDirectionUpdated() {
        page.locator("#sort-desc").click();
        assertThat(cell(FEATURES_GRID, "Person 199")).isAttached();
        waitForSorterDirection("desc");

        page.locator("#sort-asc").click();
        assertThat(cell(FEATURES_GRID, "Person 000")).isAttached();
        waitForSorterDirection("asc");
    }

    @Test
    void editor_opened_showsEditorComponent() {
        page.locator("#edit").click();
        assertThat(page.locator("#name-editor input")).hasValue("Person 000");
    }

    @Test
    void pageSize_changed_gridStillRenders() {
        page.locator("#page-size").click();
        assertThat(page.locator("#log")).hasText("page size 20");
        page.waitForFunction(
                "() => document.querySelector('#features-grid').pageSize === 20");
        assertThat(cell(FEATURES_GRID, "Person 000")).isAttached();
    }

    @Test
    void scrollToEnd_lastRowRendered() {
        page.locator("#scroll-to-end").click();
        assertThat(cell(FEATURES_GRID, "Person 199")).isAttached();
    }

    @Test
    void hierarchyColumn_toggleClicked_expands() {
        page.locator("vaadin-grid-tree-toggle")
                .filter(new Locator.FilterOptions().setHasText("Parent 1"))
                .first().click();
        assertThat(cell("tree-grid", "Parent 1 / Child 1")).isAttached();
    }

    @Test
    void componentHierarchyColumn_toggleClicked_expands() {
        page.locator("vaadin-grid-tree-toggle")
                .filter(new Locator.FilterOptions().setHasText("[Folder]"))
                .click();
        assertThat(cell("component-tree-grid", "[File]")).isAttached();
    }

    @Test
    void stringTemplate_refusedOnServer() {
        page.locator("#string-template").click();
        assertThat(page.locator("#log"))
                .hasText("refused: CspViolationException");
    }

    /**
     * Gets the first cell of the grid with the given id whose whole text is
     * the given text.
     */
    private Locator cell(String gridId, String text) {
        // Playwright runs the pattern as a JavaScript regular expression,
        // which does not support the \Q...\E quoting of Pattern.quote
        String escaped = text.replaceAll("[\\\\^$.|?*+()\\[\\]{}/]", "\\\\$0");
        return page.locator("#" + gridId + " > vaadin-grid-cell-content")
                .filter(new Locator.FilterOptions().setHasText(
                        Pattern.compile("^\\s*" + escaped + "\\s*$")))
                .first();
    }

    private void waitForSorterDirection(String direction) {
        page.waitForFunction("""
                (direction) => document.querySelector(
                    '#features-grid vaadin-grid-sorter')?.direction === direction
                """, direction);
    }
}
