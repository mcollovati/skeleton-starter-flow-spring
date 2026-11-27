package org.vaadin.example.fallbackimport;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * A {@code @JsStringFallback} interface with {@code @JsDefinitionImport}
 * (vaadin/flow#27423), which the annotation refuses: the string a fallback
 * stands in for can not name what a module exports. Added by the
 * {@code fallback-import} profile only, to see whether one refused fallback
 * interface affects the fallbacks of {@code AcmeCspFallbacks}.
 */
@JsDefinition
@JsStringFallback
@JsDefinitionImport(from = "./app-format.js", exports = "shout")
public interface FallbackImportJs extends JsInvokers {

    @JsExpression("document.title = 'acme-fallback-import'")
    void setTitle();
}
