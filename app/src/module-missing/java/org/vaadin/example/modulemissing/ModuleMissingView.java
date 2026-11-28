package org.vaadin.example.modulemissing;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsModuleInvokers;
import com.vaadin.flow.router.Route;

/**
 * U9 (vaadin/flow#27425 with #27424): a module definition, reached from this
 * route, whose module does not exist. The chunk writer must write no static
 * import for it, so the build is not stopped by it. Once #27424 is in, the
 * call throws on the server.
 */
@Route("module-missing")
public class ModuleMissingView extends Div {

    @JsDefinition
    @JsDefinitionImport(from = "./acme-module-does-not-exist.js")
    public interface MissingModuleJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    public ModuleMissingView() {
        add(new H1("Module definition, module missing (vaadin/flow#27425)"));
        Div box = new Div("u9 box");
        box.setId("u9-box");
        Span result = new Span();
        result.setId("u9-result");
        NativeButton run = new NativeButton("Run", event -> {
            try {
                getUI().orElseThrow().getPage()
                        .executeModuleJs(MissingModuleJs.class)
                        .initLazy(box.getElement());
                result.setText("called");
            } catch (RuntimeException e) {
                result.setText("failed: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        });
        run.setId("u9-run");
        add(box, run, result);
    }
}
