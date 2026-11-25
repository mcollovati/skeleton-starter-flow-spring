package org.vaadin.example;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.Position;

/**
 * Checks, add-on by add-on, whether real add-ons from the Vaadin Directory
 * work in the application that runs with {@code vaadin.contentSecurityPolicy=strict}, with the
 * fallbacks in {@code org.vaadin.example.ecosystem.fallbacks}.
 * <p>
 * Every test asserts that the add-on WORKS: no {@code ContentSecurityPolicyViolationException}
 * in Java (shown in {@code #message}), the effect of the add-on is visible in
 * the browser, and the page reports no Content Security Policy violation and
 * logs no error. So failures list what still breaks. The maplibre test is the
 * exception: a fallback does not support it, so it asserts the refusal.
 * <p>
 * Run with {@code mvn -Pit verify}, which starts the application in
 * production mode (Flow sends the CSP header only there).
 */
class EcosystemCspIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl",
            "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private final List<String> consoleErrors = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        // Uses the installed Chrome, so no browser is downloaded
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setChannel("chrome"));
        PlaywrightAssertions.setDefaultAssertionTimeout(10000);
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
        page.addInitScript("""
                window.__cspViolations = [];
                document.addEventListener('securitypolicyviolation', (e) =>
                    window.__cspViolations.push(e.violatedDirective + ' '
                        + e.blockedURI + ' ' + e.sourceFile + ':' + e.lineNumber));
                """);
    }

    @AfterEach
    void closePage() {
        try {
            // Give late scripts (detach, timers) a moment to fail
            page.waitForTimeout(500);
            Object violations = page.evaluate("() => window.__cspViolations");
            assertEquals(List.of(), violations,
                    "the browser reported CSP violations");
            assertEquals(List.of(), consoleErrors,
                    "the browser logged errors");
        } finally {
            context.close();
        }
    }

    @Test
    @DisplayName("flow-viritin: RichText, VScroller, WebStorage")
    void viritin() {
        open("viritin");
        check(() -> {
            assertThat(page.locator("#rich b")).hasText("Viritin");
            page.locator("#scroll").click();
            waitFor("() => document.querySelector('#scroller').scrollTop > 1000",
                    "the scroller did not scroll");
            assertThat(page.locator("#end")).hasText("end reached");
            page.locator("#store").click();
            assertThat(page.locator("#stored")).hasText("works");
        });
    }

    @Test
    @DisplayName("flow-viritin: MarkdownMessage")
    void viritinMarkdown() {
        open("viritin-markdown");
        check(() -> assertThat(page.locator("#markdown h1"))
                .hasText("Hello Markdown"));
    }

    @Test
    @DisplayName("vaadin-ckeditor: setValue, setReadOnly, insertText")
    void ckeditor() {
        open("ckeditor");
        check(() -> {
            Locator editable = page.locator("#editor .ck-editor__editable");
            assertThat(editable).isVisible();
            page.locator("#set-value").click();
            assertThat(editable).containsText("Hello CSP");
            page.locator("#insert").click();
            assertThat(editable).containsText("world");
            page.locator("#read-only").click();
            assertThat(editable).hasAttribute("contenteditable", "false");
        });
    }

    @Test
    @DisplayName("sortable-layout: drag C above A")
    void sortable() {
        open("sortable");
        check(() -> {
            Locator a = page.locator("#item-A");
            Locator c = page.locator("#item-C");
            assertThat(c).isVisible();
            // SortableJS uses native drag and drop, which dragTo sends
            c.dragTo(a, new Locator.DragToOptions()
                    .setTargetPosition(new Position(10, 2)));
            assertThat(page.locator("#order")).hasText("C,A,B");
        });
    }

    @Test
    @DisplayName("hugerte-for-flow: setting the old value again after typing")
    void hugerte() {
        open("hugerte");
        check(() -> {
            FrameLocator frame = page.frameLocator("#editor iframe");
            Locator body = frame.locator("body");
            assertThat(body).hasText("Original");
            body.click();
            page.keyboard().press("End");
            page.keyboard().type(" typed");
            // The editor sends its value on change, i.e. when it loses focus
            page.locator("h2").click();
            // Wait until the server has the typed value
            assertThat(page.locator("#value")).containsText("typed");
            page.locator("#reset").click();
            assertThat(body).hasText("Original");
        });
    }

    @Test
    @DisplayName("super-fields: read-only GridSelect, SuperTextField input mode")
    void superfields() {
        open("superfields");
        check(() -> {
            Locator beta = page.locator(
                    "#grid-select vaadin-grid-cell-content",
                    new Page.LocatorOptions().setHasText("Beta"));
            assertThat(beta).isVisible();
            beta.click();
            page.waitForTimeout(500);
            assertEquals(0, page.evaluate(
                    "() => document.querySelector('#grid-select vaadin-grid').selectedItems.length"),
                    "the read-only grid selected the clicked row");
            assertThat(page.locator("#text input")).hasAttribute("inputmode",
                    "numeric");
        });
    }

    @Test
    @DisplayName("super-fields: GridMultiSelect")
    void superfieldsMulti() {
        open("superfields-multi");
        check(() -> {
            Locator alpha = page.locator("#multi vaadin-grid-cell-content",
                    new Page.LocatorOptions().setHasText("Alpha"));
            assertThat(alpha).isVisible();
            // In multi selection a row is selected with its checkbox (the
            // first one is "select all")
            page.locator("#multi vaadin-grid-cell-content:has(vaadin-checkbox)")
                    .nth(1).click();
            assertThat(page.locator("#value")).hasText("Alpha");
        });
    }

    @Test
    @DisplayName("EnhancedTabs: pick a tab in the overflow menu")
    void enhancedTabs() {
        open("enhanced-tabs");
        check(() -> {
            assertThat(page.locator("#selected")).hasText("Tab 1");
            Locator overflow = page
                    .locator("#tabs vaadin-menu-bar-button[slot='overflow']");
            assertThat(overflow).isVisible();
            // The menu opens on hover; a click would close it again
            overflow.hover();
            Locator tab6 = page.locator("vaadin-tab:visible",
                    new Page.LocatorOptions().setHasText("Tab 6"));
            tab6.click();
            assertThat(page.locator("#selected")).hasText("Tab 6");
            assertThat(page.locator(
                    "vaadin-menu-bar-submenu[opened], vaadin-menu-bar-overlay[opened]"))
                    .hasCount(0);
        });
    }

    @Test
    @DisplayName("TwinColGrid: no selection column, click a row and add it")
    void twinColGrid() {
        open("twincolgrid");
        check(() -> {
            Locator beta = page.locator("#twin vaadin-grid-cell-content",
                    new Page.LocatorOptions().setHasText("Beta"));
            assertThat(beta.first()).isVisible();
            waitFor("""
                    () => {
                      const columns = [...document.querySelectorAll(
                          '#twin vaadin-grid-flow-selection-column')];
                      return columns.length > 0 && columns.every(c => c.hidden);
                    }""", "the selection columns are not hidden");
            beta.first().click();
            page.locator("#twin vaadin-button:has(vaadin-icon[icon='vaadin:angle-right'])")
                    .click();
            assertThat(page.locator("#value")).hasText("Beta");
        });
    }

    @Test
    @DisplayName("MarkdownEditor: MarkdownViewer shown and removed")
    void markdownViewer() {
        open("markdown");
        check(() -> {
            assertThat(page.locator("#viewer h1")).hasText("Hello CSP");
            page.locator("#remove").click();
            assertThat(page.locator("#removed")).hasText("removed");
            assertNoMessage();
        });
    }

    @Test
    @DisplayName("ImageCrop: setCrop draws the selection")
    void imageCrop() {
        open("image-crop");
        check(() -> assertThat(
                page.locator("#crop .ReactCrop__crop-selection")).isVisible());
    }

    @Test
    @DisplayName("Zoomist: getContainerData")
    void zoomist() {
        open("zoomist");
        check(() -> {
            assertThat(page.locator("#zoomist img").first()).isVisible();
            page.locator("#container-data").click();
            assertThat(page.locator("#width"))
                    .hasText(Pattern.compile("width=[1-9]\\d*"));
        });
    }

    @Test
    @DisplayName("XTermConsoleAddon: writeln")
    void xterm() {
        open("xterm");
        check(() -> assertThat(page.locator("#term .xterm-rows"))
                .containsText("Hello CSP"));
    }

    @Test
    @DisplayName("YearMonthCalendarAddon: class name generator")
    void yearMonthCalendar() {
        open("year-month-calendar");
        check(() -> assertThat(page.locator("#calendar .holiday"))
                .hasText("15"));
    }

    @Test
    @DisplayName("ChatAssistant: open the chat window with a message")
    void chatAssistant() {
        open("chat-assistant");
        check(() -> {
            page.locator("#open").click();
            assertThat(page.getByText("Hello CSP")).isVisible();
        });
    }

    @Test
    @DisplayName("ExtendedLoginAddon: replaced header, no forgot password")
    void extendedLogin() {
        open("extended-login");
        check(() -> {
            assertThat(page.locator("#brand")).isVisible();
            assertThat(page.locator("#brand")).hasText("ACME CSP");
            assertThat(page.locator("vaadin-login-form-wrapper [slot='forgot-password']"))
                    .hasCount(0);
        });
    }

    @Test
    @DisplayName("ChipFieldAddon: chips for the value")
    void chipField() {
        open("chipfield");
        check(() -> {
            Locator chips = page.locator("#chips paper-chip");
            assertThat(chips).hasCount(2);
            assertThat(chips.nth(0)).containsText("Java");
            assertThat(chips.nth(1)).containsText("Kotlin");
        });
    }

    @Test
    @DisplayName("OrgChartAddon: chart shown, add a child")
    void orgChart() {
        open("orgchart");
        check(() -> {
            assertThat(page.locator("#chart .node",
                    new Page.LocatorOptions().setHasText("John Williams")))
                    .isVisible();
            assertThat(page.locator("#chart .node",
                    new Page.LocatorOptions().setHasText("Anna Thompson")))
                    .isVisible();
            page.locator("#add").click();
            assertThat(page.locator("#chart .node",
                    new Page.LocatorOptions().setHasText("Mark Brown")))
                    .isVisible();
        });
    }

    @Test
    @DisplayName("vaadin-fullcalendar: defaults (browser-only new Function expected)")
    void fullCalendar() {
        open("fullcalendar");
        check(() -> assertThat(page.locator("#calendar .fc-daygrid-day").first())
                .isVisible());
    }

    @Test
    @DisplayName("vaadin-fullcalendar: select days and clear the selection")
    void fullCalendarSelect() {
        open("fullcalendar-select");
        check(() -> {
            Locator days = page.locator("#calendar .fc-daygrid-day");
            assertThat(days.first()).isVisible();
            dragFromTo(days.nth(8), days.nth(10));
            assertThat(page.locator("#selected")).hasText("selected");
            assertThat(page.locator("#calendar .fc-highlight").first())
                    .isVisible();
            page.locator("#clear").click();
            assertThat(page.locator("#calendar .fc-highlight")).hasCount(0);
        });
    }

    /**
     * Not supported: the map setup is a script built at runtime, so Flow
     * refuses it and no fallback can match it.
     */
    @Test
    @DisplayName("maplibre: refused, not supported")
    void maplibre_refusedAsNotSupported() {
        // Flow refuses the map setup while the view is attached, so the
        // page shows Vaadin's error with the message instead of the view
        page.navigate(BASE_URL + "/eco/maplibre");
        assertThat(page.locator("body")).containsText(
                "JavaScript is sent to the browser as a string");
        // The add-on also adds its stylesheet from unpkg.com, which the
        // policy (style-src 'self') blocks: more evidence, not checked
        page.waitForTimeout(500);
        page.evaluate("() => window.__cspViolations = []");
        consoleErrors.clear();
    }

    private void open(String view) {
        page.navigate(BASE_URL + "/eco/" + view);
        try {
            page.locator("h2").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            fail("the view did not open: " + bodyText());
        }
    }

    /**
     * Runs the assertions of a test and, if they fail, adds what the page
     * shows: the message of a refused call or Vaadin's error.
     */
    private void check(Runnable assertions) {
        try {
            assertNoMessage();
            assertions.run();
        } catch (AssertionError | PlaywrightException e) {
            fail(firstLine(e.getMessage()) + "\n--- page: " + bodyText(), e);
        }
    }

    private void assertNoMessage() {
        Locator message = page.locator("#message");
        if (message.count() > 0 && message.isVisible()) {
            fail("refused in Java: " + message.innerText());
        }
    }

    private String bodyText() {
        try {
            String text = page.locator("body").innerText().strip();
            return text.length() > 1500 ? text.substring(0, 1500) + "..."
                    : text;
        } catch (PlaywrightException e) {
            return "(no page text: " + e.getMessage() + ")";
        }
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int end = text.indexOf("\n", 200);
        return end < 0 ? text : text.substring(0, end);
    }

    private void waitFor(String condition, String message) {
        try {
            page.waitForFunction(condition, null,
                    new Page.WaitForFunctionOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            fail(message + " within 10 s");
        }
    }

    private void dragFromTo(Locator source, Locator target) {
        var from = source.boundingBox();
        var to = target.boundingBox();
        page.mouse().move(from.x + from.width / 2, from.y + from.height / 2);
        page.mouse().down();
        page.mouse().move(to.x + to.width / 2, to.y + to.height / 2,
                new com.microsoft.playwright.Mouse.MoveOptions().setSteps(10));
        page.mouse().up();
    }
}
