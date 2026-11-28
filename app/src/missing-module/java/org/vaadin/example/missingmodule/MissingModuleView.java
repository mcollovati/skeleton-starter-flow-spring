package org.vaadin.example.missingmodule;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.router.Route;

/**
 * Definitions that import modules the build can not resolve
 * (vaadin/flow#27424). Added by the {@code missing-module} profile only,
 * because today such a module stops the Vite build. {@code MissingModuleIT}
 * checks each row.
 * <p>
 * What #27424 asks for: the build leaves the functions out and logs the full
 * reason, the bundle says nothing about the cause, and calling such a method
 * throws on the server, every time. Methods with resolved imports, inherited
 * ones too, still work.
 * <p>
 * Each row has a box (the element the JavaScript runs on), a button and a
 * result. When the call throws on the server, the result shows
 * {@code failed <n>: <exception>}, where n counts the failed calls.
 */
@Route("missing-module")
public class MissingModuleView extends Div {

    /** U0: resolved, next to the unresolved ones. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public interface ResolvedJs extends JsInvokers {

        @JsExpression("this.textContent = format($0);")
        void show(String text);
    }

    /**
     * U1-U4: a file of the project that does not exist. U4 is the method
     * without {@code @JsExpression}: its function imports the modules of the
     * interface too.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-does-not-exist.js", defaultExport = "missing")
    public interface MissingModuleJs extends JsInvokers {

        @JsExpression("this.textContent = missing($0);")
        void show(String text);

        @JsExpression("return missing($0);")
        PendingJavaScriptResult call(String text);

        void focus();
    }

    /** U5: a resolved import that a child inherits. */
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public interface ResolvedParent extends JsInvokers {

        @JsExpression("return format($0);")
        PendingJavaScriptResult parentFormatted(String text);
    }

    /** U5: the child adds an import that does not resolve. */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-child-missing.js", exports = "child")
    public interface ChildMissingJs extends ResolvedParent {

        @JsExpression("return child($0);")
        PendingJavaScriptResult childCall(String text);
    }

    /** U6: an npm package that is not installed. */
    @JsDefinition
    @JsDefinitionImport(from = "acme-not-installed-package", exports = "run")
    public interface MissingPackageJs extends JsInvokers {

        @JsExpression("return run($0);")
        PendingJavaScriptResult call(String text);
    }

    /** U7: called through {@code Page.executeJs}. */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-page-missing.js", exports = "shout")
    public interface PageMissingJs extends JsInvokers {

        @JsExpression("return shout($0);")
        PendingJavaScriptResult shout(String text);
    }

    /**
     * U8: no route uses it. Left out of a production bundle anyway; in
     * development mode every definition is in the generated file, so it must
     * not break the others.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./acme-unreached-missing.js", defaultExport = "missing")
    public interface UnreachedMissingJs extends JsInvokers {

        @JsExpression("return missing($0);")
        PendingJavaScriptResult call(String text);
    }

    public MissingModuleView() {
        row("u0", (box, result) -> box.getElement()
                .executeJs(ResolvedJs.class).show("zero"));
        row("u1", (box, result) -> box.getElement()
                .executeJs(MissingModuleJs.class).show("one"));
        row("u3", (box, result) -> box.getElement()
                .executeJs(MissingModuleJs.class).call("three")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("browser error: " + error)));
        row("u4", (box, result) -> {
            box.getElement().setAttribute("tabindex", "0");
            box.getElement().executeJs(MissingModuleJs.class).focus();
        });
        row("u5-parent", (box, result) -> box.getElement()
                .executeJs(ChildMissingJs.class).parentFormatted("five")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("browser error: " + error)));
        row("u5-child", (box, result) -> box.getElement()
                .executeJs(ChildMissingJs.class).childCall("five")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("browser error: " + error)));
        row("u6", (box, result) -> box.getElement()
                .executeJs(MissingPackageJs.class).call("six")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("browser error: " + error)));
        row("u7", (box, result) -> UI.getCurrent().getPage()
                .executeJs(PageMissingJs.class).shout("seven")
                .then(String.class, value -> result.setText("value " + value),
                        error -> result.setText("browser error: " + error)));
    }

    private interface Call {
        void run(Div box, Span result);
    }

    private void row(String id, Call call) {
        Div box = new Div("box " + id);
        box.setId(id + "-box");
        Span result = new Span();
        result.setId(id + "-result");
        AtomicInteger failures = new AtomicInteger();
        Consumer<RuntimeException> report = exception -> result
                .setText("failed " + failures.incrementAndGet() + ": "
                        + exception);
        NativeButton run = new NativeButton("Run " + id, event -> {
            try {
                call.run(box, result);
            } catch (RuntimeException exception) {
                report.accept(exception);
            }
        });
        run.setId(id + "-run");
        add(new Div(run, box, result));
    }
}
