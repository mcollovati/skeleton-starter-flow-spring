package org.vaadin.example.imports;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * M1: change only the module, {@code ./format-v1.js} to
 * {@code ./format-v2.js}. The JavaScript stays the same, so only the import
 * tells the new function apart. M6: change {@code defaultExport} to
 * {@code "default"}, which is refused, and back.
 */
@JsDefinition
@JsDefinitionImport(from = "./format-v1.js", defaultExport = "format")
public interface ImportDirectJs extends JsInvokers {

    @JsExpression("this.textContent = format($0)")
    void show(String value);
}
