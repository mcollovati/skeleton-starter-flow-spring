package org.vaadin.example;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Functions the application declares for the ACME add-on's functions made from
 * strings: each value is the exact function body the add-on passes, and each
 * method has one parameter per capture.
 */
@JsDefinition
@JsStringFallback
public interface AcmeCspFunctionFallbacks {

    // Case 9: ResizeWatcher, addJsInitializer(String, Object...). The text
    // block gives the same string as the add-on's text block.
    @JsExpression("""
            const ro = new ResizeObserver(() => {
              this.textContent = $0 + ': ' + this.offsetWidth + 'px';
            });
            ro.observe(this);
            return () => ro.disconnect();
            """)
    JsFunction watchSize(Object label);

    // Case 10: Tooltip, JsFunction.of(String, Object...)
    @JsExpression("this.title = $0")
    JsFunction setTitle(Object text);

    // Case 15: ClickReporter, JsFunction.of(String, Object...) followed by
    // withArguments("type"), so the method declares the same name
    @JsExpression("$0.textContent = $1 + type")
    @JsFunctionParameterNames("type")
    JsFunction reportClick(Object element, Object prefix);

    // Case 11: LazyLoader, Page.addDynamicImport(String)
    @JsExpression("return new Promise(r => setTimeout(() => { window.acmeLoaded = true; r(); }, 10))")
    JsFunction loadLazily();
}
