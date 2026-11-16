package org.vaadin.example;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Shows that the Flow clients stay "active" after the server answers a request
 * with the "Internal error" notification. TestBench's {@code waitForVaadin()}
 * then waits for its full time limit before every command.
 * <p>
 * Steps: click "Fail the next request", then either click "Send a request"
 * (ROOT client case) or follow the link (TypeScript client case). The box in
 * the bottom right corner shows {@code isActive()} for every client.
 */
@Route("app-error")
public class AppErrorView extends VerticalLayout {

    /**
     * Shows {@code isActive()} of every registered client, twice a second, in a
     * box outside Flow's DOM so that it stays while Flow navigates. Uses only
     * the API that TestBench's {@code waitForVaadin()} uses.
     */
    private static final String STATUS_SCRIPT = """
            if (!window.__clientStatus) {
              const box = document.createElement('pre');
              box.id = 'client-status';
              box.style.cssText = 'position:fixed;right:8px;bottom:8px;'
                  + 'margin:0;padding:8px;background:#fff;border:1px solid #888;'
                  + 'font-size:12px;z-index:100000';
              document.body.append(box);
              const start = Date.now();
              window.__clientStatus = setInterval(() => {
                const clients = (window.Vaadin && window.Vaadin.Flow
                    && window.Vaadin.Flow.clients) || {};
                const lines = Object.entries(clients).map(([name, client]) =>
                    name + ': isActive() = ' + client.isActive());
                box.textContent = lines.join('\\n') + '\\n('
                    + Math.round((Date.now() - start) / 1000) + ' s after load)';
              }, 500);
            }
            """;

    public AppErrorView() {
        Button arm = new Button("Fail the next request",
                event -> FailNextUidlRequest.arm());
        arm.setId("arm");

        Button send = new Button("Send a request",
                event -> add(new Span("The server answered")));
        send.setId("send");

        RouterLink navigate = new RouterLink("Navigate to the target view",
                TargetView.class);
        navigate.setId("navigate");

        add(new H2("Clients stay active after an internal error"),
                new Paragraph("1. Click \"Fail the next request\". "
                        + "2. Click \"Send a request\" or follow the link. "
                        + "The server fails that request and the browser "
                        + "shows \"Internal error\". Watch the box in the "
                        + "bottom right corner."),
                arm, send, navigate);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPage().executeJs(STATUS_SCRIPT);
    }
}
