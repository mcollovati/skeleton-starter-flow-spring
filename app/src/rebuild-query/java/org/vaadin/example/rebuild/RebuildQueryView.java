package org.vaadin.example.rebuild;

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
 * Review finding 2 on vaadin/flow#27457: a path with a query,
 * {@code ./app-style.css?inline}, the Vite way to get the text of a CSS
 * file. Vite builds it, but the bundle check asks for a new bundle on every
 * build ("No file found"). Added by the {@code rebuild-query} profile only.
 */
@Route("rebuild-query")
public class RebuildQueryView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./app-style.css?inline", defaultExport = "imported")
    public interface ImportJs extends JsInvokers {

        // The Vite config of Flow gives a CSS file imported with ?inline as a
        // Lit CSSResult, not as a string
        @JsExpression("return String(imported.cssText ?? imported).trim();")
        PendingJavaScriptResult call(String text);
    }

    public RebuildQueryView() {
        Span result = new Span();
        result.setId("result");
        NativeButton run = new NativeButton("Run", event -> getElement()
                .executeJs(ImportJs.class).call("x")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("failed: " + error)));
        run.setId("run");
        add(run, result);
    }
}
