package com.acme.addon;

import com.vaadin.flow.component.UI;

/**
 * Case 5: the same string sent with a different number of parameters each
 * time. The add-on builds the {@code $0, $1, ...} list from the arguments.
 */
public final class ConsoleLogger {

    private ConsoleLogger() {
    }

    public static void log(UI ui, Object... values) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            placeholders.append(i == 0 ? "" : ", ").append('$').append(i);
        }
        // Same text for one argument count, different text for another.
        ui.getPage().executeJs("console.log('[acme]', " + placeholders + ")",
                values);
    }
}
