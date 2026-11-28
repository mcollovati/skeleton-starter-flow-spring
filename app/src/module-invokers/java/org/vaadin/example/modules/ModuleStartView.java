package org.vaadin.example.modules;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * M2 (vaadin/flow#27425): reaches no module definition. The link goes to
 * {@link ModuleCallsView} through the router, so its chunk loads on
 * navigation, not with the page.
 */
@Route("module-start")
public class ModuleStartView extends Div {

    public ModuleStartView() {
        add(new H1("Start (vaadin/flow#27425)"));
        RouterLink link = new RouterLink("To the module calls (router)",
                ModuleCallsView.class);
        link.setId("to-calls");
        add(link);
    }
}
