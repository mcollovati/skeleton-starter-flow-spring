package org.vaadin.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;

/**
 * Shows the shortcuts that fired on the server, newest first, as
 * "number-name".
 */
public class EventLog extends Div {

    private int counter;

    public EventLog() {
        setId("log");
        addClassName("log");
    }

    public void log(String name) {
        addComponentAsFirst(new Div(counter++ + "-" + name));
    }

    /** A button that empties the log. */
    public NativeButton clearButton() {
        NativeButton clear = new NativeButton("Clear log", e -> {
            removeAll();
            counter = 0;
        });
        clear.setId("clear-log");
        return clear;
    }
}
