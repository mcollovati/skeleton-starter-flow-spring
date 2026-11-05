package org.vaadin.example;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/** Index of the cases that PR vaadin/flow#26699 changes. */
@Route("")
@PermitAll
public class MainView extends VerticalLayout {

    public MainView() {
        add(new H2("PR #26699: simple executeJs call sites as JS definitions"));
        add(new Paragraph(
                "Run with vaadin.csp=warn. Every case below must work, and the "
                        + "server log must not show a CspViolationReporter "
                        + "warning that names Page.ensureResizeListener, "
                        + "Element.flashClass, AuthenticationContext.logout "
                        + "or AbstractNavigationStateRenderer."));
        add(new RouterLink("1. Window resize listener (Page)", ResizeView.class));
        add(new RouterLink("2. Element.flashClass", FlashView.class));
        add(new RouterLink("3. Logout (AuthenticationContext)", LogoutView.class));
        add(new RouterLink("4. Navigation and history", NavigationView.class));
    }
}
