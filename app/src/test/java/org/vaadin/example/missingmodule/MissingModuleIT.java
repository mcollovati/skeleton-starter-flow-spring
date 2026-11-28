package org.vaadin.example.missingmodule;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Checks vaadin/flow#27424 (unresolved modules of {@code @JsDefinitionImport})
 * with {@link MissingModuleView}. Runs only with the {@code missing-module}
 * profile:
 *
 * <pre>
 * rm -rf app/src/main/bundles
 * mvn clean -Pit,missing-module verify -pl app
 * </pre>
 *
 * Today the build stops in Vite, so nothing here runs. The {@code u*} tests
 * also run against development mode
 * ({@code mvn spring-boot:run -Pmissing-module -pl app}, then
 * {@code mvn -Pit,missing-module failsafe:integration-test -pl app '-Dit.test=MissingModuleIT#u*'}).
 */
class MissingModuleIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path SERVER_LOG = Path
            .of(System.getProperty("it.serverLog", "target/server.log"));
    private static final Path BUILD = Path
            .of("target/classes/META-INF/VAADIN/webapp/VAADIN/build");
    private static final Path STATS = Path
            .of("target/classes/META-INF/VAADIN/config/stats.json");
    private static final Path IDS = Path
            .of("src/main/frontend/generated/vaadin-js-definition-ids.json");

    /** The modules of the reached definitions that can not be resolved. */
    private static final List<String> MISSING = List.of(
            "acme-does-not-exist.js", "acme-child-missing.js",
            "acme-not-installed-package", "acme-page-missing.js");

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        assumeTrue("missing-module".equals(System.getProperty("it.extraCases")),
                "only with the missing-module profile");
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
        page.navigate(BASE_URL + "/missing-module");
        page.locator("#u0-run").waitFor();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    private Locator click(String id) {
        page.locator("#" + id + "-run").click();
        return page.locator("#" + id + "-box");
    }

    private Locator result(String id) {
        return page.locator("#" + id + "-result");
    }

    /**
     * The call threw on the server, the n-th time, with a message that names
     * the module. The exception class is not fixed by the issue.
     */
    private void assertThrewOnServer(String id, int count, String module) {
        // Playwright runs the pattern as a JavaScript regular expression,
        // which has no \Q...\E, so Pattern.quote can not be used
        String escaped = module.replaceAll("[.*+?^${}()|\\[\\]\\\\]",
                "\\\\$0");
        assertThat(result(id)).hasText(Pattern.compile(
                "failed " + count + ": .*" + escaped + ".*", Pattern.DOTALL));
        System.out.println(id + ": " + result(id).textContent());
    }

    private void assertNoConsoleErrors() {
        page.waitForTimeout(300);
        assertEquals(List.of(), consoleErrors, "the browser logged errors");
    }

    @Test
    void u0_resolvedDefinitionNextToUnresolvedOnes() {
        Locator box = click("u0");
        assertThat(box).hasText("{zero}");
        assertNoConsoleErrors();
    }

    @Test
    void u1_unresolvedProjectFileThrowsOnServer() {
        Locator box = click("u1");
        assertThrewOnServer("u1", 1, "acme-does-not-exist.js");
        assertThat(box).hasText("box u1");
        assertNoConsoleErrors();
    }

    @Test
    void u2_throwsOnEveryCall() {
        click("u1");
        assertThrewOnServer("u1", 1, "acme-does-not-exist.js");
        click("u1");
        assertThrewOnServer("u1", 2, "acme-does-not-exist.js");
        assertNoConsoleErrors();
    }

    @Test
    void u3_pendingResultThrowsAtTheCall() {
        click("u3");
        assertThrewOnServer("u3", 1, "acme-does-not-exist.js");
        assertNoConsoleErrors();
    }

    @Test
    void u4_methodWithoutExpressionThrows() {
        Locator box = click("u4");
        assertThrewOnServer("u4", 1, "acme-does-not-exist.js");
        assertThat(box).not().isFocused();
        assertNoConsoleErrors();
    }

    @Test
    void u5_inheritedResolvedMethodWorks() {
        click("u5-parent");
        assertThat(result("u5-parent")).hasText("value {five}");
        click("u5-child");
        assertThrewOnServer("u5-child", 1, "acme-child-missing.js");
        assertNoConsoleErrors();
    }

    @Test
    void u6_npmPackageNotInstalledThrows() {
        click("u6");
        assertThrewOnServer("u6", 1, "acme-not-installed-package");
        assertNoConsoleErrors();
    }

    @Test
    void u7_pageExecuteJsThrows() {
        click("u7");
        assertThrewOnServer("u7", 1, "acme-page-missing.js");
        assertNoConsoleErrors();
    }

    // Production only: what the build wrote, and the server log

    @Test
    void buildRecordsUnresolvedModules() throws IOException {
        String stats = Files.readString(STATS);
        String ids = Files.readString(IDS);
        for (String module : MISSING) {
            assertTrue(stats.contains(module), module + " not in " + STATS);
            assertTrue(ids.contains(module), module + " not in " + IDS);
        }
    }

    @Test
    void bundleDoesNotNameTheCause() throws IOException {
        for (String text : Stream.concat(MISSING.stream(),
                Stream.of("acme-unreached-missing", "MissingModuleJs",
                        "ChildMissingJs", "MissingPackageJs",
                        "PageMissingJs"))
                .toList()) {
            assertFalse(buildContains(text), text + " is in the bundle");
        }
    }

    /**
     * The #27078 check must not tell to add {@code @Uses}: the definition is
     * reached, the module is what is missing.
     */
    @Test
    void serverLogDoesNotAdviseUses() throws IOException {
        click("u1");
        click("u5-child");
        click("u7");
        page.waitForTimeout(1000);
        List<String> advice = Files.readAllLines(SERVER_LOG).stream()
                .filter(line -> line.contains("@Uses(")
                        && line.contains("Missing"))
                .toList();
        assertEquals(List.of(), advice, "lines in " + SERVER_LOG);
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
