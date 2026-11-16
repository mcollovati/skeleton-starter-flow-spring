package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.microsoft.playwright.Keyboard;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.KeyboardModifier;

/**
 * Checks {@link DomEventFilterView} in a browser while the application runs
 * in production mode with {@code vaadin.csp=strict}. Run with
 * {@code mvn -Pit verify}, which starts the application in the
 * pre-integration-test phase.
 * <p>
 * A negative check is followed by a positive one on the same listener, so the
 * count shows that the filtered event was not sent rather than not yet
 * processed. After each test, the page must have reported no Content Security
 * Policy violation and no console error.
 */
class DomEventFilterIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

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
            if ("error".equals(message.type())
                    || "warning".equals(message.type())) {
                consoleErrors.add(message.type() + ": " + message.text());
            }
        });
        page.onPageError(error -> consoleErrors.add("page error: " + error));
        page.addInitScript("""
                window.__cspViolations = [];
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI + ' ' + e.sourceFile + ':' + e.lineNumber));
                """);
        response = page.navigate(BASE_URL + "/dom-event-filters");
        assertThat(page.locator("#setup")).isAttached();
    }

    @AfterEach
    void noCspViolationsOrConsoleErrors() {
        try {
            Object violations = page.evaluate("() => window.__cspViolations");
            assertEquals(List.of(), violations, "CSP violations");
            assertEquals(List.of(), consoleErrors,
                    "console errors and warnings");
        } finally {
            context.close();
        }
    }

    @Test
    void strictPolicyIsSent() {
        String policy = response.headers().get("content-security-policy");
        assertNotNull(policy, "Content-Security-Policy header");
        assertTrue(policy.contains("script-src 'nonce-"), policy);
        assertFalse(policy.contains("unsafe-eval"), policy);
    }

    @Test
    void f1_filterOnTheEvent() {
        Locator input = page.locator("#f1-input");
        input.click();
        Keyboard keyboard = page.keyboard();
        keyboard.press("a");
        // A held key repeats: only the first keydown passes the filter
        keyboard.down("Enter");
        keyboard.down("Enter");
        keyboard.down("Enter");
        keyboard.up("Enter");
        assertThat(page.locator("#f1-out")).hasText("enter=1");
        keyboard.press("b");
        keyboard.press("Enter");
        assertThat(page.locator("#f1-out")).hasText("enter=2");
    }

    @Test
    void f2_filterOnTheElement() {
        page.locator("#f2-button").click();
        page.locator("#f2-enable").click();
        // The click after the round trip sees the new attribute
        assertThat(page.locator("#f2-button"))
                .hasAttribute("data-enabled", "true");
        page.locator("#f2-button").click();
        assertThat(page.locator("#f2-out")).hasText("clicks=1");
    }

    @Test
    void f3_filterWithEventData() {
        page.locator("#f3-button").click(new Locator.ClickOptions()
                .setModifiers(List.of(KeyboardModifier.SHIFT)));
        page.locator("#f3-button").click();
        assertThat(page.locator("#f3-out"))
                .hasText("clicks=1 detail=1 type=click");
    }

    @Test
    void f4_filterDecidesPreventDefault() {
        Locator checkbox = page.locator("#f4-checkbox");
        // Locked: the event is sent and the default is prevented
        checkbox.click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#f4-out")).hasText("locked clicks=1");
        assertFalse(checkbox.isChecked(), "checked while locked");

        page.locator("#f4-unlock").click();
        assertThat(checkbox).hasAttribute("data-locked", "no");
        // Unlocked: the filter does not match, so no event and the default
        // happens
        checkbox.click(new Locator.ClickOptions().setForce(true));
        assertTrue(checkbox.isChecked(), "not checked after unlock");
        page.waitForTimeout(500);
        assertThat(page.locator("#f4-out")).hasText("locked clicks=1");
    }

    @Test
    void f5_filterWithDebounce() {
        Locator input = page.locator("#f5-input");
        input.pressSequentially("ab");
        page.waitForTimeout(700);
        assertThat(page.locator("#f5-out")).hasText("-");
        input.pressSequentially("cd");
        assertThat(page.locator("#f5-out")).hasText("events=1 value=abcd");
    }

    @Test
    void f6_eventTypeFromAnotherPackage() {
        page.locator("#f6-button").click();
        page.locator("#f6-button").click(new Locator.ClickOptions()
                .setModifiers(List.of(KeyboardModifier.SHIFT)));
        assertThat(page.locator("#f6-out")).hasText("shift clicks=1");
    }

    @Test
    void f7_filterAndEventDataShareAnExpression() {
        page.locator("#f7-button").click();
        assertThat(page.locator("#f7-out")).hasText("primary clicks=1");
        assertThat(page.locator("#f7-data")).hasText("primary=true");
    }

    @Test
    void f8_keyUpEvent() {
        page.locator("#f8-input").click();
        page.keyboard().press("x");
        assertThat(page.locator("#f8-out")).hasText("keyups=1 key=x");
    }

    @Test
    void f9_runtimeStringFilterIsRefused() {
        assertThat(page.locator("#setup-F9"))
                .hasAttribute("data-outcome", "CspViolationException");
        assertThat(page.locator("#f9-outcome"))
                .hasText("setFilter: CspViolationException");
        // The refused filter is not set, but the listener stays registered
        page.locator("#f9-button").click();
        assertThat(page.locator("#f9-out")).hasText("clicks=1");
    }
}
