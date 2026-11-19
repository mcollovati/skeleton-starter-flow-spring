package com.acme.addon;

import com.vaadin.flow.component.html.Div;

/**
 * Case 8: {@code Element.addJsInitializer(String, Object...)}, which runs each
 * time the browser creates the element.
 */
public class ResizeWatcher extends Div {

    public ResizeWatcher(String label) {
        getStyle().set("resize", "horizontal").set("overflow", "hidden")
                .set("border", "1px dashed gray").set("width", "200px");
        getElement().addJsInitializer("""
                const ro = new ResizeObserver(() => {
                  this.textContent = $0 + ': ' + this.offsetWidth + 'px';
                });
                ro.observe(this);
                return () => ro.disconnect();
                """, label);
    }
}
