package org.vaadin.example.shorthand;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsInvokers;

/**
 * A typo in the method name. The server can not know which methods the
 * element has, so the call fails only in the browser (a risk listed in
 * vaadin/flow#27441).
 */
@JsDefinition
public interface TypoJs extends JsInvokers {

    // return this.focuss();
    PendingJavaScriptResult focuss();
}
