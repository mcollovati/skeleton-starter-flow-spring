package org.vaadin.example;

import com.acme.addon.AutoFocus;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

/**
 * How a real application meets the problem: it just uses the add-on. Under
 * strict CSP this view does not open, because {@link AutoFocus} calls
 * {@code executeJs(String)} while it attaches.
 */
@Route("plain")
public class PlainView extends Div {

    public PlainView() {
        add(new Paragraph("If you see this text, the view opened."));
        add(new AutoFocus());
    }
}
