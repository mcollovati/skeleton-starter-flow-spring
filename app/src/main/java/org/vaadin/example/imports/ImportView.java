package org.vaadin.example.imports;

import com.acme.addon.ImportBox;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Cases for vaadin/flow#27423: {@code @JsDefinitionImport} gives the
 * JavaScript of a definition names from a module. Each row has a box, a "Run"
 * button and a result. Rows I1-I11 must work, I12 fails in the browser on
 * purpose, I13 shows the order of the calls (its box gets
 * {@code data-order}), rows X1-X6 must be refused in Java and the result
 * shows the message.
 */
@Route("imports")
public class ImportView extends Div {

    public ImportView() {
        add(new H1("Imports from modules (vaadin/flow#27423)"));

        add(new H3("Add-on definition ImportBox.ImportBoxJs (modules of the jar)"));
        row("i1", "default export and a named export", (box, result) -> {
            box.js().showUpperFormatted("one");
            result.setText("called");
        });
        row("i2", "namespace, default export through it", (box, result) -> {
            box.js().showLabel("two");
            result.setText("called");
        });
        row("i3", "PendingJavaScriptResult",
                (box, result) -> show(box.js().formatted("three"), result));
        row("i4", "JavaScriptResult<String>",
                (box, result) -> box.js().labelVersion().then(
                        value -> result.setText("version " + value),
                        error -> result.setText("failed: " + error)));
        row("i5", "variadic", (box, result) -> {
            box.js().showAll("a", "b", "c");
            result.setText("called");
        });
        row("i6", "method without @JsExpression next to imports",
                (box, result) -> {
                    box.js().focus();
                    result.setText("called");
                });
        row("i7", "same JavaScript, other imports (AltFormatJs)",
                (box, result) -> {
                    box.altJs().showFormatted("seven");
                    show(box.js().formatted("seven"), result);
                });

        add(new H3("Application definitions"));
        row("i8", "AppFormatJs extends ImportBoxJs, own import of format",
                (box, result) -> {
                    AppImports.AppFormatJs js = box.getElement()
                            .executeJs(AppImports.AppFormatJs.class);
                    js.showFormatted("eight");
                    show(js.appFormatted("eight"), result);
                });
        row("i9", "Page.executeJs, module of the project",
                (box, result) -> show(UI.getCurrent().getPage()
                        .executeJs(AppImports.PageImportJs.class)
                        .shout("nine"), result));
        row("i10", "npm package (lit)", (box, result) -> {
            box.getElement().executeJs(AppImports.LitJs.class)
                    .renderBold("ten");
            result.setText("called");
        });
        row("i11", "arguments holds the imported values",
                (box, result) -> show(box.js().argumentCount("x"), result));
        row("i12", "export the module does not have: fails in the browser",
                (box, result) -> box.getElement()
                        .executeJs(AppImports.MissingExportJs.class)
                        .call("twelve")
                        .then(value -> result.setText("no error: " + value),
                                error -> result.setText(
                                        "browser error: " + error)));
        row("i13", "order: a call after an importing call in the same response",
                (box, result) -> {
                    AppImports.ConnectorCheckJs check = box.getElement()
                            .executeJs(AppImports.ConnectorCheckJs.class);
                    check.reset();
                    box.getElement().executeJs(AppImports.ConnectorJs.class)
                            .init();
                    check.record("after-init");
                    result.setText("called");
                });

        add(new H3("Refused in Java"));
        row("x1", "JsSnippets interface with an import",
                (box, result) -> JsSnippets
                        .of(AppImports.ImportSnippetsJs.class)
                        .formatted("x"));
        row("x2", "import that names nothing", (box, result) -> box
                .getElement().executeJs(AppImports.NoNamesJs.class).one());
        row("x3", "reserved word as a name (default)",
                (box, result) -> box.getElement()
                        .executeJs(AppImports.ReservedNameJs.class).one());
        row("x4", "argument name as a name ($0)",
                (box, result) -> box.getElement()
                        .executeJs(AppImports.ArgumentNameJs.class)
                        .echo("x"));
        row("x5", "the same name from two modules",
                (box, result) -> box.getElement()
                        .executeJs(AppImports.DuplicateNameJs.class)
                        .formatted("x"));
        row("x6", "bad import on a parent interface",
                (box, result) -> box.getElement()
                        .executeJs(AppImports.InheritedBadImportJs.class)
                        .one());
    }

    private static void show(PendingJavaScriptResult pending, Span result) {
        pending.then(String.class, value -> result.setText("value " + value),
                error -> result.setText("failed: " + error));
    }

    private void row(String id, String title,
            SerializableConsumer2<ImportBox, Span> action) {
        ImportBox box = new ImportBox("Box " + id);
        box.setId(id + "-box");
        box.getStyle().set("border", "1px solid gray").set("padding", "4px");
        Span result = new Span();
        result.setId(id + "-result");
        NativeButton run = new NativeButton("Run", event -> {
            try {
                action.accept(box, result);
            } catch (RuntimeException e) {
                result.setText("refused: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        });
        run.setId(id + "-run");
        Div row = new Div(new Span(id.toUpperCase() + " " + title + " "), run,
                new Span(" "), result, box);
        row.getStyle().set("margin-bottom", "12px");
        add(row);
    }

    @FunctionalInterface
    interface SerializableConsumer2<A, B> extends java.io.Serializable {
        void accept(A a, B b);
    }
}
