package org.vaadin.example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.DriverSupplier;
import com.vaadin.testbench.TestBench;

/**
 * Shows that the Flow clients stay "active" after the server answers a request
 * with the "Internal error" notification, so that TestBench's
 * {@code waitForVaadin()} waits for its full 40 s limit before every command.
 * <p>
 * Every test asserts the expected behavior, so every test FAILS while the bug
 * is present. Run with {@code mvn -Pit verify}, which starts the application
 * in the pre-integration-test phase.
 */
class AppErrorIT extends BrowserTestBase implements DriverSupplier {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    /**
     * The same check as TestBench's {@code waitForVaadin()}, but returning the
     * state of each client instead of one boolean.
     */
    private static final String CLIENT_STATES = """
            const clients = (window.Vaadin && window.Vaadin.Flow
                && window.Vaadin.Flow.clients) || {};
            const states = {};
            for (const name in clients) {
              states[name] = clients[name].isActive();
            }
            return states;
            """;

    /** Long enough for the ROOT client's resend timer (5 s + 500 ms). */
    private static final Duration WATCH_TIME = Duration.ofSeconds(15);

    @Override
    public WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox",
                "--window-size=1280,800");
        return TestBench.createDriver(new ChromeDriver(options));
    }

    @BeforeEach
    void openView() {
        getDriver().get(BASE_URL + "/app-error");
        $(ButtonElement.class).id("arm").click();
    }

    @BrowserTest
    void rootClientInactiveAfterInternalError() {
        $(ButtonElement.class).id("send").click();
        waitForSystemError();

        assertClientsBecomeInactive();
    }

    @BrowserTest
    void typeScriptClientInactiveAfterFailedNavigation() {
        // The click itself must not wait: the TypeScript client is active
        // from the moment the navigation starts
        testBench().disableWaitForVaadin();
        $(AnchorElement.class).id("navigate").click();
        waitForSystemError();

        assertClientsBecomeInactive();
    }

    @BrowserTest
    void waitForVaadinReturnsAfterInternalError() {
        $(ButtonElement.class).id("send").click();
        waitForSystemError();
        // Give the ROOT client time to resend the failed request
        sleep(WATCH_TIME.dividedBy(2));

        assertNextCommandIsQuick();
    }

    @BrowserTest
    void waitForVaadinReturnsAfterFailedNavigation() {
        testBench().disableWaitForVaadin();
        $(AnchorElement.class).id("navigate").click();
        waitForSystemError();
        testBench().enableWaitForVaadin();

        assertNextCommandIsQuick();
    }

    /**
     * Waits until the browser shows the "Internal error" notification, without
     * waiting for the clients.
     */
    private void waitForSystemError() {
        testBench().disableWaitForVaadin();
        waitUntil(driver -> !driver
                .findElements(By.className("v-system-error")).isEmpty(), 10);
        WebElement error = findElement(By.className("v-system-error"));
        assertTrue(error.getText().contains("Internal error"),
                "Unexpected notification: " + error.getText());
    }

    /**
     * Watches {@code isActive()} of every client, without waiting for them,
     * and fails if a client is still active at the end.
     */
    private void assertClientsBecomeInactive() {
        testBench().disableWaitForVaadin();
        long start = System.currentTimeMillis();
        Map<String, Boolean> states = Map.of();
        List<String> changes = new ArrayList<>();
        Map<String, Boolean> previous = new LinkedHashMap<>();
        while (System.currentTimeMillis() - start < WATCH_TIME.toMillis()) {
            states = clientStates();
            long elapsed = System.currentTimeMillis() - start;
            states.forEach((name, active) -> {
                if (!active.equals(previous.put(name, active))) {
                    changes.add(String.format("%.1f s: %s isActive() = %s",
                            elapsed / 1000.0, name, active));
                }
            });
            sleep(Duration.ofMillis(250));
        }
        List<String> active = states.entrySet().stream()
                .filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
        Assertions.assertEquals(List.of(), active,
                "Clients still active " + WATCH_TIME.toSeconds()
                        + " s after the internal error. Changes after the"
                        + " error:\n  " + String.join("\n  ", changes));
    }

    /**
     * Times one TestBench command, which runs {@code waitForVaadin()} first,
     * and fails if it takes longer than a few seconds.
     */
    private void assertNextCommandIsQuick() {
        testBench().enableWaitForVaadin();
        long start = System.currentTimeMillis();
        findElement(By.tagName("body"));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 5000, "One TestBench command took " + elapsed
                + " ms, because waitForVaadin() waited for its 40 s limit."
                + " Client states: " + clientStates());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> clientStates() {
        testBench().disableWaitForVaadin();
        return (Map<String, Boolean>) executeScript(CLIENT_STATES);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
