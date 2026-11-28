package org.vaadin.example.modules;

import java.io.Serializable;

import org.vaadin.example.modules.ModuleDefinitions.AppModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.ChildModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.LitModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.NamedAlsoJs;
import org.vaadin.example.modules.ModuleDefinitions.NoExtensionModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.OrderJs;
import org.vaadin.example.modules.ModuleDefinitions.QueryModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.SharedModuleJs;
import org.vaadin.example.modules.ModuleDefinitions.SideEffectJs;

import com.acme.addon.ModuleBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsModuleInvokers;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Cases for vaadin/flow#27425: {@code Page.executeModuleJs} calls the exports
 * of a module, which must load with the route, so the calls run in the order
 * the server sends them.
 * <p>
 * The order rows show it in {@code data-order} of the box: {@code initLazy}
 * adds {@code init} and sets up {@code $connector}, and the call after it
 * adds {@code after:ready} or, when it runs first, {@code after:missing}.
 */
@Route("module-calls")
public class ModuleCallsView extends Div {

    private static final String PACKAGE = ModuleCallsView.class
            .getPackageName();

    public ModuleCallsView() {
        add(new H1("Module definitions (vaadin/flow#27425)"));
        RouterLink toOther = new RouterLink("To the other view (router)",
                ModuleOtherView.class);
        toOther.setId("to-other");
        add(toOther);

        add(new H3("Order, calls made when the view is attached"));
        Div m1 = box("m1", "module of the project, initLazy and a call after it");
        Div m6 = box("m6", "npm package lit as this, render, then a check");
        Div m9 = box("m9", "@JsModule and @JsDefinitionImport of one file");
        Div m11 = box("m11", "sub-interface that only inherits");
        Div m12 = box("m12", "module imported with a query");
        ModuleBox m5 = new ModuleBox("m5: add-on component, module in the jar,"
                + " calls in onAttach");
        m5.setId("m5-box");
        add(m5);

        addAttachListener(event -> {
            Page page = event.getUI().getPage();
            page.executeModuleJs(AppModuleJs.class).initLazy(m1.getElement());
            order(m1).record("after");
            page.executeModuleJs(LitModuleJs.class).show("six",
                    m6.getElement());
            order(m6).checkRendered();
            page.executeModuleJs(SideEffectJs.class)
                    .initLazy(m9.getElement());
            order(m9).record("after");
            page.executeModuleJs(ChildModuleJs.class)
                    .initLazy(m11.getElement());
            order(m11).record("after");
            page.executeModuleJs(QueryModuleJs.class)
                    .initLazy(m12.getElement());
            order(m12).record("after");
        });

        add(new H3("Order, calls made in a click"));
        Div m3 = box("m3", "two round trips: initLazy, then the call after it");
        button("m3-init", "initLazy", () -> page()
                .executeModuleJs(AppModuleJs.class).initLazy(m3.getElement()));
        button("m3-record", "call after it", () -> order(m3).record("after"));

        Div m4Holder = new Div();
        add(new Span("m4: a new box, attached in the same response as"
                + " initLazy and the call after it (I13 with a module"
                + " definition)"));
        button("m4-run", "attach and call", () -> {
            m4Holder.removeAll();
            Div m4 = new Div("m4 box");
            m4.setId("m4-box");
            m4Holder.add(m4);
            page().executeModuleJs(AppModuleJs.class)
                    .initLazy(m4.getElement());
            order(m4).record("after");
        });
        add(m4Holder);

        // In a click, not on attach: when the module is not registered, its
        // warning would show up in every test that opens the view
        Div m13 = box("m13", "module imported without the extension");
        button("m13-run", "initLazy and the call after it", () -> {
            page().executeModuleJs(NoExtensionModuleJs.class)
                    .initLazy(m13.getElement());
            order(m13).record("after");
        });

        Div m14 = box("m14", "an element call made before the module call,"
                + " in the same round trip");
        button("m14-run", "Run", () -> {
            order(m14).record("before");
            page().executeModuleJs(AppModuleJs.class)
                    .initLazy(m14.getElement());
            order(m14).record("after");
        });

        add(new H3("Results"));
        row("m7", "return value through this", result -> show(
                page().executeModuleJs(AppModuleJs.class).formatted("seven"),
                result));
        row("m8", "named import next to the import without names",
                result -> show(page().executeModuleJs(NamedAlsoJs.class)
                        .both("eight"), result));
        row("m10", "module shared with ModuleOtherView, increment",
                result -> show(page().executeModuleJs(SharedModuleJs.class)
                        .increment(), result));

        add(new H3("Fallback"));
        Div f1 = box("f1", "definition reached only from ModuleOtherView,"
                + " called through reflection");
        // No reset() here: an element call is queued when the response is
        // written, so it would run after the module call (row M14). The IT
        // resets the box between clicks
        button("f1-run", "Run", () -> {
            callReflected(PACKAGE + ".ModuleOtherView$OtherOnlyJs", f1);
            order(f1).record("after");
        });
        Div f2 = box("f2", "definition that no route reaches,"
                + " called through reflection");
        button("f2-run", "Run", () -> {
            callReflected(PACKAGE + ".UnreachedModuleJs", f2);
            order(f2).record("after");
        });

        add(new H3("Rules"));
        Div target = box("r", "target of the rule rows");
        row("r1", "only a named import", result -> {
            page().executeModuleJs(RefusedModules.NamedOnlyJs.class)
                    .initLazy(target.getElement());
            result.setText("called");
        });
        row("r2", "two imports without names", result -> {
            page().executeModuleJs(RefusedModules.TwoModulesJs.class)
                    .initLazy(target.getElement());
            result.setText("called");
        });
        row("r3", "extends JsModuleInvokers and JsInvokers", result -> {
            page().executeModuleJs(RefusedModules.BothMarkersJs.class)
                    .initLazy(target.getElement());
            result.setText("called");
        });
        row("r4", "sub-interface with its own method and no import",
                result -> show(
                        page().executeModuleJs(
                                RefusedModules.ChildWithMethodJs.class)
                                .childFormatted("four"),
                        result));
        row("r5", "extends JsModuleInvokers and JsSnippets", result -> {
            page().executeModuleJs(RefusedModules.SnippetsModuleJs.class)
                    .initLazy(target.getElement());
            result.setText("called");
        });
        row("r6", "export the module does not have", result -> {
            page().executeModuleJs(RefusedModules.MissingExportModuleJs.class)
                    .notExported();
            result.setText("called");
        });
        row("r7", "no @JsDefinition", result -> {
            page().executeModuleJs(RefusedModules.NotADefinitionJs.class)
                    .initLazy(target.getElement());
            result.setText("called");
        });
    }

    private Page page() {
        return getUI().orElseThrow().getPage();
    }

    private static OrderJs order(Div box) {
        return box.getElement().executeJs(OrderJs.class);
    }

    private Div box(String id, String title) {
        add(new Span(id + ": " + title));
        Div box = new Div(id + " box");
        box.setId(id + "-box");
        box.getStyle().set("border", "1px solid gray").set("padding", "4px");
        add(box);
        return box;
    }

    private void button(String id, String text, Runnable action) {
        NativeButton button = new NativeButton(text, event -> action.run());
        button.setId(id);
        add(button);
    }

    private void row(String id, String title, ResultAction action) {
        add(new Span(id + ": " + title + " "));
        Span result = new Span();
        result.setId(id + "-result");
        NativeButton run = new NativeButton("Run", event -> {
            try {
                action.run(result);
            } catch (RuntimeException e) {
                result.setText("refused: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        });
        run.setId(id + "-run");
        add(run, result, new Div());
    }

    private static void show(PendingJavaScriptResult pending, Span result) {
        pending.then(String.class, value -> result.setText("value " + value),
                error -> result.setText("failed: " + error));
    }

    // Through reflection, so that the build does not reach the definition
    // from this view
    private void callReflected(String className, Div box) {
        try {
            Class<? extends JsModuleInvokers> type = Class.forName(className)
                    .asSubclass(JsModuleInvokers.class);
            Object definition = page().executeModuleJs(type);
            type.getMethod("initLazy", Element.class).invoke(definition,
                    box.getElement());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    interface ResultAction extends Serializable {
        void run(Span result);
    }
}
