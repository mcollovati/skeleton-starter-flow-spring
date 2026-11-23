package org.vaadin.example.ecosystem;

import org.vaadin.stefan.fullcalendar.FullCalendar;

import com.vaadin.flow.router.Route;

/**
 * vaadin-fullcalendar without the client-side entry id snippet, selectable,
 * and a button that clears the selection ("this.calendar.unselect()").
 */
@Route("eco/fullcalendar-select")
public class FullCalendarSelectView extends EcoView {

    public FullCalendarSelectView() {
        super("vaadin-fullcalendar (selectable)");
        FullCalendar calendar = withId(new FullCalendar(), "calendar");
        calendar.setAutoProvideEntryIdOnClient(false);
        calendar.options().setSelectable(true);
        calendar.setHeight("500px");
        var selected = output("selected");
        calendar.addTimeslotsSelectedListener(e -> selected.setText("selected"));
        add(calendar, selected);
        add(button("clear", "Clear selection", calendar::clearSelection));
    }
}
