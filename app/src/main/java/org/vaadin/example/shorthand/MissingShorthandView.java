package org.vaadin.example.shorthand;

import java.lang.reflect.Method;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Like {@code MissingDefinitionView}, with {@code com.acme.addon.ReflectedShorthandJs},
 * whose method has no {@code @JsExpression} (vaadin/flow#27441 with #27078).
 * Without the {@code uses-fix} profile the server must log that the bundle
 * does not carry it.
 */
@Route("missing-shorthand")
public class MissingShorthandView extends Div {

    static final String DEFINITION = "com.acme.addon.ReflectedShorthandJs";

    public MissingShorthandView() {
        Div target = new Div("Not replaced");
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
                Method method = type.getMethod("replaceChildren",
                        String[].class);
                method.invoke(js, (Object) new String[] { "Replaced ",
                        "by ReflectedShorthandJs" });
                result.setText("called");
            } catch (ReflectiveOperationException e) {
                result.setText("failed: " + e);
            }
        });
        run.setId("run");
        add(target, run, result);
    }
}
