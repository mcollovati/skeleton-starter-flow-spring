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
 * Fix c7f802e9e4 on vaadin/flow#27457: a jar file imported with a query,
 * {@code ./acme-style.css?inline} from the {@code META-INF/frontend} of the
 * add-on. Before the fix it resolved to the project folder, where there is
 * no such file. Added by the {@code rebuild-jar-query} profile only.
 */
@Route("rebuild-jar-query")
public class RebuildJarQueryView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./acme-style.css?inline", defaultExport = "imported")
    public interface ImportJs extends JsInvokers {

        // The Vite config of Flow gives a CSS file imported with ?inline as a
        // Lit CSSResult, not as a string
        @JsExpression("return String(imported.cssText ?? imported).trim();")
        PendingJavaScriptResult call(String text);
    }

    public RebuildJarQueryView() {
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
