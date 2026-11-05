package org.vaadin.example;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * AuthenticationContext.logout() does a round trip before it logs out in two
 * cases, and that round trip is now PageJs.getHref() instead of
 * executeJs("return true"):
 * <ul>
 * <li>push is connected with the WEBSOCKET transport (the app default
 * here)</li>
 * <li>logout is called from a background thread (open this view with
 * ?transport=long-polling, because with WEBSOCKET the first branch wins)</li>
 * </ul>
 * Both must end on the login view.
 */
@Route("logout")
@PermitAll
public class LogoutView extends VerticalLayout implements BeforeEnterObserver {

    private final Span transport = new Span();

    public LogoutView(AuthenticationContext authenticationContext) {
        transport.setId("transport");
        Button logout = new Button("Logout",
                e -> authenticationContext.logout());
        logout.setId("logout");
        Button background = new Button("Logout from a background thread",
                e -> {
                    UI ui = e.getSource().getUI().orElseThrow();
                    Thread.ofVirtual().start(() -> {
                        sleep();
                        ui.access(authenticationContext::logout);
                    });
                });
        background.setId("logout-background");
        add(new H3("Logout"),
                new Paragraph(new Span("Push transport: "), transport),
                new Paragraph("Open with ?transport=long-polling to test "
                        + "the background thread case (it is only reached "
                        + "when the transport is not WEBSOCKET)."),
                logout, background, new RouterLink("Back", MainView.class));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UI ui = event.getUI();
        if (event.getLocation().getQueryParameters()
                .getSingleParameter("transport")
                .filter("long-polling"::equals).isPresent()) {
            ui.getPushConfiguration().setTransport(Transport.LONG_POLLING);
        }
        transport.setText(ui.getPushConfiguration().getTransport().name());
    }

    private static void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
