package org.vaadin.example;

import java.util.function.Consumer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;

/**
 * The key, the modifiers and the options of a shortcut. With the PR they are
 * sent as data to the declared listener filter (ShortcutFilterJs) instead of
 * as a filter string. The toggle buttons change an option of an active
 * shortcut, which sets a new filter.
 */
@Route("shortcuts")
public class ShortcutsView extends Div {

    private final EventLog log = new EventLog();
    private Registration uiShortcut;

    public ShortcutsView() {
        add(new H3("Keys, modifiers and options"),
                new RouterLink("Back", MainView.class), log.clearButton(),
                log);

        // A: modifier filter and preventDefault. A shortcut listener, because
        // addClickShortcut allows the browser default.
        Shortcuts.addShortcutListener(this, () -> log.log("ctrl-s"),
                Key.KEY_S, KeyModifier.CONTROL);
        add(newCase("A. Shortcut with a modifier",
                "Ctrl+S logs ctrl-s, and the browser does not open its save "
                        + "dialog. Ctrl+Shift+S and S alone log nothing. "
                        + "(A click shortcut, like Ctrl+Enter in case F, "
                        + "allows the browser default.)"));

        // B: shortcut owned by the UI, which has no owner selector
        add(newCase("B. Shortcut owned by the UI",
                "F2 logs ui-f2, with the focus anywhere on the page."));

        // C: Alt, for which AltGraph is not a forbidden modifier
        Shortcuts.addShortcutListener(this, () -> log.log("alt-k"), Key.KEY_K,
                KeyModifier.ALT);
        add(newCase("C. Alt shortcut", "Alt+K logs alt-k."));

        // D: allowBrowserDefault, changed while the shortcut is active
        Input defaultInput = new Input();
        defaultInput.setId("default-input");
        ShortcutRegistration digit1 = Shortcuts
                .addShortcutListener(defaultInput, () -> log.log("digit-1"),
                        Key.DIGIT_1)
                .listenOn(defaultInput);
        add(newCase("D. Browser default",
                "Type 1 in the field: logs digit-1. The 1 is typed only when "
                        + "the browser default is allowed.",
                defaultInput,
                toggle("browser-default", "Browser default allowed",
                        digit1.isBrowserDefaultAllowed(),
                        digit1::setBrowserDefaultAllowed)));

        // E: allowEventPropagation, changed while the shortcut is active
        Input innerInput = new Input();
        innerInput.setId("propagation-input");
        Div outer = new Div(innerInput);
        outer.setId("propagation-outer");
        ShortcutRegistration inner = Shortcuts
                .addShortcutListener(innerInput, () -> log.log("inner-2"),
                        Key.DIGIT_2)
                .listenOn(innerInput);
        Shortcuts.addShortcutListener(outer, () -> log.log("outer-2"),
                Key.DIGIT_2).listenOn(outer);
        add(newCase("E. Event propagation",
                "Type 2 in the field: logs inner-2. It also logs outer-2 "
                        + "only when propagation is allowed.",
                outer,
                toggle("propagation", "Propagation allowed",
                        inner.isEventPropagationAllowed(),
                        inner::setEventPropagationAllowed)));

        // F: resetFocusOnActiveElement, changed while the shortcut is active
        Input name = new Input();
        name.setId("name-input");
        NativeButton greet = new NativeButton("Greet (Ctrl+Enter)",
                e -> log.log("greet:" + name.getValue()));
        greet.setId("greet");
        ShortcutRegistration greetShortcut = greet
                .addClickShortcut(Key.ENTER, KeyModifier.CONTROL)
                .resetFocusOnActiveElement();
        add(newCase("F. Reset focus",
                "Type a name and press Ctrl+Enter in the field. With reset "
                        + "focus, the field loses the focus first, so the "
                        + "value is sent: logs greet:<name>. Without it, "
                        + "the value is sent only on the next change: logs "
                        + "the old value.",
                name, greet,
                toggle("reset-focus", "Reset focus",
                        greetShortcut.isResetFocusOnActiveElement(),
                        greetShortcut::setResetFocusOnActiveElement)));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        uiShortcut = attachEvent.getUI()
                .addShortcutListener(() -> log.log("ui-f2"), Key.F2);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        uiShortcut.remove();
    }

    static Div newCase(String title, String expected,
            Component... components) {
        Div div = new Div(new H4(title), new Paragraph(expected));
        div.add(components);
        div.addClassName("case");
        return div;
    }

    static NativeButton toggle(String id, String label, boolean initial,
            Consumer<Boolean> setter) {
        boolean[] value = { initial };
        NativeButton button = new NativeButton(label + ": " + initial);
        button.setId(id);
        button.addClickListener(e -> {
            value[0] = !value[0];
            setter.accept(value[0]);
            button.setText(label + ": " + value[0]);
        });
        return button;
    }
}
