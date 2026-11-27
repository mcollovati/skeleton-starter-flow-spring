package com.acme.addon;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * Like {@link ReflectedJs}, but its JavaScript imports a module of the add-on
 * (vaadin/flow#27423). The application loads it only by its name, so with
 * {@code optimizeBundle} the bundle leaves out the function and the module,
 * and vaadin/flow#27078 must report it.
 */
@JsDefinition
@JsDefinitionImport(from = "./acme-reflected-import.js", exports = "replaceWith")
public interface ReflectedImportJs extends JsInvokers {

    @JsExpression("replaceWith(this, $0)")
    void replace(String text);
}
