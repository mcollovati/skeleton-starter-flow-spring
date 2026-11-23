package com.acme.addon;

import com.vaadin.flow.component.html.Span;

/**
 * Case 11 (browser only): an inline event handler attribute. Flow cannot see
 * this on the server; the browser refuses it because script-src has no
 * 'unsafe-inline' (and the nonce does not cover attributes).
 */
public class InlineHandler extends Span {

    public InlineHandler() {
        setText("Click me (inline onclick)");
        getStyle().set("cursor", "pointer").set("text-decoration", "underline");
        getElement().setAttribute("onclick",
                "this.textContent = 'inline onclick ran'");
    }
}
