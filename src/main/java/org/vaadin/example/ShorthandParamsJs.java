package org.vaadin.example;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsInvokers;

/**
 * vaadin/flow#27441: change the parameters of a method without
 * {@code @JsExpression} (and its call in {@link MainView}). Its JavaScript
 * changes ({@code this.append($0)} to {@code this.append($0, $1)}), so the
 * dev loop must see a frontend change, as for a changed expression.
 */
@JsDefinition
public interface ShorthandParamsJs extends JsInvokers {

    void append(String text);
}
