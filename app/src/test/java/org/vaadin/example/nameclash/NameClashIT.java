package org.vaadin.example.nameclash;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Review finding 1 on vaadin/flow#27457: runs {@code NameClashView} only with
 * {@code mvn clean -Pit,name-clash verify -pl app}. The method declares
 * {@code const format} while {@code format} is imported, and still works.
 */
class NameClashIT {

    @Test
    void constNamedLikeAnImport() {
        assumeTrue("name-clash".equals(System.getProperty("it.extraCases")),
                "only with the name-clash profile");
        String baseUrl = System.getProperty("it.baseUrl",
                "http://localhost:8080");
        try (Playwright playwright = Playwright
                .create(new Playwright.CreateOptions().setEnv(
                        Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setChannel("chrome"));
            Page page = browser.newPage();
            List<String> errors = new ArrayList<>();
            page.onConsoleMessage(message -> {
                if ("error".equals(message.type())) {
                    errors.add(message.text());
                }
            });
            page.onPageError(error -> errors.add("page error: " + error));
            page.navigate(baseUrl + "/name-clash");
            page.locator("#run").click();
            assertThat(page.locator("#result")).hasText("value <x>");
            page.locator("#format").click();
            assertThat(page.locator("#format-result")).hasText("value {y}");
            page.waitForTimeout(300);
            assertEquals(List.of(), errors, "the browser logged errors");
        }
    }
}
