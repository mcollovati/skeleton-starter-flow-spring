package org.vaadin.example;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#27001: event data declared as
 * {@code DomEventData<Element>} gives the server-side element of the DOM node
 * that the expression returns. This is the CSP-safe replacement for
 * {@code addEventDataElement(String)} with an expression, which strict mode
 * refuses (case D4 in {@link EventDataPathView}).
 * <p>
 * The application runs with {@code vaadin.csp=strict}. Each case writes what
 * the server got to a {@code #result-<case>} span.
 */
@Route("event-data-element")
public class EventDataElementView extends Div {

    @JsDefinition
    public interface ElementJs extends Serializable {

        /** The node that was clicked, also inside a shadow root. */
        @JsExpression("return event.composedPath()[0];")
        DomEventData<Element> originalTarget();

        /** The expression that case D4 refuses as a string. */
        @JsExpression("return event.target.closest('div.row');")
        DomEventData<Element> closestRow();

        /** The expression of FocusTracker in vaadin/swingbridge-emulators. */
        @JsExpression("return event.composedPath().find(n => n.nodeType === 1 && n.getRootNode() === document);")
        DomEventData<Element> documentLevelTarget();

        @JsExpression("return element.querySelector($0);")
        DomEventData<Element> query(String selector);

        @JsExpression("return document.getElementById($0);")
        DomEventData<Element> byId(String id);

        @JsExpression("return null;")
        DomEventData<Element> nothing();

        @JsExpression("return 42;")
        DomEventData<Element> notANode();

        /** Not an element: must stay a plain value. */
        @JsExpression("return event.target.id;")
        DomEventData<String> targetId();
    }

    static final ElementJs JS = JsSnippets.of(ElementJs.class);

    public EventDataElementView() {
        add(new H2("DomEventData<Element> (vaadin/flow#27001)"));
        add(new Paragraph("Runs with vaadin.csp=strict."));

        shadowRoot();
        closestRow();
        capturesAndOutside();
        noElement();
        invisibleAndUnmapped();
    }

    /** X1: inside a shadow root, event.target is the host. */
    private void shadowRoot() {
        add(new H3("X1: composedPath() inside a shadow root"));
        Div host = new Div();
        host.setId("shadow-host");
        ShadowRoot shadow = host.getElement().attachShadow();
        Element inner = new Element("span");
        inner.setAttribute("id", "shadow-inner");
        inner.setText("Click inside the shadow root");
        shadow.appendChild(inner);

        Span result = result("shadow");
        DomEventData<Element> original = JS.originalTarget();
        DomEventData<Element> documentLevel = JS.documentLevelTarget();
        host.getElement().addEventListener("click",
                e -> result.setText("originalTarget="
                        + id(e.getEventDataElement(original))
                        + " | eventTarget=" + id(e.getEventTarget())
                        + " | documentLevelTarget="
                        + id(e.getEventDataElement(documentLevel))))
                .addEventData(original).addEventData(documentLevel)
                .mapEventTargetElement();
        add(host, result);
    }

    /** X2: the expression of case D4, now declared. */
    private void closestRow() {
        add(new H3("X2: closest('div.row'), refused as a string in D4"));
        Div table = new Div();
        table.setId("rows");
        for (int i = 1; i <= 3; i++) {
            Div row = new Div();
            row.addClassName("row");
            row.setId("row-" + i);
            Span cell = new Span("Cell of row " + i);
            cell.setId("cell-" + i);
            row.add(cell);
            table.add(row);
        }
        Span result = result("row");
        DomEventData<Element> closest = JS.closestRow();
        DomEventData<String> targetId = JS.targetId();
        table.getElement().addEventListener("click", e -> {
            // getEventData gives the same element, or null
            Element viaGetEventData = e.getEventData(closest);
            result.setText("closestRow=" + id(e.getEventDataElement(closest))
                    + " | getEventData="
                    + (viaGetEventData == null ? "null"
                            : viaGetEventData.getAttribute("id"))
                    + " | targetId=" + e.getEventData(targetId));
        }).addEventData(closest).addEventData(targetId);
        add(table, result);
    }

    /** X3/X4: a captured selector, and an element outside the source. */
    private void capturesAndOutside() {
        add(new H3("X3/X4: captured selector, element outside the source"));
        Div outside = new Div("I am outside the listener's element");
        outside.setId("outside-target");

        Div source = new Div();
        source.setId("capture-source");
        Span child = new Span("Click here");
        child.setId("query-target");
        source.add(child);

        Span result = result("capture");
        DomEventData<Element> query = JS.query("#query-target");
        DomEventData<Element> byId = JS.byId("outside-target");
        source.getElement().addEventListener("click",
                e -> result.setText(
                        "query=" + id(e.getEventDataElement(query))
                                + " | outside="
                                + id(e.getEventDataElement(byId))))
                .addEventData(query).addEventData(byId);
        add(outside, source, result);
    }

    /** X5: values that are no DOM node give an empty result. */
    private void noElement() {
        add(new H3("X5: null and a number"));
        Div source = new Div("Click for null and 42");
        source.setId("no-element");
        Span result = result("no-element");
        DomEventData<Element> nothing = JS.nothing();
        DomEventData<Element> notANode = JS.notANode();
        source.getElement().addEventListener("click",
                e -> result.setText("nothing=" + id(e.getEventDataElement(nothing))
                        + " | notANode=" + id(e.getEventDataElement(notANode))
                        + " | getEventData=" + e.getEventData(notANode)))
                .addEventData(nothing).addEventData(notANode);
        add(source, result);
    }

    /**
     * X6/X7: an invisible element is not reported; a DOM node without a state
     * node maps to the closest parent that has one.
     */
    private void invisibleAndUnmapped() {
        add(new H3("X6/X7: invisible element, node without a state node"));
        Div source = new Div();
        source.setId("invisible-source");
        source.add(new Span("Click here"));
        Span invisible = new Span("Visible until hidden");
        invisible.setId("invisible-target");
        source.add(invisible);
        // Hidden after it was shown: the browser keeps the element (with the
        // hidden attribute), so only the server check keeps it out. An
        // element that is invisible from the start is never sent.
        NativeButton hide = new NativeButton("Hide",
                e -> invisible.setVisible(false));
        hide.setId("hide-invisible");
        add(hide);
        Div raw = new Div();
        raw.setId("raw-parent");
        // Created by the browser, so it has no state node
        raw.getElement().setProperty("innerHTML",
                "<b id='raw-child'>raw</b>");
        source.add(raw);

        Span result = result("invisible");
        DomEventData<Element> hidden = JS.query("#invisible-target");
        DomEventData<Element> rawChild = JS.query("#raw-child");
        source.getElement().addEventListener("click",
                e -> result.setText(
                        "invisible=" + id(e.getEventDataElement(hidden))
                                + " | rawChild="
                                + id(e.getEventDataElement(rawChild))))
                .addEventData(hidden).addEventData(rawChild);
        add(source, result);
    }

    private static Span result(String id) {
        Span span = new Span();
        span.setId("result-" + id);
        return span;
    }

    private static String id(Optional<Element> element) {
        return element.map(e -> e.getAttribute("id")).orElse("none");
    }
}
