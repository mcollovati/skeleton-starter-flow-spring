package org.vaadin.example.imports;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * M4: imports {@code res-format.js} from
 * {@code META-INF/resources/frontend} on the class path. Change
 * {@code namespace = "res"} to {@code namespace = "other"} and the expression
 * to {@code res.format($0)}: the hotswap has to import the file from
 * {@code generated/jar-resources}, as the build does.
 */
@JsDefinition
@JsDefinitionImport(from = "./res-format.js", namespace = "res")
public interface ImportResourceJs extends JsInvokers {

    @JsExpression("this.textContent = res.format($0)")
    void show(String value);
}
