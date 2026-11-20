package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.router.Route;

/**
 * XTermConsoleAddon (com.flowingcode.addons:xterm-console 3.3.0). The add-on
 * queues its calls until the browser says that the terminal is ready, so the
 * refusals happen in that event and can not be caught here.
 */
@Route("eco/xterm")
public class XTermView extends EcoView {

    public XTermView() {
        super("XTermConsoleAddon");
        run(() -> {
            XTerm term = withId(new XTerm(), "term");
            term.setCursorBlink(true);
            term.writeln("Hello CSP");
            term.setWidth("600px");
            term.setHeight("200px");
            add(term);
        });
    }
}
