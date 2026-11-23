package org.vaadin.example;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippets;

/**
 * Snippets: one of the application's own snippet type, and the same
 * JavaScript as a built-in {@link JsFunction} for comparison.
 */
@JsDefinition
public interface GreetingJs extends JsSnippets {

    @JsExpression("return $0 + ', ' + name + '!';")
    Greeting custom(String greeting);

    @JsFunctionParameterNames({ "name" })
    @JsExpression("return $0 + ', ' + name + '!';")
    JsFunction builtIn(String greeting);
}
