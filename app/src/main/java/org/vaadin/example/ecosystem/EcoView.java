package org.vaadin.example.ecosystem;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.ContentSecurityPolicyViolationException;

/**
 * Base of the views that use one real add-on each. A call to the add-on that
 * Flow refuses shows its message in {@code <pre id="message">}, like
 * {@code CasesView} does. Calls that the add-on makes on attach or before the
 * client response can not be caught here: then the page shows Vaadin's error
 * instead.
 */
public abstract class EcoView extends Div {

    private final Pre message = new Pre();

    protected EcoView(String title) {
        add(new H2(title));
        message.setId("message");
        message.getStyle().set("white-space", "pre-wrap");
        message.setVisible(false);
        add(message);
    }

    /**
     * Runs a call to the add-on and shows the message if Flow refuses it.
     */
    protected void run(SerializableRunnable call) {
        try {
            call.run();
        } catch (ContentSecurityPolicyViolationException e) {
            String text = message.getText();
            message.setText((text.isEmpty() ? "" : text + "\n\n")
                    + "REFUSED in Java: " + e.getMessage());
            message.setVisible(true);
        }
    }

    /**
     * A button with an id that runs the given call to the add-on.
     */
    protected NativeButton button(String id, String text,
            SerializableRunnable call) {
        NativeButton button = new NativeButton(text, e -> run(call));
        button.setId(id);
        return button;
    }

    protected static <T extends Component> T withId(T component, String id) {
        component.setId(id);
        return component;
    }

    protected static Div output(String id) {
        Div div = new Div();
        div.setId(id);
        return div;
    }
}
