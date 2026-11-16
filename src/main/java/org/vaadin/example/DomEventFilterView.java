package org.vaadin.example;

import org.vaadin.addon.events.AddonClickEvent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DebounceSettings;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.KeyUpEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#27023: {@code @DomEvent} filters that are not property
 * paths run from the bundle, so they work with {@code vaadin.csp=strict}. See
 * DOM-EVENT-FILTER-NOTES.md for the case table.
 */
@Route("dom-event-filters")
public class DomEventFilterView extends Div {

    @Tag("input")
    public static class TextInput extends Component {
        public TextInput(String id) {
            setId(id);
        }
    }

    @Tag("input")
    public static class Checkbox extends Component {
        public Checkbox(String id) {
            setId(id);
            getElement().setAttribute("type", "checkbox");
        }
    }

    @Tag("button")
    public static class PlainButton extends Component {
        public PlainButton(String id, String text) {
            setId(id);
            getElement().setText(text);
        }
    }

    // F1: an expression on the event
    @DomEvent(value = "keydown", filter = "event.key === 'Enter' && !event.repeat")
    public static class EnterEvent extends ComponentEvent<TextInput> {
        public EnterEvent(TextInput source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    // F2: an expression on the element the listener is on
    @DomEvent(value = "click", filter = "element.dataset.enabled === 'true'")
    public static class EnabledClickEvent extends ComponentEvent<PlainButton> {
        public EnabledClickEvent(PlainButton source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    // F3: a filter together with event data paths
    @DomEvent(value = "click", filter = "event.detail > 0 && !event.shiftKey")
    public static class DataClickEvent extends ComponentEvent<PlainButton> {
        private final int detail;
        private final String type;

        public DataClickEvent(PlainButton source, boolean fromClient,
                @EventData("event.detail") int detail,
                @EventData("event.type") String type) {
            super(source, fromClient);
            this.detail = detail;
            this.type = type;
        }
    }

    // F4: the filter also decides whether the default is prevented
    @DomEvent(value = "click", filter = "element.dataset.locked === 'yes'", preventDefault = true)
    public static class LockedClickEvent extends ComponentEvent<Component> {
        public LockedClickEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    // F5: a filter together with debounce
    @DomEvent(value = "input", filter = "element.value.length >= 3", debounce = @DebounceSettings(timeout = 2300, phases = DebouncePhase.TRAILING))
    public static class LongInputEvent extends ComponentEvent<TextInput> {
        private final String value;

        public LongInputEvent(TextInput source, boolean fromClient,
                @EventData("element.value") String value) {
            super(source, fromClient);
            this.value = value;
        }
    }

    // F7: the same expression as a filter of one type and event data of
    // another, which share one function in the bundle
    @DomEvent(value = "click", filter = "event.button === 0")
    public static class PrimaryClickEvent extends ComponentEvent<PlainButton> {
        public PrimaryClickEvent(PlainButton source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @DomEvent("click")
    public static class ButtonDataEvent extends ComponentEvent<PlainButton> {
        private final boolean primary;

        public ButtonDataEvent(PlainButton source, boolean fromClient,
                @EventData("event.button === 0") boolean primary) {
            super(source, fromClient);
            this.primary = primary;
        }
    }

    // F10: a property path filter with preventDefault. The server sends
    // "(element.dataset.locked) && event.preventDefault()", which the client
    // must read without compiling (review of vaadin/flow#27023)
    @DomEvent(value = "click", filter = "element.dataset.locked", preventDefault = true)
    public static class PathLockedClickEvent extends ComponentEvent<Component> {
        public PathLockedClickEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    private final UnorderedList setup = new UnorderedList();

    public DomEventFilterView() {
        add(new Paragraph("Run in production mode with vaadin.csp=strict and keep "
                + "the browser console open. No case may log a Content "
                + "Security Policy violation or an error."));

        setup.setId("setup");
        add(new H2("Setup results"), setup);

        // F1
        TextInput enterInput = new TextInput("f1-input");
        Span enterOut = out("f1-out");
        int[] enterCount = { 0 };
        ComponentUtil.addListener(enterInput, EnterEvent.class,
                e -> enterOut.setText("enter=" + (++enterCount[0])));
        section("F1 filter on the event",
                "Type a and b, then hold Enter down for a moment. "
                        + "Expected: enter=1. Letters and the repeated "
                        + "keydowns of a held key do not count.",
                enterInput, enterOut);

        // F2
        PlainButton enabled = new PlainButton("f2-button", "Click");
        enabled.getElement().setAttribute("data-enabled", "false");
        Span enabledOut = out("f2-out");
        int[] enabledCount = { 0 };
        ComponentUtil.addListener(enabled, EnabledClickEvent.class,
                e -> enabledOut.setText("clicks=" + (++enabledCount[0])));
        NativeButton enable = new NativeButton("Enable", e -> enabled
                .getElement().setAttribute("data-enabled", "true"));
        enable.setId("f2-enable");
        section("F2 filter on the element",
                "Click 'Click': nothing happens. Click 'Enable', then "
                        + "'Click'. Expected: clicks=1.",
                enabled, enable, enabledOut);

        // F3
        PlainButton data = new PlainButton("f3-button", "Click");
        Span dataOut = out("f3-out");
        int[] dataCount = { 0 };
        ComponentUtil.addListener(data, DataClickEvent.class,
                e -> dataOut.setText("clicks=" + (++dataCount[0]) + " detail="
                        + e.detail + " type=" + e.type));
        section("F3 filter with event data",
                "Shift-click 'Click': nothing happens. Click it "
                        + "normally. Expected: clicks=1 detail=1 type=click.",
                data, dataOut);

        // F4
        Checkbox checkbox = new Checkbox("f4-checkbox");
        checkbox.getElement().setAttribute("data-locked", "yes");
        Span lockedOut = out("f4-out");
        int[] lockedCount = { 0 };
        ComponentUtil.addListener(checkbox, LockedClickEvent.class,
                e -> lockedOut.setText("locked clicks=" + (++lockedCount[0])));
        NativeButton unlock = new NativeButton("Unlock", e -> checkbox
                .getElement().setAttribute("data-locked", "no"));
        unlock.setId("f4-unlock");
        section("F4 filter with preventDefault",
                "Click the checkbox. Expected: locked clicks=1, and the "
                        + "checkbox stays unchecked. Click 'Unlock', then "
                        + "the checkbox: it gets checked and the count stays 1.",
                checkbox, unlock, lockedOut);

        // F5
        TextInput longInput = new TextInput("f5-input");
        Span longOut = out("f5-out");
        int[] longCount = { 0 };
        ComponentUtil.addListener(longInput, LongInputEvent.class,
                e -> longOut.setText(
                        "events=" + (++longCount[0]) + " value=" + e.value));
        section("F5 filter with debounce",
                "Type ab, wait: nothing happens. Type cd quickly. "
                        + "Expected after a short pause: events=1 value=abcd.",
                longInput, longOut);

        // F6
        PlainButton addon = new PlainButton("f6-button", "Click");
        Span addonOut = out("f6-out");
        int[] addonCount = { 0 };
        ComponentUtil.addListener(addon, AddonClickEvent.class,
                e -> addonOut.setText("shift clicks=" + (++addonCount[0])));
        section("F6 event type from another package",
                "Click 'Click': nothing happens. Shift-click it. "
                        + "Expected: shift clicks=1.",
                addon, addonOut);

        // F7
        PlainButton shared = new PlainButton("f7-button", "Click");
        Span sharedOut = out("f7-out");
        Span sharedData = out("f7-data");
        int[] sharedCount = { 0 };
        ComponentUtil.addListener(shared, ButtonDataEvent.class,
                e -> sharedData.setText("primary=" + e.primary));
        ComponentUtil.addListener(shared, PrimaryClickEvent.class,
                e -> sharedOut.setText("primary clicks=" + (++sharedCount[0])));
        section("F7 filter and event data with the same expression",
                "Click 'Click'. Expected: primary clicks=1 and "
                        + "primary=true.",
                shared,
                sharedOut, sharedData);

        // F8
        TextInput keyUp = new TextInput("f8-input");
        Span keyUpOut = out("f8-out");
        int[] keyUpCount = { 0 };
        ComponentUtil.addListener(keyUp, KeyUpEvent.class,
                e -> keyUpOut.setText("keyups=" + (++keyUpCount[0]) + " key="
                        + e.getKey().getKeys().get(0)));
        section("F8 Flow's KeyUpEvent (path filter)",
                "Type x. Expected: keyups=1 key=x.", keyUp, keyUpOut);

        // F9: a string filter set at runtime is still compiled, so strict
        // refuses it. The listener is added before the filter is refused, so
        // it stays registered without a filter.
        PlainButton runtime = new PlainButton("f9-button", "Click");
        Span runtimeOut = out("f9-out");
        int[] runtimeCount = { 0 };
        String runtimeOutcome = setupCase("F9", () -> runtime.getElement()
                .addEventListener("click", e -> runtimeOut
                        .setText("clicks=" + (++runtimeCount[0])))
                .setFilter("event.button === 0"));
        Span runtimeResult = new Span("setFilter: " + runtimeOutcome);
        runtimeResult.setId("f9-outcome");
        section("F9 setFilter(String) at runtime",
                "Expected: 'setFilter: CspViolationException' next to the "
                        + "button. The server catches it, so the browser "
                        + "console shows nothing. The listener was added "
                        + "before setFilter, so it stays registered without "
                        + "a filter: a click gives clicks=1.",
                runtimeResult, runtime, runtimeOut);

        // F10
        Checkbox pathCheckbox = new Checkbox("f10-checkbox");
        pathCheckbox.getElement().setAttribute("data-locked", "yes");
        Span pathLockedOut = out("f10-out");
        int[] pathLockedCount = { 0 };
        ComponentUtil.addListener(pathCheckbox, PathLockedClickEvent.class,
                e -> pathLockedOut
                        .setText("locked clicks=" + (++pathLockedCount[0])));
        NativeButton pathUnlock = new NativeButton("Unlock", e -> pathCheckbox
                .getElement().removeAttribute("data-locked"));
        pathUnlock.setId("f10-unlock");
        section("F10 path filter with preventDefault",
                "Click the checkbox. Expected: locked clicks=1, and the "
                        + "checkbox stays unchecked. Click 'Unlock', then "
                        + "the checkbox: it gets checked and the count stays 1.",
                pathCheckbox, pathUnlock, pathLockedOut);

        // F11: a property path filter set at runtime, with stopPropagation.
        // The path is not refused, and the conditional stopPropagation must
        // not be compiled either
        Div stopOuter = new Div();
        stopOuter.setId("f11-outer");
        stopOuter.getStyle().set("display", "inline-block")
                .set("padding", "0.3em").set("background", "lightgray");
        PlainButton stopInner = new PlainButton("f11-button", "Click");
        stopOuter.add(stopInner);
        Span stopOut = out("f11-out");
        int[] innerCount = { 0 };
        int[] outerCount = { 0 };
        Runnable showStop = () -> stopOut.setText(
                "button=" + innerCount[0] + " outer=" + outerCount[0]);
        stopOuter.getElement().addEventListener("click", e -> {
            outerCount[0]++;
            showStop.run();
        });
        String stopOutcome = setupCase("F11", () -> stopInner.getElement()
                .addEventListener("click", e -> {
                    innerCount[0]++;
                    showStop.run();
                }).setFilter("event.shiftKey").stopPropagation());
        Span stopResult = new Span("setFilter: " + stopOutcome);
        stopResult.setId("f11-outcome");
        section("F11 setFilter(path) with stopPropagation at runtime",
                "Expected: 'setFilter: OK'. Shift-click 'Click'. Expected: "
                        + "button=1 outer=0, because the button stops the "
                        + "event. Click it normally. Expected: button=1 "
                        + "outer=1.",
                stopResult, stopOuter, stopOut);
    }

    private Span out(String id) {
        Span span = new Span("-");
        span.setId(id);
        return span;
    }

    private void section(String title, String instructions,
            Component... content) {
        Div div = new Div();
        div.add(new Span(title + ": "));
        div.add(content);
        Paragraph how = new Paragraph(instructions);
        how.getStyle().set("margin", "0.2em 0 1em").set("color", "gray");
        div.add(how);
        add(div);
    }

    private String setupCase(String name, Runnable action) {
        String outcome;
        try {
            action.run();
            outcome = "OK";
        } catch (RuntimeException e) {
            outcome = e.getClass().getSimpleName();
        }
        ListItem item = new ListItem(name + ": " + outcome);
        item.setId("setup-" + name);
        item.getElement().setAttribute("data-outcome", outcome);
        setup.add(item);
        return outcome;
    }

}
