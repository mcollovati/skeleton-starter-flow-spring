package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of vaadin-fullcalendar
 * (org.vaadin.stefan:fullcalendar2 8.0.0). Each declaration was pasted from
 * the message that Flow printed for the refused call; only the method names
 * were changed.
 */
@JsDefinition
@JsStringFallback
public interface FullCalendarCspFallbacks extends JsInvokers {

    @JsExpression("this.calendar.unselect()")
    PendingJavaScriptResult unselect();
}
