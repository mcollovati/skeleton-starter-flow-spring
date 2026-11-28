package org.vaadin.example.modules;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsModuleInvokers;

/**
 * Module definitions (vaadin/flow#27425) that {@link ModuleCallsView} calls.
 * The row ids are those of the view and of the README.
 */
public final class ModuleDefinitions {

    private ModuleDefinitions() {
    }

    /**
     * M1-M4, M7: a module of the project. {@code initLazy} has no
     * {@code @JsExpression}, so it runs {@code return this.initLazy($0)}.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    public interface AppModuleJs extends JsModuleInvokers {

        void initLazy(Element element);

        @JsExpression("return this.format($0);")
        PendingJavaScriptResult formatted(String text);
    }

    /** M11: inherits everything, so this is the module of the parent. */
    @JsDefinition
    public interface ChildModuleJs extends AppModuleJs {
    }

    /**
     * M8: the import without names, and a named import of another module.
     * Both must load with the view.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    @JsDefinitionImport(from = "./app-format.js", defaultExport = "appFormat")
    public interface NamedAlsoJs extends JsModuleInvokers {

        @JsExpression("return appFormat(this.format($0));")
        PendingJavaScriptResult both(String text);
    }

    /** M6: an npm package as this. */
    @JsDefinition
    @JsDefinitionImport(from = "lit")
    public interface LitModuleJs extends JsModuleInvokers {

        @JsExpression("this.render(this.html`<b class=\"m6\">${$0}</b>`, $1);")
        void show(String text, Element target);
    }

    /**
     * M9: the same file as {@code @JsModule} (side effects only) and as the
     * module of the definition. It must be evaluated once.
     */
    @JsDefinition
    @JsModule("./app-module-side.js")
    @JsDefinitionImport(from = "./app-module-side.js")
    public interface SideEffectJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    /** M10: reached from ModuleCallsView and from ModuleOtherView. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module-shared.js")
    public interface SharedModuleJs extends JsModuleInvokers {

        @JsExpression("return this.increment();")
        PendingJavaScriptResult increment();
    }

    /**
     * M12: imported with a query. The chunk and the generated function must
     * use the same registry key.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module-query.js?v=2")
    public interface QueryModuleJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    /** M13: imported without the file extension. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module-noext")
    public interface NoExtensionModuleJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    /**
     * Runs on an element after a module call. It records whether the
     * connector that {@code initLazy} sets up is already there.
     */
    @JsDefinition
    public interface OrderJs extends JsInvokers {

        @JsExpression("this.dataset.order = ((this.dataset.order ?? '') + ' '"
                + " + $0 + ':' + (this.$connector ? 'ready' : 'missing'))"
                + ".trim();")
        void record(String label);

        /** M6: whether lit rendered into the element. */
        @JsExpression("this.dataset.order = this.querySelector('b.m6')"
                + " ? 'rendered' : 'empty';")
        void checkRendered();
    }
}
