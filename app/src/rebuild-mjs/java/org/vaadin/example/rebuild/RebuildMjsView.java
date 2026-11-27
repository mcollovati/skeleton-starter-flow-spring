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
 * Review finding 2 on vaadin/flow#27457: a {@code .mjs} file, which
 * {@code vite.generated.ts} does not hash. Vite builds it, but the bundle
 * check asks for a new bundle on every build ("No hash info"). Added by the
 * {@code rebuild-mjs} profile only.
 */
@Route("rebuild-mjs")
public class RebuildMjsView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./app-plain.mjs", defaultExport = "imported")
    public interface ImportJs extends JsInvokers {

        @JsExpression("return imported($0);")
        PendingJavaScriptResult call(String text);
    }

    public RebuildMjsView() {
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
