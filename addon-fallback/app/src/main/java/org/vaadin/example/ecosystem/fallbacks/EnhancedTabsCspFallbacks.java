package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of EnhancedTabs
 * (com.flowingcode.vaadin.addons:enhanced-tabs-addon 1.2.2). Each declaration
 * was pasted from the message that Flow printed for the refused call; only
 * the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface EnhancedTabsCspFallbacks extends JsInvokers {

    @JsExpression("this.__detectOverflow()")
    PendingJavaScriptResult detectOverflow();

    @JsExpression("window.Vaadin.Flow.fcEnhancedTabsConnector.initLazy($0)")
    PendingJavaScriptResult initLazy(Object arg0);

    @JsExpression("this._item = {children:{}};")
    PendingJavaScriptResult initItem();

    @JsExpression("this.addEventListener('click', ()=>{let overlay = this.closest('vaadin-menu-bar-overlay, vaadin-context-menu-overlay'); overlay && overlay.close();})")
    PendingJavaScriptResult closeOverlayOnClick(Object arg0);
}
