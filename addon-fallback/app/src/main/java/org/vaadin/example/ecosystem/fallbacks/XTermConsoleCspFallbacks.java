package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of XTermConsoleAddon
 * (com.flowingcode.addons:xterm-console 3.3.0). Each declaration was pasted
 * from the message that Flow printed for the refused call; only the method
 * names were changed.
 */
@JsDefinition
@JsStringFallback
public interface XTermConsoleCspFallbacks extends JsInvokers {

    @JsExpression("this.insertMode=$0")
    PendingJavaScriptResult setInsertMode(Object arg0);

    @JsExpression("this.terminal.options[$0]=$1")
    PendingJavaScriptResult setOption(Object arg0, Object arg1);

    @JsExpression("return this.terminal[$0]($1)")
    PendingJavaScriptResult call1(Object arg0, Object arg1);
}
