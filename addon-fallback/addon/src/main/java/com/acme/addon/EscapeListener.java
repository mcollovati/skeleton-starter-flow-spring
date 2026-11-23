package com.acme.addon;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Case 6: {@code DomListenerRegistration.setFilter(String)} with a filter that
 * is not a property path, so the browser must compile it.
 */
public class EscapeListener extends Input {

    public EscapeListener(SerializableRunnable onEscape) {
        setPlaceholder("Press Escape here");
        getElement().addEventListener("keydown", e -> onEscape.run())
                .setFilter("event.key === 'Escape'");
    }
}
