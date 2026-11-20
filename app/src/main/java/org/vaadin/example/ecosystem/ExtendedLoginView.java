package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.extendedlogin.ExtendedLoginOverlay;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;

/**
 * ExtendedLoginAddon (org.vaadin.addons.flowingcode:extended-login 1.1.0).
 */
@Route("eco/extended-login")
public class ExtendedLoginView extends EcoView {

    public ExtendedLoginView() {
        super("ExtendedLoginAddon");
        ExtendedLoginOverlay login = withId(new ExtendedLoginOverlay(),
                "login");
        add(login);
        run(() -> {
            login.replaceHeaderComponent(withId(new H2("ACME CSP"), "brand"));
            login.removeForgotPassword();
        });
        login.setOpened(true);
    }
}
