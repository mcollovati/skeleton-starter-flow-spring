package org.vaadin.example.ecosystem;

import org.vaadin.hugerte.HugeRte;

import com.vaadin.flow.router.Route;

/**
 * hugerte-for-flow (org.parttio:hugerte-for-flow 2.3.0). After the user types,
 * setting the old value again sends "this.value = $0".
 */
@Route("eco/hugerte")
public class HugeRteView extends EcoView {

    static final String ORIGINAL = "<p>Original</p>";

    public HugeRteView() {
        super("hugerte-for-flow");
        HugeRte editor = withId(new HugeRte(), "editor");
        editor.setValue(ORIGINAL);
        var value = output("value");
        editor.addValueChangeListener(e -> value.setText(e.getValue()));
        add(editor, value);
        add(button("reset", "Reset", () -> editor.setValue(ORIGINAL)));
    }
}
