package org.vaadin.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Reproducer: {@code DomListenerRegistration.addEventData(Class)} and
 * {@code DomEvent.getEventData(Class)} do not work together.
 * <p>
 * The client sends event data with flat keys such as
 * {@code {"event.button": 0}}, but {@code getEventData(Class)} passes that
 * JSON to Jackson as is, so a nested record gets {@code null}.
 */
@Route
public class MainView extends VerticalLayout {

    /** The example from the Javadoc of addEventData(Class). */
    public record MouseEventData(EventDetails event) {
        public record EventDetails(int button, int clientX, int clientY) {
        }
    }

    /** A record whose property names match the flat keys. */
    public record FlatMouseEventData(
            @JsonProperty("event.button") int button,
            @JsonProperty("event.clientX") int clientX) {
    }

    public MainView() {
        add(new H2("Case 1: Javadoc example (nested record)"));
        add(new Paragraph("Expected: MouseEventData[event=EventDetails[...]]. "
                + "Actual: MouseEventData[event=null]."));
        Div nested = clickArea();
        Pre nestedOut = new Pre();
        nested.getElement().addEventListener("click", e -> {
            MouseEventData data = e.getEventData(MouseEventData.class);
            nestedOut.setText("raw JSON: " + e.getEventData() + "\ndecoded:  "
                    + data);
        }).addEventData(MouseEventData.class);
        add(nested, nestedOut);

        add(new H2("Case 2: flat record with @JsonProperty"));
        add(new Paragraph("addEventData(Class) registers \"button\" and "
                + "\"clientX\" instead of the @JsonProperty names. These are "
                + "not valid expressions in the browser, so check the browser "
                + "console and whether the listener runs at all."));
        Div flat = clickArea();
        Pre flatOut = new Pre();
        flat.getElement().addEventListener("click", e -> {
            FlatMouseEventData data = e.getEventData(FlatMouseEventData.class);
            flatOut.setText("raw JSON: " + e.getEventData() + "\ndecoded:  "
                    + data);
        }).addEventData(FlatMouseEventData.class);
        add(flat, flatOut);

        add(new H2("Case 3: control, @JsonProperty record with string keys"));
        add(new Paragraph("Same record as case 2, but the expressions are "
                + "added as strings. Decoding works."));
        Div control = clickArea();
        Pre controlOut = new Pre();
        control.getElement().addEventListener("click", e -> {
            FlatMouseEventData data = e.getEventData(FlatMouseEventData.class);
            controlOut.setText("raw JSON: " + e.getEventData()
                    + "\ndecoded:  " + data);
        }).addEventData("event.button").addEventData("event.clientX");
        add(control, controlOut);
    }

    private static Div clickArea() {
        Div area = new Div("Click here");
        area.getStyle().setPadding("1em").setBorder("1px solid gray")
                .setCursor("pointer");
        return area;
    }
}
