package org.vaadin.example.appdefinition;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * A view that calls JavaScript the application declares itself. No pre-built
 * bundle carries it, so the build has to compile an application bundle.
 */
@Route("app-definition")
public class AppDefinitionView extends VerticalLayout {

    /**
     * JavaScript only this application declares.
     */
    @JsDefinition
    public interface MarkJs extends JsInvokers {

        @JsExpression("this.textContent = 'Marked by ' + $0")
        void mark(String by);
    }

    public AppDefinitionView() {
        Div target = new Div("Not marked");
        target.setId("target");
        Button button = new Button("Mark", event -> target.getElement()
                .executeJs(MarkJs.class).mark("the application"));
        button.setId("mark");
        add(target, button);
    }
}
