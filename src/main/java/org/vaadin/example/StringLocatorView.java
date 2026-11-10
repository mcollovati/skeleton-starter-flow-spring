package org.vaadin.example;

import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * The locator is a JavaScript expression, the String overload that the PR
 * deprecates. The browser compiles it, so in warn mode this view logs one
 * CSP warning, and in strict mode it fails, also with the PR.
 */
@Route("string-locator")
public class StringLocatorView extends AbstractDelegateView {

    public StringLocatorView() {
        super("Browser-only element, String locator");
    }

    @Override
    @SuppressWarnings({ "deprecation", "removal" })
    protected void setLocator(Div host, String overlayId) {
        Shortcuts.setShortcutListenOnElement(
                "document.getElementById('" + overlayId + "')", host);
    }
}
