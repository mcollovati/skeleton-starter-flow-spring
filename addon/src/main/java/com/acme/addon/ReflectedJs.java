package com.acme.addon;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * A definition that the application loads only by its name, through
 * reflection (vaadin/flow#27078). The bytecode scanner can not follow that, so
 * with {@code optimizeBundle} its JavaScript is left out of the production
 * bundle unless a reached class names it with {@code @Uses}.
 */
@JsDefinition
public interface ReflectedJs extends JsInvokers {

    @JsExpression("this.textContent = 'Marked by ReflectedJs'")
    void mark();
}
