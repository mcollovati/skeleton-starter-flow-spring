package org.vaadin.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * A component with a frontend module, which the application never uses. With
 * optimizeBundle the production build does not reach it, so its module does
 * not stop the application from using the pre-built production bundle.
 */
@Tag("unused-module-component")
@JsModule("./unused-module.js")
public class UnusedModuleComponent extends Component {
}
