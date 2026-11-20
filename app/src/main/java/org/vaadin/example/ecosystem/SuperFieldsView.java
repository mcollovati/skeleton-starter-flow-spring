package org.vaadin.example.ecosystem;

import org.vaadin.miki.shared.text.TextInputMode;
import org.vaadin.miki.superfields.gridselect.GridSelect;
import org.vaadin.miki.superfields.text.SuperTextField;

import com.vaadin.flow.router.Route;

/**
 * super-fields (pl.unforgiven:superfields 0.21.2): a read-only GridSelect and
 * a SuperTextField with a numeric input mode. Both send their strings before
 * the client response, so a refusal can not be caught here.
 */
@Route("eco/superfields")
public class SuperFieldsView extends EcoView {

    public SuperFieldsView() {
        super("super-fields");
        GridSelect<String> select = withId(
                new GridSelect<>("Alpha", "Beta", "Gamma"), "grid-select");
        select.getGrid().addColumn(s -> s).setHeader("Name");
        select.setReadOnly(true);
        add(select);

        SuperTextField text = withId(new SuperTextField("Number"), "text");
        text.setTextInputMode(TextInputMode.NUMERIC);
        add(text);
    }
}
