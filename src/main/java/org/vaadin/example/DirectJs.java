package org.vaadin.example;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/** Control: a definition with no parent, used directly. */
@JsDefinition
public interface DirectJs extends JsInvokers {

    @JsExpression("this.textContent = 'direct v1: ' + $0")
    void show(String value);
}
