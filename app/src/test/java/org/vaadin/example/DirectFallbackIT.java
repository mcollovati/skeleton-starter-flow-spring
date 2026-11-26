package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Checks that a direct call to a string fallback interface
 * ({@link DirectFallbackView}) works and that the bundle check of
 * vaadin/flow#27078 does not report it as missing (fixed in 3f8c95b4fd).
 * <p>
 * Run with {@code mvn clean -Pit verify}: the server log
 * ({@code target/server.log}) is appended to, so {@code clean} starts it
 * empty.
 */
class DirectFallbackIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
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
            }
        });
        page.onPageError(error -> consoleErrors.add("page error: " + error));
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    @Test
    void directCallToStringFallback() throws IOException {
        page.navigate(BASE_URL + "/direct-fallback");
        page.locator("#run").click();
        assertThat(page.locator("#result")).hasText("scrollTop > 0");
        page.waitForTimeout(500);

        List<String> reported = Files.readAllLines(SERVER_LOG).stream()
                .filter(l -> l.contains("AcmeCspFallbacks")).toList();
        assertEquals(List.of(), reported,
                "server log lines about AcmeCspFallbacks");
        assertEquals(List.of(), consoleErrors, "the browser logged errors");
    }
}
