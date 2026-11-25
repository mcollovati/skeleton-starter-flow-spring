package org.vaadin.example;

import java.lang.reflect.Method;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Calls {@code com.acme.addon.ReflectedJs} found by its name, so no class the
 * scanner reaches refers to it (vaadin/flow#27078). Without the
 * {@code uses-fix} profile the production bundle does not carry its
 * JavaScript: the server logs an error that names the class, and the call
 * does nothing in the browser.
 */
@Route("missing")
public class MissingDefinitionView extends Div {

    static final String DEFINITION = "com.acme.addon.ReflectedJs";

    public MissingDefinitionView() {
        Div target = new Div("Not marked");
        target.setId("target");
        Paragraph result = new Paragraph();
        result.setId("result");
        NativeButton run = new NativeButton("Run", event -> {
            try {
                Class<? extends JsInvokers> type = Class
                        .forName(DEFINITION, true,
                                getClass().getClassLoader())
                        .asSubclass(JsInvokers.class);
                Object js = target.getElement().executeJs(type);
                Method mark = type.getMethod("mark");
                mark.invoke(js);
                result.setText("called");
            } catch (ReflectiveOperationException e) {
                result.setText("failed: " + e);
            }
        });
        run.setId("run");
        add(target, run, result);
    }
}
