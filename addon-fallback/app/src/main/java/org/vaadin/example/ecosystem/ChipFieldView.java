package org.vaadin.example.ecosystem;

import java.util.List;

import com.flowingcode.vaadin.addons.chipfield.ChipField;
import com.vaadin.flow.router.Route;

/**
 * ChipFieldAddon (com.flowingcode.vaadin.addons.chipfield:chipfield-addon
 * 2.8.0). It sends its strings on attach and on setValue.
 */
@Route("eco/chipfield")
public class ChipFieldView extends EcoView {

    public ChipFieldView() {
        super("ChipFieldAddon");
        ChipField<String> chips = withId(
                new ChipField<>("Languages", "Java", "Kotlin", "Scala"),
                "chips");
        add(chips);
        run(() -> chips.setValue(List.of("Java", "Kotlin")));
    }
}
