package com.acme.addon;

import java.util.function.IntConsumer;

import com.vaadin.flow.component.html.Input;

/**
 * Case 7: deprecated {@code addEventData(String)} with an expression that is
 * not a property path ({@code trim()} is a call).
 */
public class CharCounter extends Input {

    private static final String DATA = "event.target.value.trim().length";

    @SuppressWarnings("deprecation")
    public CharCounter(IntConsumer onCount) {
        setPlaceholder("Type here");
        getElement().addEventListener("input",
                e -> onCount.accept(e.getEventData().get(DATA).asInt()))
                .addEventData(DATA);
    }
}
