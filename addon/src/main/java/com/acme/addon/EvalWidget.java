package com.acme.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Case 12 (browser only): the add-on's own web component calls
 * {@code new Function(...)}. The file is in the bundle, so loading works, but
 * the browser refuses the eval at run time. Flow cannot see this on the
 * server.
 */
@Tag("acme-eval-widget")
@JsModule("./acme-eval-widget.js")
public class EvalWidget extends Component {

    public EvalWidget(String expression) {
        getElement().setAttribute("expr", expression);
    }
}
