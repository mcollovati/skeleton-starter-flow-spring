package com.acme.addon;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * An add-on component whose JavaScript definition uses the shorthand of
 * vaadin/flow#27441: a method of a {@link JsInvokers} interface without
 * {@link JsExpression} calls the DOM method of the same name on the element.
 * The application reaches this class from a route, so with
 * {@code optimizeBundle} the production bundle must carry these functions
 * although no method has an expression.
 */
public class ShorthandBox extends Div {

    public ShorthandBox(String text) {
        super(text);
        // So that focus() works on a div
        getElement().setAttribute("tabindex", "0");
    }

    public BoxJs js() {
        return getElement().executeJs(BoxJs.class);
    }

    @JsDefinition
    public interface BoxJs extends JsInvokers {

        // return this.focus();
        void focus();

        // return this.setAttribute($0, $1);
        void setAttribute(String name, String value);

        // Variadic: return this.append(...$0);
        void append(String... texts);

        // With a result
        PendingJavaScriptResult getAttribute(String name);

        // With a typed result
        JavaScriptResult<Boolean> hasAttribute(String name);

        // Overloads: two functions, one per parameter count
        void toggleAttribute(String name);

        void toggleAttribute(String name, boolean force);

        // Mixed with an annotated method in the same interface
        @JsExpression("this.dataset.acme = $0")
        void mark(String value);
    }
}
