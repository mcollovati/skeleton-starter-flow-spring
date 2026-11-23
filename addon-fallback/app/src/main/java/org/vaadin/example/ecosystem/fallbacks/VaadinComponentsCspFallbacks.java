package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of Vaadin's own components in
 * 25.4.0-beta1 (MenuBar, used by EnhancedTabs; Dialog, used by
 * ChatAssistant). Each declaration was pasted from the message that Flow
 * printed for the refused call; only the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface VaadinComponentsCspFallbacks {

    @JsExpression("window.Vaadin.Flow.menubarConnector.initLazy(this)")
    PendingJavaScriptResult menuBarInitLazy();

    @JsExpression("this.$.overlay.$.overlay.style[$0]=$1")
    PendingJavaScriptResult dialogSetDimension(Object arg0, Object arg1);

    @JsExpression("  this.$.overlay.addEventListener('vaadin-overlay-close', () => {\n    document.addEventListener('vaadin-overlay-close', (e) => {\n      if (!e.defaultPrevented && e.detail.overlay === this.$.overlay) {\n        e.preventDefault();\n        this.$server.handleClientClose();\n      }\n    }, { once: true });\n  });\n")
    PendingJavaScriptResult dialogRegisterClientCloseHandler();
}
