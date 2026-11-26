package org.vaadin.example;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsInvokers;

/**
 * vaadin/flow#27441: a method without {@code @JsExpression} calls the element
 * method of the same name ({@code return this.append($0);}). Add or remove
 * {@code @JsExpression("this.textContent = 'direct v2: ' + $0")} and apply.
 */
@JsDefinition
public interface ShorthandDirectJs extends JsInvokers {

    void append(String text);
}
