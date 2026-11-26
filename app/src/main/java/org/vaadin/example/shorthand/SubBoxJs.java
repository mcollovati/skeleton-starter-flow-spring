package org.vaadin.example.shorthand;

import com.acme.addon.ShorthandBox;

import com.vaadin.flow.js.JsDefinition;

/**
 * Declares nothing itself and inherits the shorthand methods of
 * {@link ShorthandBox.BoxJs}. The interface that declares a method decides,
 * and that one extends {@code JsInvokers}, so they still call the element
 * method.
 */
@JsDefinition
public interface SubBoxJs extends ShorthandBox.BoxJs {
}
