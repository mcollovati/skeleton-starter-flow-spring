package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.js.JsExpression;

/**
 * A generic base without {@code @JsDefinition}. The definitions that extend it
 * bind {@code T}, and the result is converted to the bound type.
 */
public interface ValueJs<T> extends Serializable {

    @JsExpression("return JSON.parse($0)")
    JavaScriptResult<T> parse(String json);
}
