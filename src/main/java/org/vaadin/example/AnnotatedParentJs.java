package org.vaadin.example;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/** A parent with {@code @JsDefinition}, used through {@link AnnotatedParentChildJs}. */
@JsDefinition
public interface AnnotatedParentJs extends JsInvokers {

    @JsExpression("this.textContent = 'annotated parent v1: ' + $0")
    void show(String value);
}
