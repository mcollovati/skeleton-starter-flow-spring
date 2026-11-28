package org.vaadin.example.modulemissing;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * U9 (vaadin/flow#27425 with #27424): a reached module definition whose module
 * does not exist. Runs only with the {@code module-missing} profile:
 *
 * <pre>
 * rm -rf app/src/main/bundles
 * mvn clean -Pit,module-missing verify -pl app
 * </pre>
 */
class ModuleMissingIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");
    private static final Path FRONTEND_GENERATED = Path
            .of("src/main/frontend/generated");

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        assumeTrue("module-missing".equals(System.getProperty("it.extraCases")),
                "only with the module-missing profile");
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

    /** The chunk writer must not write a static import of the module. */
    @Test
    void noStaticImportOfTheMissingModule() throws IOException {
        try (Stream<Path> files = Files.walk(FRONTEND_GENERATED)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".js")
                            || file.toString().endsWith(".ts"))
                    .forEach(file -> {
                        try {
                            String text = Files.readString(file);
                            assertFalse(text.contains("import * as")
                                    && text.contains(
                                            "acme-module-does-not-exist"),
                                    "static import in " + file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    /** With #27424, the call throws on the server and names the module. */
    @Test
    void u9_callThrowsOnServer() {
        Page page = browser.newPage();
        page.navigate(BASE_URL + "/module-missing");
        page.locator("#u9-run").click();
        assertThat(page.locator("#u9-result")).hasText(
                Pattern.compile("failed: .*acme-module-does-not-exist.*"));
        System.out.println(
                "u9: " + page.locator("#u9-result").textContent());
        page.close();
    }
}
