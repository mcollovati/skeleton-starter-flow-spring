package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of ChatAssistant
 * (org.vaadin.addons.flowingcode:chat-assistant-addon 5.2.0). Each
 * declaration was pasted from the message that Flow printed for the refused
 * call; only the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface ChatAssistantCspFallbacks extends JsInvokers {

    @JsExpression("window.fcChatAssistantPortalFab && window.fcChatAssistantPortalFab(this, $0)")
    PendingJavaScriptResult portalFab(Object arg0);

    @JsExpression("if(!this['fc-chat-assistant-drag-listener']) { window.fcChatAssistantMovement($0, $1, $2, $3, $4, $5); }\nif(!this['fc-chat-assistant-drag-listener']) {\n  this['fc-chat-assistant-drag-listener'] = 'fc-chat-assistant-drag-listener';\n};\n")
    PendingJavaScriptResult refreshedDrag(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("window.fcChatAssistantRestoreWindowSize($0, $1);")
    PendingJavaScriptResult restoreWindowSize(Object arg0, Object arg1);

    @JsExpression("if(!this['fc-chat-assistant-resize-top-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'top'); }\nif(!this['fc-chat-assistant-resize-top-listener']) {\n  this['fc-chat-assistant-resize-top-listener'] = 'fc-chat-assistant-resize-top-listener';\n};\n")
    PendingJavaScriptResult resizeTop(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-bottom-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'bottom'); }\nif(!this['fc-chat-assistant-resize-bottom-listener']) {\n  this['fc-chat-assistant-resize-bottom-listener'] = 'fc-chat-assistant-resize-bottom-listener';\n};\n")
    PendingJavaScriptResult resizeBottom(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-right-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'right'); }\nif(!this['fc-chat-assistant-resize-right-listener']) {\n  this['fc-chat-assistant-resize-right-listener'] = 'fc-chat-assistant-resize-right-listener';\n};\n")
    PendingJavaScriptResult resizeRight(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-top-right-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'top-right'); }\nif(!this['fc-chat-assistant-resize-top-right-listener']) {\n  this['fc-chat-assistant-resize-top-right-listener'] = 'fc-chat-assistant-resize-top-right-listener';\n};\n")
    PendingJavaScriptResult resizeTopRight(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-bottom-right-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'bottom-right'); }\nif(!this['fc-chat-assistant-resize-bottom-right-listener']) {\n  this['fc-chat-assistant-resize-bottom-right-listener'] = 'fc-chat-assistant-resize-bottom-right-listener';\n};\n")
    PendingJavaScriptResult resizeBottomRight(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-left-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'left'); }\nif(!this['fc-chat-assistant-resize-left-listener']) {\n  this['fc-chat-assistant-resize-left-listener'] = 'fc-chat-assistant-resize-left-listener';\n};\n")
    PendingJavaScriptResult resizeLeft(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-top-left-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'top-left'); }\nif(!this['fc-chat-assistant-resize-top-left-listener']) {\n  this['fc-chat-assistant-resize-top-left-listener'] = 'fc-chat-assistant-resize-top-left-listener';\n};\n")
    PendingJavaScriptResult resizeTopLeft(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("if(!this['fc-chat-assistant-resize-bottom-left-listener']) { window.fcChatAssistantResize($0, $1, $2, $3, $4, $5, 'bottom-left'); }\nif(!this['fc-chat-assistant-resize-bottom-left-listener']) {\n  this['fc-chat-assistant-resize-bottom-left-listener'] = 'fc-chat-assistant-resize-bottom-left-listener';\n};\n")
    PendingJavaScriptResult resizeBottomLeft(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @JsExpression("[...this.childNodes].forEach(node => node.nodeType === 3 && this.removeChild(node));if ($0) this.appendChild(document.createTextNode($0));")
    PendingJavaScriptResult setMessageText(Object arg0);
}
