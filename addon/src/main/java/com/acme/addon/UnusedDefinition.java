package com.acme.addon;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * Not used by the application (vaadin/flow#27077). With {@code optimizeBundle}
 * the production build must leave out both the definition and the bundled
 * filter and event data of the event, because no route reaches this class.
 */
public class UnusedDefinition extends Div {

    @JsDefinition
    public interface UnusedJs extends JsInvokers {
        @JsExpression("return 'acme-unused-definition'")
        PendingJavaScriptResult unusedMarker();
    }

    @DomEvent(value = "keydown", filter = "event.key === 'AcmeUnusedKey'")
    public static class UnusedEvent extends ComponentEvent<UnusedDefinition> {
        public UnusedEvent(UnusedDefinition source, boolean fromClient,
                @EventData("'acme-unused-event-data'") String data) {
            super(source, fromClient);
        }
    }
}
