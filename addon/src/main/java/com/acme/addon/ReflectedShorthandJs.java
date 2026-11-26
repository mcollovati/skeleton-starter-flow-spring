package com.acme.addon;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsInvokers;

/**
 * Like {@link ReflectedJs}, but with a shorthand method (vaadin/flow#27441).
 * The application loads it only by its name, so with {@code optimizeBundle}
 * the bundle leaves it out, and vaadin/flow#27078 must report it although
 * the method has no {@code @JsExpression}.
 * <p>
 * No other definition declares {@code replaceChildren}, so no function of a
 * reached definition has the same identifier.
 */
@JsDefinition
public interface ReflectedShorthandJs extends JsInvokers {

    // return this.replaceChildren(...$0);
    void replaceChildren(String... texts);
}
