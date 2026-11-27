package org.vaadin.example.imports;

import com.acme.addon.ImportBox;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;

/**
 * The definitions of the application for vaadin/flow#27423
 * ({@code @JsDefinitionImport}). {@link ImportView} calls each one.
 */
public final class AppImports {

    private AppImports() {
    }

    /**
     * I8: extends the add-on definition and imports {@code format} from a
     * module of the project. The inherited methods keep the {@code format} of
     * the add-on; the own method gets the one of the project, although it has
     * the same JavaScript as {@link ImportBox.ImportBoxJs#formatted}.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public interface AppFormatJs extends ImportBox.ImportBoxJs {

        @JsExpression("return format($0);")
        PendingJavaScriptResult appFormatted(String text);
    }

    /** I9: called through {@code Page.executeJs}, so it runs on no element. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", exports = "shout")
    public interface PageImportJs extends JsInvokers {

        @JsExpression("return shout($0);")
        PendingJavaScriptResult shout(String text);
    }

    /** I10: an npm package, which Flow already depends on. */
    @JsDefinition
    @JsDefinitionImport(from = "lit", exports = { "html", "render" })
    public interface LitJs extends JsInvokers {

        @JsExpression("render(html`<b class=\"lit\">${$0}</b>`, this);")
        void renderBold(String text);
    }

    /**
     * I12: names an export the module does not have. Nothing checks it, so
     * the name is {@code undefined} and the call fails in the browser.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", exports = "notExported")
    public interface MissingExportJs extends JsInvokers {

        @JsExpression("return notExported($0);")
        PendingJavaScriptResult call(String text);
    }

    // Refused in Java (and left out of the bundle by the build)

    /** X1: a snippet function is not async, so it can not wait for a module. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public interface ImportSnippetsJs extends JsSnippets {

        @JsExpression("format($0)")
        JsSnippet formatted(String text);
    }

    /** X2: names nothing; {@code @JsModule} is for side effects only. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js")
    public interface NoNamesJs extends JsInvokers {

        @JsExpression("return 1;")
        PendingJavaScriptResult one();
    }

    /** X3: a reserved word as a name. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "default")
    public interface ReservedNameJs extends JsInvokers {

        @JsExpression("return 1;")
        PendingJavaScriptResult one();
    }

    /** X4: the name of an argument. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", namespace = "$0")
    public interface ArgumentNameJs extends JsInvokers {

        @JsExpression("return $0;")
        PendingJavaScriptResult echo(String text);
    }

    /** X5: the same name from two modules. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    @JsDefinitionImport(from = "./acme-format.js", defaultExport = "format")
    public interface DuplicateNameJs extends JsInvokers {

        @JsExpression("return format($0);")
        PendingJavaScriptResult formatted(String text);
    }

    /**
     * X6: the bad import is on a parent that is not a definition itself; the
     * definition that extends it is refused.
     */
    @JsDefinitionImport(from = "./app-format.js", namespace = "class")
    public interface BadImportParent extends JsInvokers {

        @JsExpression("return 1;")
        PendingJavaScriptResult one();
    }

    @JsDefinition
    public interface InheritedBadImportJs extends BadImportParent {
    }

    /**
     * On a class the annotation does nothing: the build logs an error and goes
     * on ("Ignoring @JsDefinitionImport on ...ImportOnClass").
     */
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "format")
    public static class ImportOnClass {
    }
}
