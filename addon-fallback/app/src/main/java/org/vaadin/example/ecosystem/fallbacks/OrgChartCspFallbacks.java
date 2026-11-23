package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of OrgChartAddon
 * (com.flowingcode.vaadin.addons:orgchart-addon 5.4.0). Each declaration was
 * pasted from the message that Flow printed for the refused call; only the
 * method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface OrgChartCspFallbacks extends JsInvokers {

    @JsExpression("this.initializeOrgChart($0,$1,$2)")
    PendingJavaScriptResult initializeOrgChart(Object arg0, Object arg1, Object arg2);

    @JsExpression("this.addChildren($0, $1)")
    PendingJavaScriptResult addChildren(Object arg0, Object arg1);
}
