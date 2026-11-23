package com.acme.addon;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.shared.Registration;

/**
 * Control (works under strict): {@code @DomEvent} filter and
 * {@code @EventData} expressions in an add-on are found at build time and put
 * in the bundle, even though they are not property paths. This is the model
 * the string APIs above do not have.
 */
public class EnterField extends Input {

    @DomEvent(value = "keydown", filter = "event.key === 'Enter'")
    public static class EnterEvent extends ComponentEvent<EnterField> {
        private final String trimmed;

        public EnterEvent(EnterField source, boolean fromClient,
                @EventData("event.target.value.trim()") String trimmed) {
            super(source, fromClient);
            this.trimmed = trimmed;
        }

        public String getTrimmed() {
            return trimmed;
        }
    }

    public EnterField() {
        setPlaceholder("Type and press Enter");
    }

    public Registration addEnterListener(
            ComponentEventListener<EnterEvent> listener) {
        return addListener(EnterEvent.class, listener);
    }
}
