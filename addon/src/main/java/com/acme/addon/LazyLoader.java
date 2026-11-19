package com.acme.addon;

import com.vaadin.flow.component.UI;

/**
 * Case 10: {@code Page.addDynamicImport(String)}.
 */
public final class LazyLoader {

    private LazyLoader() {
    }

    public static void load(UI ui) {
        ui.getPage().addDynamicImport(
                "return new Promise(r => setTimeout(() => { window.acmeLoaded = true; r(); }, 10))");
    }
}
