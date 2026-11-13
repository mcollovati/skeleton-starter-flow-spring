package org.vaadin.example;

import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsSnippet;

/**
 * A snippet type of the application (vaadin/flow#26960). Flow implements it
 * with a JDK proxy when a definition method returns it, and a native image
 * can only create a proxy that was registered at build time.
 */
@JsCalledWith({ "name" })
public interface Greeting extends JsSnippet {
}
