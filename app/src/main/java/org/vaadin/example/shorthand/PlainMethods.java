package org.vaadin.example.shorthand;

/**
 * Does not extend {@code JsInvokers}, so a method without
 * {@code @JsExpression} declares nothing, also when a definition mixes this
 * interface in.
 */
public interface PlainMethods {

    void blur();
}
