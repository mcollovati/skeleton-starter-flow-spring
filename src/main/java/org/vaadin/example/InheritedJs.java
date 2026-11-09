package org.vaadin.example;

import java.util.List;

import com.vaadin.flow.js.JsDefinition;

/**
 * Definitions that inherit {@link ValueJs#parse(String)} and bind its type
 * variable.
 */
public final class InheritedJs {

    private InheritedJs() {
    }

    /** Binds {@code T} to {@code Integer}. */
    @JsDefinition
    public interface IntValueJs extends ValueJs<Integer> {
    }

    /** Binds {@code T} to a generic type. */
    @JsDefinition
    public interface PeopleValueJs extends ValueJs<List<Person>> {
    }

    /** Inherits {@code ValueJs} as a raw type, so it must be refused. */
    @SuppressWarnings("rawtypes")
    @JsDefinition
    public interface RawValueJs extends ValueJs {
    }
}
