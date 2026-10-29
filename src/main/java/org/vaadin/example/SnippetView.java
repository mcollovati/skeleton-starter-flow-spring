package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Exercises the JsSnippet feature (PR #26729): a JsFunction declared as a
 * bundled snippet, passed to a declared call, with no string compiled in the
 * browser.
 *
 * {@link FormatJs#createFormatter} declares a function whose body is in the
 * bundle. {@link ApplyJs#applyFormatter} is a declared call that receives that
 * function and invokes it. The server sends only the two function ids and the
 * captured label, so nothing needs {@code unsafe-eval}.
 */
@Route("snippet")
public class SnippetView extends VerticalLayout {

    /** A snippet: its method returns a JsFunction declared in the bundle. */
    @JsDefinition
    public interface FormatJs extends Serializable {
        @JsExpression("return $0 + ': ' + value;")
        @JsCalledWith("value")
        JsFunction createFormatter(String label);
    }

    /** A call: run on the element it is obtained from, receives the snippet. */
    @JsDefinition
    public interface ApplyJs extends Serializable {
        @JsExpression("this.textContent = $0('world');")
        void applyFormatter(JsFunction formatter);
    }

    // The snippet implementation holds no state, so it can be a constant.
    private static final FormatJs FORMAT = JsSnippets.of(FormatJs.class);

    public SnippetView() {
        add(new H3("JsSnippet demo"));

        Div out = new Div();
        out.setId("out");
        out.getElement().getStyle().set("padding", "0.5em")
                .set("border", "1px solid #ccc");
        add(out);

        NativeButton run = new NativeButton("Run snippet", e -> {
            // Pass the declared snippet to the declared call. The whole path is
            // free of string JavaScript: createFormatter returns a JsFunction
            // in id form, applyFormatter is a bundled function.
            out.getElement().executeJs(ApplyJs.class)
                    .applyFormatter(FORMAT.createFormatter("hello"));
        });
        run.setId("run");
        add(run);

        add(new Paragraph("Click the button. '#out' should read"
                + " \"hello: world\", produced by bundled JavaScript (no"
                + " unsafe-eval)."));
    }
}
