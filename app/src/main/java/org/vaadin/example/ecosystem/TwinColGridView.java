package org.vaadin.example.ecosystem;

import java.util.List;
import java.util.TreeSet;

import com.flowingcode.vaadin.addons.twincolgrid.TwinColGrid;
import com.vaadin.flow.router.Route;

/**
 * TwinColGrid (com.flowingcode.vaadin.addons:twincolgrid 4.0.1).
 */
@Route("eco/twincolgrid")
public class TwinColGridView extends EcoView {

    public TwinColGridView() {
        super("TwinColGrid");
        TwinColGrid<String> twin = withId(
                new TwinColGrid<>(List.of("Alpha", "Beta", "Gamma")), "twin");
        twin.addColumn(s -> s);
        run(twin::selectRowOnClick);
        run(() -> twin.setAutoResize(true));
        var value = output("value");
        twin.addValueChangeListener(
                e -> value.setText(String.join(",", new TreeSet<>(e.getValue()))));
        add(twin, value);
    }
}
