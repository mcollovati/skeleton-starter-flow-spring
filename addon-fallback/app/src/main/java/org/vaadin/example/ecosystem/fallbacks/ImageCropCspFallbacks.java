package org.vaadin.example.ecosystem.fallbacks;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;

/**
 * Fallbacks for the string JavaScript of ImageCrop
 * (com.flowingcode.vaadin.addons:image-crop-addon 1.3.0). Each declaration
 * was pasted from the message that Flow printed for the refused call; only
 * the method names were changed.
 */
@JsDefinition
@JsStringFallback
public interface ImageCropCspFallbacks {

    @JsExpression("this._updateCroppedImage(this.crop)")
    PendingJavaScriptResult updateCroppedImage();
}
