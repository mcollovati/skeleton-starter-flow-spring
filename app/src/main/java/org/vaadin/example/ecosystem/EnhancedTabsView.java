package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.enhancedtabs.EnhancedTabs;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.Route;

/**
 * EnhancedTabs (com.flowingcode.vaadin.addons:enhanced-tabs-addon 1.2.2): 8
 * tabs in 250px, so most of them go to the overflow menu.
 */
@Route("eco/enhanced-tabs")
public class EnhancedTabsView extends EcoView {

    public EnhancedTabsView() {
        super("EnhancedTabs");
        EnhancedTabs tabs = withId(new EnhancedTabs(), "tabs");
        tabs.setWidth("250px");
        run(() -> {
            for (int i = 1; i <= 8; i++) {
                tabs.add(new Tab("Tab " + i));
            }
        });
        var selected = output("selected");
        selected.setText(tabs.getSelectedTab().getLabel());
        tabs.addSelectedChangeListener(e -> selected
                .setText(e.getSelectedTab() == null ? "none"
                        : e.getSelectedTab().getLabel()));
        add(tabs, selected);
    }
}
