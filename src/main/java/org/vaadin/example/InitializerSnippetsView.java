package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * {@code Element.addJsInitializer(JsFunction)}, added by vaadin/flow#26889,
 * with an initializer declared as a snippet, and the forms that are refused.
 */
@Route("initializer-snippets")
public class InitializerSnippetsView extends Div {

    @JsDefinition
    public interface InitializerJs extends Serializable {

        /**
         * Counts each install in the first element and each cleanup in the
         * second one.
         */
        @JsExpression("""
                $0.textContent = String(parseInt($0.textContent, 10) + 1);
                return () => {
                    $1.textContent = String(parseInt($1.textContent, 10) + 1);
                };""")
        JsFunction countInitAndCleanup(Element initCounter,
                Element cleanupCounter);

        /** Declares a runtime argument, which an initializer never gets. */
        @JsExpression("return arg;")
        @JsFunctionParameterNames("arg")
        JsFunction withRuntimeArgument();
    }

    private static final InitializerJs JS = JsSnippets.of(InitializerJs.class);

    private final Div target = new Div("I1 target");
    private final Span initCounter = new Span("0");
    private final Span cleanupCounter = new Span("0");
    private Registration registration;

    public InitializerSnippetsView() {
        setId("initializer-snippets");
        target.setId("i1-target");
        initCounter.setId("i1-init");
        cleanupCounter.setId("i1-cleanup");

        NativeButton register = new NativeButton("Register", e -> {
            if (registration == null) {
                registration = target.getElement()
                        .addJsInitializer(JS.countInitAndCleanup(
                                initCounter.getElement(),
                                cleanupCounter.getElement()));
            }
        });
        register.setId("i1-register");
        NativeButton detach = new NativeButton("Detach", e -> remove(target));
        detach.setId("i1-detach");
        NativeButton reattach = new NativeButton("Reattach",
                e -> addComponentAtIndex(0, target));
        reattach.setId("i1-reattach");
        NativeButton removeRegistration = new NativeButton("Remove", e -> {
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        });
        removeRegistration.setId("i1-remove");

        UnorderedList setup = new UnorderedList();
        setup.setId("setup");
        Element element = new Div().getElement();
        setupCase(setup, "I2",
                () -> element.addJsInitializer(JS.withRuntimeArgument()));
        setupCase(setup, "I3",
                () -> element.addJsInitializer("this.dataset.x = '1'"));
        setupCase(setup, "I4",
                () -> element.addJsInitializer((JsFunction) null));
        setupCase(setup, "I5", () -> JsFunction.of("return 1;"));

        add(new Paragraph("Run in production mode with vaadin.csp=strict and keep "
                + "the browser console open. No case may log a Content "
                + "Security Policy violation or an error."));
        add(new Paragraph("I1: click Register, Detach, Reattach and Remove in "
                + "this order. Expected after each click: init=1 "
                + "cleanup=0, init=1 cleanup=1, init=2 cleanup=1, init=2 "
                + "cleanup=2. Register again counts one more init."));
        add(target, register, detach, reattach, removeRegistration,
                new Div(new Span("init="), initCounter, new Span(" cleanup="),
                        cleanupCounter));
        add(new Paragraph("I2-I5 run when the view opens. Expected below: I2 "
                + "IllegalArgumentException, I3 CspViolationException, I4 "
                + "NullPointerException, I5 CspViolationException. The "
                + "server catches them, so the browser console shows "
                + "nothing."), setup);
    }

    private static void setupCase(UnorderedList setup, String caseId,
            SerializableRunnable code) {
        String outcome;
        try {
            code.run();
            outcome = "OK";
        } catch (RuntimeException e) {
            outcome = e.getClass().getSimpleName();
        }
        ListItem item = new ListItem(caseId + ": " + outcome);
        item.setId(caseId.toLowerCase());
        item.getElement().setAttribute("data-outcome", outcome);
        setup.add(item);
    }
}
