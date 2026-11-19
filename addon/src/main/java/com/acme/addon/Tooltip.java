package com.acme.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.JsFunction;

/**
 * Case 9: {@code JsFunction.of(String, Object...)}, here given to
 * {@code addJsInitializer(JsFunction)}.
 */
public final class Tooltip {

    private Tooltip() {
    }

    public static void attach(Component component, String text) {
        component.getElement()
                .addJsInitializer(JsFunction.of("this.title = $0", text));
    }
}
