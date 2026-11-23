package com.acme.addon;

import com.vaadin.flow.component.Component;

/**
 * Case 4: the JavaScript string is built by concatenation, so its text changes
 * with the input. No fixed list of strings can match all calls.
 */
public final class Highlighter {

    private Highlighter() {
    }

    public static void highlight(Component component, String color) {
        component.getElement().executeJs(
                "this.style.outline = '3px solid " + color + "'");
    }
}
