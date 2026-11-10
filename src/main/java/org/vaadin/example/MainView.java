package org.vaadin.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/** Index of the shortcut cases. */
@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        add(new H2("Shortcuts as declared JavaScript"));
        add(new Paragraph("See SHORTCUT-NOTES.md for the expected results. "
                + "The views use only Flow HTML components, so that the "
                + "CSP log shows only the shortcut call sites."));
        add(new RouterLink("1. Keys, modifiers and options",
                ShortcutsView.class));
        add(new RouterLink("2. Popover scope of the shortcut owner",
                PopoverView.class));
        add(new RouterLink("3. Browser-only element, declared locator",
                DelegateView.class));
        add(new RouterLink("4. Browser-only element, String locator",
                StringLocatorView.class));
    }
}
