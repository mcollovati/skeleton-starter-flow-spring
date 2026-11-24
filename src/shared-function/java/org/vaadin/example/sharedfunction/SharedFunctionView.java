package org.vaadin.example.sharedfunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * A view that calls JavaScript which {@code IFrame.ReloadJs} declares too. The
 * two methods are one function in the bundle, which the bundle names after
 * {@code IFrame}. A pre-built bundle that carries it should still serve the
 * application.
 */
@Route("shared-function")
public class SharedFunctionView extends VerticalLayout {

    /**
     * The same JavaScript as {@code IFrame.ReloadJs.reload()}, under a name of
     * the application.
     */
    @JsDefinition
    public interface SharedReloadJs extends JsInvokers {

        @JsExpression("this.src = this.src")
        void reload();
    }

    public SharedFunctionView() {
        IFrame frame = new IFrame("about:blank");
        frame.setId("frame");
        Button button = new Button("Reload",
                event -> frame.getElement().executeJs(SharedReloadJs.class)
                        .reload());
        button.setId("reload");
        add(frame, button);
    }
}
