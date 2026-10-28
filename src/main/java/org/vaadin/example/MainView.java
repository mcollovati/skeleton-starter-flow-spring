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
 * <p>
 * {@code addEventDetail(Class)} and {@code getEventDetail(Class)} have the
 * same problem for nested types, because the detail object is only rebuilt
 * one level deep. Both are fixed by vaadin/flow#26716 (cases 1 and 5).
 * <p>
 * Still open: {@code addEventData(Class)} and {@code addEventDetail(Class)}
 * ignore {@code @JsonProperty} (cases 2 and 6), and a {@code @JsonProperty}
 * name with dots on a nested property is split in the wrong place when the
 * nested objects are rebuilt (case 7).
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

    /** A flat detail record. */
    public record RgbColor(int r, int g, int b) {
    }

    /** A nested detail record. */
    public record DragDetail(Position start, Position end) {
        public record Position(int x, int y) {
        }
    }

    /** A detail record with a renamed property. */
    public record RenamedRgbColor(@JsonProperty("r") int red, int g, int b) {
    }

    /** A nested record with a dotted @JsonProperty name. */
    public record NestedDottedData(Inner event) {
        public record Inner(@JsonProperty("detail.x") int x) {
        }
    }

    public MainView() {
        add(new H2("Case 1: Javadoc example (nested record)"));
        add(new Paragraph("Expected: MouseEventData[event=EventDetails[...]]. "
                + "Before vaadin/flow#26716: MouseEventData[event=null]."));
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

        add(new H2("Case 4: control, flat detail record"));
        add(new Paragraph("Clicking fires a \"color-change\" event with "
                + "detail {r: 255, g: 128, b: 64}. Decoding works."));
        Div flatDetail = customEventArea("color-change",
                "{r: 255, g: 128, b: 64}");
        Pre flatDetailOut = new Pre();
        flatDetail.getElement().addEventListener("color-change", e -> {
            RgbColor data = e.getEventDetail(RgbColor.class);
            flatDetailOut.setText("raw JSON: " + e.getEventData()
                    + "\ndecoded:  " + data);
        }).addEventDetail(RgbColor.class);
        add(flatDetail, flatDetailOut);

        add(new H2("Case 5: nested detail record"));
        add(new Paragraph("Clicking fires a \"drag\" event with detail "
                + "{start: {x: 1, y: 2}, end: {x: 3, y: 4}}. Expected: "
                + "DragDetail[start=Position[x=1, y=2], ...]. Before "
                + "vaadin/flow#26716: DragDetail[start=null, end=null]."));
        Div nestedDetail = customEventArea("drag",
                "{start: {x: 1, y: 2}, end: {x: 3, y: 4}}");
        Pre nestedDetailOut = new Pre();
        nestedDetail.getElement().addEventListener("drag", e -> {
            DragDetail data = e.getEventDetail(DragDetail.class);
            nestedDetailOut.setText("raw JSON: " + e.getEventData()
                    + "\ndecoded:  " + data);
        }).addEventDetail(DragDetail.class);
        add(nestedDetail, nestedDetailOut);

        add(new H2("Case 6: detail record with @JsonProperty"));
        add(new Paragraph("Same event as case 4. addEventDetail(Class) "
                + "registers \"event.detail.red\" instead of the "
                + "@JsonProperty name \"r\". Expected: red=255. Actual: "
                + "red=0."));
        Div renamedDetail = customEventArea("color-change",
                "{r: 255, g: 128, b: 64}");
        Pre renamedDetailOut = new Pre();
        renamedDetail.getElement().addEventListener("color-change", e -> {
            RenamedRgbColor data = e.getEventDetail(RenamedRgbColor.class);
            renamedDetailOut.setText("raw JSON: " + e.getEventData()
                    + "\ndecoded:  " + data);
        }).addEventDetail(RenamedRgbColor.class);
        add(renamedDetail, renamedDetailOut);

        add(new H2("Case 7: nested property with a dotted @JsonProperty"));
        add(new Paragraph("Clicking fires a \"pick\" event with detail "
                + "{x: 5}. The expression \"event.detail.x\" is added as a "
                + "string, so the value is sent. The nested objects are "
                + "rebuilt by splitting the key on every dot, but Jackson "
                + "expects {\"event\": {\"detail.x\": 5}}. Expected: "
                + "NestedDottedData[event=Inner[x=5]]. Actual: x=0."));
        Div dotted = customEventArea("pick", "{x: 5}");
        Pre dottedOut = new Pre();
        dotted.getElement().addEventListener("pick", e -> {
            NestedDottedData data = e.getEventData(NestedDottedData.class);
            dottedOut.setText("raw JSON: " + e.getEventData()
                    + "\ndecoded:  " + data);
        }).addEventData("event.detail.x");
        add(dotted, dottedOut);
    }

    /**
     * Creates a click area that fires a custom event with the given detail
     * when clicked.
     */
    private static Div customEventArea(String eventName, String detail) {
        Div area = clickArea();
        area.getElement().executeJs("this.addEventListener('click', () => "
                + "this.dispatchEvent(new CustomEvent($0, {detail: " + detail
                + "})))", eventName);
        return area;
    }

    private static Div clickArea() {
        Div area = new Div("Click here");
        area.getStyle().setPadding("1em").setBorder("1px solid gray")
                .setCursor("pointer");
        return area;
    }
}
