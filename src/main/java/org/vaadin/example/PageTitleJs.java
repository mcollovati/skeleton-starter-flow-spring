package org.vaadin.example;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * vaadin/flow#27441: called through {@code Page.executeJs(Class)}, which
 * refuses methods without {@code @JsExpression}. The check is cached per
 * class and cleared before a hotswap. Remove the annotation and apply: the
 * page call must now be refused. Add it back and apply: it must work again.
 */
@JsDefinition
public interface PageTitleJs extends JsInvokers {

    @JsExpression("document.title = $0")
    void setTitle(String title);
}
