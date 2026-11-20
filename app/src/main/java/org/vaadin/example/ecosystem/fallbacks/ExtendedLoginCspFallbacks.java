package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of ExtendedLoginAddon
 * (org.vaadin.addons.flowingcode:extended-login 1.1.0). Each declaration was
 * pasted from the message that Flow printed for the refused call; only the
 * method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface ExtendedLoginCspFallbacks {

    @JsExpression("setTimeout(() => {\n  var overlayWrapper = document.getElementById('vaadinLoginOverlayWrapper');\n  if (!overlayWrapper) {\n    var loginOverlay = document.querySelector('vaadin-login-overlay');\n    if (loginOverlay && loginOverlay.shadowRoot) {\n      overlayWrapper = loginOverlay.shadowRoot\n        .querySelector('vaadin-login-overlay-wrapper');\n    }\n  }\n  if (!overlayWrapper) return;\n  var brand = overlayWrapper.shadowRoot ? overlayWrapper.shadowRoot.querySelector('[part=\"brand\"]') : overlayWrapper.querySelector('[part=\"brand\"]');\nif (brand) {\n  brand.replaceChildren();\n}\n\n});\n")
    PendingJavaScriptResult brandClear();

    @JsExpression("setTimeout(() => {\n  var overlayWrapper = document.getElementById('vaadinLoginOverlayWrapper');\n  if (!overlayWrapper) {\n    var loginOverlay = document.querySelector('vaadin-login-overlay');\n    if (loginOverlay && loginOverlay.shadowRoot) {\n      overlayWrapper = loginOverlay.shadowRoot\n        .querySelector('vaadin-login-overlay-wrapper');\n    }\n  }\n  if (!overlayWrapper) return;\n  var brand = overlayWrapper.shadowRoot ? overlayWrapper.shadowRoot.querySelector('[part=\"brand\"]') : overlayWrapper.querySelector('[part=\"brand\"]');\nif (brand) {\n  brand.appendChild($0);\n}\n\n});\n")
    PendingJavaScriptResult brandAppend(Object arg0);

    @JsExpression("setTimeout(() => {\n  var overlayWrapper = document.getElementById('vaadinLoginOverlayWrapper');\n  if (!overlayWrapper) {\n    var loginOverlay = document.querySelector('vaadin-login-overlay');\n    if (loginOverlay && loginOverlay.shadowRoot) {\n      overlayWrapper = loginOverlay.shadowRoot\n        .querySelector('vaadin-login-overlay-wrapper');\n    }\n  }\n  if (!overlayWrapper) return;\n  var formWrapper = overlayWrapper.querySelector('vaadin-login-form-wrapper');\n  if (!formWrapper) return;\n  var forgotPassword = formWrapper.querySelector('[slot=\"forgot-password\"]');\nif (forgotPassword) {\n  forgotPassword.remove();\n}\n\n});\n")
    PendingJavaScriptResult removeForgotPassword();
}
