package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
 * Checks {@link BundledEventDataView} in a browser while the application runs
 * with {@code vaadin.csp=strict}. Run with {@code mvn -Pit verify}, which
 * starts the application in the pre-integration-test phase.
 * <p>
 * Every test opens the view in a new page. After each test, the page must
 * have reported no Content Security Policy violation and no console error.
 */
class BundledEventDataIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    /** Where the production build writes the bundle. */
    private static final Path BUNDLE_DIR = Path
            .of("target/classes/META-INF/VAADIN/webapp/VAADIN/build");

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
        response = page.navigate(BASE_URL + "/bundled-event-data");
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

    // A1
    @Test
    void expressionsPathAndElementPath_allReceived() {
        page.locator("#mixed").click();
        assertThat(result("mixed")).hasText("detailTimesTen=10"
                + " | upperType=CLICK | targetId=mixed | element=mixed");
    }

    // A2
    @Test
    void sharedExpression_worksInBothEventClasses() {
        page.locator("#shared").dblclick();
        // A double click fires click (detail 1), click (detail 2), dblclick
        assertThat(result("shared"))
                .hasText("click=10 click=20 dblclick=20");
    }

    // A2
    @Test
    void sharedExpression_oneFunctionInBundle() throws IOException {
        String sharedId = functionIds().get(0);
        // The minifier may change the quotes around the key, so count the id
        assertEquals(1, countInBundle(sharedId),
                "functions registered for " + BundledEventDataView.SHARED);
    }

    // A3
    @Test
    void syntaxEdgeCases_survivedCodeGeneration() {
        page.locator("#syntax").click();
        assertThat(result("syntax")).hasText("template=click:1"
                + " | trailingComment=click!" + " | multiLine=101"
                + " | quotes=a\"bc'd\\");
    }

    // A4
    @Test
    void dropEventOffsets_fromLibraryEventClass() {
        page.locator("#drag-source").dragTo(page.locator("#drop-target"),
                new Locator.DragToOptions().setTargetPosition(10, 20));
        // Playwright measures from inside the 1px border, the offsets from
        // the border box
        assertThat(result("drop")).hasText("offsetX=11 | offsetY=21");
    }

    // A1-A4: every bundled value is registered in the browser
    @Test
    void bundledFunctions_registeredInBrowser() {
        for (String id : functionIds()) {
            assertEquals("function", page.evaluate(
                    "id => typeof window.Vaadin.Flow.jsSnippets[id]", id),
                    "snippet " + id);
        }
    }

    // A5
    @Test
    void elementExpression_notBundled_refusedByStrictMode() {
        assertOutcome("element-expression", "CspViolationException");
    }

    // A6
    @Test
    void stringApiWithSameExpression_refusedByStrictMode() {
        assertOutcome("string-api-same-expression", "CspViolationException");
    }

    private List<String> functionIds() {
        Locator ids = page.locator("#function-ids .function-id");
        assertThat(ids).hasCount(6);
        return ids.allTextContents();
    }

    private static long countInBundle(String text) throws IOException {
        long count = 0;
        try (Stream<Path> files = Files.list(BUNDLE_DIR)) {
            for (Path file : files
                    .filter(f -> f.toString().endsWith(".js")).toList()) {
                String content = Files.readString(file);
                for (int i = content.indexOf(text); i >= 0; i = content
                        .indexOf(text, i + 1)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void assertOutcome(String id, String expected) {
        assertThat(page.locator("#setup-" + id))
                .hasAttribute("data-outcome", expected);
    }

    private Locator result(String id) {
        return page.locator("#result-" + id);
    }
}
