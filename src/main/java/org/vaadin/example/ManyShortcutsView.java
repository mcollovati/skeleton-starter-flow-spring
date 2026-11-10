package org.vaadin.example;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.Route;

/**
 * Review finding 5: cost of the shortcut filters on every keydown.
 * <p>
 * All shortcuts here listen on the UI, so every keydown anywhere on the page
 * runs the filter of each one. On main the filter is an expression that the
 * client compiles once and caches. With the branch the client decodes the
 * eight captured values of each filter again (decodeCopyWithTypeInfo), calls
 * the snippet factory, builds the options object and splits the modifier
 * strings, for each filter and each keydown.
 * <p>
 * Open /many-shortcuts, record a Performance profile in the browser DevTools
 * while holding a letter key in the input for a few seconds, and compare the
 * time in the keydown handler (evaluateSnippet) with main.
 */
@Route("many-shortcuts")
public class ManyShortcutsView extends Div {

    static final int COUNT = 300;

    public ManyShortcutsView() {
        EventLog log = new EventLog();
        Input input = new Input();
        input.setPlaceholder("hold a letter key here");
        add(new H3(COUNT + " shortcuts on the UI"),
                new RouterLink("Back", MainView.class),
                new Paragraph("Hold a letter key in the input while a "
                        + "Performance profile records."),
                input, log);
        Key[] keys = { Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6 };
        KeyModifier[] modifiers = KeyModifier.values();
        for (int i = 0; i < COUNT; i++) {
            Key key = keys[i % keys.length];
            KeyModifier modifier = modifiers[(i / keys.length)
                    % modifiers.length];
            String name = i + "";
            // Different owners, so that the filters are not shared
            Div owner = new Div();
            add(owner);
            Shortcuts.addShortcutListener(owner, () -> log.log(name), key,
                    modifier);
        }
    }
}
