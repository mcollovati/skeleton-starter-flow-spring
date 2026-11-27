package org.vaadin.example.rebuild;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Review finding 2 on vaadin/flow#27457: checks that the call of the view a
 * {@code rebuild-*} profile adds works, with
 * {@code mvn clean -Pit,rebuild-query verify -pl app} (or
 * {@code rebuild-no-extension} or {@code rebuild-jar-query}).
 */
class RebuildPathIT {

    private static final Map<String, String> EXPECTED = Map.of(
            "rebuild-query", "value \\.app-style ?\\{ ?color: ?green;? ?\\}",
            "rebuild-jar-query",
            "value .*\\.acme-style ?\\{ ?color: ?(blue|#00f);? ?\\}",
            "rebuild-no-extension", "value probe x");

    @Test
    void importedModuleWorks() {
        String profile = System.getProperty("it.extraCases", "none");
        assumeTrue(EXPECTED.containsKey(profile),
                "only with a rebuild-* profile");
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
            page.navigate(baseUrl + "/" + profile);
            page.locator("#run").click();
            assertThat(page.locator("#result")).hasText(
                    Pattern.compile(EXPECTED.get(profile), Pattern.DOTALL));
            System.out.println(profile + ": "
                    + page.locator("#result").textContent());
            page.waitForTimeout(300);
            assertEquals(List.of(), errors, "the browser logged errors");
        }
    }
}
