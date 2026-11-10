package org.vaadin.example;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;

import static org.vaadin.example.ShortcutsView.newCase;

/**
 * The guard that ignores a keydown from an open popover that does not contain
 * the shortcut owner (#24974). With the PR, the owner selector is sent as data
 * to the declared listener filter. The popover opens with popovertarget, so
 * the view sends no JavaScript of its own.
 */
@Route("popover")
public class PopoverView extends Div {

    private final EventLog log = new EventLog();
    private Registration uiShortcut;

    public PopoverView() {
        add(new H3("Popover scope of the shortcut owner"),
                new RouterLink("Back", MainView.class), log.clearButton(),
                log);

        Input pageInput = new Input();
        pageInput.setId("page-input");
        pageInput.setPlaceholder("page input");

        Input popoverInput = new Input();
        popoverInput.setId("popover-input");
        popoverInput.setPlaceholder("popover input");
        NativeButton inside = new NativeButton("Inside (Alt+I)",
                e -> log.log("inside-i"));
        inside.setId("inside");
        inside.addClickShortcut(Key.KEY_I, KeyModifier.ALT);
        NativeButton close = new NativeButton("Close popover");
        close.getElement().setAttribute("popovertarget", "popover");
        close.getElement().setAttribute("popovertargetaction", "hide");
        Div popover = new Div(popoverInput, inside, close);
        popover.setId("popover");
        popover.getElement().setAttribute("popover", "manual");

        NativeButton open = new NativeButton("Open popover");
        open.setId("open-popover");
        open.getElement().setAttribute("popovertarget", "popover");

        NativeButton outside = new NativeButton("Outside (Alt+G)",
                e -> log.log("outside-g"));
        outside.setId("outside");
        outside.addClickShortcut(Key.KEY_G, KeyModifier.ALT);

        NativeButton nestedAllowed = new NativeButton(
                "Outside, nested modals allowed (Alt+H)",
                e -> log.log("outside-h"));
        nestedAllowed.setId("nested-allowed");
        nestedAllowed.addClickShortcut(Key.KEY_H, KeyModifier.ALT)
                .allowEventsFromNestedModals();

        add(newCase("Owner outside the popover",
                "Popover closed, focus in the page input: Alt+G, Alt+H, F4 "
                        + "and Alt+I all log. Popover open, focus in the "
                        + "page input: Alt+G, Alt+H and F4 log, Alt+I logs "
                        + "nothing. Popover open, focus in the popover "
                        + "input: Alt+H and Alt+I log, Alt+G and F4 log "
                        + "nothing.",
                pageInput, open, outside, nestedAllowed, popover));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        uiShortcut = attachEvent.getUI()
                .addShortcutListener(() -> log.log("ui-f4"), Key.F4);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        uiShortcut.remove();
    }
}
