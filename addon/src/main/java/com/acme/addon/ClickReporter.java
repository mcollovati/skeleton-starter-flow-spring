package com.acme.addon;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.JsFunction;

/**
 * Case 15: {@code JsFunction.of(String, Object...)} with a runtime argument
 * named by {@code withArguments}, given to {@code Element.executeJs(String,
 * Object...)} that calls it on every click.
 */
public class ClickReporter extends Span {

    public ClickReporter(String prefix) {
        setText("click here");
        JsFunction report = JsFunction
                .of("$0.textContent = $1 + type", getElement(), prefix)
                .withArguments("type");
        getElement().executeJs(
                "this.addEventListener('click', e => $0(e.type))", report);
    }
}
