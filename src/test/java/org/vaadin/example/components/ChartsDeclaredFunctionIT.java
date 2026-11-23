package org.vaadin.example.components;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Checks {@link ChartsDeclaredFunctionView} under {@code vaadin.csp=strict}:
 * the declared formatter inside the configuration formats the data labels, and
 * the page reports no CSP violation and no console error.
 */
class ChartsDeclaredFunctionIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
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

    @Test
    void declaredFunctionInConfiguration_dataLabelsFormatted() {
        try (var context = browser.newContext()) {
            Page page = context.newPage();
            List<String> consoleErrors = new ArrayList<>();
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
            page.navigate(BASE_URL + "/charts-declared-function");

            Locator dataLabels = page
                    .locator("#declared-config .highcharts-data-label text");
            for (String text : List.of("2k", "4k", "8k")) {
                assertThat(dataLabels.filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile(
                                "^" + text + "(?:\\u200B" + text + ")?$")))
                        .first()).isAttached();
            }
            assertThat(dataLabels.filter(new Locator.FilterOptions()
                    .setHasText("2000"))).hasCount(0);

            assertEquals(List.of(),
                    page.evaluate("() => window.__cspViolations"),
                    "CSP violations");
            assertEquals(List.of(), consoleErrors, "console errors");
        }
    }
}
