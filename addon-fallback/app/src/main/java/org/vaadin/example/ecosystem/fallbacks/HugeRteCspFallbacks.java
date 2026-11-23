package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of hugerte-for-flow
 * (org.parttio:hugerte-for-flow 2.3.0). The declaration was taken from the
 * add-on code (the string the released jar sends), since the test did not
 * reach this call before the fallbacks were in place; it matched at runtime.
 */
@JsDefinition
@JsStringFallback
public interface HugeRteCspFallbacks extends JsInvokers {

    @JsExpression("this.value = $0")
    PendingJavaScriptResult setValue(Object arg0);
}
