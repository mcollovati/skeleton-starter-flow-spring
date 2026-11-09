package org.vaadin.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks that the pushed counter keeps going after one pushed message is lost.
 * <p>
 * {@code drop-one-push.js} drops the 6th message the server pushes, so the
 * client asks the server to resynchronize. With {@code WEBSOCKET} the request
 * goes over the WebSocket and is never answered:
 * {@link #websocketRecoversFromALostMessage()} fails. The other two tests are
 * the controls and pass.
 * <p>
 * Run with {@code mvn verify -Pit}: the profile starts the app on port 8080.
 * Another base URL: {@code -Dit.base.url=http://localhost:9090/}.
 */
class PushResyncIT {

    /** Long enough for the client's resync request, which comes after ~5 s. */
    private static final double TIMEOUT_MS = 20_000;

    private static Playwright playwright;
    private static Browser browser;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void newPage() {
        page = browser.newPage();
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    @Test
    void websocketRecoversFromALostMessage() {
        assertCounterKeepsGoing("");
    }

    @Test
    void websocketWithoutALostMessage() {
        assertCounterKeepsGoing("?nodrop");
    }

    @Test
    void websocketXhrRecoversFromALostMessage() {
        assertCounterKeepsGoing("?transport=WEBSOCKET_XHR");
    }

    private void assertCounterKeepsGoing(String query) {
        page.navigate(baseUrl() + query);
        waitForCounter(3, query, "did not start");
        // The 6th pushed message is dropped. 15 is 6 s of ticks after it;
        // the timeout leaves room for the resync request and its answer.
        waitForCounter(15, query, "stopped");
    }

    private void waitForCounter(int target, String query, String problem) {
        try {
            page.waitForFunction(
                    "target => Number(document.getElementById('counter')"
                            + "?.textContent) >= target",
                    target,
                    new Page.WaitForFunctionOptions().setTimeout(TIMEOUT_MS));
        } catch (PlaywrightException e) {
            fail("'" + baseUrl() + query + "': the pushed counter " + problem
                    + " (now " + counter() + "). Connection state: "
                    + page.evaluate(
                            "() => window.Vaadin?.connectionState?.state"));
        }
    }

    private int counter() {
        return ((Number) page.evaluate(
                "() => Number(document.getElementById('counter')?.textContent)"))
                .intValue();
    }

    private static String baseUrl() {
        return System.getProperty("it.base.url", "http://localhost:8080/");
    }
}
