package org.vaadin.example.modules;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * Checks vaadin/flow#27425 ({@code JsModuleInvokers},
 * {@code Page.executeModuleJs}) with the views of the
 * {@code module-invokers} profile. Needs Flow with that change:
 *
 * <pre>
 * rm -rf app/src/main/bundles
 * mvn install -DskipTests -Pmodule-invokers -pl addon
 * mvn clean -Pit,module-invokers verify -pl app
 * </pre>
 *
 * The browser tests also run against development mode
 * ({@code mvn spring-boot:run -Pmodule-invokers -pl app}, then
 * {@code mvn -Pit,module-invokers failsafe:integration-test -pl app -Dit.test=ModuleCallsIT -Dit.devMode=true}).
 * Tests of what the production build wrote are then skipped.
 */
class ModuleCallsIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final boolean DEV_MODE = Boolean
            .getBoolean("it.devMode");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));
    private static final Path BUILD = Path
            .of("target/classes/META-INF/VAADIN/webapp/VAADIN/build");

    /** The text of the warning that a call loads its module itself. */
    private static final String FALLBACK = "since no view shown so far"
            + " loaded it";

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        assumeTrue(Boolean.getBoolean("it.moduleInvokers"),
                "only with the module-invokers profile");
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
    void newPage() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("warning".equals(message.type())) {
                warnings.add(message.text());
            } else if ("error".equals(message.type())) {
                errors.add(message.text());
            }
        });
        page.onPageError(error -> errors.add("page error: " + error));
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    private void open(String path) {
        page.navigate(BASE_URL + "/" + path);
        page.locator("h1").first().waitFor();
    }

    private Locator box(String id) {
        return page.locator("#" + id + "-box");
    }

    /** Clears what initLazy and the calls after it left on the box. */
    private void reset(String id) {
        page.evaluate("id => { const box = document.getElementById(id);"
                + " delete box.$connector; box.dataset.order = ''; }",
                id + "-box");
    }

    private void assertOrder(String id, String order) {
        assertThat(box(id)).hasAttribute("data-order", order);
    }

    private List<String> fallbackWarnings() {
        page.waitForTimeout(300);
        return warnings.stream().filter(text -> text.contains(FALLBACK))
                .toList();
    }

    /** The calls did not load a module themselves. */
    private void assertNoFallback() {
        assertEquals(List.of(), fallbackWarnings(),
                "a call loaded its module itself");
    }

    @SuppressWarnings("unchecked")
    private List<String> registryKeys() {
        return (List<String>) page.evaluate(
                "() => Object.keys(window.Vaadin?.Flow?.jsDefinitionModules"
                        + " ?? {})");
    }

    private boolean registered(String file) {
        return registryKeys().stream().anyMatch(key -> key.contains(file));
    }

    private Object evals(String module) {
        return page.evaluate("m => (window.__moduleEvals ?? {})[m] ?? 0",
                module);
    }

    // Order of the calls that the view makes on attach

    @Test
    void m1_moduleOfTheProject_callsRunInOrder() {
        open("module-calls");
        assertOrder("m1", "init after:ready");
        assertNoFallback();
        System.out.println("registry keys: " + registryKeys());
        assertTrue(registered("app-module.js"), "keys " + registryKeys());
    }

    @Test
    void m1_moduleOfAnotherViewIsNotLoaded() {
        assumeFalse(DEV_MODE, "development mode registers every module");
        open("module-calls");
        assertOrder("m1", "init after:ready");
        assertFalse(registered("app-module-other.js"),
                "keys " + registryKeys());
        assertEquals(0, ((Number) evals("app-module-other")).intValue());
    }

    @Test
    void m2_routerNavigation_callsRunInOrder() {
        open("module-start");
        page.locator("#to-calls").click();
        assertOrder("m1", "init after:ready");
        assertOrder("m5", "init after:ready");
        assertOrder("m11", "init after:ready");
        assertNoFallback();
    }

    @Test
    void m3_twoRoundTrips() {
        open("module-calls");
        page.locator("#m3-init").click();
        assertOrder("m3", "init");
        page.locator("#m3-record").click();
        assertOrder("m3", "init after:ready");
        assertNoFallback();
    }

    @Test
    void m4_attachedInAClick_callsRunInOrder() {
        open("module-calls");
        page.locator("#m4-run").click();
        assertOrder("m4", "init after:ready");
        page.locator("#m4-run").click();
        assertOrder("m4", "init after:ready");
        assertNoFallback();
    }

    @Test
    void m5_addOnModuleInTheJar_callsRunInOrder() {
        open("module-calls");
        assertOrder("m5", "init after:ready");
        assertNoFallback();
        assertTrue(registered("acme-module.js"), "keys " + registryKeys());
    }

    @Test
    void m6_npmPackage_rendersBeforeTheCheck() {
        open("module-calls");
        assertOrder("m6", "rendered");
        assertThat(box("m6").locator("b.m6")).hasText("six");
        assertNoFallback();
    }

    @Test
    void m7_returnValue() {
        open("module-calls");
        page.locator("#m7-run").click();
        assertThat(page.locator("#m7-result")).hasText("value <seven>");
        assertNoFallback();
    }

    @Test
    void m8_namedImportNextToTheModule() {
        open("module-calls");
        page.locator("#m8-run").click();
        assertThat(page.locator("#m8-result")).hasText("value {<eight>}");
        assertNoFallback();
        System.out.println("registry keys: " + registryKeys());
    }

    @Test
    void m9_jsModuleAndImportOfOneFile_evaluatedOnce() {
        open("module-calls");
        assertOrder("m9", "init after:ready");
        assertEquals(1, ((Number) evals("app-module-side")).intValue());
        assertNoFallback();
    }

    @Test
    void m10_moduleOfTwoViews_evaluatedOnceAndShared() {
        open("module-calls");
        page.locator("#m10-run").click();
        assertThat(page.locator("#m10-result")).hasText("value 1");
        page.locator("#to-other").click();
        page.locator("#o10-run").click();
        assertThat(page.locator("#o10-result")).hasText("value 2");
        assertEquals(1, ((Number) evals("app-module-shared")).intValue());
        assertNoFallback();
    }

    @Test
    void m11_inheritedModule() {
        open("module-calls");
        assertOrder("m11", "init after:ready");
        assertNoFallback();
    }

    @Test
    void m12_moduleWithAQuery_registryKeyMatches() {
        open("module-calls");
        assertOrder("m12", "init after:ready");
        assertNoFallback();
    }

    /**
     * Fails on #27493 73da7a8d1f: the chunk writer checks that
     * {@code ./app-module-noext} exists as written, finds no such file and
     * writes no registration. Vite resolves the import, so the call falls
     * back on every page load.
     */
    @Test
    void m13_moduleWithoutExtension_registryKeyMatches() {
        open("module-calls");
        page.locator("#m13-run").click();
        System.out.println("M13 order: " + box("m13").getAttribute(
                "data-order") + ", registry keys " + registryKeys());
        assertOrder("m13", "init after:ready");
        assertNoFallback();
    }

    /**
     * An element call is queued when the response is written, a page call at
     * once. So an element call made before the module call runs after it, as
     * with Page.executeJs today. Checks what happens, the issue does not
     * decide it.
     */
    @Test
    void m14_elementCallMadeBefore_runsAfterTheModuleCall() {
        open("module-calls");
        page.locator("#m14-run").click();
        page.waitForTimeout(500);
        System.out.println("M14 order: " + box("m14").getAttribute(
                "data-order"));
        assertOrder("m14", "init before:ready after:ready");
        assertNoFallback();
    }

    @Test
    void m_noBrowserErrorsOnAttach() {
        open("module-calls");
        assertOrder("m1", "init after:ready");
        page.waitForTimeout(300);
        assertEquals(List.of(), errors);
    }

    // Fallback

    @Test
    void f1_moduleOfAnotherView_loadedByTheCallOnceWithAWarning() {
        assumeFalse(DEV_MODE, "development mode registers every module");
        open("module-calls");
        page.locator("#f1-run").click();
        // The call loads the module, so the call after it runs first
        assertOrder("f1", "after:missing init");
        reset("f1");
        page.locator("#f1-run").click();
        assertOrder("f1", "init after:ready");
        reset("f1");
        page.locator("#f1-run").click();
        assertOrder("f1", "init after:ready");

        List<String> fallbacks = fallbackWarnings();
        System.out.println("F1 warnings: " + fallbacks);
        assertEquals(1, fallbacks.size(), "warnings " + fallbacks);
        assertTrue(fallbacks.get(0).contains("app-module-other"),
                fallbacks.get(0));
        // A production bundle must not name Java classes
        assertFalse(fallbacks.get(0).contains("OtherOnlyJs"));
        assertFalse(fallbacks.get(0).contains("org.vaadin"));
    }

    @Test
    void f1_afterTheOtherViewWasShown_callsRunInOrder() {
        open("module-other");
        assertOrder("o1", "init after:ready");
        page.locator("#to-calls").click();
        page.locator("#f1-run").click();
        assertOrder("f1", "init after:ready");
        assertNoFallback();
    }

    @Test
    void f2_definitionNoRouteReaches() {
        open("module-calls");
        page.locator("#f2-run").click();
        page.waitForTimeout(1000);
        System.out.println("F2 order: " + box("f2").getAttribute("data-order")
                + ", warnings " + warnings + ", errors " + errors);
        if (DEV_MODE) {
            assertOrder("f2", "init after:ready");
            return;
        }
        // vaadin/flow#27078: the bundle has no such function, the server
        // logs an error
        assertThat(box("f2")).hasAttribute("data-order", "after:missing");
    }

    @Test
    void f2_serverLogNamesTheDefinition() throws IOException {
        assumeFalse(DEV_MODE, "production only");
        open("module-calls");
        page.locator("#f2-run").click();
        page.waitForTimeout(1000);
        List<String> lines = Files.readAllLines(SERVER_LOG).stream()
                .filter(line -> line.contains("UnreachedModuleJs")).toList();
        System.out.println("F2 server log: " + lines);
        assertFalse(lines.isEmpty(), "no line names UnreachedModuleJs");
    }

    // Rules

    private void assertRefused(String id) {
        page.locator("#" + id + "-run").click();
        assertThat(page.locator("#" + id + "-result"))
                .hasText(Pattern.compile("refused: .*"));
        System.out.println(id + ": " + page.locator("#" + id + "-result")
                .textContent());
    }

    @Test
    void r1_onlyANamedImport_refused() {
        open("module-calls");
        assertRefused("r1");
    }

    @Test
    void r2_twoImportsWithoutNames_refused() {
        open("module-calls");
        assertRefused("r2");
    }

    @Test
    void r3_twoMarkers_refused() {
        open("module-calls");
        assertRefused("r3");
    }

    /** Not decided by the issue: records what happens. */
    @Test
    void r4_childWithOwnMethodAndNoImport() {
        open("module-calls");
        page.locator("#r4-run").click();
        page.waitForTimeout(1000);
        System.out.println("r4: " + page.locator("#r4-result").textContent());
        assertThat(page.locator("#r4-result")).not().isEmpty();
    }

    @Test
    void r5_moduleAndSnippets_refused() {
        open("module-calls");
        assertRefused("r5");
    }

    @Test
    void r6_missingExport_failsInTheBrowser() {
        open("module-calls");
        page.locator("#r6-run").click();
        assertThat(page.locator("#r6-result")).hasText("called");
        page.waitForTimeout(500);
        System.out.println("r6 errors: " + errors);
        assertFalse(errors.isEmpty(), "no browser error");
    }

    @Test
    void r7_noJsDefinition_refused() {
        open("module-calls");
        assertRefused("r7");
    }

    // What the production build wrote

    @Test
    void build_registersReachedModulesOnly() throws IOException {
        assumeFalse(DEV_MODE, "production only");
        assertTrue(buildContains("jsDefinitionModules"));
        // The unreached definition and its module are left out
        assertFalse(buildContains("app-module-unreached"),
                "app-module-unreached is in the bundle");
        // The rule rows are reached but refused: their modules are those of
        // other rows, so nothing to check for them
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
}
