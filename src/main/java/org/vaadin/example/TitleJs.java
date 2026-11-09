package org.vaadin.example;

import java.io.Serializable;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * CSP-safe JavaScript: the build puts this expression in the bundle, so the
 * browser does not compile a string at run time.
 */
@JsDefinition
public interface TitleJs extends Serializable {

    @JsExpression("this.title = $0")
    void setTitle(String title);
}
