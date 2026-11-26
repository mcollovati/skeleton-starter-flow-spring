package org.vaadin.example;

import com.vaadin.flow.js.JsInvokers;

/**
 * vaadin/flow#27441: a parent without {@code @JsDefinition} whose only method
 * has no {@code @JsExpression}. Used only through
 * {@link ShorthandParentChildJs}. Before #27441 the hotswapper did not see
 * that this interface declares JavaScript, so changing it did not regenerate
 * the JavaScript of the child. Add or remove
 * {@code @JsExpression("this.textContent = 'parent v2: ' + $0")} and apply.
 */
public interface ShorthandParentJs extends JsInvokers {

    void append(String text);
}
