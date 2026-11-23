package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of Zoomist
 * (com.flowingcode.vaadin.addons:zoomist-addon 1.3.0). The declaration was
 * taken from the add-on code (the string the released jar sends), since the
 * test did not reach this call before the fallbacks were in place; it matched
 * at runtime.
 */
@JsDefinition
@JsStringFallback
public interface ZoomistCspFallbacks extends JsInvokers {

    @JsExpression("return this.containerData")
    PendingJavaScriptResult containerData();
}
