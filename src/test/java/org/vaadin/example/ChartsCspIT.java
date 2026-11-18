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
 * Checks {@link ChartsCspView} in a browser while the application runs with
 * {@code vaadin.csp=strict}. Run with {@code mvn -Pit verify}, which starts
 * the application in the pre-integration-test phase.
 * <p>
 * Every test opens the view in a new page. After each test, the page must
 * have reported no Content Security Policy violation and no console error.
 */
class ChartsCspIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

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
        response = page.navigate(BASE_URL + "/charts-csp");
        assertThat(chartText("basic", "12 kg")).isAttached();
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
    void basicChart_dataLabelFormat_rendered() {
        for (String text : List.of("12 kg", "7 kg", "3 kg")) {
            assertThat(chartText("basic", text)).isAttached();
        }
    }

    @Test
    void formatString_templateHelpers_rendered() {
        // {divide value 1000}k on the y axis
        assertThat(page.locator("#format .highcharts-yaxis-labels text")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^(?:\\d+(\\.\\d+)?k)+$")))
                .first()).isAttached();
        // {#if (gt y 5000)}high{else}low{/if} on the data labels
        assertThat(chartText("format", "low")).isAttached();
        assertThat(chartText("format", "high")).isAttached();
    }

    @Test
    void tooltip_hovered_headerPointAndFooterFormatsRendered() {
        page.locator("#basic .highcharts-point").first().hover();
        Locator tooltip = page.locator("#basic .highcharts-tooltip text");
        // header {point.key}, point "{series.name}: {point.y} kg", footer
        assertThat(tooltip).containsText("Apples");
        assertThat(tooltip).containsText("Weight: 12 kg");
        assertThat(tooltip).containsText("per crate");
    }

    @Test
    void legend_labelFormat_rendered() {
        assertThat(page.locator("#basic .highcharts-legend-item text"))
                .hasText("Weight series");
    }

    @Test
    void numberFormatAndMoreHelpers_rendered() {
        // {value:,.2f} on the y axis
        assertThat(page.locator("#helpers .highcharts-yaxis-labels text")
                .filter(new Locator.FilterOptions().setHasText("2.00"))
                .first()).isAttached();
        // {ucfirst key}: {multiply y 2}{#unless (gt y 5)} small{/unless}
        assertThat(chartText("helpers", "Apples: 6 small")).isAttached();
        assertThat(chartText("helpers", "Pears: 16")).isAttached();
    }

    @Test
    void pointAddedFromServer_rendered() {
        page.locator("#add-point").click();
        assertThat(page.locator("#log")).hasText("point added");
        assertThat(chartText("dynamic", "value 20")).isAttached();
    }

    @Test
    void stringFormatter_refusedOnServer() {
        page.locator("#string-formatter").click();
        assertThat(page.locator("#log"))
                .hasText("refused: CspViolationException");
    }

    @Test
    void declaredFunction_dataLabelsFormatted() {
        for (String text : List.of("2k*", "4k*", "8k*")) {
            assertThat(chartText("declared", text)).isAttached();
        }
    }

    /**
     * Gets the first SVG text of the chart with the given id whose text is the
     * given text. Highcharts draws an outline copy of a data label inside the
     * same text element, separated by a zero-width space, so the text may
     * appear twice. The locator pierces the shadow root of the chart.
     */
    private Locator chartText(String chartId, String text) {
        String escaped = text.replaceAll("[\\\\^$.|?*+()\\[\\]{}/]", "\\\\$0");
        return page.locator("#" + chartId + " text")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile(
                        "^" + escaped + "(?:\\u200B" + escaped + ")?$")))
                .first();
    }
}
