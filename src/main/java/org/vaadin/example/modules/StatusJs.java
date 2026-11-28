package org.vaadin.example.modules;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * The call after a module call, in the same round trip. It shows the version
 * of the connector that the module call set up, or {@code missing} when it
 * runs first.
 */
@JsDefinition
public interface StatusJs extends JsInvokers {

    @JsExpression("this.textContent = (this.$connector"
            + " ? this.$connector.version : 'missing') + ' ' + $0;")
    void show(String text);
}
