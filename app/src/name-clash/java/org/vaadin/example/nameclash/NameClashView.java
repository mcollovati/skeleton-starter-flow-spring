package org.vaadin.example.nameclash;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Review finding 1 on vaadin/flow#27457: the names of the imports are the
 * parameters of the function the expression is pasted into, so a
 * {@code const} with the same name is a SyntaxError. {@code local()} does not
 * use the import at all, and was valid before the import was added. Added by
 * the {@code name-clash} profile only.
 */
@Route("name-clash")
public class NameClashView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public interface NameClashJs extends JsInvokers {

        @JsExpression("return format($0);")
        PendingJavaScriptResult formatted(String text);

        @JsExpression("const format = '<' + $0 + '>'; return format;")
        PendingJavaScriptResult local(String text);
    }

    public NameClashView() {
        Span result = new Span();
        result.setId("result");
        NativeButton run = new NativeButton("Run", event -> getElement()
                .executeJs(NameClashJs.class).local("x")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("failed: " + error)));
        run.setId("run");
        add(run, result);
    }
}
