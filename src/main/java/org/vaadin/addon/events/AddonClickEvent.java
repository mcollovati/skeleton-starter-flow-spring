package org.vaadin.addon.events;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;

/**
 * An event type from another package, like one from an add-on. Its filter is
 * not a property path, so the build has to find this class to put the filter
 * into the bundle.
 */
@DomEvent(value = "click", filter = "event.altKey === true")
public class AddonClickEvent extends ComponentEvent<Component> {

    public AddonClickEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }
}
