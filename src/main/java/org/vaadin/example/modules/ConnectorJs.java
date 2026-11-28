package org.vaadin.example.modules;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsModuleInvokers;

/**
 * A module definition (vaadin/flow#27425): its methods run with
 * {@code connector-v1.js} as this. J1 changes the module to
 * {@code ./connector-v2.js}, J2 changes the expression of
 * {@link #initLazyTagged}.
 */
@JsDefinition
@JsDefinitionImport(from = "./connector-v1.js")
public interface ConnectorJs extends JsModuleInvokers {

    void initLazy(Element element);

    @JsExpression("this.initLazy($0); $0.$connector.version += ' expr-v1';")
    void initLazyTagged(Element element);
}
