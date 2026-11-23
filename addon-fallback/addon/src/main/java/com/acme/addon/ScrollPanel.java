package com.acme.addon;

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.component.html.Div;

/**
 * Case 2: constant {@code Element.executeJs(String)} calls, one without
 * parameters and one that returns a value.
 */
public class ScrollPanel extends Div {

    public ScrollPanel() {
        getStyle().set("overflow", "auto").set("height", "80px")
                .set("border", "1px solid gray");
    }

    public void scrollToBottom() {
        getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    public CompletableFuture<Double> getScrollTop() {
        return getElement().executeJs("return this.scrollTop")
                .toCompletableFuture(Double.class);
    }
}
