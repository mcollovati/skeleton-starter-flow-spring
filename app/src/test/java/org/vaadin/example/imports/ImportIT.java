package org.vaadin.example.imports;

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
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Checks vaadin/flow#27423 ({@code @JsDefinitionImport}) in the production
 * bundle that {@code optimizeBundle} makes, under strict CSP. See
 * {@link ImportView} and {@link MissingImportView}.
 * <p>
 * Run with {@code mvn clean -Pit verify [-Puses-fix]}, like
 * {@code MissingDefinitionIT}.
 */
class ImportIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));
    private static final Path DEFINITIONS = Path
            .of("src/main/frontend/generated/vaadin-js-definitions.js");
    private static final Path BUILD = Path
            .of("target/classes/META-INF/VAADIN/webapp/VAADIN/build");
    private static final boolean USES_FIX = Boolean.getBoolean("it.usesFix");
    private static final String MISSING_ERROR = "com.acme.addon.ReflectedImportJs"
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

    private void open() {
        page.navigate(BASE_URL + "/imports");
        page.locator("#i1-run").waitFor();
    }

    private Locator run(String id) {
        open();
        return click(id);
    }

    private Locator click(String id) {
        page.locator("#" + id + "-run").click();
        return page.locator("#" + id + "-box");
    }

    private Locator result(String id) {
        return page.locator("#" + id + "-result");
    }

    private Object eval(String expression) {
        return page.evaluate(expression);
    }

    private void assertNoConsoleErrors() {
        page.waitForTimeout(300);
        assertEquals(List.of(), consoleErrors, "the browser logged errors");
    }

    @Test
    void i1_defaultAndNamedExport() {
        Locator box = run("i1");
        assertThat(box).hasText("[ONE]");
        assertNoConsoleErrors();
    }

    @Test
    void i2_namespace() {
        Locator box = run("i2");
        assertThat(box).hasText("label: two label-1 label-default");
        assertNoConsoleErrors();
    }

    @Test
    void i3_pendingResult() {
        run("i3");
        assertThat(result("i3")).hasText("value [three]");
        assertNoConsoleErrors();
    }

    @Test
    void i4_typedResult() {
        run("i4");
        assertThat(result("i4")).hasText("version label-1");
        assertNoConsoleErrors();
    }

    @Test
    void i5_variadic() {
        Locator box = run("i5");
        assertThat(box).hasText("[a] [b] [c]");
        assertNoConsoleErrors();
    }

    @Test
    void i6_shorthandNextToImports() {
        Locator box = run("i6");
        assertThat(result("i6")).hasText("called");
        assertThat(box).isFocused();
        assertNoConsoleErrors();
    }

    @Test
    void i7_sameJavaScriptOtherImports() {
        Locator box = run("i7");
        assertThat(box).hasText("<seven>");
        assertThat(result("i7")).hasText("value [seven]");
        assertNoConsoleErrors();
    }

    @Test
    void i8_inheritedMethodsKeepTheirImports() {
        Locator box = run("i8");
        assertThat(box).hasText("[eight]");
        assertThat(result("i8")).hasText("value {eight}");
        assertNoConsoleErrors();
    }

    @Test
    void i9_pageExecuteJs() {
        run("i9");
        assertThat(result("i9")).hasText("value NINE!");
        assertNoConsoleErrors();
    }

    @Test
    void i10_npmPackage() {
        Locator box = run("i10");
        assertThat(box.locator("b.lit")).hasText("ten");
        assertNoConsoleErrors();
    }

    @Test
    void i11_argumentsHoldsImportedValues() {
        run("i11");
        // format, upper and labels
        assertThat(result("i11")).hasText("value 3");
        assertNoConsoleErrors();
    }

    @Test
    void i12_missingExportFailsInBrowser() {
        run("i12");
        // The production build minifies the names the generated function
        // gets, so the message does not name notExported
        assertThat(result("i12")).hasText(Pattern
                .compile("^browser error: TypeError: \\w+ is not a function"));
        System.out.println("I12: " + result("i12").textContent()
                + " | console: " + consoleErrors);
    }

    @Test
    void modulesLoadOnFirstCallAndOnce() {
        open();
        assertEquals(null, eval("window.acmeFormatLoads"),
                "acme-format.js loaded before a call");
        assertEquals(null, eval("window.acmeLabelLoads"),
                "acme-label.js loaded before a call");
        assertEquals(null, eval("window.appFormatLoads"),
                "app-format.js loaded before a call");
        assertEquals(true, eval("window.acmeEagerLoaded"),
                "@JsModule on the definition not loaded with the view");

        // Three calls through two definitions that import acme-format.js
        assertThat(click("i1")).hasText("[ONE]");
        click("i3");
        assertThat(result("i3")).hasText("value [three]");
        click("i8");
        assertThat(result("i8")).hasText("value {eight}");

        assertEquals(1, eval("window.acmeFormatLoads"));
        // ImportBoxJs imports acme-label.js too, for every method
        assertEquals(1, eval("window.acmeLabelLoads"));
        assertEquals(1, eval("window.appFormatLoads"));
        assertNoConsoleErrors();
    }

    @Test
    void x1_snippetsRefused() {
        assertRefused("x1", ".*ImportSnippetsJs is annotated with"
                + " @JsDefinitionImport, which only applies to an interface"
                + " that extends JsInvokers.*");
    }

    @Test
    void x2_noNamesRefused() {
        assertRefused("x2", ".*NoNamesJs has @JsDefinitionImport\\(from ="
                + " \"./app-format.js\"\\) with no names to import.*");
    }

    @Test
    void x3_reservedWordRefused() {
        assertRefused("x3", ".*ReservedNameJs declares 'default' in"
                + " @JsDefinitionImport.*");
    }

    @Test
    void x4_argumentNameRefused() {
        assertRefused("x4", ".*ArgumentNameJs declares '\\$0' in"
                + " @JsDefinitionImport.*");
    }

    @Test
    void x5_duplicateNameRefused() {
        assertRefused("x5", ".*DuplicateNameJs declares 'format' more than"
                + " once in @JsDefinitionImport.*");
    }

    @Test
    void x6_badImportOnParentRefused() {
        assertRefused("x6", ".*InheritedBadImportJs extends"
                + " .*BadImportParent, which declares 'class' in"
                + " @JsDefinitionImport.*");
    }

    private void assertRefused(String id, String message) {
        run(id);
        assertThat(result(id)).hasText(Pattern
                .compile("^refused: IllegalArgumentException: " + message));
        System.out.println(id.toUpperCase() + ": "
                + result(id).textContent());
    }

    @Test
    void bundleCarriesOnlyReachedImports() throws IOException {
        String definitions = Files.readString(DEFINITIONS);
        assertTrue(definitions.contains("acme-format.js\")"),
                "acme-format.js imported by the definitions");
        assertTrue(definitions.contains("acme-label.js\")"),
                "acme-label.js imported by the definitions");
        assertTrue(definitions.contains("app-format.js\")"),
                "app-format.js imported by the definitions");
        assertTrue(definitions.contains("import(\"lit\")"),
                "lit imported by the definitions");
        assertFalse(definitions.contains("acme-unused-import.js"),
                "unreached UnusedImportJs in the definitions");
        assertEquals(USES_FIX, definitions.contains("acme-reflected-import.js"),
                "ReflectedImportJs in the definitions");

        assertFalse(buildContains("acme-unused-import-module"),
                "unreached acme-unused-import.js in the build");
        assertEquals(USES_FIX, buildContains("acme-reflected-import-module"),
                "acme-reflected-import.js in the build");
    }

    private static boolean buildContains(String text) throws IOException {
        try (Stream<Path> files = Files.walk(BUILD)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".js"))
                    .anyMatch(file -> {
                        try {
                            return Files.readString(file).contains(text);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Test
    void missingImportingDefinition() throws IOException {
        page.navigate(BASE_URL + "/missing-import");
        page.locator("#run").click();
        assertThat(page.locator("#result")).hasText("called");
        page.locator("#run").click();
        page.waitForTimeout(1000);

        List<String> errors = Files.readAllLines(SERVER_LOG).stream()
                .filter(l -> l.contains(MISSING_ERROR)).toList();
        if (USES_FIX) {
            assertThat(page.locator("#target")).hasText(
                    "Replaced by ReflectedImportJs (acme-reflected-import-module)");
            assertEquals(List.of(), errors);
            assertEquals(List.of(), consoleErrors);
        } else {
            assertThat(page.locator("#target")).hasText("Not replaced");
            assertEquals(1, errors.size(), "error lines in " + SERVER_LOG);
            assertTrue(errors.get(0).contains("ERROR") && errors.get(0)
                    .contains("@Uses(ReflectedImportJs.class)"),
                    errors.get(0));
            System.out.println("Server log: " + errors.get(0));
            System.out.println("Browser console errors: " + consoleErrors);
        }
    }
}
