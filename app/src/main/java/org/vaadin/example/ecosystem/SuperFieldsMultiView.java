package org.vaadin.example.ecosystem;

import java.util.TreeSet;

import org.vaadin.miki.superfields.gridselect.GridMultiSelect;

import com.vaadin.flow.router.Route;

/**
 * super-fields GridMultiSelect. Its string uses the element as $0 with no
 * arguments, which the fallback gets as its last parameter.
 */
@Route("eco/superfields-multi")
public class SuperFieldsMultiView extends EcoView {

    public SuperFieldsMultiView() {
        super("super-fields GridMultiSelect");
        GridMultiSelect<String> select = withId(
                new GridMultiSelect<>("Alpha", "Beta", "Gamma"), "multi");
        select.getGrid().addColumn(s -> s).setHeader("Name");
        select.setMaximumSelectionSize(1);
        add(select);
        var value = output("value");
        select.addValueChangeListener(
                e -> value.setText(String.join(",", new TreeSet<>(e.getValue()))));
        add(value);
    }
}
