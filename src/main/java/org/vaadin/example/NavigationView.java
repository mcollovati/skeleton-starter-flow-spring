package org.vaadin.example;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * AbstractNavigationStateRenderer.pushHistoryStateIfNeeded was simplified:
 * <ul>
 * <li>a router link to a missing route no longer sends
 * "this.scrollPositionHandlerAfterServerNavigation($0)", a client function
 * that no longer exists</li>
 * <li>the two identical history push branches were merged; the URL after
 * each navigation must be the same as before</li>
 * </ul>
 */
@Route("navigation")
@PermitAll
public class NavigationView extends VerticalLayout {

    public NavigationView() {
        Anchor missing = new Anchor("does-not-exist",
                "Router link to a missing route");
        missing.getElement().setAttribute(
                ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "");
        missing.setId("missing-link");

        Button navigate = new Button("UI.navigate to the target view",
                e -> UI.getCurrent().navigate(TargetView.class));
        navigate.setId("navigate");
        Button forward = new Button(
                "UI.navigate to a view that forwards to the target view",
                e -> UI.getCurrent().navigate(ForwardingView.class));
        forward.setId("navigate-forward");

        add(new H3("Navigation and history"),
                new Paragraph("Missing route: the error view is shown and "
                        + "there is no browser console error. UI.navigate: "
                        + "the address bar shows /target and Back returns "
                        + "here. Forward: the address bar shows /target "
                        + "(not /forwarding), one history entry is added, "
                        + "and Back returns here."),
                missing, navigate, forward,
                new RouterLink("Back", MainView.class));
    }

    @Route("target")
    @PermitAll
    public static class TargetView extends VerticalLayout {
        public TargetView() {
            add(new H3("Target view"), new RouterLink("Back to navigation",
                    NavigationView.class));
        }
    }

    @Route("forwarding")
    @PermitAll
    public static class ForwardingView extends VerticalLayout
            implements com.vaadin.flow.router.BeforeEnterObserver {
        @Override
        public void beforeEnter(com.vaadin.flow.router.BeforeEnterEvent event) {
            event.forwardTo(TargetView.class);
        }
    }
}
