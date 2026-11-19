package com.acme.addon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Input;

/**
 * Case 1: constant {@code Element.executeJs(String)} in {@code onAttach}.
 * <p>
 * The most common add-on pattern. Under strict CSP the call throws while the
 * component attaches, so the whole view that holds it fails to open.
 */
public class AutoFocus extends Input {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().executeJs("setTimeout(() => this.focus(), 0)");
    }
}
