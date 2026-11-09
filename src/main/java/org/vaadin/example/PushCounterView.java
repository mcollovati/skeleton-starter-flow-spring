package org.vaadin.example;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Pushes a counter every 500 ms, with no user input.
 * <p>
 * Expected: after the dropped message the client resynchronizes and the
 * counter keeps going. Actual: the counter stops a few seconds after the
 * dropped message, and the server logs
 * {@code ResynchronizationRequiredException} as "Unexpected error".
 * <p>
 * {@code ?transport=WEBSOCKET_XHR} for comparison: the client then sends its
 * requests, the resynchronization request too, over HTTP, and the counter
 * keeps going.
 */
@Route("")
public class PushCounterView extends VerticalLayout
        implements BeforeEnterObserver {

    // A daemon thread, so it does not keep the JVM alive after shutdown.
    private static final ScheduledExecutorService TICKER = Executors
            .newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "push-counter");
                thread.setDaemon(true);
                return thread;
            });

    private final Span counter = new Span("0");
    private ScheduledFuture<?> task;
    private int value;

    public PushCounterView() {
        counter.setId("counter");
        add(new Paragraph("Pushed counter, +1 every 500 ms:"), counter);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        List<String> transport = event.getLocation().getQueryParameters()
                .getParameters().get("transport");
        if (transport != null && !transport.isEmpty()) {
            event.getUI().getPushConfiguration()
                    .setTransport(Transport.valueOf(transport.get(0)));
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        UI ui = event.getUI();
        task = TICKER.scheduleAtFixedRate(() -> ui.access(
                () -> counter.setText(String.valueOf(++value))), 500, 500,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDetach(DetachEvent event) {
        task.cancel(false);
    }
}
