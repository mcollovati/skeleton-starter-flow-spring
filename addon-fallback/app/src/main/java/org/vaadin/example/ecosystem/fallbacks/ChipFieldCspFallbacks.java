package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of ChipFieldAddon
 * (com.flowingcode.vaadin.addons.chipfield:chipfield-addon 2.8.0). Each
 * declaration was pasted from the message that Flow printed for the refused
 * call; only the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface ChipFieldCspFallbacks extends JsInvokers {

    @JsExpression("this.splice('items', 0, this.items.length);")
    PendingJavaScriptResult clearItems();

    @JsExpression("this.required = false; this.autoValidate = false; this._value = '';")
    PendingJavaScriptResult resetInput();

    @JsExpression("this.push('items', $0);")
    PendingJavaScriptResult pushItem(Object arg0);
}
