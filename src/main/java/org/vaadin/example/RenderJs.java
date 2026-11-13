package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * Runs on the element: calls the snippet it gets and shows the result.
 */
@JsDefinition
public interface RenderJs extends Serializable {

    @JsExpression("this.textContent = $0('native image');")
    void renderCustom(Greeting greeting);

    @JsExpression("this.textContent = $0('native image');")
    void renderBuiltIn(JsFunction greeting);
}
