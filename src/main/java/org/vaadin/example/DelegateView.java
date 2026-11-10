package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * The locator is a declared function, the new
 * setShortcutListenOnElementLocator(JsFunction, Component) of the PR. main does not
 * have this overload: delete this file to run the app against main.
 */
@Route("delegate")
public class DelegateView extends AbstractDelegateView {

    @JsDefinition
    public interface LocatorJs extends Serializable {

        @JsExpression("return document.getElementById($0);")
        JsFunction byId(String id);
    }

    public DelegateView() {
        super("Browser-only element, declared locator");
    }

    @Override
    protected void setLocator(Div host, String overlayId) {
        // A new function each time, for the same declared call
        Shortcuts.setShortcutListenOnElementLocator(
                JsSnippets.of(LocatorJs.class).byId(overlayId), host);
    }
}
