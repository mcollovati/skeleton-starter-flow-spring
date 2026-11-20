package org.vaadin.example.ecosystem;

import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import com.vaadin.flow.router.Route;

/**
 * flow-viritin MarkdownMessage. Its constructor sends a string that uses the
 * element as $0 with no arguments, which the fallback gets as its last
 * parameter.
 */
@Route("eco/viritin-markdown")
public class ViritinMarkdownView extends EcoView {

    public ViritinMarkdownView() {
        super("flow-viritin MarkdownMessage");
        run(() -> add(withId(new MarkdownMessage("# Hello Markdown", "Bot"),
                "markdown")));
    }
}
