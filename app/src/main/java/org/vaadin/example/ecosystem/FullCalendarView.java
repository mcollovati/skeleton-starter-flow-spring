package org.vaadin.example.ecosystem;

import org.vaadin.stefan.fullcalendar.FullCalendar;

import com.vaadin.flow.router.Route;

/**
 * vaadin-fullcalendar (org.vaadin.stefan:fullcalendar2 8.0.0) with the
 * defaults. Its client compiles eventDidMount with new Function, which only
 * the browser refuses: not supported.
 */
@Route("eco/fullcalendar")
public class FullCalendarView extends EcoView {

    public FullCalendarView() {
        super("vaadin-fullcalendar (defaults)");
        FullCalendar calendar = withId(new FullCalendar(), "calendar");
        calendar.setHeight("500px");
        add(calendar);
    }
}
