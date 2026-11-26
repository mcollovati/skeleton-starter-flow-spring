package org.vaadin.example.shorthand;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsInvokers;

/**
 * Mixes in {@link PlainMethods#blur()}, which is declared in an interface that
 * does not extend {@code JsInvokers}. So it needs {@code @JsExpression}, and
 * the definition is refused.
 */
@JsDefinition
public interface MixedInJs extends JsInvokers, PlainMethods {
}
