package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of flow-viritin (in.virit:viritin
 * 3.9.1). Each declaration was pasted from the message that Flow printed for
 * the refused call; only the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface FlowViritinCspFallbacks extends JsInvokers {

    @JsExpression("\n$0.querySelector('vaadin-avatar').style.setProperty('--vaadin-avatar-user-color', null);$0.querySelector('vaadin-avatar').setAttribute('has-color-index', true);")
    PendingJavaScriptResult markdownMessageAvatarColor();

    @JsExpression("    const md = window.markdownit(); \n    const input = $0;\n    this.markdown = input;\n    const html = md.render(input);\n    this.innerHTML = html;\n")
    PendingJavaScriptResult markdownMessageSetMarkdown(Object arg0);

    @JsExpression("this.innerHTML = $0")
    PendingJavaScriptResult setInnerHtml(Object arg0);

    @JsExpression("    var self = this;\n    this.addEventListener(\"scroll\", function(e) {\n        // if rather close the the end, fire it!\n        if(self.scrollTop + self.clientHeight > (self.scrollHeight - self.clientHeight/4)) {\n            var e = new Event(\"scroll-to-end\");\n            self.dispatchEvent(e);\n        }\n    });\n")
    PendingJavaScriptResult scrollerScrollToEnd();

    @JsExpression("this.scrollTop = $0")
    PendingJavaScriptResult setScrollTop(Object arg0);

    @JsExpression("window[$0].setItem($1,$2)")
    PendingJavaScriptResult webStorageSetItem(Object arg0, Object arg1, Object arg2);

    @JsExpression("return window[$0].getItem($1);")
    PendingJavaScriptResult webStorageGetItem(Object arg0, Object arg1);
}
