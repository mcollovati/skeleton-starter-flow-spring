package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * preventDefault(), stopPropagation() and debounce() with declared event data
 * and filters (PR vaadin/flow#25661).
 * <p>
 * These cases use no string JavaScript in the application code, but Flow
 * itself still sends some strings that the browser compiles with
 * {@code new Function}: {@code event.preventDefault()},
 * {@code event.stopPropagation()}, and the always-true filter {@code 1}. Each
 * case says what Flow compiles. Under a Content Security Policy without
 * {@code unsafe-eval}, the browser refuses to compile them.
 */
@Route("event-data-side-effects")
public class SideEffectsView extends VerticalLayout {

    @JsDefinition
    public interface SideJs extends Serializable {
        @JsExpression("return event.key === $0;")
        DomEventFilter isKey(String key);

        @JsExpression("return event.detail === $0;")
        DomEventFilter clickCount(int count);

        @JsExpression("return element.value;")
        DomEventData<String> value();

        @JsExpression("return event.key;")
        DomEventData<String> key();
    }

    private static final SideJs JS = JsSnippets.of(SideJs.class);

    public SideEffectsView() {
        add(new H3("preventDefault, stopPropagation and debounce"));
        add(new Paragraph("No case except S6 uses string JavaScript in the"
                + " application code. 'Flow compiles' says which strings Flow"
                + " still sends, which a CSP without unsafe-eval blocks."));

        preventDefaultWithoutFilter();
        preventDefaultWithDeclaredFilter();
        stopPropagationWithDeclaredFilter();
        debounceWithoutFilter();
        filteredAndUnfilteredListeners();
        preventDefaultWithStringFilter();
    }

    private void preventDefaultWithoutFilter() {
        Output out = section("s1", "S1. preventDefault() without a filter",
                "Click the checkbox. Expected: it stays unchecked, and"
                        + " 'click received' shows.",
                "event.preventDefault()");
        Input box = new Input();
        box.setType("checkbox");
        box.setId("s1Box");
        box.getElement()
                .addEventListener("click", e -> out.append("click received"))
                .preventDefault();
        addCase(out, box);
    }

    private void preventDefaultWithDeclaredFilter() {
        Output out = section("s2",
                "S2. preventDefault() with a declared filter",
                "Type 'axb' in the field. Expected: the field shows 'ab',"
                        + " and 'blocked: x' shows.",
                "nothing (the filter runs as a declared function with the"
                        + " side effect); but the server logs a CSP warning"
                        + " for preventDefault()");
        Input input = new Input();
        input.setId("s2Input");
        DomEventData<String> key = JS.key();
        input.getElement()
                .addEventListener("keydown",
                        e -> out.append("blocked: " + e.getEventData(key)))
                .setFilter(JS.isKey("x")).addEventData(key).preventDefault();
        addCase(out, input);
    }

    private void stopPropagationWithDeclaredFilter() {
        Output out = section("s3",
                "S3. stopPropagation() with a declared filter",
                "Type 'a' in the field, then press Enter. Expected: 'parent"
                        + " got: a' and 'field stopped Enter', but no 'parent"
                        + " got: Enter'.",
                "nothing (as in S2); the server logs a CSP warning for"
                        + " stopPropagation()");
        Div parent = new Div();
        parent.setId("s3Parent");
        Input input = new Input();
        input.setId("s3Input");
        parent.add(input);
        DomEventData<String> key = JS.key();
        parent.getElement()
                .addEventListener("keydown",
                        e -> out.append("parent got: " + e.getEventData(key)))
                .addEventData(key);
        input.getElement()
                .addEventListener("keydown",
                        e -> out.append("field stopped Enter"))
                .setFilter(JS.isKey("Enter")).stopPropagation();
        addCase(out, parent);
    }

    private void debounceWithoutFilter() {
        Output out = section("s4", "S4. debounce() without a filter",
                "Type 'hello' fast. Expected: one line 'value = hello'"
                        + " about 400 ms after you stop typing.",
                "1 (the always-true filter that carries the debounce);"
                        + " the server logs no CSP warning");
        Input input = new Input();
        input.setId("s4Input");
        DomEventData<String> value = JS.value();
        input.getElement()
                .addEventListener("input",
                        e -> out.append("value = " + e.getEventData(value)))
                .addEventData(value).debounce(400);
        addCase(out, input);
    }

    private void filteredAndUnfilteredListeners() {
        Output out = section("s5",
                "S5. Filtered and unfiltered listeners on one element",
                "Click the button once. Expected: 'any click'. A double"
                        + " click also gives 'double click'.",
                "1 (added so that the unfiltered listener still gets"
                        + " events that no filter matches); the server logs"
                        + " no CSP warning");
        NativeButton button = new NativeButton("Click");
        button.setId("s5Button");
        button.getElement()
                .addEventListener("click", e -> out.append("double click"))
                .setFilter(JS.clickCount(2));
        button.getElement().addEventListener("click",
                e -> out.append("any click"));
        addCase(out, button);
    }

    private void preventDefaultWithStringFilter() {
        Output out = section("s6",
                "S6. preventDefault() with a string filter (string API,"
                        + " for comparison)",
                "Type 'axb' in the field. Expected: the field shows 'ab',"
                        + " and 'blocked x' shows.",
                "event.key === 'x' and (event.key === 'x') &&"
                        + " event.preventDefault(); this needs unsafe-eval"
                        + " anyway, because the filter is a string");
        Input input = new Input();
        input.setId("s6Input");
        input.getElement()
                .addEventListener("keydown", e -> out.append("blocked x"))
                .setFilter("event.key === 'x'").preventDefault();
        addCase(out, input);
    }

    /** Adds a title and the texts of a case, and returns its output. */
    private Output section(String id, String title, String expected,
            String compiled) {
        add(new H4(title), new Paragraph(expected),
                new Paragraph("Flow compiles: " + compiled));
        return new Output(id + "Out");
    }

    private void addCase(Output out, Component... components) {
        add(components);
        add(out);
    }

    /** Shows the result of a case, one line for each event. */
    private static class Output extends Div {
        Output(String id) {
            setId(id);
            getStyle().set("font-family", "monospace")
                    .set("white-space", "pre-wrap")
                    .set("border", "1px solid #ccc")
                    .set("padding", "0.25em 0.5em").set("min-height", "1.5em")
                    .set("width", "100%").set("box-sizing", "border-box");
        }

        void append(String line) {
            String old = getText();
            setText(old == null || old.isEmpty() ? line : old + "\n" + line);
        }
    }
}
