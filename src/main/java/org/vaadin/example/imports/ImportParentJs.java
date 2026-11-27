package org.vaadin.example.imports;

import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * M2: the import is on a parent without {@code @JsDefinition}. Change
 * {@code ./format-v1.js} to {@code ./format-v2.js} here; the definition that
 * runs it is {@link ImportParentChildJs}.
 */
@JsDefinitionImport(from = "./format-v1.js", defaultExport = "format")
public interface ImportParentJs extends JsInvokers {

    @JsExpression("this.textContent = 'parent ' + format($0)")
    void show(String value);
}
