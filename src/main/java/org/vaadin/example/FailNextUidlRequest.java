package org.vaadin.example;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

/**
 * Makes the next UIDL request of a session fail with an exception that escapes
 * request handling.
 * <p>
 * Such an exception reaches {@code VaadinService.handleExceptionDuringRequest},
 * which answers with the "Internal error" critical notification
 * ({@code meta.appError}). This is the same answer the browser gets when, for
 * example, a {@code CspViolationException} escapes during a request. An
 * exception thrown in a component listener is not enough: Flow passes it to the
 * session error handler and sends a normal response.
 * <p>
 * Request handlers added here run before Flow's own handlers, so this handler
 * sees the UIDL request before {@code UidlRequestHandler} does.
 */
@Component
public class FailNextUidlRequest implements VaadinServiceInitListener {

    private static final String FLAG = FailNextUidlRequest.class.getName();

    /**
     * Makes the next UIDL request of the current session fail. Only that one
     * request fails, so the server handles later requests normally.
     */
    public static void arm() {
        // The wrapped (HTTP) session can be used without the VaadinSession
        // lock, which the request handler below does not hold
        VaadinSession.getCurrent().getSession().setAttribute(FLAG, true);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(new FailingHandler());
    }

    private static final class FailingHandler implements RequestHandler {

        @Override
        public boolean handleRequest(VaadinSession session,
                VaadinRequest request, VaadinResponse response) {
            if (HandlerHelper.isRequestType(request,
                    HandlerHelper.RequestType.UIDL)
                    && session.getSession().getAttribute(FLAG) != null) {
                session.getSession().removeAttribute(FLAG);
                throw new IllegalStateException(
                        "Simulated failure of a UIDL request");
            }
            return false;
        }
    }
}
