package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of vaadin-ckeditor
 * (com.wontlost:ckeditor-vaadin 6.1.0). Each declaration was pasted from the
 * message that Flow printed for the refused call; only the method names were
 * changed.
 */
@JsDefinition
@JsStringFallback
public interface VaadinCkEditorCspFallbacks {

    @JsExpression("this.updateData(this.editorData)")
    PendingJavaScriptResult updateData();

    @JsExpression("this.insertText($0)")
    PendingJavaScriptResult insertText(Object arg0);

    @JsExpression("this.setReadOnly($0)")
    PendingJavaScriptResult setReadOnly(Object arg0);
}
