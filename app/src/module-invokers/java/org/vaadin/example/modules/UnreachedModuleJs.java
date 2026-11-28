package org.vaadin.example.modules;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsModuleInvokers;

/**
 * F2 (vaadin/flow#27425): a module definition that no route reaches.
 * {@link ModuleCallsView} calls it only through reflection, so with
 * {@code optimizeBundle} the production bundle does not have it at all and
 * vaadin/flow#27078 logs an error, as for the other kinds of definitions.
 */
@JsDefinition
@JsDefinitionImport(from = "./app-module-unreached.js")
public interface UnreachedModuleJs extends JsModuleInvokers {

    void initLazy(Element element);
}
