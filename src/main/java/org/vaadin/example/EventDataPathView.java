package org.vaadin.example;

import java.util.Optional;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#26999: {@code addEventDataPath(String)} and
 * {@code addEventDataElementPath(String)}, the deprecated string methods and
 * the bean-based methods that now check their paths.
 * <p>
 * The application runs with {@code vaadin.csp=strict}, so the browser must not
 * compile any event data, and the server refuses event data that the browser
 * would compile.
 * <p>
 * Two kinds of cases:
 * <ul>
 * <li>Setup cases run when the view is created. Each one is a row in the
 * {@code #setup} list that says {@code OK} or the name of the exception.</li>
 * <li>Click cases write what the server got to a {@code #result-<case>}
 * span.</li>
 * </ul>
 */
@Route("event-data-path")
public class EventDataPathView extends Div {

    /**
     * Strings that are not property paths. Both new methods must throw
     * {@link IllegalArgumentException} for each of them.
     */
    static final String[] NOT_PATHS = { "event.detail + 1", "event.detail[0]",
            "event['detail']", "", " event.detail", "detail.value",
            "window.location", "this.value", "event..detail", "event.detail.",
            "event.target.closest('div')", "event.größe" };

    /**
     * Edge cases that are property paths. Both new methods must accept them.
     */
    static final String[] PATHS = { "event", "element", "event?.detail",
            "element.$x_1", "event.detail?.a?.b" };

    private final UnorderedList setup = new UnorderedList();

    public EventDataPathView() {
        add(new H2("addEventDataPath (vaadin/flow#26999)"));
        add(new Paragraph("Runs with vaadin.csp=strict."));
        setup.setId("setup");

        validPaths();
        elementPaths();
        rejectedStrings();
        nothingAddedOnReject();
        beanMethods();
        deprecatedMethods();
        domEventAnnotation();
        tokenMethods();
        resizeListener();

        add(new H3("Setup results"), setup);
    }

    /** P1: property paths are read in the browser, with the path as key. */
    private void validPaths() {
        add(new H3("P1: valid paths"));
        NativeButton button = new NativeButton("Click for paths");
        button.setId("paths");
        button.getElement().setAttribute("data-case", "paths");
        Span result = result("paths");
        button.getElement()
                .addEventListener("click",
                        e -> result.setText(describe(e.getEventData(),
                                "event.type", "event.detail",
                                "event.target.id", "element.dataset.case",
                                "event.relatedTarget?.id")))
                .addEventDataPath("event.type")
                // UIEvent.detail, the click count for a click
                .addEventDataPath("event.detail")
                .addEventDataPath("event.target.id")
                .addEventDataPath("element.dataset.case")
                // relatedTarget is null for a click: ?. must stop there
                .addEventDataPath("event.relatedTarget?.id");
        add(new Div(button, result));
    }

    /** P2: element paths are mapped to the server-side element. */
    private void elementPaths() {
        add(new H3("P2: element paths"));
        Div container = new Div();
        container.setId("elements-container");
        Div parent = new Div();
        parent.setId("elements-parent");
        Span child = new Span("Click this child");
        child.setId("elements-child");
        parent.add(child);
        container.add(parent);
        Span result = result("elements");
        parent.getElement().addEventListener("click", e -> result.setText(
                "event.target.parentElement="
                        + id(e.getEventDataElement(
                                "event.target.parentElement"))
                        + " | event.target=" + id(e.getEventTarget())
                        + " | element.parentElement=" + id(e
                                .getEventDataElement("element.parentElement"))))
                .addEventDataElementPath("event.target.parentElement")
                // mapped like mapEventTargetElement()
                .addEventDataElementPath("event.target")
                .addEventDataElementPath("element.parentElement");
        add(container, result);
    }

    /**
     * R1: strings that are not paths are refused when the listener is set up,
     * paths at the edge of the grammar are accepted.
     */
    private void rejectedStrings() {
        Element probe = new Element("span");
        getElement().appendChild(probe);
        for (int i = 0; i < NOT_PATHS.length; i++) {
            String value = NOT_PATHS[i];
            setupRow("not-path-" + i, "addEventDataPath(\"" + value + "\")",
                    () -> probe.addEventListener("click", e -> {
                    }).addEventDataPath(value));
            setupRow("not-element-path-" + i,
                    "addEventDataElementPath(\"" + value + "\")",
                    () -> probe.addEventListener("click", e -> {
                    }).addEventDataElementPath(value));
        }
        for (int i = 0; i < PATHS.length; i++) {
            String value = PATHS[i];
            setupRow("path-" + i, "addEventDataPath(\"" + value + "\")",
                    () -> probe.addEventListener("click", e -> {
                    }).addEventDataPath(value));
            setupRow("element-path-" + i,
                    "addEventDataElementPath(\"" + value + "\")",
                    () -> probe.addEventListener("click", e -> {
                    }).addEventDataElementPath(value));
        }
        setupRow("null-path", "addEventDataPath(null)",
                () -> probe.addEventListener("click", e -> {
                }).addEventDataPath(null));
        setupRow("null-element-path", "addEventDataElementPath(null)",
                () -> probe.addEventListener("click", e -> {
                }).addEventDataElementPath(null));
    }

    /**
     * R2: a refused string adds nothing; the listener still works with the
     * data added before.
     */
    private void nothingAddedOnReject() {
        add(new H3("R2: a refused string adds nothing"));
        NativeButton button = new NativeButton("Click after refused strings");
        button.setId("rejected-probe");
        Span result = result("rejected-probe");
        DomListenerRegistration registration = button.getElement()
                .addEventListener("click",
                        e -> result.setText(e.getEventData().toString()))
                .addEventDataPath("element.id");
        setupRow("rejected-probe-path", "probe addEventDataPath(expression)",
                () -> registration.addEventDataPath("element.id + 1"));
        setupRow("rejected-probe-element-path",
                "probe addEventDataElementPath(expression)",
                () -> registration.addEventDataElementPath(
                        "event.target.closest('div')"));
        add(new Div(button, result));
    }

    public record ClickPart(String type, int detail) {
    }

    /** Only event: every path is valid. */
    public record ValidBean(ClickPart event) {
    }

    /** "type" is not event or element: "type" is not a path. */
    public record TopLevelTypeBean(ClickPart event, String type) {
    }

    /** "größe" has non-ASCII letters, so "event.detail.größe" is no path. */
    public record NonAsciiDetail(int größe, int width) {
    }

    /** B1-B3: addEventData(Class) and addEventDetail(Class). */
    private void beanMethods() {
        add(new H3("B1: addEventData(Class) with valid paths"));
        NativeButton button = new NativeButton("Click for bean");
        button.setId("bean");
        Span result = result("bean");
        button.getElement().addEventListener("click", e -> {
            ValidBean bean = e.getEventData(ValidBean.class);
            result.setText("type=" + bean.event().type() + " | detail="
                    + bean.event().detail() + " | keys="
                    + e.getEventData().toString());
        }).addEventData(ValidBean.class);
        add(new Div(button, result));

        add(new H3("B2/B3: invalid beans add nothing"));
        NativeButton probe = new NativeButton("Click after invalid beans");
        probe.setId("bean-probe");
        Span probeResult = result("bean-probe");
        DomListenerRegistration registration = probe.getElement()
                .addEventListener("click",
                        e -> probeResult.setText(e.getEventData().toString()))
                .addEventDataPath("element.id");
        // B2: event.type and event.detail are valid, type is not
        setupRow("bean-top-level-type", "addEventData(TopLevelTypeBean.class)",
                () -> registration.addEventData(TopLevelTypeBean.class));
        // B3: event.detail.width is valid, event.detail.größe is not
        setupRow("bean-non-ascii-detail",
                "addEventDetail(NonAsciiDetail.class)",
                () -> registration.addEventDetail(NonAsciiDetail.class));
        add(new Div(probe, probeResult));
    }

    /** D1-D4: the deprecated string methods under strict CSP. */
    @SuppressWarnings("deprecation")
    private void deprecatedMethods() {
        add(new H3("D1/D2: deprecated methods with paths still work"));
        Div parent = new Div();
        parent.setId("deprecated-parent");
        NativeButton button = new NativeButton("Click deprecated");
        button.setId("deprecated");
        parent.add(button);
        Span result = result("deprecated");
        button.getElement()
                .addEventListener("click",
                        e -> result.setText("event.type="
                                + e.getEventData().get("event.type").asString()
                                + " | event.target.parentElement="
                                + id(e.getEventDataElement(
                                        "event.target.parentElement"))))
                .addEventData("event.type")
                .addEventDataElement("event.target.parentElement");
        add(parent, result);

        // D3/D4: expressions are refused by strict mode, as before the PR
        Element probe = new Element("span");
        getElement().appendChild(probe);
        setupRow("deprecated-expression", "addEventData(\"event.detail + 1\")",
                () -> probe.addEventListener("click", e -> {
                }).addEventData("event.detail + 1"));
        setupRow("deprecated-element-expression",
                "addEventDataElement(\"event.target.closest('div')\")",
                () -> probe.addEventListener("click", e -> {
                }).addEventDataElement("event.target.closest('div')"));
    }

    @Tag("button")
    public static class EventButton extends Component {
        public EventButton(String text) {
            getElement().setText(text);
        }

        public <T extends ComponentEvent<?>> void listen(Class<T> type,
                ComponentEventListener<T> listener) {
            addListener(type, listener);
        }
    }

    @com.vaadin.flow.component.DomEvent("click")
    public static class PathClickEvent extends ComponentEvent<EventButton> {
        private final int detail;
        private final String id;

        public PathClickEvent(EventButton source, boolean fromClient,
                @EventData("event.detail") int detail,
                @EventData("element.id") String id) {
            super(source, fromClient);
            this.detail = detail;
            this.id = id;
        }
    }

    @com.vaadin.flow.component.DomEvent("click")
    public static class ExpressionClickEvent
            extends ComponentEvent<EventButton> {
        public ExpressionClickEvent(EventButton source, boolean fromClient,
                @EventData("event.detail + 1") int detailPlusOne) {
            super(source, fromClient);
        }
    }

    /**
     * E1/E2: @EventData values go through the internal helper that replaced
     * addEventData(List).
     */
    private void domEventAnnotation() {
        add(new H3("E1: @DomEvent with path @EventData"));
        EventButton button = new EventButton("Click @DomEvent");
        button.setId("dom-event");
        Span result = result("dom-event");
        button.listen(PathClickEvent.class, e -> result
                .setText("detail=" + e.detail + " | id=" + e.id));
        add(new Div(button, result));

        EventButton expression = new EventButton("Not used");
        setupRow("dom-event-expression",
                "@EventData(\"event.detail + 1\") listener",
                () -> expression.listen(ExpressionClickEvent.class, e -> {
                }));
    }

    /** T1-T3: methods that add internal tokens, no longer through
     * addEventData(String). */
    private void tokenMethods() {
        add(new H3("T1: preventDefault()"));
        Anchor link = new Anchor("#prevented", "Click this link");
        link.setId("prevent-link");
        Span preventResult = result("prevent");
        link.getElement()
                .addEventListener("click",
                        e -> preventResult.setText("clicked"))
                .preventDefault();
        add(new Div(link, preventResult));

        add(new H3("T2: stopPropagation()"));
        Div outer = new Div();
        outer.setId("propagation-outer");
        NativeButton inner = new NativeButton("Click inner");
        inner.setId("propagation-inner");
        outer.add(inner);
        Span propagationResult = result("propagation");
        outer.getElement().addEventListener("click",
                e -> propagationResult.setText(
                        (propagationResult.getText() + " outer").trim()));
        inner.getElement()
                .addEventListener("click",
                        e -> propagationResult.setText(
                                (propagationResult.getText() + " inner")
                                        .trim()))
                .stopPropagation();
        add(outer, propagationResult);

        add(new H3("T3: synchronizeProperty()"));
        Element input = new Element("input");
        input.setAttribute("id", "sync-input");
        Span syncResult = result("sync");
        input.addPropertyChangeListener("value", "change",
                e -> syncResult.setText("value=" + e.getValue()));
        Div wrapper = new Div();
        wrapper.getElement().appendChild(input);
        add(wrapper, syncResult);
    }

    /** W1: Page resize listener now uses addEventDataPath. */
    private void resizeListener() {
        add(new H3("W1: browser window resize listener"));
        Span result = result("resize");
        add(new Div(result));
        addAttachListener(event -> {
            UI ui = event.getUI();
            ui.getPage().addBrowserWindowResizeListener(e -> result
                    .setText(e.getWidth() + "x" + e.getHeight()));
        });
    }

    private void setupRow(String id, String label, Runnable action) {
        String outcome;
        try {
            action.run();
            outcome = "OK";
        } catch (RuntimeException e) {
            outcome = e.getClass().getSimpleName();
        }
        ListItem item = new ListItem(label + " -> " + outcome);
        item.setId("setup-" + id);
        item.getElement().setAttribute("data-outcome", outcome);
        setup.add(item);
    }

    private static Span result(String id) {
        Span span = new Span();
        span.setId("result-" + id);
        return span;
    }

    private static String describe(JsonNode data, String... keys) {
        StringBuilder text = new StringBuilder();
        for (String key : keys) {
            if (!text.isEmpty()) {
                text.append(" | ");
            }
            text.append(key).append('=');
            text.append(data.has(key) ? data.get(key).toString() : "missing");
        }
        return text.toString();
    }

    private static String id(Optional<Element> element) {
        return element.map(e -> e.getAttribute("id")).orElse("none");
    }
}
