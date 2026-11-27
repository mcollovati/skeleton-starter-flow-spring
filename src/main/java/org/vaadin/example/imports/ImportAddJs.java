package org.vaadin.example.imports;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * M3: starts without an import. Add
 * {@code @JsDefinitionImport(from = "./format-v1.js", exports = "shout")} and
 * change the expression to {@code this.textContent = shout($0)}, then take
 * both away again.
 */
@JsDefinition
public interface ImportAddJs extends JsInvokers {

    @JsExpression("this.textContent = 'no import: ' + $0")
    void show(String value);
}
