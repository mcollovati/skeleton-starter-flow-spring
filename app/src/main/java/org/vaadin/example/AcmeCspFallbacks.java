package org.vaadin.example;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * JavaScript the application declares for the string calls of the ACME
 * add-on. Each value is the exact string the add-on sends, and each method has
 * as many parameters as the add-on passes. The build puts these functions in
 * the bundle and lists the interface for the server, because of
 * {@link JsStringFallback}, and Flow runs these functions instead of the
 * strings.
 * <p>
 * Case 5 (Highlighter) is left out on purpose: its string changes with the
 * color, so no fixed text can match it.
 */
@JsDefinition
@JsStringFallback
public interface AcmeCspFallbacks {

    // Case 1: AutoFocus
    @JsExpression("setTimeout(() => this.focus(), 0)")
    void autoFocus();

    // Case 2: ScrollPanel.scrollToBottom
    @JsExpression("this.scrollTop = this.scrollHeight")
    void scrollToBottom();

    // Case 3: ScrollPanel.getScrollTop
    @JsExpression("return this.scrollTop")
    PendingJavaScriptResult scrollTop();

    // Case 4: PageTitle (a page call, so `this` is not an element)
    @JsExpression("document.title = $0 + ' (' + $1 + ')'")
    void setTitle(Object title, Object count);

    // Case 6: ConsoleLogger, one method per argument count it is used with
    @JsExpression("console.log('[acme]', $0)")
    void log(Object value);

    @JsExpression("console.log('[acme]', $0, $1, $2)")
    void log(Object first, Object second, Object third);
}
