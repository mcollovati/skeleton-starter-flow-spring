package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of TwinColGrid
 * (com.flowingcode.vaadin.addons:twincolgrid 4.0.1). Each declaration was
 * pasted from the message that Flow printed for the refused call; only the
 * method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface TwinColGridCspFallbacks extends JsInvokers {

    @JsExpression("if (this.querySelector('vaadin-grid-flow-selection-column')) { this.querySelector('vaadin-grid-flow-selection-column').hidden = true }")
    PendingJavaScriptResult hideSelectionColumn();

    @JsExpression("fcTwinColGridAutoResize.observe($0)")
    PendingJavaScriptResult observeAutoResize(Object arg0);
}
