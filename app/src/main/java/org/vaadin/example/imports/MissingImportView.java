package org.vaadin.example.imports;

import java.lang.reflect.Method;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Like {@code MissingDefinitionView}, with
 * {@code com.acme.addon.ReflectedImportJs}, whose JavaScript imports a module
 * (vaadin/flow#27423 with #27078). Without the {@code uses-fix} profile the
 * server must log that the bundle does not carry it.
 */
@Route("missing-import")
public class MissingImportView extends Div {

    static final String DEFINITION = "com.acme.addon.ReflectedImportJs";

    public MissingImportView() {
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
                Method method = type.getMethod("replace", String.class);
                method.invoke(js, "Replaced by ReflectedImportJs");
                result.setText("called");
            } catch (ReflectiveOperationException e) {
                result.setText("failed: " + e);
            }
        });
        run.setId("run");
        add(target, run, result);
    }
}
