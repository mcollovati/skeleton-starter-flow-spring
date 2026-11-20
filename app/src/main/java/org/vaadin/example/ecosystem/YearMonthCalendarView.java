package org.vaadin.example.ecosystem;

import java.time.YearMonth;

import com.flowingcode.addons.ycalendar.MonthCalendar;
import com.vaadin.flow.router.Route;

/**
 * YearMonthCalendarAddon (org.vaadin.addons.flowingcode:year-month-calendar
 * 4.6.2).
 */
@Route("eco/year-month-calendar")
public class YearMonthCalendarView extends EcoView {

    public YearMonthCalendarView() {
        super("YearMonthCalendarAddon");
        run(() -> {
            MonthCalendar calendar = withId(
                    new MonthCalendar(YearMonth.of(2026, 1)), "calendar");
            calendar.setClassNameGenerator(
                    d -> d.getDayOfMonth() == 15 ? "holiday" : null);
            add(calendar);
        });
    }
}
