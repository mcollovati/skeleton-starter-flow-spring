package org.vaadin.example.shorthand;

import com.acme.addon.ShorthandBox;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#27441: methods of a {@code JsInvokers} interface
 * without {@code @JsExpression} call the element method of the same name.
 * Each row has a box, a "Run" button and a result. Rows S1-S9 must work;
 * rows R1-R3 must be refused in Java, and the result shows the message.
 */
@Route("shorthand")
public class ShorthandView extends Div {

    public ShorthandView() {
        add(new H1("JsInvokers shorthand (vaadin/flow#27441)"));

        add(new H3("Calls (add-on definition ShorthandBox.BoxJs)"));
        row("s1", "focus()", (box, result) -> {
            box.js().focus();
            result.setText("called");
        });
        row("s2", "setAttribute(name, value)", (box, result) -> {
            box.js().setAttribute("title", "set by shorthand");
            result.setText("called");
        });
        row("s3", "append(String...), variadic", (box, result) -> {
            box.js().append(" one", " two", " three");
            result.setText("called");
        });
        row("s4", "getAttribute(name), PendingJavaScriptResult",
                (box, result) -> {
                    box.getElement().setAttribute("data-value", "forty-two");
                    box.js().getAttribute("data-value").then(String.class,
                            value -> result.setText("value " + value),
                            error -> result.setText("failed: " + error));
                });
        row("s5", "hasAttribute(name), JavaScriptResult<Boolean>",
                (box, result) -> box.js().hasAttribute("tabindex").then(
                        value -> result.setText("hasAttribute " + value),
                        error -> result.setText("failed: " + error)));
        row("s6", "toggleAttribute(name) and toggleAttribute(name, force)",
                (box, result) -> {
                    box.js().toggleAttribute("data-one");
                    box.js().toggleAttribute("data-two", true);
                    result.setText("called");
                });
        row("s7", "mark(value), annotated, same interface", (box, result) -> {
            box.js().mark("marked");
            result.setText("called");
        });
        row("s8", "SubBoxJs (app) inherits the shorthand methods",
                (box, result) -> {
                    SubBoxJs js = box.getElement().executeJs(SubBoxJs.class);
                    js.setAttribute("title", "set through SubBoxJs");
                    js.focus();
                    result.setText("called");
                });
        row("s9", "TypoJs.focuss(): fails in the browser", (box, result) -> box
                .getElement().executeJs(TypoJs.class).focuss()
                .then(value -> result.setText("no error: " + value),
                        error -> result.setText("browser error: " + error)));

        add(new H3("Refused in Java"));
        row("r1", "Page.executeJs(BoxJs.class)", (box, result) -> UI
                .getCurrent().getPage().executeJs(ShorthandBox.BoxJs.class)
                .focus());
        row("r2", "MixedInJs, blur() mixed in from a plain interface",
                (box, result) -> box.getElement().executeJs(MixedInJs.class)
                        .blur());
        row("r3", "JsSnippets interface with a method without @JsExpression",
                (box, result) -> JsSnippets.of(ShorthandSnippetsJs.class)
                        .focus());
    }

    private void row(String id, String title,
            SerializableConsumer2<ShorthandBox, Span> action) {
        ShorthandBox box = new ShorthandBox("Box " + id);
        box.setId(id + "-box");
        box.getStyle().set("border", "1px solid gray").set("padding", "4px");
        Span result = new Span();
        result.setId(id + "-result");
        NativeButton run = new NativeButton("Run", event -> {
            try {
                action.accept(box, result);
            } catch (RuntimeException e) {
                result.setText("refused: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        });
        run.setId(id + "-run");
        Div row = new Div(new Span(id.toUpperCase() + " " + title + " "), run,
                new Span(" "), result, box);
        row.getStyle().set("margin-bottom", "12px");
        add(row);
    }

    @FunctionalInterface
    interface SerializableConsumer2<A, B> extends java.io.Serializable {
        void accept(A a, B b);
    }
}
