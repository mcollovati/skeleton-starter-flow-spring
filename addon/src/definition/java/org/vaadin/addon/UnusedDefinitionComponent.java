package org.vaadin.addon;

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * A component that declares its JavaScript, which the application never uses.
 * The production build still adds the definition to the bundle, so the
 * application can no longer use the pre-built production bundle.
 */
@Tag("div")
public class UnusedDefinitionComponent extends Component {

    @JsDefinition
    public interface GreetJs extends Serializable {

        @JsExpression("this.textContent = 'Hello ' + $0")
        void greet(String name);
    }

    public UnusedDefinitionComponent() {
        getElement().executeJs(GreetJs.class).greet("world");
    }
}
