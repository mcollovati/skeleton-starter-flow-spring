package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

/**
 * Declared JavaScript that fails, for vaadin/flow#27025 (fixes
 * vaadin/flow#27024). Each button runs one case. The browser side of the test
 * records what the page reports: an uncaught error, which is how Flow reports
 * the stack trace of a failed {@code executeJs(String)} call, or an unhandled
 * promise rejection, which nothing in Flow reports.
 */
@Route("declared-js-errors")
public class DeclaredJsErrorsView extends Div {

    @JsDefinition
    public interface ErrorJs extends Serializable {

        @JsExpression("throw new Error('void-throws')")
        void voidThrows();

        @JsExpression("throw new Error('unsubscribed-throws')")
        PendingJavaScriptResult unsubscribedThrows();

        @JsExpression("throw new Error('subscribed-throws')")
        PendingJavaScriptResult subscribedThrows();

        @JsExpression("""
                return new Promise((resolve, reject) =>
                    setTimeout(() => reject(new Error('async-rejects')), 0));
                """)
        void asyncRejects();

        @JsExpression("return 42")
        PendingJavaScriptResult succeeds();
    }

    public DeclaredJsErrorsView() {
        Span subscribedResult = new Span();
        subscribedResult.setId("subscribed-result");
        Span succeedsResult = new Span();
        succeedsResult.setId("succeeds-result");
        Span stringResult = new Span();
        stringResult.setId("string-result");

        add(button("void-throws", "Declared void method throws",
                () -> js().voidThrows()));
        add(button("unsubscribed-throws",
                "Declared method throws, result not used",
                () -> js().unsubscribedThrows()));
        add(button("subscribed-throws",
                "Declared method throws, result used",
                () -> js().subscribedThrows().then(
                        value -> subscribedResult.setText("value: " + value),
                        error -> subscribedResult.setText("error: " + error))));
        add(button("async-rejects",
                "Declared void method rejects its promise later",
                () -> js().asyncRejects()));
        add(button("succeeds", "Declared method succeeds, result used",
                () -> js().succeeds().then(Integer.class,
                        value -> succeedsResult.setText("value: " + value))));
        // For comparison. With vaadin.csp=strict the server refuses it.
        add(button("string-throws", "executeJs(String) throws", () -> {
            try {
                UI.getCurrent().getPage()
                        .executeJs("throw new Error('string-throws')");
                stringResult.setText("sent");
            } catch (RuntimeException e) {
                stringResult.setText("refused: " + e.getClass().getSimpleName());
            }
        }));
        add(new Div(subscribedResult), new Div(succeedsResult),
                new Div(stringResult));
    }

    private static ErrorJs js() {
        return UI.getCurrent().getPage().executeJs(ErrorJs.class);
    }

    private static NativeButton button(String id, String text,
            Runnable action) {
        NativeButton button = new NativeButton(text, e -> action.run());
        button.setId(id);
        return button;
    }
}
