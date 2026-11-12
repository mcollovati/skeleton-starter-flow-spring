package org.vaadin.example;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#27000: {@code @EventData} values that are not property
 * paths are generated into the bundle at build time, so the browser runs them
 * without compiling anything.
 * <p>
 * The application runs with {@code vaadin.csp=strict}, so the browser must not
 * compile any event data, and the server refuses event data that the browser
 * would compile.
 * <p>
 * Setup cases run when the view is created. Each one is a row in the
 * {@code #setup} list that says {@code OK} or the name of the exception. Click
 * cases write what the server got to a {@code #result-<case>} span.
 */
@Route("bundled-event-data")
public class BundledEventDataView extends Div {

    /** Used by two event classes: the bundle must have one function. */
    static final String SHARED = "event.detail * 10";

    static final String TEMPLATE = "`${event.type}:${event.detail}`";

    static final String TRAILING_COMMENT = "event.type + \"!\" // a comment";

    static final String MULTI_LINE = "event.detail\n    + 100";

    private final UnorderedList setup = new UnorderedList();

    public BundledEventDataView() {
        add(new H2("@EventData from the bundle (vaadin/flow#27000)"));
        add(new Paragraph("Runs with vaadin.csp=strict."));
        setup.setId("setup");

        mixedEventData();
        sharedExpression();
        syntaxEdgeCases();
        dragAndDrop();
        notBundled();
        functionIds();

        add(new H3("Setup results"), setup);
    }

    // ---- A1: expressions next to a path and an element path ----

    @DomEvent("click")
    public static class MixedClickEvent extends ComponentEvent<Div> {
        final int detailTimesTen;
        final String upperType;
        final String targetId;
        final Element element;

        public MixedClickEvent(Div source, boolean fromClient,
                @EventData(SHARED) int detailTimesTen,
                @EventData("event.type.toUpperCase()") String upperType,
                @EventData("event.target.id") String targetId,
                @EventData("element") Element element) {
            super(source, fromClient);
            this.detailTimesTen = detailTimesTen;
            this.upperType = upperType;
            this.targetId = targetId;
            this.element = element;
        }
    }

    private void mixedEventData() {
        add(new H3("A1: expressions, a path and an element path"));
        Div area = area("mixed", "Click for mixed event data");
        Span result = result("mixed");
        ComponentUtil.addListener(area, MixedClickEvent.class,
                e -> result.setText("detailTimesTen=" + e.detailTimesTen
                        + " | upperType=" + e.upperType + " | targetId="
                        + e.targetId + " | element="
                        + e.element.getAttribute("id")));
        add(area, result);
    }

    // ---- A2: the same expression in two event classes ----

    @DomEvent("click")
    public static class SharedClickEvent extends ComponentEvent<Div> {
        final int value;

        public SharedClickEvent(Div source, boolean fromClient,
                @EventData(SHARED) int value) {
            super(source, fromClient);
            this.value = value;
        }
    }

    @DomEvent("dblclick")
    public static class SharedDoubleClickEvent extends ComponentEvent<Div> {
        final int value;

        public SharedDoubleClickEvent(Div source, boolean fromClient,
                @EventData(SHARED) int value) {
            super(source, fromClient);
            this.value = value;
        }
    }

    private void sharedExpression() {
        add(new H3("A2: one expression in two event classes"));
        Div area = area("shared", "Click and double click");
        Span result = result("shared");
        ComponentUtil.addListener(area, SharedClickEvent.class,
                e -> result.setText(
                        (result.getText() + " click=" + e.value).trim()));
        ComponentUtil.addListener(area, SharedDoubleClickEvent.class,
                e -> result.setText(
                        (result.getText() + " dblclick=" + e.value).trim()));
        add(area, result);
    }

    // ---- A3: values that must survive the code generation ----

    @DomEvent("click")
    public static class SyntaxClickEvent extends ComponentEvent<Div> {
        final String template;
        final String trailingComment;
        final int multiLine;
        final String quotes;

        public SyntaxClickEvent(Div source, boolean fromClient,
                @EventData(TEMPLATE) String template,
                @EventData(TRAILING_COMMENT) String trailingComment,
                @EventData(MULTI_LINE) int multiLine,
                @EventData("'a\"b' + \"c'd\" + '\\\\'") String quotes) {
            super(source, fromClient);
            this.template = template;
            this.trailingComment = trailingComment;
            this.multiLine = multiLine;
            this.quotes = quotes;
        }
    }

    private void syntaxEdgeCases() {
        add(new H3("A3: template literal, trailing comment, two lines, quotes"));
        Div area = area("syntax", "Click for syntax edge cases");
        Span result = result("syntax");
        ComponentUtil.addListener(area, SyntaxClickEvent.class,
                e -> result.setText("template=" + e.template
                        + " | trailingComment=" + e.trailingComment
                        + " | multiLine=" + e.multiLine + " | quotes="
                        + e.quotes));
        add(area, result);
    }

    // ---- A4: DropEvent of flow-dnd, an event class from a library ----

    private void dragAndDrop() {
        add(new H3("A4: flow-dnd DropEvent offsets (event class in a jar)"));
        Div source = area("drag-source", "Drag me");
        Div target = area("drop-target", "Drop here");
        target.getStyle().set("width", "200px").set("height", "100px")
                .set("background", "#eef");
        Span result = result("drop");
        DragSource.create(source);
        DropTarget.create(target).addDropListener(e -> result.setText(
                "offsetX=" + e.getOffsetX() + " | offsetY=" + e.getOffsetY()));
        add(source, target, result);
    }

    // ---- A5/A6: values that are not put into the bundle ----

    @DomEvent("click")
    public static class ElementExpressionEvent extends ComponentEvent<Div> {
        public ElementExpressionEvent(Div source, boolean fromClient,
                @EventData("event.target.closest('div')") Element closest) {
            super(source, fromClient);
        }
    }

    @SuppressWarnings("deprecation")
    private void notBundled() {
        // A5: a value mapped to an element is not bundled, so strict mode
        // still refuses it
        Div probe = new Div();
        setupRow("element-expression",
                "@EventData(\"event.target.closest('div')\") Element",
                () -> ComponentUtil.addListener(probe,
                        ElementExpressionEvent.class, e -> {
                        }));
        // A6: the same value as a bundled @EventData, through the string API:
        // only @EventData uses the bundle
        setupRow("string-api-same-expression",
                "addEventData(\"" + SHARED + "\")",
                () -> probe.getElement().addEventListener("click", e -> {
                }).addEventData(SHARED));
    }

    // ---- function ids, for the bundle check in the test ----

    private void functionIds() {
        Div ids = new Div();
        ids.setId("function-ids");
        for (String expression : new String[] { SHARED, TEMPLATE,
                TRAILING_COMMENT, MULTI_LINE, "event.type.toUpperCase()",
                "event.clientX - event.currentTarget.getBoundingClientRect().left" }) {
            Span id = new Span(BundledEventData.getFunctionId(expression));
            id.addClassName("function-id");
            ids.add(id);
        }
        // Not setVisible(false): Flow does not send the children of an
        // invisible component
        ids.getStyle().set("display", "none");
        add(ids);
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

    private static Div area(String id, String text) {
        Div area = new Div(text);
        area.setId(id);
        area.getStyle().set("padding", "8px").set("margin", "4px 0")
                .set("border", "1px solid #888").set("cursor", "pointer");
        return area;
    }

    private static Span result(String id) {
        Span span = new Span();
        span.setId("result-" + id);
        return span;
    }
}
