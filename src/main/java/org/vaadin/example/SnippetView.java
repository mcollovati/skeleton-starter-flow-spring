package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CspViolationException;

/**
 * Exercises every supported shape of a JsSnippet (PR #26729). Each case
 * declares a {@link JsFunction} as a bundled snippet, passes it to a declared
 * call that invokes it, and writes the result into its own output element, so
 * the whole matrix can be checked in a browser. Nothing is compiled from a
 * string on the declared path.
 */
@Route("snippet")
public class SnippetView extends VerticalLayout {

    /**
     * The snippet variants. An interface of snippets declares only snippet
     * methods (each returns a {@link JsFunction}).
     */
    @JsDefinition
    public interface Snippets extends Serializable {

        /** With {@code @JsCalledWith}: one capture ($0) and one called param. */
        @JsExpression("return $0 + ': ' + value;")
        @JsCalledWith("value")
        JsFunction labeled(String label);

        /**
         * A capture used together with a called param. Before vaadin/flow#26830
         * this case used the factory form, return (name) => ..., which is
         * removed.
         */
        @JsExpression("return $0 + ', ' + name + '!';")
        @JsCalledWith("name")
        JsFunction greeter(String greeting);

        /**
         * No {@code @JsCalledWith}: the function takes no parameters
         * (vaadin/flow#26830; before, this needed {@code @JsCalledWith({})}).
         */
        @JsExpression("return $0;")
        JsFunction constant(String text);

        /** Several called params with a capture ($0) between them. */
        @JsExpression("return a + $0 + b;")
        @JsCalledWith({ "a", "b" })
        JsFunction join(String separator);

        /** No captures (no method parameters) with a called param. */
        @JsExpression("return value.toUpperCase();")
        @JsCalledWith("value")
        JsFunction upper();

        /** Variadic captures ($0 fixed, $1 the rest) with a called param. */
        @JsExpression("return [$0, ...$1].join(sep);")
        @JsCalledWith("sep")
        JsFunction prefixed(String head, String... tail);
    }

    /**
     * The declared calls that invoke a snippet. An interface of calls declares
     * only call methods (void / PendingJavaScriptResult). Each writes the
     * result of invoking the passed function into its own {@code this}.
     */
    @JsDefinition
    public interface Sink extends Serializable {
        @JsExpression("this.textContent = $0();")
        void show0(JsFunction fn);

        @JsExpression("this.textContent = $0('world');")
        void show1(JsFunction fn);

        @JsExpression("this.textContent = $0('A', 'B');")
        void show2(JsFunction fn);

        @JsExpression("this.textContent = $0('-');")
        void showDash(JsFunction fn);
    }

    // Snippet implementations hold no state, so they are shared constants.
    private static final Snippets S = JsSnippets.of(Snippets.class);

    public SnippetView() {
        add(new H3("JsSnippet demo — all supported shapes"));

        // case id, expected text, and the call that runs it
        run("labeled", "hello: world",
                out -> out.executeJs(Sink.class).show1(S.labeled("hello")));
        run("greeter", "Hi, world!",
                out -> out.executeJs(Sink.class).show1(S.greeter("Hi")));
        run("constant", "fixed",
                out -> out.executeJs(Sink.class).show0(S.constant("fixed")));
        run("join", "A | B",
                out -> out.executeJs(Sink.class).show2(S.join(" | ")));
        run("upper", "WORLD",
                out -> out.executeJs(Sink.class).show1(S.upper()));
        run("prefixed", "x-y-z",
                out -> out.executeJs(Sink.class)
                        .showDash(S.prefixed("x", "y", "z")));

        // Contrast: a string executeJs expression is compiled with new
        // Function, which needs unsafe-eval. Under CspMode.STRICT the server
        // refuses it with a CspViolationException before it is ever sent, so the
        // snippet (still eval-free itself) never reaches a compile step.
        Div viaString = new Div();
        viaString.add(new Paragraph("viaString (expect: blocked server-side)"));
        Div viaStringOut = new Div();
        viaStringOut.setId("viaString");
        viaString.add(viaStringOut);
        add(viaString);
        try {
            viaStringOut.getElement().executeJs("this.textContent = $0('world')",
                    S.labeled("stringy"));
        } catch (CspViolationException e) {
            viaStringOut.setText("blocked: " + e.getMessage());
        }

        add(new Paragraph("Each row shows '<case> = <result>'. Declared cases"
                + " run without unsafe-eval; 'viaString' uses a string"
                + " expression."));
    }

    private void run(String id, String expected,
            java.util.function.Consumer<Element> invoker) {
        Div row = new Div();
        row.add(new Paragraph(id + " (expect: " + expected + ")"));
        Div out = new Div();
        out.setId(id);
        out.getElement().getStyle().set("padding", "0.25em 0.5em")
                .set("border", "1px solid #ccc").set("font-family", "monospace");
        row.add(out);
        add(row);
        // executeJs is deferred until the element is attached
        invoker.accept(out.getElement());
    }
}
