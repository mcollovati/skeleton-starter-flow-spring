package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * Snippets: one of the application's own snippet type, and the same
 * JavaScript as a built-in {@link JsFunction} for comparison.
 */
@JsDefinition
public interface GreetingJs extends Serializable {

    @JsExpression("return $0 + ', ' + name + '!';")
    Greeting custom(String greeting);

    @JsCalledWith({ "name" })
    @JsExpression("return $0 + ', ' + name + '!';")
    JsFunction builtIn(String greeting);
}
