package org.vaadin.example.modules;

import org.vaadin.example.modules.ModuleDefinitions.OrderJs;
import org.vaadin.example.modules.ModuleDefinitions.SharedModuleJs;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsModuleInvokers;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * The only route that reaches {@link OtherOnlyJs}, so only its chunk
 * registers {@code app-module-other.js} (vaadin/flow#27425, rows F1 and M10).
 */
@Route("module-other")
public class ModuleOtherView extends Div {

    /** F1: reached only from this view. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module-other.js")
    public interface OtherOnlyJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    public ModuleOtherView() {
        add(new H1("Other view (vaadin/flow#27425)"));
        RouterLink back = new RouterLink("Back to the module calls (router)",
                ModuleCallsView.class);
        back.setId("to-calls");
        add(back);

        add(new Span("o1: OtherOnlyJs.initLazy and a call after it, on attach"));
        Div o1 = new Div("o1 box");
        o1.setId("o1-box");
        add(o1);
        addAttachListener(event -> {
            event.getUI().getPage().executeModuleJs(OtherOnlyJs.class)
                    .initLazy(o1.getElement());
            o1.getElement().executeJs(OrderJs.class).record("after");
        });

        Span result = new Span();
        result.setId("o10-result");
        NativeButton increment = new NativeButton("o10: increment", event -> getUI()
                .orElseThrow().getPage().executeModuleJs(SharedModuleJs.class)
                .increment().then(String.class,
                        value -> result.setText("value " + value),
                        error -> result.setText("failed: " + error)));
        increment.setId("o10-run");
        add(new Div(increment, result));
    }
}
