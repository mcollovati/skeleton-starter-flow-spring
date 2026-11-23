package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of MarkdownEditor
 * (com.flowingcode.vaadin.addons:markdown-editor-addon 2.0.3). Each
 * declaration was pasted from the message that Flow printed for the refused
 * call; only the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface MarkdownEditorCspFallbacks {

    @JsExpression("window.Vaadin.Flow.fcMarkdownEditorConnector.observeThemeChange($0)")
    PendingJavaScriptResult observeThemeChange(Object arg0);

    @JsExpression("window.Vaadin.Flow.fcMarkdownEditorConnector.unobserveThemeChange($0)")
    PendingJavaScriptResult unobserveThemeChange(Object arg0);
}
