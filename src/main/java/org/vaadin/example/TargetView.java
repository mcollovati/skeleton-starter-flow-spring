package org.vaadin.example;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The navigation target of {@link AppErrorView}. When the navigation request
 * fails, the browser never shows this view.
 */
@Route("app-error/target")
public class TargetView extends VerticalLayout {

    public TargetView() {
        Paragraph text = new Paragraph("Target view");
        text.setId("target");
        add(text);
    }
}
