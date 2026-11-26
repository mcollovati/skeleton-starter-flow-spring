package org.vaadin.example.shorthand;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Checks vaadin/flow#27441 (methods of a {@code JsInvokers} interface without
 * {@code @JsExpression}) in the production bundle that {@code optimizeBundle}
 * makes, under strict CSP. See {@link ShorthandView} and
 * {@link MissingShorthandView}.
 * <p>
 * Run with {@code mvn clean -Pit verify [-Puses-fix]}, like
 * {@code MissingDefinitionIT}.
 */
class ShorthandIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));
    private static final Path DEFINITIONS = Path
            .of("src/main/frontend/generated/vaadin-js-definitions.js");
    private static final boolean USES_FIX = Boolean.getBoolean("it.usesFix");
    private static final String MISSING_ERROR = "com.acme.addon.ReflectedShorthandJs"
            + " declares JavaScript that was not included when creating the"
            + " production bundle";

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

    private Locator run(String id) {
        page.navigate(BASE_URL + "/shorthand");
        page.locator("#" + id + "-run").click();
        return page.locator("#" + id + "-box");
    }

    private Locator result(String id) {
        return page.locator("#" + id + "-result");
    }

    private void assertNoConsoleErrors() {
        page.waitForTimeout(300);
        assertEquals(List.of(), consoleErrors, "the browser logged errors");
    }

    @Test
    void s1_focus() {
        Locator box = run("s1");
        assertThat(result("s1")).hasText("called");
        assertThat(box).isFocused();
        assertNoConsoleErrors();
    }

    @Test
    void s2_setAttribute() {
        Locator box = run("s2");
        assertThat(box).hasAttribute("title", "set by shorthand");
        assertNoConsoleErrors();
    }

    @Test
    void s3_variadicAppend() {
        Locator box = run("s3");
        assertThat(box).hasText("Box s3 one two three");
        assertNoConsoleErrors();
    }

    @Test
    void s4_getAttribute() {
        run("s4");
        assertThat(result("s4")).hasText("value forty-two");
        assertNoConsoleErrors();
    }

    @Test
    void s5_typedResult() {
        run("s5");
        assertThat(result("s5")).hasText("hasAttribute true");
        assertNoConsoleErrors();
    }

    @Test
    void s6_overloads() {
        Locator box = run("s6");
        assertThat(box).hasAttribute("data-one", "");
        assertThat(box).hasAttribute("data-two", "");
        assertNoConsoleErrors();
    }

    @Test
    void s7_annotatedNextToShorthand() {
        Locator box = run("s7");
        assertThat(box).hasAttribute("data-acme", "marked");
        assertNoConsoleErrors();
    }

    @Test
    void s8_inheritedShorthand() {
        Locator box = run("s8");
        assertThat(box).hasAttribute("title", "set through SubBoxJs");
        assertThat(box).isFocused();
        assertNoConsoleErrors();
    }

    @Test
    void s9_typoFailsInBrowser() {
        run("s9");
        assertThat(result("s9"))
                .hasText(Pattern.compile("^browser error: .*focuss.*"));
        System.out.println("S9: " + result("s9").textContent()
                + " | console: " + consoleErrors);
    }

    @Test
    void r1_pageExecuteJsRefused() {
        run("r1");
        assertThat(result("r1")).hasText(Pattern.compile(
                "^refused: IllegalArgumentException: .*BoxJs has .*focus.* without @JsExpression.*"));
        System.out.println("R1: " + result("r1").textContent());
    }

    @Test
    void r2_mixedInMethodRefused() {
        run("r2");
        assertThat(result("r2")).hasText(Pattern
                .compile("^refused: IllegalArgumentException: .*blur.*"));
        System.out.println("R2: " + result("r2").textContent());
    }

    @Test
    void r3_snippetWithoutExpressionRefused() {
        run("r3");
        assertThat(result("r3")).hasText(Pattern
                .compile("^refused: IllegalArgumentException: .*focus.*"));
        System.out.println("R3: " + result("r3").textContent());
    }

    @Test
    void bundleCarriesOnlyReachedShorthand() throws IOException {
        String bundle = Files.readString(DEFINITIONS);
        assertTrue(bundle.contains("this.append(...$0)"),
                "variadic BoxJs.append in the bundle");
        assertTrue(bundle.contains("this.toggleAttribute($0, $1)"),
                "BoxJs.toggleAttribute in the bundle");
        assertFalse(bundle.contains("acmeUnusedShorthand"),
                "unreached UnusedShorthandJs in the bundle");
        assertEquals(USES_FIX, bundle.contains("this.replaceChildren("),
                "ReflectedShorthandJs in the bundle");
    }

    @Test
    void missingShorthandDefinition() throws IOException {
        page.navigate(BASE_URL + "/missing-shorthand");
        page.locator("#run").click();
        assertThat(page.locator("#result")).hasText("called");
        page.locator("#run").click();
        page.waitForTimeout(1000);

        List<String> errors = Files.readAllLines(SERVER_LOG).stream()
                .filter(l -> l.contains(MISSING_ERROR)).toList();
        if (USES_FIX) {
            assertThat(page.locator("#target"))
                    .hasText("Replaced by ReflectedShorthandJs");
            assertEquals(List.of(), errors);
            assertEquals(List.of(), consoleErrors);
        } else {
            assertThat(page.locator("#target")).hasText("Not replaced");
            assertEquals(1, errors.size(), "error lines in " + SERVER_LOG);
            assertTrue(errors.get(0).contains("ERROR") && errors.get(0)
                    .contains("@Uses(ReflectedShorthandJs.class)"),
                    errors.get(0));
            System.out.println("Server log: " + errors.get(0));
            System.out.println("Browser console errors: " + consoleErrors);
        }
    }
}
