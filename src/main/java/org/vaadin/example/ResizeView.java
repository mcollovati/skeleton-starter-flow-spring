package org.vaadin.example;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Page.addBrowserWindowResizeListener now installs the window listener with
 * ElementJs.forwardWindowResize() instead of a string executeJs.
 */
@Route("resize")
@PermitAll
public class ResizeView extends VerticalLayout {

    public ResizeView() {
        Span size = new Span("not resized yet");
        size.setId("size");
        Span count = new Span("0");
        count.setId("count");
        int[] events = { 0 };
        add(new H3("Window resize listener"),
                new Paragraph("Resize the browser window. The size below "
                        + "must follow the window (viewport width x height)."),
                new Paragraph(new Span("Size: "), size),
                new Paragraph(new Span("Events: "), count),
                new RouterLink("Back", MainView.class));
        addAttachListener(attach -> attach.getUI().getPage()
                .addBrowserWindowResizeListener(e -> {
                    size.setText(e.getWidth() + "x" + e.getHeight());
                    count.setText(String.valueOf(++events[0]));
                }));
    }
}
