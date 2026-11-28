package com.acme.addon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsModuleInvokers;

/**
 * An add-on component that sets up its connector the way Grid does
 * (vaadin/flow#27425): on attach, it calls {@code initLazy} of a module in the
 * add-on jar ({@code META-INF/frontend/acme-module.js}) and, in the same round
 * trip, a call that uses {@code this.$connector}. The second call must find
 * the connector.
 */
public class ModuleBox extends Div {

    /** Calls the exports of {@code acme-module.js}, with it as this. */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-module.js")
    public interface ModuleBoxJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    /** Runs on the element, after the connector is set up. */
    @JsDefinition
    public interface ModuleBoxConnectedJs extends JsInvokers {

        @JsExpression("this.dataset.order = ((this.dataset.order ?? '') + ' '"
                + " + $0 + ':' + (this.$connector ? 'ready' : 'missing'))"
                + ".trim();")
        void record(String label);
    }

    public ModuleBox(String text) {
        setText(text);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().executeModuleJs(ModuleBoxJs.class)
                .initLazy(getElement());
        getElement().executeJs(ModuleBoxConnectedJs.class).record("after");
    }
}
