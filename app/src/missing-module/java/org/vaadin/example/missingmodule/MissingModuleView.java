package org.vaadin.example.missingmodule;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * A reached definition that imports a module that does not exist
 * (vaadin/flow#27423; unresolved modules are vaadin/flow#27424). Added by the
 * {@code missing-module} profile only, to see what the build does.
 */
@Route("missing-module")
public class MissingModuleView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./acme-does-not-exist.js", defaultExport = "missing")
    public interface MissingModuleJs extends JsInvokers {

        @JsExpression("return missing($0);")
        PendingJavaScriptResult call(String text);
    }

    public MissingModuleView() {
        NativeButton run = new NativeButton("Run",
                event -> getElement().executeJs(MissingModuleJs.class)
                        .call("x"));
        run.setId("run");
        add(run);
    }
}
