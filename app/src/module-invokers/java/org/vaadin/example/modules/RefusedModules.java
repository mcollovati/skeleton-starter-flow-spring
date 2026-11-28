package org.vaadin.example.modules;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsModuleInvokers;
import com.vaadin.flow.js.JsSnippets;

/**
 * Module definitions that break a rule of vaadin/flow#27425 (rows R1-R7 of
 * {@link ModuleCallsView}). {@code Element.executeJs} with a module
 * definition is not here: it does not compile.
 */
public final class RefusedModules {

    private RefusedModules() {
    }

    /** R1: only a named import, so nothing binds this. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js", exports = "initLazy")
    public interface NamedOnlyJs extends JsModuleInvokers {

        @JsExpression("initLazy($0);")
        void initLazy(Element element);
    }

    /** R2: two imports without names. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    @JsDefinitionImport(from = "./app-module-side.js")
    public interface TwoModulesJs extends JsModuleInvokers {

        void initLazy(Element element);
    }

    /** R3: extends two markers. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    public interface BothMarkersJs extends JsModuleInvokers, JsInvokers {

        void initLazy(Element element);
    }

    /**
     * R4: a sub-interface that declares its own method but no import. The
     * parent's import binds this for the inherited method; the own method is
     * declared where nothing is imported.
     */
    @JsDefinition
    public interface ChildWithMethodJs extends ModuleDefinitions.AppModuleJs {

        @JsExpression("return this.format('child ' + $0);")
        PendingJavaScriptResult childFormatted(String text);
    }

    /** R5: a module definition that is also a snippets interface. */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    public interface SnippetsModuleJs extends JsModuleInvokers, JsSnippets {

        void initLazy(Element element);
    }

    /**
     * R6: calls an export the module does not have. Nothing checks the name,
     * so the call fails in the browser.
     */
    @JsDefinition
    @JsDefinitionImport(from = "./app-module.js")
    public interface MissingExportModuleJs extends JsModuleInvokers {

        void notExported();
    }

    /** R7: no {@code @JsDefinition}. */
    @JsDefinitionImport(from = "./app-module.js")
    public interface NotADefinitionJs extends JsModuleInvokers {

        void initLazy(Element element);
    }
}
