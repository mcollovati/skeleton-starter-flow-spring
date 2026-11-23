package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of sortable-layout
 * (org.vaadin.jchristophe:sortable-layout 3.0.1). Each declaration was pasted
 * from the message that Flow printed for the refused call; only the method
 * names were changed.
 */
@JsDefinition
@JsStringFallback
public interface SortableLayoutCspFallbacks extends JsInvokers {

    @JsExpression("window.Vaadin.Flow.sortableConnector.initLazy($0, $1, $2)")
    PendingJavaScriptResult initLazy(Object arg0, Object arg1, Object arg2);
}
