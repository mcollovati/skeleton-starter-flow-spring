package org.vaadin.example.usesfix;

import com.acme.addon.ReflectedImportJs;
import com.acme.addon.ReflectedJs;
import com.acme.addon.ReflectedShorthandJs;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * The fix that the error of vaadin/flow#27078 suggests: a class the scanner
 * reaches (a route) names the definition with {@code @Uses}, so the production
 * bundle carries its JavaScript and {@code /missing} works.
 */
@Route("uses-fix")
@Uses(ReflectedJs.class)
@Uses(ReflectedShorthandJs.class)
@Uses(ReflectedImportJs.class)
public class UsesFixView extends Div {
}
