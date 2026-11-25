package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Checks vaadin/flow#27078 with a definition that the application calls only
 * through reflection ({@link MissingDefinitionView}).
 * <p>
 * Without the {@code uses-fix} profile the production bundle does not carry
 * {@code com.acme.addon.ReflectedJs}: the call does nothing in the browser, and
 * the server logs one error that names the class and suggests {@code @Uses},
 * however often the view calls it. With {@code -Puses-fix} the call works and
 * nothing is logged.
 * <p>
 * Run with {@code mvn clean -Pit verify [-Puses-fix]}: the server log
 * ({@code target/server.log}) is appended to, so {@code clean} starts it
 * empty.
 */
class MissingDefinitionIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));
    private static final boolean USES_FIX = Boolean.getBoolean("it.usesFix");
    private static final String ERROR = "com.acme.addon.ReflectedJs declares"
            + " JavaScript that was not included when creating the production"
            + " bundle";

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
    void reflectedDefinition() throws IOException {
        page.navigate(BASE_URL + "/missing");
        // Twice: the error is logged once per class
        page.locator("#run").click();
        assertThat(page.locator("#result")).hasText("called");
        page.locator("#run").click();
        page.waitForTimeout(1000);

        long errors = errorLines();
        if (USES_FIX) {
            assertThat(page.locator("#target"))
                    .hasText("Marked by ReflectedJs");
            assertEquals(0, errors, "error lines in " + SERVER_LOG);
            assertEquals(List.of(), consoleErrors,
                    "the browser logged errors");
        } else {
            assertThat(page.locator("#target")).hasText("Not marked");
            assertEquals(1, errors, "error lines in " + SERVER_LOG);
            String line = Files.readAllLines(SERVER_LOG).stream()
                    .filter(l -> l.contains(ERROR)).findFirst().orElseThrow();
            assertTrue(line.contains("ERROR")
                    && line.contains("@Uses(ReflectedJs.class)"), line);
            System.out.println("Server log: " + line);
            System.out.println("Browser console errors: " + consoleErrors);
        }
    }

    private static long errorLines() throws IOException {
        return Files.readAllLines(SERVER_LOG).stream()
                .filter(l -> l.contains(ERROR)).count();
    }
}
