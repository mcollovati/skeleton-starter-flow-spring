package org.vaadin.example;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.vaadin.example.ShortcutsView.newCase;

/**
 * Shortcuts that listen on a component whose keydown events come from another
 * element, set with Shortcuts.setShortcutListenOnElement, like Dialog does
 * with its overlay. The shortcut registers a keydown delegate on the client
 * (ShortcutJs with the PR).
 * <p>
 * The overlay is not inside the host, so its keydown events reach the host
 * only through the delegate. A popover inside the overlay checks the guard
 * against events from a nested popover.
 */
public abstract class AbstractDelegateView extends Div {

    private final EventLog log = new EventLog();

    protected AbstractDelegateView(String title) {
        add(new H3(title), new RouterLink("Back", MainView.class),
                log.clearButton(), log);

        Div host = new Div("Host (the listen-on component)");
        host.setId("host");

        Input overlayInput = new Input();
        overlayInput.setId("overlay-input");
        overlayInput.setPlaceholder("overlay input");
        Input nestedInput = new Input();
        nestedInput.setId("nested-input");
        nestedInput.setPlaceholder("nested popover input");
        Div nested = new Div(nestedInput);
        nested.setId("nested");
        nested.getElement().setAttribute("popover", "manual");
        NativeButton openNested = new NativeButton("Open nested popover");
        openNested.setId("open-nested");
        openNested.getElement().setAttribute("popovertarget", "nested");
        Div overlay = new Div(new Paragraph("Overlay (located in the browser)"),
                overlayInput, openNested, nested);
        overlay.setId("overlay");
        overlay.addClassName("case");

        Input pageInput = new Input();
        pageInput.setId("page-input");
        pageInput.setPlaceholder("page input");

        // The locator is set again before each shortcut, as a component does
        // in each listenOn call. Both shortcuts have the same key and options,
        // so they share one delegate: each fires once per keydown.
        setLocator(host, "overlay");
        Shortcuts.addShortcutListener(host, () -> log.log("delegate-a"),
                Key.KEY_D, KeyModifier.ALT).listenOn(host);
        setLocator(host, "overlay");
        Shortcuts.addShortcutListener(host, () -> log.log("delegate-b"),
                Key.KEY_D, KeyModifier.ALT).listenOn(host);
        Shortcuts.addShortcutListener(host, () -> log.log("nested-e"),
                Key.KEY_E, KeyModifier.ALT).listenOn(host)
                .allowEventsFromNestedModals();

        add(newCase("Delegate",
                "Focus in the overlay input: Alt+D logs delegate-a and "
                        + "delegate-b, once each. Alt+E logs nested-e. Open "
                        + "the nested popover and focus its input: Alt+D "
                        + "logs nothing, Alt+E logs nested-e. Focus in the "
                        + "page input: Alt+D and Alt+E log nothing.",
                pageInput, host, overlay));
    }

    /**
     * Sets the locator of the element that the shortcuts of the host listen
     * on.
     */
    protected abstract void setLocator(Div host, String overlayId);
}
