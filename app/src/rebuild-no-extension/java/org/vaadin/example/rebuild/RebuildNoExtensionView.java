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
 * Review finding 2 on vaadin/flow#27457: a path without a file extension,
 * {@code ./app-probe} for {@code app-probe.js}. Vite builds it, but the
 * bundle check asks for a new bundle on every build ("No file found"). Added
 * by the {@code rebuild-no-extension} profile only.
 */
@Route("rebuild-no-extension")
public class RebuildNoExtensionView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./app-probe", defaultExport = "imported")
    public interface ImportJs extends JsInvokers {

        @JsExpression("return imported($0);")
        PendingJavaScriptResult call(String text);
    }

    public RebuildNoExtensionView() {
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
