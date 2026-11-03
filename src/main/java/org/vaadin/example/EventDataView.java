package org.vaadin.example;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Use cases for declared DOM event data ({@link DomEventData}) and filters
 * ({@link DomEventFilter}), from the Flow branch
 * 25658-event-data-expression-parameters.
 * <p>
 * Part 1 shows the cases that should work. Part 2 reproduces the findings of
 * the code review of that branch. Every case says what to do and what to
 * expect.
 */
@Route("event-data")
public class EventDataView extends VerticalLayout {

    /** A typed value that event data is decoded to. */
    public record Point(int x, int y) {
    }

    /**
     * Event data and filters. Each method returns a snippet; the expression
     * gets {@code event} and {@code element}, and the method arguments as
     * {@code $0}, {@code $1}, ...
     */
    @JsDefinition
    public interface EventJs extends Serializable {

        @JsExpression("return element.getAttribute($0);")
        DomEventData<String> attribute(String name);

        @JsExpression("return element.value;")
        DomEventData<String> value();

        @JsExpression("return event.detail;")
        DomEventData<Integer> clickCount();

        @JsExpression("return event.shiftKey;")
        DomEventData<Boolean> shiftKey();

        @JsExpression("return { x: Math.round(event.offsetX), y: Math.round(event.offsetY) };")
        DomEventData<Point> offset();

        @JsExpression("return ['shiftKey', 'ctrlKey', 'altKey', 'metaKey'].filter(k => event[k]);")
        DomEventData<List<String>> modifiers();

        @JsExpression("return Math.round(event.clientX - $0.getBoundingClientRect().left);")
        DomEventData<Integer> xRelativeTo(Component other);

        /** Null-safe: returns null when the captured element is null. */
        @JsExpression("return $0 === null ? null : $0.id;")
        DomEventData<String> capturedId(Element other);

        @JsExpression("throw new Error('event data snippet failed');")
        DomEventData<String> failing();

        @JsExpression("return event.key === $0 && !event.repeat;")
        DomEventFilter isKey(String key);

        @JsExpression("return event.key === $0 && event.ctrlKey === $1;")
        DomEventFilter isKeyWithCtrl(String key, boolean ctrl);

        @JsExpression("return element.value.length >= $0;")
        DomEventFilter minLength(int length);
    }

    private static final EventJs JS = JsSnippets.of(EventJs.class);

    // Elements captured by event data. Captures must be created after the
    // element is attached, so these cases are wired in onAttach.
    private final Div ruler = new Div("ruler (captured element)");
    private final Div capturedRuler = new Div("ruler2 (captured element)");
    private final NativeButton rulerButton = new NativeButton(
            "Click (captures the ruler)");
    private final Output rulerOut = new Output("detachedOut");

    private final Div lateTarget = new Div("late target");
    private final NativeButton lateButton = new NativeButton(
            "Click (two captures of the same element)");
    private final Output lateOut = new Output("lateOut");
    // Created in the constructor, while lateTarget is not attached yet
    private final DomEventData<String> earlyCapture;

    private final NativeButton relativeButton = new NativeButton(
            "Click anywhere on this wide button");
    private final Output relativeOut = new Output("relativeOut");

    public EventDataView() {
        add(new H3("DomEventData and DomEventFilter"));
        add(new Paragraph("The app runs with vaadin.csp=warn. Declared"
                + " snippets must not log a CSP warning. Only the cases"
                + " marked 'string API' should log one, and also the helper"
                + " buttons with a click listener (Remove ruler, Add ruler"
                + " back, Clear output), because Flow's ClickEvent uses string"
                + " event data."));

        add(new H3("Part 1: use cases that should work"));
        stringValue();
        sameMethodTwice();
        typedValues();
        relativeToCapturedElement();
        keyFilters();
        filterWithTwoCaptures();
        filterWithDebounce();
        mixedWithStringApi();

        add(new H3("Part 2: code review findings"));
        lateTarget.setId("lateTarget");
        earlyCapture = JS.capturedId(lateTarget.getElement());
        detachedCapture();
        captureCreatedBeforeAttach();
        debounceMerge();
        throwingEventData();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (!attachEvent.isInitialAttach()) {
            return;
        }
        // Finding 1: listener with a captured element, and an unrelated
        // plain listener for the same event on the same element
        DomEventData<String> rulerId = JS
                .capturedId(capturedRuler.getElement());
        rulerButton.getElement()
                .addEventListener("click", e -> rulerOut.append(
                        "listener with capture: " + e.getEventData(rulerId)))
                .addEventData(rulerId);
        rulerButton.getElement().addEventListener("click",
                e -> rulerOut.append("plain listener (no event data)"));

        // Finding 2: capture created now (attached) vs. in the constructor
        DomEventData<String> attachedCapture = JS
                .capturedId(lateTarget.getElement());
        lateButton.getElement().addEventListener("click", e -> lateOut
                .show("created in constructor: " + e.getEventData(earlyCapture)
                        + " | created in onAttach: "
                        + e.getEventData(attachedCapture)))
                .addEventData(earlyCapture).addEventData(attachedCapture);

        // Use case 4: value computed against another element
        DomEventData<Integer> x = JS.xRelativeTo(ruler);
        relativeButton.getElement().addEventListener("click",
                e -> relativeOut.show("x relative to the ruler = "
                        + typed(e.getEventData(x))))
                .addEventData(x);
    }

    // ---------------------------------------------------------------------
    // Part 1
    // ---------------------------------------------------------------------

    private void stringValue() {
        Output out = section("1. String value",
                "Click the button. Expect: 'hello from data-label (String)'.");
        NativeButton button = new NativeButton("Click");
        button.getElement().setAttribute("data-label", "hello from data-label");
        DomEventData<String> label = JS.attribute("data-label");
        button.getElement().addEventListener("click",
                e -> out.show(typed(e.getEventData(label))))
                .addEventData(label);
        addCase(out, button);
    }

    private void sameMethodTwice() {
        Output out = section("2. Same method, two captures, one listener",
                "Click. Expect: 'a = first, b = second'. The values must not"
                        + " mix up, although both use attribute(...).");
        NativeButton button = new NativeButton("Click");
        button.getElement().setAttribute("data-a", "first");
        button.getElement().setAttribute("data-b", "second");
        DomEventData<String> a = JS.attribute("data-a");
        DomEventData<String> b = JS.attribute("data-b");
        button.getElement()
                .addEventListener("click", e -> out.show("a = "
                        + e.getEventData(a) + ", b = " + e.getEventData(b)))
                .addEventData(a).addEventData(b);
        addCase(out, button);
    }

    private void typedValues() {
        Output out = section("3. Typed values (Integer, Boolean, record, List)",
                "Double-click with Shift held. Expect: clickCount=2 (Integer),"
                        + " shift=true (Boolean), offset=Point[x=.., y=..]"
                        + " (Point), modifiers=[shiftKey] (List...).");
        NativeButton button = new NativeButton("Click or double-click");
        DomEventData<Integer> count = JS.clickCount();
        DomEventData<Boolean> shift = JS.shiftKey();
        DomEventData<Point> offset = JS.offset();
        DomEventData<List<String>> modifiers = JS.modifiers();
        button.getElement().addEventListener("click", e -> {
            List<String> mods = e.getEventData(modifiers);
            out.show("clickCount=" + typed(e.getEventData(count)) + " shift="
                    + typed(e.getEventData(shift)) + " offset="
                    + typed(e.getEventData(offset)) + " modifiers="
                    + typed(mods));
        }).addEventData(count).addEventData(shift).addEventData(offset)
                .addEventData(modifiers);
        addCase(out, button);
    }

    private void relativeToCapturedElement() {
        section("4. Captured element as an argument",
                "Click near the left edge, then near the right edge of the"
                        + " wide button. Expect a small number, then a large"
                        + " number (pixels from the left edge of the ruler).");
        ruler.setId("ruler");
        ruler.getStyle().set("border", "1px dashed gray").set("width", "400px");
        relativeButton.setWidth("400px");
        add(ruler, relativeButton, relativeOut);
    }

    private void keyFilters() {
        Output out = section("5. Key filters (same method, two captures)",
                "Type in the field and press Enter, then Escape, then other"
                        + " keys. Expect: 'Enter: <text>' and 'Escape' lines"
                        + " only. Other keys send nothing. Holding Enter"
                        + " sends only one event (repeat is filtered).");
        Input input = new Input();
        DomEventData<String> value = JS.value();
        input.getElement()
                .addEventListener("keydown",
                        e -> out.append("Enter: " + e.getEventData(value)))
                .setFilter(JS.isKey("Enter")).addEventData(value);
        input.getElement()
                .addEventListener("keydown", e -> out.append("Escape"))
                .setFilter(JS.isKey("Escape"));
        addCase(out, input);
    }

    private void filterWithTwoCaptures() {
        Output out = section("6. Filter with two captures",
                "Press 's' and Ctrl+S in the field. Expect: 's' gives"
                        + " 'plain s', Ctrl+S gives 'Ctrl+S'. Note: the browser"
                        + " may also handle Ctrl+S (save page).");
        Input input = new Input();
        input.getElement().addEventListener("keydown", e -> out.append("plain s"))
                .setFilter(JS.isKeyWithCtrl("s", false));
        input.getElement().addEventListener("keydown", e -> out.append("Ctrl+S"))
                .setFilter(JS.isKeyWithCtrl("s", true));
        addCase(out, input);
    }

    private void filterWithDebounce() {
        Output out = section("7. Filter together with debounce",
                "Type fast in the field. Expect: nothing for the first two"
                        + " letters, then one line about 500 ms after you stop"
                        + " typing, with the current text.");
        Input input = new Input();
        DomEventData<String> value = JS.value();
        input.getElement()
                .addEventListener("input", e -> out.append(
                        "value = " + e.getEventData(value) + " (phase "
                                + e.getPhase() + ")"))
                .setFilter(JS.minLength(3)).addEventData(value).debounce(500);
        addCase(out, input);
    }

    private void mixedWithStringApi() {
        String stringData = "event.clientX";
        String stringFilter = "event.button === 0";
        Output out = section("8. Declared and string API on one listener"
                + " (string API)",
                "Click. Expect: declared clickCount, string event.clientX, and"
                        + " the raw event data JSON. Review finding 5: the"
                        + " string filter result can not be read under its"
                        + " expression any more (shows null), and the declared"
                        + " value is under a hashed key in the raw JSON.");
        NativeButton button = new NativeButton("Click");
        DomEventData<Integer> count = JS.clickCount();
        button.getElement().addEventListener("click", e -> out.show(
                "declared clickCount=" + e.getEventData(count)
                        + " | string clientX="
                        + e.getEventData().get(stringData)
                        + " | filter result under its expression="
                        + e.getEventData().get(stringFilter)
                        + " | raw=" + e.getEventData()))
                .addEventData(count).addEventData(stringData)
                .setFilter(stringFilter);
        addCase(out, button);
    }

    // ---------------------------------------------------------------------
    // Part 2: review findings
    // ---------------------------------------------------------------------

    private void detachedCapture() {
        section("F1. Captured element is removed (fixed in aa26a579d7)",
                "Click the button: two lines (capture = 'ruler2', and plain"
                        + " listener). Click 'Remove ruler', then click the"
                        + " button again. Expected: the capture is null and"
                        + " the plain listener still runs. 'Add ruler back'"
                        + " gives 'ruler2' again. Before the fix, nothing at"
                        + " all was sent (not even to the plain listener), and"
                        + " the browser console showed a TypeError.");
        ruler2Setup();
    }

    private void ruler2Setup() {
        // Separate from the ruler of use case 4, so removing it does not
        // break that case
        Div ruler2Holder = new Div();
        ruler2Holder.add(capturedRuler);
        capturedRuler.setId("ruler2");
        NativeButton remove = new NativeButton("Remove ruler",
                e -> ruler2Holder.remove(capturedRuler));
        NativeButton back = new NativeButton("Add ruler back",
                e -> ruler2Holder.add(capturedRuler));
        add(ruler2Holder, rulerButton, remove, back, rulerOut);
    }

    private void captureCreatedBeforeAttach() {
        section("F2. Capture created before the element is attached"
                + " (documented pitfall)",
                "Click. Expect 'created in constructor: null | created in"
                        + " onAttach: lateTarget'. The element is attached in"
                        + " both cases when the listener runs, but the first"
                        + " capture was encoded while it was detached.");
        add(lateTarget, lateButton, lateOut);
    }

    private void debounceMerge() {
        Output out = section("F3. Debounce settings of filters from the same"
                + " method (fixed in aa26a579d7)",
                "Press Enter, then Escape, in the first field (declared"
                        + " filters). Expected: 'declared Enter, phase LEADING'"
                        + " and, 300 ms later, 'declared Escape, phase"
                        + " TRAILING'. Before the fix, the Enter line never"
                        + " showed: both filters come from isKey(...), so"
                        + " Enter got the 300 ms trailing debounce of the"
                        + " Escape listener. The browser flushed that"
                        + " debouncer at once and sent Enter only as a"
                        + " 'trailing' event, which the Enter listener (no"
                        + " debounce) does not accept. The second field does"
                        + " the same with string filters (string API), for"
                        + " comparison.");
        Input declared = new Input();
        declared.setPlaceholder("declared filters");
        declared.getElement()
                .addEventListener("keydown",
                        e -> out.append("declared Enter, phase " + e.getPhase()))
                .setFilter(JS.isKey("Enter"));
        declared.getElement()
                .addEventListener("keydown",
                        e -> out.append("declared Escape, phase " + e.getPhase()))
                .setFilter(JS.isKey("Escape"))
                .debounce(300, DebouncePhase.TRAILING);

        Input strings = new Input();
        strings.setPlaceholder("string filters (string API)");
        strings.getElement()
                .addEventListener("keydown",
                        e -> out.append("string Enter, phase " + e.getPhase()))
                .setFilter("event.key === 'Enter'");
        strings.getElement()
                .addEventListener("keydown",
                        e -> out.append("string Escape, phase " + e.getPhase()))
                .setFilter("event.key === 'Escape'")
                .debounce(300, DebouncePhase.TRAILING);

        NativeButton clear = new NativeButton("Clear output",
                e -> out.clear());
        addCase(out, declared, strings, clear);
    }

    private void throwingEventData() {
        Output out = section("F4 (stand-in). Event data function throws",
                "A declared function that is missing from the bundle throws"
                        + " in the same place, so this case uses a snippet that"
                        + " throws. Click the first button: nothing is sent,"
                        + " not even to the plain listener, and the console"
                        + " shows the error. The second button does the same"
                        + " with a string expression (string API): it also"
                        + " sends nothing, so this behavior is not new.");
        NativeButton declared = new NativeButton("Declared, throws");
        DomEventData<String> failing = JS.failing();
        declared.getElement()
                .addEventListener("click",
                        e -> out.append("declared: listener with failing data"))
                .addEventData(failing);
        declared.getElement().addEventListener("click",
                e -> out.append("declared: plain listener"));

        NativeButton string = new NativeButton("String, throws");
        string.getElement()
                .addEventListener("click",
                        e -> out.append("string: listener with failing data"))
                .addEventData("(() => { throw new Error('string data failed') })()");
        string.getElement().addEventListener("click",
                e -> out.append("string: plain listener"));
        addCase(out, declared, string);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String typed(Object value) {
        return value == null ? "null"
                : value + " (" + value.getClass().getSimpleName() + ")";
    }

    /** Adds a title and a description, and returns a new output. */
    private Output section(String title, String expect) {
        add(new H4(title), new Paragraph(expect));
        return new Output(null);
    }

    /** Adds the components, then the output after them. */
    private void addCase(Output out, Component... components) {
        add(components);
        add(out);
    }

    /** Shows the result of a case. */
    private static class Output extends Div {
        Output(String id) {
            if (id != null) {
                setId(id);
            }
            getStyle().set("font-family", "monospace")
                    .set("white-space", "pre-wrap")
                    .set("border", "1px solid #ccc")
                    .set("padding", "0.25em 0.5em").set("min-height", "1.5em")
                    .set("width", "100%").set("box-sizing", "border-box");
        }

        void show(String text) {
            setText(text);
        }

        void append(String line) {
            String old = getText();
            setText(old == null || old.isEmpty() ? line : old + "\n" + line);
        }

        void clear() {
            setText("");
        }
    }
}
