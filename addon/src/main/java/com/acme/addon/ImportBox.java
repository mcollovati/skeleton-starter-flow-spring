package com.acme.addon;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * An add-on component whose JavaScript definitions get names from modules of
 * the add-on jar with {@link JsDefinitionImport} (vaadin/flow#27423). The
 * application reaches this class from a route, so with {@code optimizeBundle}
 * the production bundle must carry the functions and the modules they import,
 * and load each module only on the first call that needs it.
 * <p>
 * {@code acme-format.js} is in {@code META-INF/frontend} and
 * {@code acme-label.js} in {@code META-INF/resources/frontend}, the two places
 * a jar can keep frontend files.
 */
public class ImportBox extends Div {

    public ImportBox(String text) {
        super(text);
        // So that focus() works on a div
        getElement().setAttribute("tabindex", "0");
    }

    public ImportBoxJs js() {
        return getElement().executeJs(ImportBoxJs.class);
    }

    public AltFormatJs altJs() {
        return getElement().executeJs(AltFormatJs.class);
    }

    @JsDefinition
    @JsModule("./acme-eager.js")
    @JsDefinitionImport(from = "./acme-format.js", defaultExport = "format", exports = "upper")
    @JsDefinitionImport(from = "./acme-label.js", namespace = "labels")
    public interface ImportBoxJs extends JsInvokers {

        // Default export and a named export
        @JsExpression("this.textContent = upper(format($0));")
        void showUpperFormatted(String text);

        // A namespace, with the default export reached through it
        @JsExpression("this.textContent = labels.label($0) + ' ' + labels.version + ' ' + labels.default;")
        void showLabel(String text);

        // The same JavaScript as AltFormatJs.showFormatted, other imports
        @JsExpression("this.textContent = format($0);")
        void showFormatted(String text);

        // With a result
        @JsExpression("return format($0);")
        PendingJavaScriptResult formatted(String text);

        // With a typed result
        @JsExpression("return labels.version;")
        JavaScriptResult<String> labelVersion();

        // Variadic: the arguments stay in the outer function
        @JsExpression("this.textContent = $0.map(format).join(' ');")
        void showAll(String... texts);

        // The Javadoc says that arguments holds the imported values
        @JsExpression("return arguments.length;")
        PendingJavaScriptResult argumentCount(String ignored);

        // A method without @JsExpression (vaadin/flow#27441) next to imports
        void focus();
    }

    /**
     * Declares the same JavaScript as {@link ImportBoxJs#showFormatted} with
     * {@code format} from another module, so it must be another function.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-format-alt.js", defaultExport = "format")
    public interface AltFormatJs extends JsInvokers {

        @JsExpression("this.textContent = format($0);")
        void showFormatted(String text);
    }
}
