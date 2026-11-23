package org.vaadin.example;

import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * A parent without {@code @JsDefinition}: it is never used directly, only
 * through {@link PlainParentChildJs}.
 */
public interface PlainParentJs extends JsInvokers {

    @JsExpression("this.textContent = 'plain parent v1: ' + $0")
    void show(String value);
}
