package org.vaadin.example;

import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Two rows, each with a button. The "built-in" row uses a JsFunction snippet
 * and is the control. The "custom" row uses the application's own snippet
 * type {@link Greeting}. For each row, the server-side status shows OK or
 * the error, and the output shows what the client rendered.
 */
@Route("")
public class MainView extends Div {

    public MainView() {
        add(new H1("JsSnippet sub-interface in a native image"));
        add(new Paragraph("Runtime: "
                + (System.getProperty("org.graalvm.nativeimage.imagecode") != null
                        ? "native image"
                        : "JVM")));
        addRow("built-in", output -> output.getElement()
                .executeJs(RenderJs.class).renderBuiltIn(
                        JsSnippets.of(GreetingJs.class).builtIn("Hello")));
        addRow("custom", output -> output.getElement()
                .executeJs(RenderJs.class).renderCustom(
                        JsSnippets.of(GreetingJs.class).custom("Hello")));
    }

    private void addRow(String name, Consumer<Div> action) {
        Div status = new Div("Status: not run");
        status.setId(name + "-status");
        Div output = new Div("Output: (empty)");
        output.setId(name + "-output");
        NativeButton button = new NativeButton("Run " + name + " snippet",
                click -> {
                    try {
                        action.accept(output);
                        status.setText("Status: OK");
                    } catch (Throwable t) {
                        LoggerFactory.getLogger(MainView.class)
                                .error("The {} snippet failed", name, t);
                        status.setText("Status: FAILED " + t);
                    }
                });
        button.setId(name + "-button");
        add(new Div(button, status, output));
    }
}
