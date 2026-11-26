package org.vaadin.example.shorthand;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;

/**
 * A snippet interface has no shorthand: every method needs
 * {@code @JsExpression}, so the definition is refused.
 */
@JsDefinition
public interface ShorthandSnippetsJs extends JsSnippets {

    JsSnippet focus();
}
